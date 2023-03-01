package org.opencds.cqf.cql.evaluator.engine.retrieve;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.cql.evaluator.engine.util.CodeUtil;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.BundleUtil;

public class BundleRetrieveProvider extends TerminologyAwareRetrieveProvider {

  private static final Logger logger = LoggerFactory.getLogger(BundleRetrieveProvider.class);

  private final CodeUtil codeUtil;
  private final IFhirPath fhirPath;

  private final Map<String, List<IBaseResource>> resourceMap = new HashMap<>();

  public BundleRetrieveProvider(final FhirContext fhirContext, final IBaseBundle iBaseBundle) {

    requireNonNull(fhirContext, "bundle can not be null.");
    requireNonNull(iBaseBundle, "bundle can not be null.");
    this.codeUtil = new CodeUtil(fhirContext);
    this.fhirPath = FhirPathCache.cachedForContext(fhirContext);

    var resources = BundleUtil.toListOfResources(fhirContext, iBaseBundle);
    for (var r : resources) {
      resourceMap.computeIfAbsent(r.fhirType(), k -> new ArrayList<>()).add(r);
    }
  }

  @Override
  public Iterable<Object> retrieve(final String context, final String contextPath,
      final Object contextValue, final String dataType, final String templateId,
      final String codePath, final Iterable<Code> codes, final String valueSet,
      final String datePath, final String dateLowPath, final String dateHighPath,
      final Interval dateRange) {

    return resourceMap.computeIfAbsent(dataType, k -> Collections.emptyList()).stream()
        .filter(filterByTemplateId(dataType, templateId))
        .filter(filterByContext(dataType, context, contextPath, contextValue))
        .filter(filterByTerminology(dataType, codePath, codes, valueSet))
        .collect(Collectors.<Object>toList());
  }

  private boolean anyCodeMatch(final Iterable<Code> left, final Iterable<Code> right) {
    if (left == null || right == null) {
      return false;
    }

    for (final Code code : left) {
      for (final Code otherCode : right) {
        if (code.getCode() != null && code.getCode().equals(otherCode.getCode())
            && code.getSystem() != null && code.getSystem().equals(otherCode.getSystem())) {
          return true;
        }
      }
    }

    return false;
  }

  public boolean anyCodeInValueSet(final Iterable<Code> codes, final String valueSet) {
    if (codes == null || valueSet == null) {
      return false;
    }

    if (this.terminologyProvider == null) {
      throw new IllegalStateException(String.format(
          "Unable to check code membership for in ValueSet %s. terminologyProvider is null.",
          valueSet));
    }

    final ValueSetInfo valueSetInfo = new ValueSetInfo().withId(valueSet);
    for (final Code code : codes) {
      if (this.terminologyProvider.in(code, valueSetInfo)) {
        return true;
      }
    }

    return false;
  }

  // Special case filtering to handle "codes" that are actually ids. This is a
  // workaround to handle filtering by Id.
  private boolean isPrimitiveMatch(final String dataType, final IPrimitiveType<?> code,
      final Iterable<Code> codes) {
    if (code == null || codes == null) {
      return false;
    }

    // This handles the case that the value is a reference such as
    // "Medication/med-id"
    final String primitiveString = code.getValueAsString().replace(dataType + "/", "");
    for (final Object c : codes) {
      if (c instanceof String) {
        final String s = (String) c;
        if (s.equals(primitiveString)) {
          return true;
        }
      }
    }

    return false;
  }

  private Predicate<? super IBaseResource> filterByTerminology(final String dataType,
      final String codePath, final Iterable<Code> codes, final String valueSet) {
    if (codes == null && valueSet == null) {
      return resource -> true;
    }

    if (codePath == null) {
      return resource -> true;
    }

    return (IBaseResource res) -> {
      final List<IBase> values = this.fhirPath.evaluate(res, codePath, IBase.class);

      if (values != null && values.size() == 1) {
        if (values.get(0) instanceof IPrimitiveType) {
          return isPrimitiveMatch(dataType, (IPrimitiveType<?>) values.get(0), codes);
        }

        if (values.get(0).fhirType().equals("CodeableConcept")) {
          String codeValueSet = getValueSetFromCode(values.get(0));
          if (codeValueSet != null) {
            // TODO: If the value sets are not equal by name, test whether they have the
            // same expansion...
            return valueSet != null && codeValueSet.equals(valueSet);
          }
        }
      }

      final List<Code> resourceCodes = this.codeUtil.getElmCodesFromObject(values);
      return anyCodeMatch(resourceCodes, codes) || anyCodeInValueSet(resourceCodes, valueSet);
    };
  }

