package org.opencds.cqf.fhir.cr.inputparameters.dstu3;

import static org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.dstu3.Parameters.part;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.inputparameters.BaseInputParameterResolver;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the default parameters passed into an operation as CQL Resource parameters
 * for evaluation. e.g. "%subject"
 */
public class InputParameterResolver extends BaseInputParameterResolver {
    private static final Logger logger = LoggerFactory.getLogger(InputParameterResolver.class);

    private final Parameters parameters;

    public InputParameterResolver(
            Repository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data) {
        super(repository, subjectId, encounterId, practitionerId, parameters, useServerData, data);
        this.parameters = resolveParameters(parameters);
    }

    @Override
    protected final Parameters resolveParameters(IBaseParameters baseParameters) {
        var params = parameters();
        if (baseParameters != null) {
            params.getParameter().addAll(((Parameters) baseParameters).getParameter());
        }
        var subjectClass =
                fhirContext().getResourceDefinition(subjectId.getResourceType()).getImplementingClass();
        var subject = readRepository(subjectClass, subjectId);
        if (subject != null) {
            params.addParameter(part("%subject", (Resource) subject));
        }
        if (encounterId != null && !encounterId.isEmpty()) {
            var encounter = readRepository(Encounter.class, encounterId);
            if (encounter != null) {
                params.addParameter(part("%encounter", encounter));
            }
        }
        if (practitionerId != null && !practitionerId.isEmpty()) {
            var practitioner = readRepository(Practitioner.class, practitionerId);
            if (practitioner != null) {
                params.addParameter(part("%practitioner", practitioner));
            }
        }
        return params;
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public <T extends ICompositeType> IBaseParameters resolveInputParameters(List<T> dataRequirement) {
        var dataRequirements =
                dataRequirement.stream().map(i -> (DataRequirement) i).collect(Collectors.toList());
        var params = parameters();
        if (parameters != null) {
            params.getParameter().addAll(parameters.getParameter());
        }
        for (var req : dataRequirements) {
            if (!req.hasId()) {
                continue;
            }

            var parameter = part("%" + String.format("%s", req.getId()));
            if (req.hasCodeFilter()) {
                for (var filter : req.getCodeFilter()) {
                    if (filter != null && filter.hasPath() && filter.hasValueSet()) {
                        var valueSets = repository.search(
                                Bundle.class,
                                ValueSet.class,
                                Searches.byCanonical(filter.getValueSet().primitiveValue()));
                        if (valueSets.hasEntry()) {
                            var valueSet =
                                    (ValueSet) valueSets.getEntryFirstRep().getResource();
                            var codes = valueSet.hasExpansion()
                                    ? valueSet.getExpansion().getContains().stream()
                                            .map(c -> new Coding(c.getSystem(), c.getCode(), c.getDisplay()))
                                            .collect(Collectors.toList())
                                    : new ArrayList<Coding>();

                            var searchBuilder = Searches.builder();
                            codes.forEach(c -> searchBuilder.withTokenParam("code", c.getCode()));
                            var resourceType = fhirContext()
                                    .getResourceDefinition(req.getType())
                                    .getImplementingClass();
                            var searchResults =
                                    repository.search(Bundle.class, resourceType, searchBuilder.build(), null);
                            if (searchResults.hasEntry()) {
                                parameter.setResource(
                                        searchResults.getEntry().size() > 1
                                                ? searchResults
                                                : searchResults
                                                        .getEntry()
                                                        .get(0)
                                                        .getResource());
                            }
                        }
                        logger.debug("Could not find ValueSet with url {} on the local server.", filter.getValueSet());
                    }
                }
            }
            params.addParameter(parameter);
        }
        return params;
    }
}
