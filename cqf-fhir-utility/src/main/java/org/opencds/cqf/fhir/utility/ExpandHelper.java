package org.opencds.cqf.fhir.utility;

import static org.opencds.cqf.fhir.utility.ValueSets.addCodeToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.addParameterToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInExpansion;
import static org.opencds.cqf.fhir.utility.adapter.AdapterFactory.createAdapterForResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.EndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.ParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;
import org.opencds.cqf.fhir.utility.visitor.VisitorHelper;

public class ExpandHelper {

    private final FhirContext fhirContext;
    private final TerminologyServerClient terminologyServerClient;
    public static final List<String> unsupportedParametersToRemove =
            Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(Constants.CANONICAL_VERSION)));

    public ExpandHelper(FhirContext fhirContext, TerminologyServerClient server) {
        this.fhirContext = fhirContext;
        terminologyServerClient = server;
    }

    @SuppressWarnings("unchecked")
    private static void filterOutUnsupportedParameters(ParametersAdapter parameters) {
        var paramsToSet = parameters.getParameter();
        unsupportedParametersToRemove.forEach(parameterUrl -> {
            while (parameters.getParameter(parameterUrl) != null) {
                paramsToSet.remove(parameters.getParameter(parameterUrl));
                parameters.setParameter((List<IBaseBackboneElement>) paramsToSet);
            }
        });
    }

    public void expandValueSet(
            ValueSetAdapter valueSet,
            ParametersAdapter expansionParameters,
            Optional<EndpointAdapter> terminologyEndpoint,
            List<ValueSetAdapter> valueSets,
            List<String> expandedList,
            Repository repository,
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
                .map(url -> TerminologyServerClient.getAddressBase(url, fhirContext))
                .orElse(null);
        // If terminologyEndpoint exists and we have no authoritativeSourceUrl or the authoritativeSourceUrl matches the
        // terminologyEndpoint address then we will use the terminologyEndpoint for expansion
        if (terminologyEndpoint.isPresent()
                && authoritativeSourceUrl.equals(
                                terminologyEndpoint.get().getAddress())) {
            try {
                var expandedValueSet = (ValueSetAdapter) createAdapterForResource(
                        terminologyServerClient.expand(valueSet, terminologyEndpoint.get(), expansionParameters));
                valueSet.setExpansion(expandedValueSet.getExpansion());
            } catch (Exception ex) {
                throw new UnprocessableEntityException(String.format(
                        "Terminology Server expansion failed for ValueSet (%s): %s",
                        valueSet.getId(), ex.getMessage()));
            }
        }
        // Else if the ValueSet has a simple compose then we will perform naive expansion.
        else if (valueSet.hasSimpleCompose()) {
            valueSet.naiveExpand();
        }
        // Else if the ValueSet has a grouping compose then we will attempt to group.
        else if (valueSet.hasGroupingCompose()) {
            var expansion = valueSet.newExpansion();
            var includes = valueSet.getValueSetIncludes();
            includes.forEach(reference -> {
                // Grab the ValueSet
                var split = reference.split("\\|");
                var url = split.length == 1 ? reference : split[0];
                var version = split.length == 1 ? null : split[1];
                var includedVS = valueSets.stream()
                        .filter(v -> v.getUrl().equals(url)
                                && (version == null || v.getVersion().equals(version)))
                        .findFirst()
                        .orElseGet(() -> {
                            if (terminologyEndpoint.isPresent()) {
                                return terminologyServerClient
                                        .getResource(
                                                terminologyEndpoint.get(),
                                                reference,
                                                valueSet.get().getStructureFhirVersionEnum())
                                        .map(r -> (ValueSetAdapter) createAdapterForResource(r))
                                        .orElse(null);
                            } else {
                                return VisitorHelper.tryGetLatestVersion(reference, repository)
                                        .map(a -> (ValueSetAdapter) a)
                                        .orElse(null);
                            }
                        });
                if (includedVS != null) {
                    // Expand the ValueSet if we haven't already
                    if (!expandedList.contains(url)) {
                        // update url and version exp params for child expansions
                        var childExpParams = (ParametersAdapter) createAdapterForResource(expansionParameters.copy());
                        var urlParam = childExpParams.getParameter(TerminologyServerClient.urlParamName);
                        if (urlParam != null) {
                            var ind = childExpParams.getParameter().indexOf(urlParam);
                            childExpParams.getParameter().remove(ind);
                            if (includedVS.hasUrl()) {
                                childExpParams.addParameter(
                                        fhirContext.getVersion().getVersion() == FhirVersionEnum.DSTU3
                                                ? Parameters.newUriPart(
                                                        fhirContext,
                                                        TerminologyServerClient.urlParamName,
                                                        includedVS.getUrl())
                                                : Parameters.newUrlPart(
                                                        fhirContext,
                                                        TerminologyServerClient.urlParamName,
                                                        includedVS.getUrl()));
                            }
                        }
                        var versionParam = childExpParams.getParameter(TerminologyServerClient.versionParamName);
                        if (versionParam != null) {
                            var ind = childExpParams.getParameter().indexOf(versionParam);
                            childExpParams.getParameter().remove(ind);
                            if (includedVS.hasVersion()) {
                                childExpParams.addParameter(Parameters.newStringPart(
                                        fhirContext,
                                        TerminologyServerClient.versionParamName,
                                        includedVS.getVersion()));
                            }
                        }
                        expandValueSet(
                                includedVS,
                                childExpParams,
                                terminologyEndpoint,
                                valueSets,
                                expandedList,
                                repository,
                                expansionTimestamp);
                    }
                    Optional.ofNullable(getCodesInExpansion(fhirContext, includedVS.get()))
                            .ifPresent(e -> e.forEach(code -> {
                                // Add the code if not already present
                                var existingCodes = getCodesInExpansion(fhirContext, expansion);
                                if (existingCodes == null
                                        || existingCodes.stream()
                                                .noneMatch(expandedCode -> code.getSystem()
                                                                .equals(expandedCode.getSystem())
                                                        && code.getCode().equals(expandedCode.getCode())
                                                        && (StringUtils.isEmpty(code.getVersion())
                                                                || code.getVersion()
                                                                        .equals(expandedCode.getVersion())))) {
                                    try {
                                        addCodeToExpansion(fhirContext, expansion, code);
                                    } catch (Exception ex) {
                                        throw new UnprocessableEntityException(String.format(
                                                "Encountered exception attempting to expand ValueSet %s: %s",
                                                includedVS.get().getId(), ex.getMessage()));
                                    }
                                }
                            }));
                    // If any included expansion is naive it makes the expansion naive
                    if (includedVS.hasNaiveParameter() && !valueSet.hasNaiveParameter()) {
                        addParameterToExpansion(fhirContext, expansion, valueSet.createNaiveParameter());
                    }
                } else {
                    throw new UnprocessableEntityException("Terminology Server expansion failed for ValueSet '"
                            + valueSet.getUrl() + "' because Child ValueSet '" + reference + "' could not be found. ");
                }
            });
            try {
                ValueSets.setExpansionTimestamp(
                        fhirContext, expansion, expansionTimestamp == null ? new Date() : expansionTimestamp);
            } catch (InstantiationException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | SecurityException e) {
                throw new UnprocessableEntityException(e.getMessage());
            }
            valueSet.setExpansion(expansion);
            // ignore ValueSets without a compose
        } else if (valueSet.hasCompose()) {
            throw new UnprocessableEntityException(
                    "Cannot expand ValueSet without a terminology server: " + valueSet.getId());
        }
        expandedList.add(valueSet.getUrl());
    }
}
