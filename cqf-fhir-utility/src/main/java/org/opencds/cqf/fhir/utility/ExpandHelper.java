package org.opencds.cqf.fhir.utility;

import static org.opencds.cqf.fhir.utility.ValueSets.addCodeToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.addParameterToExpansion;
import static org.opencds.cqf.fhir.utility.ValueSets.getCodesInExpansion;
import static org.opencds.cqf.fhir.utility.adapter.AdapterFactory.createAdapterForResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
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

    public ExpandHelper(FhirContext fhirContext, TerminologyServerClient server) {
        this.fhirContext = fhirContext;
        terminologyServerClient = server;
    }

    public void expandValueSet(
            ValueSetAdapter valueSet,
            ParametersAdapter expansionParameters,
            Optional<EndpointAdapter> terminologyEndpoint,
            List<ValueSetAdapter> valueSets,
            List<String> expandedList,
            Repository repository) {
        // Have we already expanded this ValueSet?
        if (expandedList.contains(valueSet.getUrl())) {
            // Nothing to do here
            return;
        }

        // Gather the Terminology Service from the valueSet's authoritativeSourceUrl.
        @SuppressWarnings("unchecked")
        var authoritativeSourceUrl = valueSet.getExtension().stream()
                .filter(e -> e.getUrl().equals(Constants.AUTHORITATIVE_SOURCE_URL))
                .findFirst()
                .map(url -> ((IPrimitiveType<String>) url.getValue()).getValueAsString())
                .orElse(null);

        // If terminologyEndpoint exists and we have no authoritativeSourceUrl or the authoritativeSourceUrl matches the
        // terminologyEndpoint address then we will use the terminologyEndpoint for expansion
        if (terminologyEndpoint.isPresent()
                && (authoritativeSourceUrl == null
                        || authoritativeSourceUrl.equals(
                                terminologyEndpoint.get().getAddress()))) {
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
                var vs = valueSets.stream()
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
                if (vs != null) {
                    // Expand the ValueSet if we haven't already
                    if (!expandedList.contains(url)) {
                        expandValueSet(
                                vs, expansionParameters, terminologyEndpoint, valueSets, expandedList, repository);
                    }
                    getCodesInExpansion(fhirContext, vs.get()).forEach(code -> {
                        // Add the code if not already present
                        var existingCodes = getCodesInExpansion(fhirContext, expansion);
                        if (existingCodes == null
                                || existingCodes.stream()
                                        .noneMatch(expandedCode -> code.getSystem()
                                                        .equals(expandedCode.getSystem())
                                                && code.getCode().equals(expandedCode.getCode())
                                                && (StringUtils.isEmpty(code.getVersion())
                                                        || code.getVersion().equals(expandedCode.getVersion())))) {
                            try {
                                addCodeToExpansion(fhirContext, expansion, code);
                            } catch (Exception ex) {
                                throw new UnprocessableEntityException(String.format(
                                        "Encountered exception attempting to expand ValueSet %s: %s",
                                        vs.get().getId(), ex.getMessage()));
                            }
                        }
                    });
                    // If any included expansion is naive it makes the expansion naive
                    if (vs.hasNaiveParameter() && !valueSet.hasNaiveParameter()) {
                        addParameterToExpansion(fhirContext, expansion, valueSet.createNaiveParameter());
                    }
                } else {
                    // Log that expansion failed due to missing leafs
                }
            });
            valueSet.setExpansion(expansion);
        } else {
            throw new UnprocessableEntityException(
                    "Cannot expand ValueSet without a terminology server: " + valueSet.getId());
        }
        expandedList.add(valueSet.getUrl());
    }
}