  private Predicate<? super IBaseResource> filterByTemplateId(final String dataType,
      final String templateId) {
    if (templateId == null || templateId
        .startsWith(String.format("http://hl7.org/fhir/StructureDefinition/%s", dataType))) {
      logger.debug("No profile-specific template id specified. Returning unfiltered resources.");
      return resource -> true;
    }

    return (IBaseResource res) -> {
      if (res.getMeta() != null && res.getMeta().getProfile() != null) {
        for (IPrimitiveType<?> profile : res.getMeta().getProfile()) {
          if (profile.hasValue() && profile.getValueAsString().equals(templateId)) {
            return true;
          }
        }
      }
      return false;
    };
  }

  // Super hackery, just to get this running for connectathon
  private String getValueSetFromCode(IBase base) {
    if (base instanceof org.hl7.fhir.r4.model.CodeableConcept) {
      org.hl7.fhir.r4.model.CodeableConcept cc = (org.hl7.fhir.r4.model.CodeableConcept) base;
      org.hl7.fhir.r4.model.Extension e = cc.getExtensionByUrl(
          "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDoneValueSet");
      if (e != null && e.hasValue()) {
        return e.getValueAsPrimitive().getValueAsString();
      }
    }
    return null;
  }

  private Predicate<? super IBaseResource> filterByContext(final String dataType,
      final String context, final String contextPath, final Object contextValue) {
    if (context == null || contextValue == null || contextPath == null) {
      logger.debug(
          "Unable to relate {} to {} context with contextPath: {} and contextValue: {}. Returning unfiltered resources.",
          dataType, context, contextPath, contextValue);
      return resource -> true;
    }

    return (IBaseResource res) -> {
      final Optional<IBase> resContextValue =
          this.fhirPath.evaluateFirst(res, contextPath, IBase.class);
      if (resContextValue.isPresent() && resContextValue.get() instanceof IIdType) {
        String id = ((IIdType) resContextValue.get()).getIdPart();

        if (id == null) {
          logger.debug("Found null id for {} resource. Skipping.", dataType);
          return false;
        }

        if (id.startsWith("urn:")) {
          logger.debug("Found {} with urn: prefix. Stripping.", dataType);
          id = stripUrnScheme(id);
        }
        if (!id.equals(contextValue)) {
          logger.debug("Found {} with id {}. Skipping.", dataType, id);
          return false;
        }
      } else if (resContextValue.isPresent() && resContextValue.get() instanceof IBaseReference) {
        String reference =
            ((IBaseReference) resContextValue.get()).getReferenceElement().getValue();
        if (reference == null) {
          logger.debug("Found null reference for {} resource. Skipping.", dataType);
          return false;
        }

        if (reference.startsWith("urn:")) {
          logger.debug("Found reference on {} resource with urn: prefix. Stripping.", dataType);
          reference = stripUrnScheme(reference);
        }

        if (reference.contains("/")) {
          reference = reference.split("/")[1];
        }

        if (!reference.equals(contextValue)) {
          logger.debug("Found {} with reference {}. Skipping.", dataType, reference);
          return false;
        }
      } else {
        final Optional<IBase> reference =
            this.fhirPath.evaluateFirst(res, "reference", IBase.class);
        if (!reference.isPresent()) {
          logger.debug("Found {} resource unrelated to context. Skipping.", dataType);
          return false;
        }

        String referenceString = ((IPrimitiveType<?>) reference.get()).getValueAsString();
        if (referenceString.startsWith("urn:")) {
          logger.debug("Found reference on {} resource with urn: prefix. Stripping.", dataType);
          referenceString = stripUrnScheme(referenceString);
        }

        if (referenceString.contains("/")) {
          referenceString =
              referenceString.substring(referenceString.indexOf("/") + 1, referenceString.length());
        }

        if (!referenceString.equals(contextValue)) {
          logger.debug("Found {} resource for context value: {} when expecting: {}. Skipping.",
              dataType, referenceString, contextValue);
          return false;
        }
      }

      return true;
    };
  }

  private String stripUrnScheme(String uri) {
    if (uri.startsWith("urn:uuid:")) {
      return uri.substring(9);
    } else if (uri.startsWith("urn:oid:")) {
      return uri.substring(8);
    } else {
      return uri;
    }
  }
}
