package org.opencds.cqf.fhir.cql.engine.retrieve;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.InternalCodingDt;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.util.ExtensionUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.retrieve.TerminologyAwareRetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.fhir.cql.engine.utility.CodeExtractor;
import org.opencds.cqf.fhir.utility.FhirPathCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RetrieveProvider extends TerminologyAwareRetrieveProvider {
    private static final Logger logger = LoggerFactory.getLogger(RetrieveProvider.class);
    private final CodeExtractor codeUtil;
    private final IFhirPath fhirPath;
    private boolean filterBySearchParam = true;
    private boolean searchByTemplate = false;

    protected RetrieveProvider(final FhirContext fhirContext) {
        requireNonNull(fhirContext, "The FhirContext can not be null.");
        this.codeUtil = new CodeExtractor(fhirContext);
        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    }

    public Predicate<IBaseResource> filterByTemplateId(final String dataType, final String templateId) {
        if (templateId == null
                || templateId.startsWith(String.format("http://hl7.org/fhir/StructureDefinition/%s", dataType))) {
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

    public Predicate<IBaseResource> filterByContext(
            final String dataType, final String context, final String contextPath, final Object contextValue) {
        if (context == null || contextValue == null || contextPath == null) {
            logger.debug(
                    "Unable to relate {} to {} context with contextPath: {} and contextValue: {}. Returning unfiltered resources.",
                    dataType,
                    context,
                    contextPath,
                    contextValue);
            return resource -> true;
        }

        return (IBaseResource res) -> {
            final Optional<IBase> resContextValue = this.fhirPath.evaluateFirst(res, contextPath, IBase.class);
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
                String reference = ((IBaseReference) resContextValue.get())
                        .getReferenceElement()
                        .getValue();
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
                final Optional<IBase> reference = this.fhirPath.evaluateFirst(res, "reference", IBase.class);
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
                    referenceString = referenceString.substring(referenceString.indexOf("/") + 1);
                }

                if (!referenceString.equals(contextValue)) {
                    logger.debug(
                            "Found {} resource for context value: {} when expecting: {}. Skipping.",
                            dataType,
                            referenceString,
                            contextValue);
                    return false;
                }
            }

            return true;
        };
    }

    public Predicate<IBaseResource> filterByTerminology(
            final String dataType, final String codePath, final Iterable<Code> codes, final String valueSet) {
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
                        return codeValueSet.equals(valueSet);
                    }
                }
            }

            final List<Code> resourceCodes = this.codeUtil.getElmCodesFromObject(values);
            return anyCodeMatch(resourceCodes, codes) || anyCodeInValueSet(resourceCodes, valueSet);
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

    private boolean anyCodeMatch(final Iterable<Code> left, final Iterable<Code> right) {
        if (left == null || right == null) {
            return false;
        }

        for (final Code code : left) {
            for (final Code otherCode : right) {
                if (code.getCode() != null
                        && code.getCode().equals(otherCode.getCode())
                        && code.getSystem() != null
                        && code.getSystem().equals(otherCode.getSystem())) {
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

    // Super hackery, just to get this running for connectathon
    private String getValueSetFromCode(IBase base) {
        IBaseExtension<?, ?> ext = ExtensionUtil.getExtensionByUrl(
                base, "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDoneValueSet");
        if (ext != null && ext.getValue() != null && ext.getValue() instanceof IPrimitiveType) {
            return ((IPrimitiveType<?>) ext.getValue()).getValueAsString();
        }
        return null;
    }

    public void populateTemplateSearchParams(
            Map<String, List<IQueryParameterType>> searchParams, final String templateId) {
        if (Boolean.TRUE.equals(this.searchByTemplate) && StringUtils.isNotBlank(templateId)) {
            searchParams.put("_profile", Collections.singletonList(new ReferenceParam(templateId)));
        }
    }

    public void populateContextSearchParams(
            Map<String, List<IQueryParameterType>> searchParams,
            final String contextPath,
            final String context,
            final Object contextValue) {
        if (StringUtils.isNotBlank(contextPath) && contextValue != null) {
            IdDt ref = StringUtils.isNotBlank(context)
                    ? new IdDt((String) contextValue).withResourceType(context)
                    : new IdDt((String) contextValue);
            var path = contextPath.equals("id") ? "_id" : contextPath;
            searchParams.put(path, Collections.singletonList(new ReferenceParam(ref)));
        }
    }

    public void populateTerminologySearchParams(
            Map<String, List<IQueryParameterType>> searchParams,
            final String codePath,
            final Iterable<Code> codes,
            final String valueSet) {
        if (StringUtils.isNotBlank(codePath)) {
            if (codes != null) {
                List<IQueryParameterType> codeList = new ArrayList<>();
                for (Code code : codes) {
                    codeList.add(new TokenParam(
                            new InternalCodingDt().setSystem(code.getSystem()).setCode(code.getCode())));
                }
                searchParams.put(codePath, codeList);
            } else if (valueSet != null) {
                if (this.terminologyProvider != null && this.isExpandValueSets()) {
                    List<IQueryParameterType> codeList = new ArrayList<>();
                    for (Code code : this.terminologyProvider.expand(new ValueSetInfo().withId(valueSet))) {
                        codeList.add(new TokenParam(new InternalCodingDt()
                                .setSystem(code.getSystem())
                                .setCode(code.getCode())));
                    }
                    searchParams.put(codePath, codeList);
                } else {
                    searchParams.put(
                            codePath,
                            Collections.singletonList(new TokenParam()
                                    .setModifier(TokenParamModifier.IN)
                                    .setValue(valueSet)));
                }
            }
        }
    }

    public void populateDateSearchParams(
            Map<String, List<IQueryParameterType>> searchParams,
            final String datePath,
            final String dateLowPath,
            final String dateHighPath,
            final Interval dateRange) {
        if (!StringUtils.isAllBlank(datePath, dateLowPath, dateHighPath)) {
            if (dateRange == null) {
                throw new IllegalStateException("A date range must be provided when filtering using date parameters");
            }

            Date start;
            Date end;
            if (dateRange.getStart() instanceof DateTime) {
                start = ((DateTime) dateRange.getStart()).toJavaDate();
                end = ((DateTime) dateRange.getEnd()).toJavaDate();
            } else if (dateRange.getStart() instanceof org.opencds.cqf.cql.engine.runtime.Date) {
                start = ((org.opencds.cqf.cql.engine.runtime.Date) dateRange.getStart()).toJavaDate();
                end = ((org.opencds.cqf.cql.engine.runtime.Date) dateRange.getEnd()).toJavaDate();
            } else {
                throw new UnsupportedOperationException(
                        "Expected Interval of type org.opencds.cqf.cql.engine.runtime.Date or org.opencds.cqf.cql.engine.runtime.DateTime, found "
                                + dateRange.getStart().getClass().getSimpleName());
            }

            if (StringUtils.isNotBlank(datePath)) {
                List<IQueryParameterType> dateRangeParam = new ArrayList<>();
                DateParam dateParam = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start);
                dateRangeParam.add(dateParam);
                dateParam = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end);
                dateRangeParam.add(dateParam);
                searchParams.put(datePath, dateRangeParam);
            } else if (StringUtils.isNotBlank(dateLowPath)) {
                List<IQueryParameterType> dateRangeParam = new ArrayList<>();
                DateParam dateParam = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start);
                dateRangeParam.add(dateParam);
                searchParams.put(dateLowPath, dateRangeParam);
            } else if (StringUtils.isNotBlank(dateHighPath)) {
                List<IQueryParameterType> dateRangeParam = new ArrayList<>();
                DateParam dateParam = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end);
                dateRangeParam.add(dateParam);
                searchParams.put(dateHighPath, dateRangeParam);
            } else {
                throw new IllegalStateException("A date path must be provided when filtering using date parameters");
            }
        }
    }

    public CodeExtractor getCodeUtil() {
        return codeUtil;
    }

    public IFhirPath getFhirPath() {
        return fhirPath;
    }

    public boolean isFilterBySearchParam() {
        return filterBySearchParam;
    }

    public RetrieveProvider setFilterBySearchParam(boolean filterBySearchParam) {
        this.filterBySearchParam = filterBySearchParam;
        return this;
    }

    public RetrieveProvider setSearchByTemplate(boolean searchByTemplate) {
        this.searchByTemplate = searchByTemplate;
        return this;
    }
}
