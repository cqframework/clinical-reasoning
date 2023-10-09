package org.opencds.cqf.fhir.cql.engine.retrieve;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
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
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.fhir.cql.engine.utility.CodeExtractor;
import org.opencds.cqf.fhir.utility.FhirPathCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRetrieveProvider implements RetrieveProvider {
    private static final Logger logger = LoggerFactory.getLogger(BaseRetrieveProvider.class);
    private final CodeExtractor codeUtil;
    private final IFhirPath fhirPath;
    private final RetrieveSettings retrieveSettings;
    private final TerminologyProvider terminologyProvider;
    private final SearchParameterResolver resolver;

    public enum TERMINOLOGY_MODE {
        INLINE, // Use code:in=valueset where available
        EXPAND, // Use code=system|code where possible (and fetch the expansion yourself)
        AUTO // Use best available option
    }

    public enum FILTER_MODE {
        REPOSITORY, // Offload all search parameters
        INTERNAL, // Don't offload any search parameters, filter them all client side (this also impacts terminology
        // filtering)
        AUTO // Offload parameters you can, manually filter the ones you can't
    }

    // TODO: Profiles are completely unsupported for now. This just lays out some potential options for doing that
    //
    public enum PROFILE_MODE {
        // Always check the resource profile by validating the returned resource against the profile
        // This requires access to the structure defs that define the profile at runtime
        // Meaning, they need to be loaded on the server or otherwise. If they are unavailable, it's an automatic
        // failure.
        ENFORCED,
        // Same as above, but don't error if you don't have access to the profiles at runtime
        OPTIONAL,
        // Check that the resources declare the profile they conform too (generally considered a bad practice)
        DECLARED,
        // Let the underlying repository validate profiles (IOW, offload validation)
        TRUST,
        // Don't check resource profile, even if specified by the engine
        OFF
    }

    protected BaseRetrieveProvider(
            final FhirContext fhirContext,
            final TerminologyProvider terminologyProvider,
            final RetrieveSettings retrieveSettings) {
        requireNonNull(fhirContext, "fhirContext can not be null.");
        this.retrieveSettings = requireNonNull(retrieveSettings, "retrieveSettings can not be null");
        this.terminologyProvider = requireNonNull(terminologyProvider, "terminologyProvider can not be null");
        this.codeUtil = new CodeExtractor(fhirContext);
        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
        this.resolver = new SearchParameterResolver(fhirContext);
    }

    public Predicate<IBaseResource> filterByTemplateId(final String dataType, final String templateId) {

        if (this.getRetrieveSettings().getProfileMode() == PROFILE_MODE.OFF) {
            return resource -> true;
        }

        if (templateId == null
                || templateId.startsWith(String.format("http://hl7.org/fhir/StructureDefinition/%s", dataType))) {
            logger.debug("No profile-specific template id specified. Returning unfiltered resources.");
            return resource -> true;
        }

        // TODO: If profile mode is TRUST, this works. But for ENFORCED we should use the validator
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
    private String getValueSetFromCode(
            IBase base) { // what valuesets is it a part of, but just picking one, why not done association Chris
        IBaseExtension<?, ?> ext = ExtensionUtil.getExtensionByUrl(
                base, "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDoneValueSet");
        if (ext != null && ext.getValue() != null && ext.getValue() instanceof IPrimitiveType) {
            return ((IPrimitiveType<?>) ext.getValue()).getValueAsString();
        }
        return null;
    }

    public void populateTemplateSearchParams(
            Map<String, List<IQueryParameterType>> searchParams, final String templateId) {

        // TODO: If profile mode is optional, trust, or enforced AND the repository supports the _profile
        // parameter, we should add it.
        if (this.getRetrieveSettings().getProfileMode() != PROFILE_MODE.OFF && StringUtils.isNotBlank(templateId)) {
            searchParams.put("_profile", Collections.singletonList(new ReferenceParam(templateId)));
        }
    }

    public void populateContextSearchParams(
            Map<String, List<IQueryParameterType>> searchParams,
            final String dataType,
            final String contextPath,
            final String context,
            final Object contextValue) {
        if (contextPath == null || contextPath.isEmpty() || contextValue == null) {
            return;
        }

        IdDt ref = StringUtils.isNotBlank(context)
                ? new IdDt((String) contextValue).withResourceType(context)
                : new IdDt((String) contextValue);
        var sp = this.resolver.getSearchParameterDefinition(dataType, contextPath);

        // Self-references are token params.
        if (sp.getName().equals("_id")) {
            searchParams.put(sp.getName(), Collections.singletonList(new TokenParam((String) contextValue)));
        } else {
            searchParams.put(sp.getName(), Collections.singletonList(new ReferenceParam(ref)));
        }
    }

    public void populateTerminologySearchParams(
            Map<String, List<IQueryParameterType>> searchParams,
            final String dataType,
            final String codePath,
            final Iterable<Code> codes,
            final String valueSet) {
        if (codePath == null || codePath.isEmpty()) {
            return;
        }

        var sp = this.resolver.getSearchParameterDefinition(dataType, codePath, RestSearchParameterTypeEnum.TOKEN);
        if (codes != null) {
            List<IQueryParameterType> codeList = new ArrayList<>();
            // TODO: Fix up the RetrieveProvider API in the engine
            // This is stupid hacky
            for (Object code : codes) {
                if (code instanceof Code) {
                    var c = (Code) code;
                    codeList.add(new TokenParam(
                            new InternalCodingDt().setSystem(c.getSystem()).setCode(c.getCode())));
                } else if (code != null) {
                    codeList.add(new TokenParam(code.toString()));
                }
            }
            searchParams.put(sp.getName(), codeList);
        } else if (valueSet != null) {
            boolean shouldInline = shouldInline(valueSet);
            if (shouldInline) {
                searchParams.put(
                        sp.getName(),
                        Collections.singletonList(new TokenParam()
                                .setModifier(TokenParamModifier.IN)
                                .setValue(valueSet)));
            } else {
                List<IQueryParameterType> codeList = new ArrayList<>();
                for (Code code : this.terminologyProvider.expand(new ValueSetInfo().withId(valueSet))) {
                    codeList.add(new TokenParam(
                            new InternalCodingDt().setSystem(code.getSystem()).setCode(code.getCode())));
                }
                searchParams.put(sp.getName(), codeList);
            }
        }
    }

    protected boolean shouldInline(String valueSet) {
        return this.retrieveSettings.getTerminologyMode() == TERMINOLOGY_MODE.INLINE
                || (this.retrieveSettings.getTerminologyMode() == TERMINOLOGY_MODE.AUTO && inlineSupported(valueSet));
    }

    protected boolean inlineSupported(String valueSet) {
        // TODO: Check valueSet in the capability statement.
        return true;
    }

    public void populateDateSearchParams(
            Map<String, List<IQueryParameterType>> searchParams,
            final String dataType,
            final String datePath,
            final String dateLowPath,
            final String dateHighPath,
            final Interval dateRange) {
        if (datePath == null && dateHighPath == null && dateRange == null) {
            return;
        }

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
            var sp = this.resolver.getSearchParameterDefinition(dataType, datePath);
            searchParams.put(sp.getName(), dateRangeParam);
        } else if (StringUtils.isNotBlank(dateLowPath)) {
            List<IQueryParameterType> dateRangeParam = new ArrayList<>();
            DateParam dateParam = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start);
            dateRangeParam.add(dateParam);
            var sp = this.resolver.getSearchParameterDefinition(dataType, dateLowPath);
            searchParams.put(sp.getName(), dateRangeParam);
        } else if (StringUtils.isNotBlank(dateHighPath)) {
            List<IQueryParameterType> dateRangeParam = new ArrayList<>();
            DateParam dateParam = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end);
            dateRangeParam.add(dateParam);
            var sp = this.resolver.getSearchParameterDefinition(dataType, dateHighPath);
            searchParams.put(sp.getName(), dateRangeParam);
        } else {
            throw new IllegalStateException("A date path must be provided when filtering using date parameters");
        }
    }

    protected CodeExtractor getCodeUtil() {
        return codeUtil;
    }

    protected IFhirPath getFhirPath() {
        return fhirPath;
    }

    protected TerminologyProvider getTerminologyProvider() {
        return this.terminologyProvider;
    }

    protected RetrieveSettings getRetrieveSettings() {
        return this.retrieveSettings;
    }
}
