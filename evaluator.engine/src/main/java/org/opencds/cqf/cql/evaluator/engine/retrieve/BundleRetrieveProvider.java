package org.opencds.cqf.cql.evaluator.engine.retrieve;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

	private final IBaseBundle bundle;
	private final FhirContext fhirContext;
	private final CodeUtil codeUtil;
	private final IFhirPath fhirPath;

	public BundleRetrieveProvider(final FhirContext fhirContext, final IBaseBundle iBaseBundle) {
		
		this.fhirContext = requireNonNull(fhirContext, "bundle can not be null.");
		this.bundle = requireNonNull(iBaseBundle, "bundle can not be null.");
		this.codeUtil = new CodeUtil(fhirContext);
		this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
	}

	@Override
	public Iterable<Object> retrieve(final String context, final String contextPath, final Object contextValue, final String dataType,
			final String templateId, final String codePath, final Iterable<Code> codes, final String valueSet, final String datePath,
			final String dateLowPath, final String dateHighPath, final Interval dateRange) {

		List<? extends IBaseResource> resources = BundleUtil.toListOfResourcesOfType(
				this.fhirContext, this.bundle,
				this.fhirContext.getResourceDefinition(dataType).getImplementingClass());

		resources = this.filterByContext(dataType, context, contextPath, contextValue, resources);
		resources = this.filterByTerminology(dataType, codePath, codes, valueSet, resources);

		return resources.stream().map(x -> (Object) x).collect(Collectors.toList());
	}

	private boolean anyCodeMatch(final Iterable<Code> left, final Iterable<Code> right) {
		if (left == null || right == null) {
			return false;
		}

		for (final Code code : left) {
			for (final Code otherCode : right) {
				if (code.getCode() != null && code.getCode().equals(otherCode.getCode()) && code.getSystem() != null && code.getSystem().equals(otherCode.getSystem())) {
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
					"Unable to check code membership for in ValueSet %s. terminologyProvider is null.", valueSet));
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
	private boolean isPrimitiveMatch(final String dataType, final IPrimitiveType<?> code, final Iterable<Code> codes) {
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

	private List<? extends IBaseResource> filterByTerminology(final String dataType, final String codePath, final Iterable<Code> codes,
			final String valueSet, final List<? extends IBaseResource> resources) {
		if (codes == null && valueSet == null) {
			return resources;
		}

		if (codePath == null) {
			return resources;
		}

		final List<IBaseResource> filtered = new ArrayList<>();

		for (final IBaseResource res : resources) {
			final List<IBase> values = this.fhirPath.evaluate(res, codePath, IBase.class);

			if (values != null && values.size() == 1 && values.get(0) instanceof IPrimitiveType) {
				if (isPrimitiveMatch(dataType, (IPrimitiveType<?>) values.get(0), codes)) {
					filtered.add(res);
				}
				continue;
			}

			final List<Code> resourceCodes = this.codeUtil.getElmCodesFromObject(values);
			if (resourceCodes == null) {
				continue;
			}

			if (anyCodeMatch(resourceCodes, codes)) {
				filtered.add(res);
				continue;
			}

			if (anyCodeInValueSet(resourceCodes, valueSet)) {
				filtered.add(res);
				continue;
			}
		}

		return filtered;
	}

	private List<? extends IBaseResource> filterByContext(final String dataType, final String context, final String contextPath,
			final Object contextValue, final List<? extends IBaseResource> resources) {
		if (context == null || contextValue == null || contextPath == null) {
			logger.debug(
					"Unable to relate {} to {} context with contextPath: {} and contextValue: {}. Returning unfiltered resources.",
					dataType, context, contextPath, contextValue);
			return resources;
		}

		final List<IBaseResource> filtered = new ArrayList<>();

		for (final IBaseResource res : resources) {
			final Optional<IBase> resContextValue = this.fhirPath.evaluateFirst(res, contextPath, IBase.class);
			if (resContextValue.isPresent() && resContextValue.get() instanceof IIdType) {
				String id = ((IIdType)resContextValue.get()).getIdPart();

				if (id == null) {
					logger.debug("Found null id for {} resource. Skipping.", dataType);
					continue;
				}

				if (id.startsWith("urn:")) {
					logger.debug("Found {} with urn: prefix. Stripping.", dataType);
					id = stripUrnScheme(id);
				}
				if (!id.equals(contextValue)) {
					logger.debug("Found {} with id {}. Skipping.", dataType, id);
					continue;
				}
			}
			else if (resContextValue.isPresent() && resContextValue.get() instanceof IBaseReference) {
					String reference = ((IBaseReference)resContextValue.get()).getReferenceElement().getValue();
					if (reference == null) {
						logger.debug("Found null reference for {} resource. Skipping.", dataType);
						continue;
					}

					if (reference.startsWith("urn:")) {
						logger.debug("Found reference with urn: prefix. Stripping.", dataType);
						reference = stripUrnScheme(reference);
					}
	
					if (reference.contains("/")) {
						reference = reference.split("/")[1];
					}
					
					if (!reference.equals(contextValue)) {
						logger.debug("Found {} with reference {}. Skipping.", dataType, reference);
						continue;
					}
				}
			else {
				final Optional<IBase> reference = this.fhirPath.evaluateFirst(res, "reference", IBase.class);
				if (!reference.isPresent()) {
					logger.debug("Found {} resource unrelated to context. Skipping.", dataType);
					continue;
				}

				String referenceString = ((IPrimitiveType<?>)reference.get()).getValueAsString();
				if (referenceString.startsWith("urn:")) {
					logger.debug("Found reference with urn: prefix. Stripping.", dataType);
					referenceString = stripUrnScheme(referenceString);
				}

				if (referenceString.contains("/")) {
					referenceString = referenceString.substring(referenceString.indexOf("/") + 1,
							referenceString.length());
				}

				if (!referenceString.equals((String) contextValue)) {
					logger.debug("Found {} resource for context value: {} when expecting: {}. Skipping.", dataType,
							referenceString, (String) contextValue);
					continue;
				}
			}

			filtered.add(res);
		}

		return filtered;
	}

    private String stripUrnScheme(String uri) {
        if (uri.startsWith("urn:uuid:")) {
            return uri.substring(9);
        }
        else if (uri.startsWith("urn:oid:")) {
            return uri.substring(8);
        }
        else {
            return uri;
        }
    }
}