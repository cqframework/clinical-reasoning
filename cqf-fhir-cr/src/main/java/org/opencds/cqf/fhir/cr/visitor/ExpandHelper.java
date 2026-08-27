package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.ValueSets.addCodeToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.addParameterToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInExpansion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.ParametersUtil;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.ValueSets;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptSetAdapter;
import org.opencds.cqf.fhir.utility.client.ExpandRunner.TerminologyServerExpansionException;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings.TxResourceMode;
import org.opencds.cqf.fhir.utility.client.terminology.ArtifactEndpointConfiguration;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyProviderRouter;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandHelper {

    private static final Logger log = LoggerFactory.getLogger(ExpandHelper.class);
    private final IRepository repository;
    private final IAdapterFactory adapterFactory;
    private final ITerminologyProviderRouter terminologyServerRouter;
    public static final List<String> unsupportedParametersToRemove = List.of(Constants.CANONICAL_VERSION);

    // Parameters we care to validate round-trip in the expansion
    private static final List<String> EXPANSION_PARAMETERS_TO_VALIDATE = List.of("system-version", "valueset-version");

    // If the server uses "used-*" variants, they should be treated as satisfying the request
    private static final Map<String, String> EXPANSION_PARAMETER_USED_NAME_OVERRIDES = Map.of(
            "system-version", "used-system-version",
            "valueset-version", "used-valueset-version");

    public ExpandHelper(IRepository repository, ITerminologyProviderRouter server) {
        this.repository = repository;
        adapterFactory = IAdapterFactory.forFhirContext(this.repository.fhirContext());
        terminologyServerRouter = server;
    }

    private FhirContext fhirContext() {
        return repository.fhirContext();
    }

    private static void filterOutUnsupportedParameters(IParametersAdapter parameters) {
        var paramsToSet = parameters.getParameter();
        unsupportedParametersToRemove.forEach(parameterUrl -> {
            while (parameters.hasParameter(parameterUrl)) {
                parameters.setParameter(paramsToSet.stream()
                        .filter(p -> !p.getName().equals(parameterUrl))
                        .map(IParametersParameterComponentAdapter::get)
                        .toList());
            }
        });
    }

    private boolean canDoLocalExpansion(IValueSetAdapter valueSet) {
        if (valueSet == null || !valueSet.hasCompose()) {
            return false;
        }
        // Hard exclusions first (these force a fallback to the terminology server)
        if (valueSet.hasComposeExclude() || valueSet.hasComposeFilters()) {
            return false;
        }
        // Is there anything defined that we can actually expand
        return valueSet.hasExplicitConcepts() || valueSet.hasValueSetReferences();
    }

    public void expandValueSet(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<String> expandedList,
            Date expansionTimestamp) {
        expandValueSet(
                valueSet,
                expansionParameters,
                terminologyEndpoint,
                valueSets,
                List.of(),
                expandedList,
                expansionTimestamp);
    }

    /**
     * Same as {@link #expandValueSet(IValueSetAdapter, IParametersAdapter, Optional, List, List, Date)}, but
     * additionally accepts the package-authored CodeSystems ({@code packageCodeSystems}) that make up the
     * package closure. When a remote terminology server is used to expand, the package-authored ValueSets and
     * CodeSystems referenced by the target ValueSet are supplied to the {@code $expand} call via the
     * {@code tx-resource} parameter so version-pinned artifacts the server lacks can be resolved from the
     * supplied resources.
     */
    public void expandValueSet(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<IBaseResource> packageCodeSystems,
            List<String> expandedList,
            Date expansionTimestamp) {
        // Have we already expanded this ValueSet?
        if (expandedList.contains(valueSet.getUrl())) {
            // Nothing to do here
            return;
        }
        filterOutUnsupportedParameters(expansionParameters);
        // Gather the Terminology Service from the valueSet's authoritativeSourceUrl.
        @SuppressWarnings("unchecked")
        var authoritativeSourceUrl = valueSet.getExtension().stream()
                .filter(e -> e.getUrl().equals(Constants.AUTHORITATIVE_SOURCE_URL))
                .findFirst()
                .map(url -> ((IPrimitiveType<String>) url.getValue()).getValueAsString())
                .map(url -> ITerminologyServerClient.getAddressBase(url, fhirContext()))
                .orElse(null);
        // Normalize the endpoint address the same way as the authoritativeSourceUrl so the comparison
        // is not defeated by http/https, trailing-slash, or resource-path differences.
        var endpointAddressBase = terminologyEndpoint
                .map(IEndpointAdapter::getAddress)
                .filter(address -> !address.isBlank())
                .map(address -> ITerminologyServerClient.getAddressBase(address, fhirContext()))
                .orElse(null);
        // The endpoint is the authoritative source for this ValueSet when the ValueSet declares no
        // authoritative source or it matches the endpoint. In that case expand against it directly;
        // otherwise prefer local composition (e.g. a grouper authored on a different server) and only
        // fall back to the endpoint below if local expansion turns out not to be possible.
        var endpointIsAuthoritativeSource = terminologyEndpoint.isPresent()
                && (authoritativeSourceUrl == null || authoritativeSourceUrl.equals(endpointAddressBase));
        if (endpointIsAuthoritativeSource) {
            try {
                terminologyServerExpand(
                        valueSet, expansionParameters, terminologyEndpoint.get(), valueSets, packageCodeSystems);
                return;
            } catch (TerminologyServerExpansionException e) {
                log.warn(
                        "Failed to expand value set {}. Reason: {}. Will attempt to expand locally.",
                        valueSet.getUrl(),
                        e.getMessage());
            }
        }

        // Hybrid local expansion for ValueSets with explicit concepts and/or valueSet
        // references
        if (canDoLocalExpansion(valueSet) && (valueSet.hasExplicitConcepts() || valueSet.hasValueSetReferences())) {
            var expansion = valueSet.newExpansion();
            boolean isNaive = false;

            // Expand referenced ValueSets first
            if (valueSet.hasValueSetReferences()) {
                var includeExpansion = expandIncludes(
                        valueSet,
                        expansionParameters,
                        terminologyEndpoint,
                        valueSets,
                        packageCodeSystems,
                        expandedList,
                        repository,
                        expansionTimestamp);

                var copyAdapter = (IValueSetAdapter) adapterFactory.createResource(valueSet.copy());
                copyAdapter.setExpansion(includeExpansion);

                addCodesToExpansion(expansion, copyAdapter);

                if (copyAdapter.hasNaiveParameter()) {
                    isNaive = true;
                }
            }

            // Expand explicit concepts
            if (valueSet.hasExplicitConcepts()) {
                var explicitAdapter = (IValueSetAdapter) adapterFactory.createResource(valueSet.copy());
                explicitAdapter.naiveExpand();

                addCodesToExpansion(expansion, explicitAdapter);
                isNaive = true;
            }

            // Apply naive parameter if any naive expansions occurred
            if (isNaive) {
                addParameterToExpansion(fhirContext(), expansion, valueSet.createNaiveParameter());
            }

            // Set timestamp
            try {
                ValueSets.setExpansionTimestamp(
                        fhirContext(), expansion, expansionTimestamp == null ? new Date() : expansionTimestamp);
            } catch (Exception e) {
                throw new UnprocessableEntityException(e.getMessage());
            }

            valueSet.setExpansion(expansion);
            expandedList.add(valueSet.getUrl());
            return;
        }

        // Fallback for ValueSets that could not be expanded locally.
        if (valueSet.hasCompose()) {
            // A terminologyEndpoint was supplied but not used above because the ValueSet's authoritative
            // source differs from it. Since local expansion was not possible, use the configured endpoint
            // rather than failing — a general terminology server can expand ValueSets authored elsewhere.
            if (terminologyEndpoint.isPresent() && !endpointIsAuthoritativeSource) {
                try {
                    terminologyServerExpand(
                            valueSet, expansionParameters, terminologyEndpoint.get(), valueSets, packageCodeSystems);
                    // The endpoint used is not this ValueSet's authoritative source, so surface a
                    // warning that the expansion was completed non-authoritatively.
                    addExpansionWarningParameter(
                            valueSet,
                            "Expansion for ValueSet %s was completed by the configured terminology endpoint, which is not its authoritative source; the expansion may be non-authoritative."
                                    .formatted(valueSet.getUrl()));
                    expandedList.add(valueSet.getUrl());
                    return;
                } catch (TerminologyServerExpansionException e) {
                    log.warn(
                            "Failed to expand value set {} via the configured terminology endpoint. Reason: {}.",
                            valueSet.getUrl(),
                            e.getMessage());
                }
            }
            try {
                var headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");

                var vs = repository.invoke(
                        valueSet.get().getClass(),
                        "$expand",
                        (IBaseParameters) expansionParameters.get(),
                        valueSet.get().getClass(),
                        headers);

                var expandedValueSet = (IValueSetAdapter) adapterFactory.createResource(vs);

                // expansions are only valid for a particular version
                if (!valueSet.hasVersion()) {
                    valueSet.setVersion(expandedValueSet.getVersion());
                }
                valueSet.setExpansion(expandedValueSet.getExpansion());
                // Validate that the expansion parameters reflect what we asked for
                validateExpansionParameters(valueSet, expansionParameters);
            } catch (Exception e) {
                throw new UnprocessableEntityException(
                        "Cannot expand ValueSet without terminology server: " + valueSet.getUrl());
            }
        }

        expandedList.add(valueSet.getUrl());
    }

    /**
     * Expands a ValueSet using CRMI artifact endpoint configurations for routing.
     * Falls back to legacy terminologyEndpoint if no configurations match, then to local expansion.
     *
     * @param valueSet the ValueSet to expand
     * @param expansionParameters expansion parameters
     * @param artifactEndpointConfigurations CRMI endpoint configurations for routing
     * @param terminologyEndpoint legacy single endpoint (used as fallback)
     * @param valueSets list of all ValueSets being processed
     * @param expandedList list of already expanded ValueSet URLs
     * @param expansionTimestamp timestamp for expansion
     */
    public void expandValueSet(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            List<ArtifactEndpointConfiguration> artifactEndpointConfigurations,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<String> expandedList,
            Date expansionTimestamp) {
        expandValueSet(
                valueSet,
                expansionParameters,
                artifactEndpointConfigurations,
                terminologyEndpoint,
                valueSets,
                List.of(),
                expandedList,
                expansionTimestamp);
    }

    /**
     * Same as
     * {@link #expandValueSet(IValueSetAdapter, IParametersAdapter, List, Optional, List, List, Date)}, but
     * additionally accepts the package-authored CodeSystems ({@code packageCodeSystems}) that make up the
     * package closure so they can be supplied to a remote {@code $expand} via {@code tx-resource}.
     */
    public void expandValueSet(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            List<ArtifactEndpointConfiguration> artifactEndpointConfigurations,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<IBaseResource> packageCodeSystems,
            List<String> expandedList,
            Date expansionTimestamp) {
        // Have we already expanded this ValueSet?
        if (expandedList.contains(valueSet.getUrl())) {
            return;
        }
        filterOutUnsupportedParameters(expansionParameters);

        // Try CRMI configuration-based routing first if configurations are provided
        if (artifactEndpointConfigurations != null && !artifactEndpointConfigurations.isEmpty()) {
            try {
                var expandedResult = terminologyServerRouter.expandWithConfigurations(
                        valueSet, artifactEndpointConfigurations, expansionParameters);
                if (expandedResult != null) {
                    var expandedValueSet = (IValueSetAdapter) adapterFactory.createResource(expandedResult);
                    if (!valueSet.hasVersion()) {
                        valueSet.setVersion(expandedValueSet.getVersion());
                    }
                    valueSet.setExpansion(expandedValueSet.getExpansion());
                    validateExpansionParameters(valueSet, expansionParameters);
                    expandedList.add(valueSet.getUrl());
                    return;
                }
            } catch (TerminologyServerExpansionException e) {
                log.warn(
                        "Failed to expand value set {} using artifact endpoint configurations. Reason: {}. "
                                + "Will attempt fallback expansion.",
                        valueSet.getUrl(),
                        e.getMessage());
            }
        }

        // Fall back to legacy single endpoint or local expansion
        expandValueSet(
                valueSet,
                expansionParameters,
                terminologyEndpoint,
                valueSets,
                packageCodeSystems,
                expandedList,
                expansionTimestamp);
    }

    private void terminologyServerExpand(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            IEndpointAdapter terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<IBaseResource> packageCodeSystems) {
        // Supply the target ValueSet and the package's own resources it references to the $expand call
        // via the tx-resource parameter so version-pinned artifacts the server lacks can be resolved
        // from them. Attach to a per-expansion COPY of the parameters: the caller's parameters object is
        // reused across every ValueSet in a $package run, so mutating it here would cause tx-resource
        // params to accumulate across expansions.
        var expandParameters = (IParametersAdapter) adapterFactory.createResource(expansionParameters.copy());
        attachTxResourceParameters(valueSet, expandParameters, terminologyEndpoint, valueSets, packageCodeSystems);
        var expandedValueSet = (IValueSetAdapter) adapterFactory.createResource(
                terminologyServerRouter.expand(valueSet, terminologyEndpoint, expandParameters));
        // expansions are only valid for a particular version
        if (!valueSet.hasVersion()) {
            valueSet.setVersion(expandedValueSet.getVersion());
        }
        valueSet.setExpansion(expandedValueSet.getExpansion());
        // Validate that the expansion parameters reflect what we asked for
        validateExpansionParameters(valueSet, expansionParameters);
    }

    /**
     * Attaches {@code tx-resource} parameters to {@code expansionParameters} so they are supplied to the
     * remote {@code $expand} call. Attaches the target ValueSet itself (the server may not hold the
     * requested version) plus the package-authored ValueSets and CodeSystems it references: referenced
     * ValueSet canonicals ({@code include[].valueSet}) matched against {@code valueSets}, and referenced
     * CodeSystem systems ({@code include[].system}) matched against {@code packageCodeSystems}. Matches are
     * de-duplicated by {@code url|version}. When the configured {@link TxResourceMode} is {@code DISABLED}
     * nothing is attached.
     */
    private void attachTxResourceParameters(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            IEndpointAdapter terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<IBaseResource> packageCodeSystems) {
        var settings = terminologyServerRouter.getTerminologyServerClientSettings(terminologyEndpoint);
        var mode = settings != null ? settings.getTxResourceMode() : TxResourceMode.AUTO;
        if (mode == TxResourceMode.DISABLED) {
            return;
        }

        // De-duplicate attached resources by url|version, preserving insertion order.
        var toAttach = new LinkedHashMap<String, IBaseResource>();

        // Supply the target ValueSet itself. The reported failure mode is that the terminology server
        // does not hold the requested ValueSet version; because $expand is invoked by url reference, the
        // server cannot resolve a version it lacks. Supplied as a tx-resource, it is "used preferentially
        // to those known to the system" (per the $expand OperationDefinition).
        toAttach.put(urlVersionKey(valueSet.getUrl(), valueSet.getVersion()), valueSet.get());

        boolean noValueSets = valueSets == null || valueSets.isEmpty();
        boolean noCodeSystems = packageCodeSystems == null || packageCodeSystems.isEmpty();

        if (!noValueSets || !noCodeSystems) {
            // Referenced ValueSet canonicals from the target's compose (url|version strings)
            var referencedValueSets = valueSet.getValueSetIncludes();
            // Referenced CodeSystem systems from the target's compose includes
            var referencedSystems = valueSet.getComposeInclude().stream()
                    .filter(IValueSetConceptSetAdapter::hasSystem)
                    .map(IValueSetConceptSetAdapter::getSystem)
                    .collect(Collectors.toSet());

            if (!noValueSets) {
                for (var packageVs : valueSets) {
                    for (var reference : referencedValueSets) {
                        var refUrl = Canonicals.getUrl(reference);
                        var refVersion = Canonicals.getVersion(reference);
                        if (packageVs.getUrl() != null
                                && packageVs.getUrl().equals(refUrl)
                                && (refVersion == null || refVersion.equals(packageVs.getVersion()))) {
                            toAttach.putIfAbsent(
                                    urlVersionKey(packageVs.getUrl(), packageVs.getVersion()), packageVs.get());
                        }
                    }
                }
            }

            if (!noCodeSystems) {
                for (var codeSystem : packageCodeSystems) {
                    IKnowledgeArtifactAdapter csAdapter =
                            adapterFactory.createKnowledgeArtifactAdapter((IDomainResource) codeSystem);
                    if (csAdapter.getUrl() != null && referencedSystems.contains(csAdapter.getUrl())) {
                        toAttach.putIfAbsent(urlVersionKey(csAdapter.getUrl(), csAdapter.getVersion()), codeSystem);
                    }
                }
            }
        }

        toAttach.values().forEach(resource -> expansionParameters.addParameter(Constants.TX_RESOURCE, resource));

        log.info(
                "Attached {} tx-resource(s) (mode={}) for $expand of ValueSet {}: [{}]",
                toAttach.size(),
                mode,
                valueSet.getUrl(),
                String.join(", ", toAttach.keySet()));
    }

    private static String urlVersionKey(String url, String version) {
        return (url == null ? "" : url) + "|" + (version == null ? "" : version);
    }

    private IBaseBackboneElement expandIncludes(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<IBaseResource> packageCodeSystems,
            List<String> expandedList,
            IRepository repository,
            Date expansionTimestamp) {
        var expansion = valueSet.newExpansion();
        valueSet.getValueSetIncludes().forEach(reference -> {
            // Grab the ValueSet
            var url = Canonicals.getUrl(reference);
            var version = Canonicals.getVersion(reference);
            var includedVS = getIncludedValueSet(terminologyEndpoint, valueSets, repository, reference, url, version);
            if (includedVS != null) {
                // Expand the ValueSet if we haven't already
                if (!expandedList.contains(url)) {
                    expandIncluded(
                            expansionParameters,
                            terminologyEndpoint,
                            valueSets,
                            packageCodeSystems,
                            expandedList,
                            expansionTimestamp,
                            includedVS);
                }
                addCodesToExpansion(expansion, includedVS);
                // If any included expansion is naive it makes the expansion naive
                if (includedVS.hasNaiveParameter() && !valueSet.hasNaiveParameter()) {
                    addParameterToExpansion(fhirContext(), expansion, valueSet.createNaiveParameter());
                }
            } else {
                throw new UnprocessableEntityException("Terminology Server expansion failed for ValueSet '"
                        + valueSet.getUrl() + "' because Child ValueSet '" + reference + "' could not be found. ");
            }
        });
        return expansion;
    }

    private void addCodesToExpansion(IBaseBackboneElement expansion, IValueSetAdapter includedVS) {
        Optional.ofNullable(getCodesInExpansion(fhirContext(), includedVS.get()))
                .ifPresent(e -> e.forEach(code -> {
                    // Add the code if not already present
                    var existingCodes = getCodesInExpansion(fhirContext(), expansion);
                    if (existingCodes == null
                            || existingCodes.stream()
                                    .noneMatch(expandedCode -> code.getSystem().equals(expandedCode.getSystem())
                                            && code.getCode().equals(expandedCode.getCode())
                                            && (StringUtils.isEmpty(code.getVersion())
                                                    || code.getVersion().equals(expandedCode.getVersion())))) {
                        try {
                            addCodeToExpansion(fhirContext(), expansion, code);
                        } catch (Exception ex) {
                            throw new UnprocessableEntityException(
                                    "Encountered exception attempting to expand ValueSet %s: %s"
                                            .formatted(includedVS.get().getId(), ex.getMessage()));
                        }
                    }
                }));
    }

    private IValueSetAdapter getIncludedValueSet(
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            IRepository repository,
            String reference,
            String url,
            String version) {
        return valueSets.stream()
                .filter(v -> v.getUrl().equals(url)
                        && (version == null || v.getVersion().equals(version)))
                .findFirst()
                .orElseGet(() -> {
                    if (terminologyEndpoint.isPresent()) {
                        try {
                            return terminologyServerRouter
                                    .getValueSetResource(terminologyEndpoint.get(), reference)
                                    .map(r -> (IValueSetAdapter) adapterFactory.createResource(r))
                                    .orElse(null);
                        } catch (Exception ex) {
                            log.warn(
                                    "Failed to retrieve ValueSet resource for ValueSet '{}', will attempt to retrieve locally",
                                    reference,
                                    ex);
                        }
                    }
                    return (IValueSetAdapter) VisitorHelper.tryGetLatestVersion(reference, repository)
                            .orElse(null);
                });
    }

    private void expandIncluded(
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<IBaseResource> packageCodeSystems,
            List<String> expandedList,
            Date expansionTimestamp,
            IValueSetAdapter includedVS) {
        // update url and version exp params for child expansions
        var childExpParams = (IParametersAdapter) adapterFactory.createResource(expansionParameters.copy());
        if (childExpParams.hasParameter(ITerminologyServerClient.urlParamName)) {
            var newParams = childExpParams.getParameter().stream()
                    .filter(p -> !p.getName().equals(ITerminologyServerClient.urlParamName))
                    .collect(Collectors.toList());
            if (includedVS.hasUrl()) {
                newParams.add(adapterFactory.createParametersParameter((IBaseBackboneElement)
                        (fhirContext().getVersion().getVersion() == FhirVersionEnum.DSTU3
                                ? Parameters.newUriPart(
                                        fhirContext(), ITerminologyServerClient.urlParamName, includedVS.getUrl())
                                : Parameters.newUrlPart(
                                        fhirContext(), ITerminologyServerClient.urlParamName, includedVS.getUrl()))));
            }
            childExpParams.setParameter(newParams.stream()
                    .map(IParametersParameterComponentAdapter::get)
                    .toList());
        }
        if (childExpParams.hasParameter(ITerminologyServerClient.versionParamName)) {
            var newParams = childExpParams.getParameter().stream()
                    .filter(p -> !p.getName().equals(ITerminologyServerClient.versionParamName))
                    .collect(Collectors.toList());
            if (includedVS.hasVersion()) {
                newParams.add(adapterFactory.createParametersParameter((IBaseBackboneElement) Parameters.newStringPart(
                        fhirContext(), ITerminologyServerClient.versionParamName, includedVS.getVersion())));
            }
            childExpParams.setParameter(newParams.stream()
                    .map(IParametersParameterComponentAdapter::get)
                    .toList());
        }
        expandValueSet(
                includedVS,
                childExpParams,
                terminologyEndpoint,
                valueSets,
                packageCodeSystems,
                expandedList,
                expansionTimestamp);
    }

    /**
     * Validates that the expansion parameters used by the terminology expansion match the parameters
     * that were requested for the expansion.
     * <p>
     * Currently this validation focuses on the {@code system-version} and {@code valueset-version}
     * parameters. For each of these parameters that is present in the {@code requestedExpansionParameters}
     * with a non-blank value, this method verifies that the resulting {@link IValueSetAdapter} expansion
     * contains either:
     * <ul>
     *   <li>a parameter with the same name and the same primitive value, or</li>
     *   <li>a parameter with the corresponding {@code used-} name (for example,
     *       {@code system-version} → {@code used-system-version}) and the same primitive value.</li>
     * </ul>
     * If there is no expansion at all, or if any requested parameter is missing or has a mismatched value
     * in the expansion, a single {@code warning} expansion parameter is added via
     * {@link #addExpansionWarningParameter(IValueSetAdapter, String)} indicating that the expected
     * expansion parameters were not used.
     *
     * @param expandedValueSet            the {@link IValueSetAdapter} containing the expansion to validate;
     *                                    may be {@code null}, in which case this method is a no-op
     * @param requestedExpansionParameters the {@link IParametersAdapter} representing the parameters that
     *                                     were originally sent to the terminology service; may be {@code null},
     *                                     in which case this method is a no-op
     */
    private void validateExpansionParameters(
            IValueSetAdapter expandedValueSet, IParametersAdapter requestedExpansionParameters) {

        if (expandedValueSet == null || requestedExpansionParameters == null) {
            return;
        }

        // 1. No expansion at all -> immediate warning
        if (!expandedValueSet.hasExpansion()) {
            addExpansionWarningParameter(
                    expandedValueSet,
                    "Expansion for ValueSet %s did not use expected expansion parameters (no expansion element)."
                            .formatted(expandedValueSet.getUrl()));
            return;
        }

        // 2. Build map of requested parameters we care about (system-version, valueset-version) -> expected string
        // value
        Map<String, String> requestedValues = new HashMap<>();
        for (String paramName : EXPANSION_PARAMETERS_TO_VALIDATE) {
            if (!requestedExpansionParameters.hasParameter(paramName)) {
                continue;
            }

            var valueOpt = ParametersUtil.getNamedParameterValueAsString(
                    fhirContext(), (IBaseParameters) requestedExpansionParameters.get(), paramName);

            valueOpt.filter(StringUtils::isNotBlank).ifPresent(v -> requestedValues.put(paramName, v));
        }

        if (requestedValues.isEmpty()) {
            // We didn’t actually request any of the parameters we care about with usable values
            return;
        }

        // 3. For each requested parameter, see if the expansion has either:
        //    - the same name with that value, or
        //    - the used-* variant with that value.
        var missingOrMismatchedParams = requestedValues.entrySet().stream()
                .filter(entry -> {
                    var requestedName = entry.getKey();
                    var expectedValue = entry.getValue();

                    var usedName = EXPANSION_PARAMETER_USED_NAME_OVERRIDES.get(requestedName);

                    boolean hasRequested = expandedValueSet.hasExpansionStringParameter(requestedName, expectedValue);
                    boolean hasUsed =
                            usedName != null && expandedValueSet.hasExpansionStringParameter(usedName, expectedValue);

                    // Neither name has the expected value -> treat as missing/mismatched
                    return !(hasRequested || hasUsed);
                })
                // Include both the parameter name and the expected value to make the warning more informative.
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .toList();

        // 4. If any requested parameter is missing or mismatched, add a single warning parameter that
        // includes the list of parameters that were not honored in the expansion.
        if (!missingOrMismatchedParams.isEmpty()) {
            var paramList = String.join(", ", missingOrMismatchedParams);
            addExpansionWarningParameter(
                    expandedValueSet,
                    "Expansion for ValueSet %s did not use expected expansion parameters: %s."
                            .formatted(expandedValueSet.getUrl(), paramList));
        }
    }

    /**
     * Adds a {@code warning} parameter to the supplied ValueSet expansion to surface issues
     * encountered during expansion parameter validation.
     * <p>
     * This method logs the supplied message at {@code WARN} level and, if an expansion is present
     * on the {@link IValueSetAdapter}, delegates to {@link IValueSetAdapter#addExpansionStringParameter(String, String)}
     * to append a {@code warning} expansion parameter whose value is the given message. If the
     * ValueSet does not currently have an expansion, the method logs but does not attempt to add
     * a parameter.
     *
     * @param valueSet the {@link IValueSetAdapter} whose expansion will be annotated; may be {@code null},
     *                 in which case this method is a no-op
     * @param message  the warning message to log and add as the value of the {@code warning} expansion parameter;
     *                 must not be {@code null}, but may be empty
     */
    private void addExpansionWarningParameter(IValueSetAdapter valueSet, String message) {
        log.warn(message);
        var expansion = valueSet.getExpansion();
        if (expansion == null) {
            return;
        }
        valueSet.addExpansionStringParameter("warning", message);
    }
}
