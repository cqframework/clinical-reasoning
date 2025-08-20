package org.opencds.cqf.fhir.cr.visitor;

import static org.opencds.cqf.fhir.utility.ValueSets.addCodeToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.addParameterToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInExpansion;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
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
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;

public class ExpandHelper {
    private final IRepository repository;
    private final IAdapterFactory adapterFactory;
    private final TerminologyServerClient terminologyServerClient;
    public static final List<String> unsupportedParametersToRemove = List.of(Constants.CANONICAL_VERSION);

    public ExpandHelper(IRepository repository, TerminologyServerClient server) {
        this.repository = repository;
        adapterFactory = IAdapterFactory.forFhirContext(this.repository.fhirContext());
        terminologyServerClient = server;
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
                .map(url -> TerminologyServerClient.getAddressBase(url, fhirContext()))
                .orElse(null);
        // If terminologyEndpoint exists and we have no authoritativeSourceUrl or the authoritativeSourceUrl matches the
        // terminologyEndpoint address then we will use the terminologyEndpoint for expansion
        if (terminologyEndpoint.isPresent()
                && (authoritativeSourceUrl == null
                        || authoritativeSourceUrl.equals(
                                terminologyEndpoint.get().getAddress()))) {
            terminologyServerExpand(valueSet, expansionParameters, terminologyEndpoint.get());
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

    private void terminologyServerExpand(
            IValueSetAdapter valueSet, IParametersAdapter expansionParameters, IEndpointAdapter terminologyEndpoint) {
        var expandedValueSet = (IValueSetAdapter) adapterFactory.createResource(
                terminologyServerClient.expand(valueSet, terminologyEndpoint, expansionParameters));
        // expansions are only valid for a particular version
        if (!valueSet.hasVersion()) {
            valueSet.setVersion(expandedValueSet.getVersion());
        }
        valueSet.setExpansion(expandedValueSet.getExpansion());
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
                        return terminologyServerClient
                                .getValueSetResource(terminologyEndpoint.get(), reference)
                                .map(r -> (IValueSetAdapter) adapterFactory.createResource(r))
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
        var childExpParams = (IParametersAdapter) adapterFactory.createResource(expansionParameters.copy());
        if (childExpParams.hasParameter(TerminologyServerClient.urlParamName)) {
            var newParams = childExpParams.getParameter().stream()
                    .filter(p -> !p.getName().equals(TerminologyServerClient.urlParamName))
                    .collect(Collectors.toList());
            if (includedVS.hasUrl()) {
                newParams.add(adapterFactory.createParametersParameter((IBaseBackboneElement)
                        (fhirContext().getVersion().getVersion() == FhirVersionEnum.DSTU3
                                ? Parameters.newUriPart(
                                        fhirContext(), TerminologyServerClient.urlParamName, includedVS.getUrl())
                                : Parameters.newUrlPart(
                                        fhirContext(), TerminologyServerClient.urlParamName, includedVS.getUrl()))));
            }
            childExpParams.setParameter(newParams.stream()
                    .map(IParametersParameterComponentAdapter::get)
                    .toList());
        }
        if (childExpParams.hasParameter(TerminologyServerClient.versionParamName)) {
            var newParams = childExpParams.getParameter().stream()
                    .filter(p -> !p.getName().equals(TerminologyServerClient.versionParamName))
                    .collect(Collectors.toList());
            if (includedVS.hasVersion()) {
                newParams.add(adapterFactory.createParametersParameter((IBaseBackboneElement) Parameters.newStringPart(
                        fhirContext(), TerminologyServerClient.versionParamName, includedVS.getVersion())));
            }
            childExpParams.setParameter(newParams.stream()
                    .map(IParametersParameterComponentAdapter::get)
                    .toList());
        }
        expandValueSet(includedVS, childExpParams, terminologyEndpoint, valueSets, expandedList, expansionTimestamp);
    }
}
