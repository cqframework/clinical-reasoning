package org.opencds.cqf.fhir.cr.inputparameters.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.inputparameters.BaseInputParameterResolver;
import org.opencds.cqf.fhir.utility.Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
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
            IBaseBundle data,
            List<IParametersParameterComponentAdapter> context,
            List<IBaseExtension<?, ?>> launchContext) {
        super(repository, subjectId, encounterId, practitionerId, useServerData, data);
        this.parameters = resolveParameters(parameters, context, launchContext);
    }

    @Override
    protected final Parameters resolveParameters(
            IBaseParameters baseParameters,
            List<IParametersParameterComponentAdapter> context,
            List<IBaseExtension<?, ?>> launchContext) {
        var params = parameters();
        if (baseParameters != null) {
            params.getParameter().addAll(((Parameters) baseParameters).getParameter());
        }
        if (subjectId != null) {
            var subjectClass = fhirContext()
                    .getResourceDefinition(subjectId.getResourceType())
                    .getImplementingClass();
            var subject = readRepository(subjectClass, subjectId);
            if (subject != null) {
                params.addParameter(part("%subject", (Resource) subject));
            }
        }
        if (encounterId != null) {
            var encounter = readRepository(Encounter.class, encounterId);
            if (encounter != null) {
                params.addParameter(part("%encounter", encounter));
            }
        }
        if (practitionerId != null) {
            var practitioner = readRepository(Practitioner.class, practitionerId);
            if (practitioner != null) {
                params.addParameter(part("%practitioner", practitioner));
            }
        }
        if (launchContext != null && !launchContext.isEmpty()) {
            resolveLaunchContext(params, context, launchContext);
        }
        return params;
    }

    protected boolean validateContext(SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE code, String type) {
        switch (code) {
            case PATIENT:
                return type.equals(ResourceType.Patient.name());
            case ENCOUNTER:
                return type.equals(ResourceType.Encounter.name());
            case LOCATION:
                return type.equals(ResourceType.Location.name());
            case USER:
                return type.equals(ResourceType.Patient.name())
                        || type.equals(ResourceType.Practitioner.name())
                        || type.equals(ResourceType.PractitionerRole.name())
                        || type.equals(ResourceType.RelatedPerson.name());
            case STUDY:
                return type.equals(ResourceType.ResearchStudy.name());
            case CLINICAL:
                return true;

            default:
                return false;
        }
    }

    protected void resolveLaunchContext(
            Parameters params,
            List<IParametersParameterComponentAdapter> contexts,
            List<IBaseExtension<?, ?>> launchContexts) {
        launchContexts.stream().map(e -> (Extension) e).forEach(launchContext -> {
            var name = ((Coding) launchContext.getExtensionByUrl("name").getValue()).getCode();
            var types = launchContext.getExtensionsByUrl("type").stream()
                    .map(type -> type.getValueAsPrimitive().getValueAsString())
                    .collect(Collectors.toList());
            // if (!validateContext(SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE.valueOf(name.toUpperCase()), type)) {
            //     throw new IllegalArgumentException(String.format("Unsupported launch context for %s: %s", name,
            // type));
            // }
            var content = getContent(contexts, name);
            if (content != null && !content.isEmpty()) {
                var value = getValue(types, content);
                if (!value.isEmpty()) {
                    var cqlParameterName = name.substring(0, 1).toUpperCase().concat(name.substring(1));
                    // Add a parameter with the FHIRPath name and one with the CQL name
                    // If there are multiple values multiple parameters with the same name are created and treated as a
                    // list
                    value.forEach(resource -> {
                        params.addParameter(part("%" + name, resource));
                        params.addParameter(part(cqlParameterName, resource));
                    });
                }
                //  else {
                //     throw new IllegalArgumentException(
                //             String.format("Unable to retrieve resource for context: %s", name));
                // }
            }
        });
    }

    protected List<ParametersParameterComponent> getContent(
            List<IParametersParameterComponentAdapter> contexts, String name) {
        return contexts == null
                ? null
                : contexts.stream()
                        .map(c -> (ParametersParameterComponent) c.get())
                        .filter(c -> c.getPart().stream()
                                .filter(p -> p.getName().equals("name"))
                                .anyMatch(p -> ((StringType) p.getValue())
                                        .getValueAsString()
                                        .equals(name)))
                        .flatMap(c ->
                                c.getPart().stream().filter(p -> p.getName().equals("content")))
                        .collect(Collectors.toList());
    }

    protected List<Resource> getValue(List<String> types, List<ParametersParameterComponent> content) {
        return content.stream()
                .map(p -> {
                    if (p.getValue() instanceof Reference ref) {
                        Resource resource = null;
                        for (var type : types) {
                            try {
                                resource = (Resource) readRepository(
                                        fhirContext()
                                                .getResourceDefinition(type)
                                                .getImplementingClass(),
                                        ref.getReferenceElement());
                                if (resource != null) {
                                    break;
                                }
                            } catch (Exception e) {
                                // Do nothing
                            }
                        }
                        return resource;
                    } else {
                        return p.getResource();
                    }
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());
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
                                Bundle.class, ValueSet.class, Searches.byCanonical(filter.getValueSet()));
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
