package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.utility.Parameters.newPart;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.repository.FederatedRepository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides functionality to resolve parameters passed into an operation as CQL Resource parameters
 * for evaluation. e.g. "%subject"
 */
public class InputParameterResolver implements IInputParameterResolver {
    private static final Logger logger = LoggerFactory.getLogger(InputParameterResolver.class);

    protected final IIdType subjectId;
    protected final IIdType encounterId;
    protected final IIdType practitionerId;
    protected final IAdapterFactory adapterFactory;
    protected final Repository repository;
    protected final IParametersAdapter parameters;

    public InputParameterResolver(
            Repository repository,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IBaseParameters parameters,
            IBaseBundle data,
            List<IParametersParameterComponentAdapter> context,
            List<IBaseExtension<?, ?>> launchContext) {
        this.subjectId = subjectId;
        this.encounterId = encounterId;
        this.practitionerId = practitionerId;
        this.repository = resolveRepository(repository, data);
        adapterFactory = IAdapterFactory.forFhirContext(this.repository.fhirContext());
        this.parameters = resolveParameters(parameters, context, launchContext);
    }

    protected final Repository resolveRepository(Repository serverRepository, IBaseBundle data) {
        return data == null
                ? serverRepository
                : new FederatedRepository(
                        serverRepository, new InMemoryFhirRepository(serverRepository.fhirContext(), data));
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }

    protected FhirVersionEnum fhirVersion() {
        return fhirContext().getVersion().getVersion();
    }

    protected <R extends IBaseResource> R readRepository(Class<R> resourceType, IIdType id) {
        try {
            return repository.read(resourceType, id);
        } catch (Exception e) {
            return null;
        }
    }

    protected final IParametersAdapter resolveParameters(
            IBaseParameters baseParameters,
            List<IParametersParameterComponentAdapter> context,
            List<IBaseExtension<?, ?>> launchContext) {
        var params = adapterFactory.createParameters(
                (IBaseParameters) Resources.newBaseForVersion("Parameters", fhirVersion()));
        if (baseParameters != null) {
            adapterFactory.createParameters(baseParameters).getParameter().forEach(p -> params.addParameter(p.get()));
        }
        if (subjectId != null) {
            var subjectClass = fhirContext()
                    .getResourceDefinition(subjectId.getResourceType())
                    .getImplementingClass();
            var subject = readRepository(subjectClass, subjectId);
            if (subject != null) {
                params.addParameter("%subject", subject);
            }
        }
        if (encounterId != null) {
            var encounterClass = fhirContext()
                    .getResourceDefinition(encounterId.getResourceType())
                    .getImplementingClass();
            var encounter = readRepository(encounterClass, encounterId);
            if (encounter != null) {
                params.addParameter("%encounter", encounter);
            }
        }
        if (practitionerId != null) {
            var practitionerClass = fhirContext()
                    .getResourceDefinition(practitionerId.getResourceType())
                    .getImplementingClass();
            var practitioner = readRepository(practitionerClass, practitionerId);
            if (practitioner != null) {
                params.addParameter("%practitioner", practitioner);
            }
        }
        if (launchContext != null && !launchContext.isEmpty()) {
            resolveLaunchContext(params, context, launchContext);
        }
        return params;
    }

    protected void resolveLaunchContext(
            IParametersAdapter params,
            List<IParametersParameterComponentAdapter> contexts,
            List<IBaseExtension<?, ?>> launchContexts) {
        launchContexts.forEach(launchContext -> {
            var name = launchContext.getExtension().stream()
                    .map(e -> ((IBaseExtension<?, ?>) e))
                    .filter(e -> e.getUrl().equals("name"))
                    .map(e -> adapterFactory
                            .createCoding((ICompositeType) e.getValue())
                            .getCode())
                    .findFirst()
                    .orElse(null);
            var types = launchContext.getExtension().stream()
                    .map(e -> ((IBaseExtension<?, ?>) e))
                    .filter(e -> e.getUrl().equals("type"))
                    .map(type -> ((IPrimitiveType<?>) type.getValue()).getValueAsString())
                    .toList();
            if (name == null || types.isEmpty()) {
                throw new UnprocessableEntityException("Encountered invalid Launch Context extension");
            }
            var content = getContent(contexts, name);
            if (content != null && !content.isEmpty()) {
                var value = getValue(types, content);
                if (!value.isEmpty()) {
                    var cqlParameterName = name.substring(0, 1).toUpperCase().concat(name.substring(1));
                    // Add a parameter with the FHIRPath name and one with the CQL name
                    // If there are multiple values multiple parameters with the same name are created and treated as a
                    // list
                    value.forEach(resource -> {
                        params.addParameter("%" + name, resource);
                        params.addParameter(cqlParameterName, resource);
                    });
                }
            }
        });
    }

