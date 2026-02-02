package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.ValueSets.addCodeToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.addParameterToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInExpansion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.ParametersUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.ValueSets;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.ExpandRunner.TerminologyServerExpansionException;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyServerClient;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyProviderRouter;
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

    public void expandValueSet(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
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
        // If terminologyEndpoint exists, and we have no authoritativeSourceUrl or the authoritativeSourceUrl matches the
        // terminologyEndpoint address then we will use the terminologyEndpoint for expansion
        if (terminologyEndpoint.isPresent()
                && (authoritativeSourceUrl == null
                        || authoritativeSourceUrl.equals(
                                terminologyEndpoint.get().getAddress()))) {
            try {
                terminologyServerExpand(valueSet, expansionParameters, terminologyEndpoint.get());
                return;
            } catch (TerminologyServerExpansionException e) {
                log.warn(
                        "Failed to expand value set {}. Reason: {}. Will attempt to expand locally.",
                        valueSet.getUrl(),
                        e.getMessage());
            }
        }
        // Else if the ValueSet has a simple compose then we will perform naive expansion.
        if (valueSet.hasSimpleCompose()) {
            valueSet.naiveExpand();
        }
        // Else if the ValueSet has a grouping compose then we will attempt to group.
        else if (valueSet.hasGroupingCompose()) {
            groupExpand(
                    valueSet,
                    expansionParameters,
                    terminologyEndpoint,
                    valueSets,
                    expandedList,
                    repository,
                    expansionTimestamp);
        } else if (valueSet.hasCompose()) {
            try {
                var headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                var vs = repository.invoke(
                        valueSet.get().getClass(),
                        "$expand",
                        (IBaseParameters) expansionParameters.get(),
                        valueSet.get().getClass(),
                        headers);
                valueSet = (IValueSetAdapter) IAdapterFactory.createAdapterForResource(vs);
                // Validate that the expansion parameters reflect what we asked for
                validateExpansionParameters(valueSet, expansionParameters);
            } catch (Exception e) {
                throw new UnprocessableEntityException(
                        "Cannot expand ValueSet without a terminology server: " + valueSet.getId());
            }
        }
        expandedList.add(valueSet.getUrl());
    }

    private void terminologyServerExpand(
            IValueSetAdapter valueSet, IParametersAdapter expansionParameters, IEndpointAdapter terminologyEndpoint) {
        var expandedValueSet = (IValueSetAdapter) adapterFactory.createResource(
                terminologyServerRouter.expand(valueSet, terminologyEndpoint, expansionParameters));
        // expansions are only valid for a particular version
        if (!valueSet.hasVersion()) {
            valueSet.setVersion(expandedValueSet.getVersion());
        }
        valueSet.setExpansion(expandedValueSet.getExpansion());
        // Validate that the expansion parameters reflect what we asked for
        validateExpansionParameters(valueSet, expansionParameters);
    }

    private void groupExpand(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<String> expandedList,
            IRepository repository,
            Date expansionTimestamp) {
        var expansion = expandIncludes(
                valueSet,
                expansionParameters,
                terminologyEndpoint,
                valueSets,
                expandedList,
                repository,
                expansionTimestamp);
        try {
            ValueSets.setExpansionTimestamp(
                    fhirContext(), expansion, expansionTimestamp == null ? new Date() : expansionTimestamp);
        } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            throw new UnprocessableEntityException(e.getMessage());
        }
        valueSet.setExpansion(expansion);
    }

    private IBaseBackboneElement expandIncludes(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
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
        expandValueSet(includedVS, childExpParams, terminologyEndpoint, valueSets, expandedList, expansionTimestamp);
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
