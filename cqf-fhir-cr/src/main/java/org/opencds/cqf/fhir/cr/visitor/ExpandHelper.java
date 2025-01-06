package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.ValueSets.addCodeToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.addParameterToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInExpansion;
import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.createAdapterForResource;

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
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Parameters;
import org.opencds.cqf.fhir.utility.ValueSets;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpandHelper {
    private static final Logger myLogger = LoggerFactory.getLogger(ExpandHelper.class);
    private final Repository repository;
    private final TerminologyServerClient terminologyServerClient;
    public static final List<String> unsupportedParametersToRemove =
            Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(Constants.CANONICAL_VERSION)));

    public ExpandHelper(Repository repository, TerminologyServerClient server) {
        this.repository = repository;
        terminologyServerClient = server;
    }

    private FhirContext fhirContext() {
        return repository.fhirContext();
    }

    private static void filterOutUnsupportedParameters(IParametersAdapter parameters) {
        var paramsToSet = parameters.getParameter();
        unsupportedParametersToRemove.forEach(parameterUrl -> {
            while (parameters.getParameter(parameterUrl) != null) {
                paramsToSet.remove(parameters.getParameter(parameterUrl));
                parameters.setParameter(paramsToSet);
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
                .map(url -> TerminologyServerClient.getAddressBase(url, fhirContext()))
                .orElse(null);
        // If terminologyEndpoint exists and we have no authoritativeSourceUrl or the authoritativeSourceUrl matches the
        // terminologyEndpoint address then we will use the terminologyEndpoint for expansion
        if (terminologyEndpoint.isPresent()
                && (authoritativeSourceUrl == null
                        || authoritativeSourceUrl.equals(
                                terminologyEndpoint.get().getAddress()))) {
            try {
                var expandedValueSet = (IValueSetAdapter) createAdapterForResource(
                        terminologyServerClient.expand(valueSet, terminologyEndpoint.get(), expansionParameters));
                // expansions are only valid for a particular version
                if (!valueSet.hasVersion()) {
                    valueSet.setVersion(expandedValueSet.getVersion());
                }
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
            groupExpand(
                    valueSet,
                    expansionParameters,
                    terminologyEndpoint,
                    valueSets,
                    expandedList,
                    repository,
                    expansionTimestamp);
        } else if (valueSet.hasCompose()) {
            throw new UnprocessableEntityException(
                    "Cannot expand ValueSet without a terminology server: " + valueSet.getId());
        }
        expandedList.add(valueSet.getUrl());
    }

    private void groupExpand(
            IValueSetAdapter valueSet,
            IParametersAdapter expansionParameters,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            List<String> expandedList,
            Repository repository,
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
            Repository repository,
            Date expansionTimestamp) {
        var expansion = valueSet.newExpansion();
        valueSet.getValueSetIncludes().forEach(reference -> {
            // Grab the ValueSet
            var url = Canonicals.getUrl(reference);
            var version = Canonicals.getVersion(reference);
            var includedVS =
                    getIncludedValueSet(valueSet, terminologyEndpoint, valueSets, repository, reference, url, version);
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
                            throw new UnprocessableEntityException(String.format(
                                    "Encountered exception attempting to expand ValueSet %s: %s",
                                    includedVS.get().getId(), ex.getMessage()));
                        }
                    }
                }));
    }

    private IValueSetAdapter getIncludedValueSet(
            IValueSetAdapter valueSet,
            Optional<IEndpointAdapter> terminologyEndpoint,
            List<IValueSetAdapter> valueSets,
            Repository repository,
            String reference,
            String url,
            String version) {
        return valueSets.stream()
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
                                .map(r -> (IValueSetAdapter) createAdapterForResource(r))
                                .orElse(null);
                    } else {
                        return VisitorHelper.tryGetLatestVersion(reference, repository)
                                .map(a -> (IValueSetAdapter) a)
                                .orElse(null);
                    }
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
        var childExpParams = (IParametersAdapter) createAdapterForResource(expansionParameters.copy());
        var urlParam = childExpParams.getParameter(TerminologyServerClient.urlParamName);
        if (urlParam != null) {
            var ind = childExpParams.getParameter().indexOf(urlParam);
            childExpParams.getParameter().remove(ind);
            if (includedVS.hasUrl()) {
                childExpParams.addParameter(
                        fhirContext().getVersion().getVersion() == FhirVersionEnum.DSTU3
                                ? Parameters.newUriPart(
                                        fhirContext(), TerminologyServerClient.urlParamName, includedVS.getUrl())
                                : Parameters.newUrlPart(
                                        fhirContext(), TerminologyServerClient.urlParamName, includedVS.getUrl()));
            }
        }
        var versionParam = childExpParams.getParameter(TerminologyServerClient.versionParamName);
        if (versionParam != null) {
            var ind = childExpParams.getParameter().indexOf(versionParam);
            childExpParams.getParameter().remove(ind);
            if (includedVS.hasVersion()) {
                childExpParams.addParameter(Parameters.newStringPart(
                        fhirContext(), TerminologyServerClient.versionParamName, includedVS.getVersion()));
            }
        }
        expandValueSet(includedVS, childExpParams, terminologyEndpoint, valueSets, expandedList, expansionTimestamp);
    }
}