    protected List<IParametersParameterComponentAdapter> getContent(
            List<IParametersParameterComponentAdapter> contexts, String name) {
        return contexts == null
                ? null
                : contexts.stream()
                        // .map(c -> (ParametersParameterComponent) c.get())
                        .filter(c -> c.getPart().stream()
                                .filter(p -> p.getName().equals("name"))
                                .anyMatch(p -> ((IPrimitiveType<?>) p.getValue())
                                        .getValueAsString()
                                        .equals(name)))
                        .flatMap(c ->
                                c.getPart().stream().filter(p -> p.getName().equals("content")))
                        .toList();
    }

    protected List<IBaseResource> getValue(List<String> types, List<IParametersParameterComponentAdapter> content) {
        return content.stream()
                .map(p -> {
                    if (p.getValue() instanceof IBaseReference ref) {
                        IBaseResource resource = null;
                        for (var type : types) {
                            try {
                                resource = readRepository(
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
                .filter(Objects::nonNull)
                .toList();
    }

    public IBaseParameters getParameters() {
        return parameters == null ? null : (IBaseParameters) parameters.get();
    }

    public <T extends ICompositeType> IBaseParameters resolveInputParameters(List<T> dataRequirement) {
        var dataRequirements = dataRequirement.stream()
                .map(adapterFactory::createDataRequirement)
                .toList();
        var params = adapterFactory.createParameters(
                (IBaseParameters) Resources.newBaseForVersion("Parameters", fhirVersion()));
        if (parameters != null) {
            parameters.getParameter().forEach(p -> params.addParameter(p.get()));
        }
        for (var req : dataRequirements) {
            // only resolve requirements that have both an id and code filter
            if (!req.hasId() || !req.hasCodeFilter()) {
                continue;
            }

            var parameter = adapterFactory.createParametersParameter(
                    (IBaseBackboneElement) newPart(fhirContext(), "%" + String.format("%s", req.getId())));
            resolveCodeFilters(req, parameter);
            params.addParameter(parameter.get());
        }
        return (IBaseParameters) params.get();
    }

    private void resolveCodeFilters(IDataRequirementAdapter req, IParametersParameterComponentAdapter parameter) {
        for (var filter : req.getCodeFilter()) {
            if (filter != null && filter.hasPath() && filter.hasValueSet()) {
                try {
                    var valueSet = adapterFactory.createValueSet(
                            SearchHelper.searchRepositoryByCanonical(repository, filter.getValueSet()));
                    var codes = valueSet.getExpansionContains().stream()
                            .map(c -> adapterFactory
                                    .createCoding((ICompositeType) Resources.newBaseForVersion("Coding", fhirVersion()))
                                    .setCode(c.getCode())
                                    .setSystem(c.getSystem())
                                    .setDisplay(c.getDisplay()))
                            .toList();

                    var searchBuilder = Searches.builder();
                    codes.forEach(c -> searchBuilder.withTokenParam("code", c.getCode()));
                    var resourceType =
                            fhirContext().getResourceDefinition(req.getType()).getImplementingClass();
                    var searchResults = SearchHelper.searchRepositoryWithPaging(
                            repository, resourceType, searchBuilder.build(), null);
                    if (!BundleHelper.getEntry(searchResults).isEmpty()) {
                        parameter.setResource(
                                BundleHelper.getEntry(searchResults).size() > 1
                                        ? searchResults
                                        : BundleHelper.getEntryResourceFirstRep(searchResults));
                    }
                } catch (Exception e) {
                    logger.debug("Could not find ValueSet with url {} on the local server.", filter.getValueSet());
                }
            }
        }
    }
}
