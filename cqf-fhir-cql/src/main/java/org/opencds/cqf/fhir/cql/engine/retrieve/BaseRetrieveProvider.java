package org.opencds.cqf.fhir.cql.engine.retrieve;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
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
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.util.ExtensionUtil;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.PROFILE_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.utility.CodeExtractor;
import org.opencds.cqf.fhir.utility.FhirPathCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRetrieveProvider implements RetrieveProvider {
    private static final Logger logger = LoggerFactory.getLogger(BaseRetrieveProvider.class);
    private final FhirVersionEnum fhirVersion;
    private final CodeExtractor codeUtil;
    private final IFhirPath fhirPath;
    private final RetrieveSettings retrieveSettings;
    private final TerminologyProvider terminologyProvider;
    private final SearchParameterResolver resolver;

    protected BaseRetrieveProvider(
            final FhirContext fhirContext,
            final TerminologyProvider terminologyProvider,
            final RetrieveSettings retrieveSettings) {
        requireNonNull(fhirContext, "fhirContext can not be null.");
        fhirVersion = fhirContext.getVersion().getVersion();
        this.retrieveSettings = requireNonNull(retrieveSettings, "retrieveSettings can not be null");
        this.terminologyProvider = requireNonNull(terminologyProvider, "terminologyProvider can not be null");
        this.codeUtil = new CodeExtractor(fhirContext);
        this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
        this.resolver = new SearchParameterResolver(fhirContext);
    }

    public Predicate<IBaseResource> filterByTemplateId(final String dataType, final String templateId) {
        var profileMode = this.getRetrieveSettings().getProfileMode();
        if (profileMode == PROFILE_MODE.OFF) {
            return resource -> true;
        }

        if (templateId == null
                || templateId.startsWith("http://hl7.org/fhir/StructureDefinition/%s".formatted(dataType))) {
            logger.debug("No profile-specific template id specified. Returning unfiltered resources.");
            return resource -> true;
        }

        if (profileMode == PROFILE_MODE.DECLARED || profileMode == PROFILE_MODE.OPTIONAL) {
            return (IBaseResource res) -> {

                // DECLARED == require a declared profile to be there, and use it.
                // OPTIONAL == use the profile if it's there, but don't require it
                if (res.getMeta() == null
                        || res.getMeta().getProfile() == null
                        || res.getMeta().getProfile().isEmpty()) {
                    return profileMode == PROFILE_MODE.OPTIONAL;
                }

                for (IPrimitiveType<?> profile : res.getMeta().getProfile()) {
                    if (profile.hasValue() && profile.getValueAsString().equals(templateId)) {
                        return true;
                    }
                }

                return false;
            };
        }

        // Should never see TRUST, since that should be handled by the repository.
        // ENFORCED is not yet supported.

        throw new UnsupportedOperationException("%s profile mode is not yet supported.".formatted(profileMode));
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
                if (reference.isEmpty()) {
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
            if (c instanceof String s && s.equals(primitiveString)) {
                return true;
            }
        }

        return false;
    }

    // Super hackery, just to get this running for connectathon
    private String getValueSetFromCode(
            IBase base) { // what valuesets is it a part of, but just picking one, why not done
        // association Chris
        IBaseExtension<?, ?> ext = ExtensionUtil.getExtensionByUrl(
                base, "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDoneValueSet");
        if (ext != null && ext.getValue() != null && ext.getValue() instanceof IPrimitiveType) {
            return ((IPrimitiveType<?>) ext.getValue()).getValueAsString();
        }
        return null;
    }

    public void populateTemplateSearchParams(
            Multimap<String, List<IQueryParameterType>> searchParams, final String dataType, final String templateId) {
        if (getRetrieveSettings().getProfileMode() != PROFILE_MODE.OFF
                && StringUtils.isNotBlank(templateId)
                && !templateId.startsWith("http://hl7.org/fhir/StructureDefinition/%s".formatted(dataType))) {
            var profileParam = getFhirVersion().isOlderThan(FhirVersionEnum.R5)
                    ? new UriParam(templateId)
                    : new ReferenceParam(templateId);
            searchParams.put("_profile", makeMutableSingleElementList(profileParam));
        }
    }

    public void populateContextSearchParams(
            Multimap<String, List<IQueryParameterType>> searchParams,
            final String dataType,
            final String context,
            final String contextPath,
            final Object contextValue) {
        if (contextPath == null || contextPath.isEmpty() || contextValue == null) {
            return;
        }

        IdDt ref = StringUtils.isNotBlank(context)
                ? new IdDt((String) contextValue).withResourceType(context)
                : new IdDt((String) contextValue);
        var sp = this.resolver.getSearchParameterDefinition(dataType, contextPath);

        if (sp == null) {
            throw new InternalErrorException("resolved search parameter definition is null");
        }
        // Self-references are token params.
        if (sp.getName().equals("_id")) {
            searchParams.put(sp.getName(), makeMutableSingleElementList(new TokenParam((String) contextValue)));
        } else {
            searchParams.put(sp.getName(), makeMutableSingleElementList(new ReferenceParam(ref)));
        }
    }

    public void populateTerminologySearchParams(
            Multimap<String, List<IQueryParameterType>> searchParams,
            final String dataType,
            final String codePath,
            final Iterable<Code> codes,
            final String valueSet) {
        if (codePath == null || codePath.isEmpty()) {
            return;
        }

        var sp = this.resolver.getSearchParameterDefinition(dataType, codePath, RestSearchParameterTypeEnum.TOKEN);
        if (sp == null) {
            throw new InternalErrorException("resolved search parameter definition is null");
        }
        if (codes != null) {
            List<IQueryParameterType> codeList = new ArrayList<>();
            // TODO: Fix up the RetrieveProvider API in the engine
            // This is stupid hacky
            for (Object code : codes) {
                if (code instanceof Code c) {
                    codeList.add(new TokenParam(
                            new InternalCodingDt().setSystem(c.getSystem()).setCode(c.getCode())));
                } else if (code != null) {
                    codeList.add(new TokenParam(code.toString()));
                }
            }
            searchParams.put(sp.getName(), codeList);
        } else if (valueSet != null) {
            boolean shouldUseInCodeModifier = shouldUseInCodeModifier(valueSet, dataType, sp.getName());
            if (shouldUseInCodeModifier) {
                // Use the in modifier e.g. Observation?code:in=valueSetUrl
                searchParams.put(
                        sp.getName(),
                        makeMutableSingleElementList(new TokenParam()
                                .setModifier(TokenParamModifier.IN)
                                .setValue(valueSet)));
            } else {
                // Inline the codes into the retrieve e.g.
                // Observation?code=system|code,system|code
                List<IQueryParameterType> codeList = new ArrayList<>();
                for (Code code : this.terminologyProvider.expand(new ValueSetInfo().withId(valueSet))) {
                    codeList.add(new TokenParam(
                            new InternalCodingDt().setSystem(code.getSystem()).setCode(code.getCode())));
                }
                searchParams.put(sp.getName(), codeList);
            }
        }
    }

    protected boolean shouldUseInCodeModifier(String valueSet, String resourceName, String searchParamName) {
        return this.retrieveSettings.getTerminologyParameterMode() == TERMINOLOGY_FILTER_MODE.USE_VALUE_SET_URL
                || (this.retrieveSettings.getTerminologyParameterMode() == TERMINOLOGY_FILTER_MODE.AUTO
                        && inModifierSupported(valueSet, resourceName, searchParamName));
    }

    protected boolean inModifierSupported(String valueSet, String resourceName, String searchParamName) {
        // TODO: Check valueSet in the capability statement,
        // also check that the selected search parameter supports that modifier.
        return true;
    }

    public void populateDateSearchParams(
            Multimap<String, List<IQueryParameterType>> searchParams,
            final String dataType,
            final String dateParamName,
            final String dateLowPath,
            final String dateHighPath,
            final Interval dateRange) {
        if (dateParamName == null && dateHighPath == null && dateRange == null) {
            return;
        }

        if (dateRange == null) {
            throw new IllegalStateException("A date range must be provided when filtering using date parameters");
        }

        Date start;
        Date end;
        if (dateRange.getStart() instanceof DateTime) {
            start = ((DateTime) dateRange.getStart()).toJavaDate();
            var dateRangeEnd = dateRange.getEnd();
            if (dateRangeEnd == null) {
                throw new InternalErrorException("resolved search parameter definition is null");
            }
            end = ((DateTime) dateRange.getEnd()).toJavaDate();
        } else if (dateRange.getStart() instanceof org.opencds.cqf.cql.engine.runtime.Date) {
            start = ((org.opencds.cqf.cql.engine.runtime.Date) dateRange.getStart()).toJavaDate();
            var dateRangeEnd = dateRange.getEnd();
            if (dateRangeEnd == null) {
                throw new InternalErrorException("resolved search parameter definition is null");
            }
            end = ((org.opencds.cqf.cql.engine.runtime.Date) dateRangeEnd).toJavaDate();
        } else {
            throw new UnsupportedOperationException(
                    "Expected Interval of type org.opencds.cqf.cql.engine.runtime.Date or org.opencds.cqf.cql.engine.runtime.DateTime, found "
                            + Optional.ofNullable(dateRange.getStart())
                                    .map(innerStart -> innerStart.getClass().getSimpleName())
                                    .orElse(null));
        }

        if (StringUtils.isNotBlank(dateParamName)) {
            var sp = this.resolver.getSearchParameterDefinition(dataType, dateParamName);

            if (sp == null) {
                throw new InternalErrorException("resolved search parameter definition is null");
            }

            // a date range is a search AND condition — each put() on the Multimap
            // adds a separate AND clause (one for >= start, one for <= end)
            DateParam gte = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start);
            DateParam lte = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end);

            searchParams.put(sp.getName(), makeMutableSingleElementList(gte));
            searchParams.put(sp.getName(), makeMutableSingleElementList(lte));
        } else if (StringUtils.isNotBlank(dateLowPath)) {
            List<IQueryParameterType> dateRangeParam = new ArrayList<>();
            DateParam dateParam = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, start);
            dateRangeParam.add(dateParam);
            var sp = this.resolver.getSearchParameterDefinition(dataType, dateLowPath);

            if (sp == null) {
                throw new InternalErrorException("resolved search parameter definition is null");
            }

            searchParams.put(sp.getName(), dateRangeParam);
        } else if (StringUtils.isNotBlank(dateHighPath)) {
            List<IQueryParameterType> dateRangeParam = new ArrayList<>();
            DateParam dateParam = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, end);
            dateRangeParam.add(dateParam);
            var sp = this.resolver.getSearchParameterDefinition(dataType, dateHighPath);

            if (sp == null) {
                throw new InternalErrorException("resolved search parameter definition is null");
            }

            searchParams.put(sp.getName(), dateRangeParam);
        } else {
            throw new IllegalStateException("A date path must be provided when filtering using date parameters");
        }
    }

    protected FhirVersionEnum getFhirVersion() {
        return fhirVersion;
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

    private static <T> ArrayList<T> makeMutableSingleElementList(T singleElement) {
        return new ArrayList<>(List.of(singleElement));
    }
}
