package org.opencds.cqf.fhir.utility.visitor;

import static org.opencds.cqf.fhir.utility.visitor.VisitorHelper.findUnsupportedCapability;
import static org.opencds.cqf.fhir.utility.visitor.VisitorHelper.processCanonicals;

import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;

public class DataRequirementsVisitor implements KnowledgeArtifactVisitor {

    @Override
    public IBase visit(KnowledgeArtifactAdapter adapter, Repository repository, IBaseParameters parameters) {
        var fhirVersion = adapter.get().getStructureFhirVersionEnum();
        Optional<String> artifactRoute = VisitorHelper.getParameter("artifactRoute", parameters, IPrimitiveType.class)
                .map(r -> (String) r.getValue());
        Optional<String> endpointUri = VisitorHelper.getParameter("endpointUri", parameters, IPrimitiveType.class)
                .map(r -> (String) r.getValue());
        Optional<IBaseResource> endpoint =
                VisitorHelper.getResourceParameter("endpoint", parameters, IBaseResource.class);
        Optional<IBaseResource> terminologyEndpoint =
                VisitorHelper.getResourceParameter("terminologyEndpoint", parameters, IBaseResource.class);
        List<String> artifactVersion = VisitorHelper.getListParameter(
                        "artifactVersion", parameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> checkArtifactVersion = VisitorHelper.getListParameter(
                        "checkArtifactVersion", parameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> forceArtifactVersion = VisitorHelper.getListParameter(
                        "forceArtifactVersion", parameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());

        if ((artifactRoute.isPresent()
                        && !StringUtils.isBlank(artifactRoute.get())
                        && !artifactRoute.get().isEmpty())
                || (endpointUri.isPresent()
                        && !StringUtils.isBlank(endpointUri.get())
                        && !endpointUri.get().isEmpty())
                || endpoint.isPresent()) {
            throw new NotImplementedOperationException(
                    "This repository is not implementing custom Content and endpoints at this time");
        }

        var resource = adapter.get();
        var library = Resources.newResource(
                repository.fhirContext().getResourceDefinition("Library").getImplementingClass());
        var libraryAdapter = AdapterFactory.forFhirVersion(fhirVersion).createLibrary(library);
        libraryAdapter.setType("module-definition");
        recursiveGather(
                resource,
                libraryAdapter,
                repository,
                forceArtifactVersion,
                forceArtifactVersion,
                artifactVersion,
                checkArtifactVersion,
                forceArtifactVersion);
        return libraryAdapter.get();
    }

    protected void recursiveGather(
            IDomainResource resource,
            LibraryAdapter libraryAdapter,
            Repository repository,
            List<String> capability,
            List<String> include,
            List<String> artifactVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion)
            throws PreconditionFailedException {
        if (resource != null) {
            var fhirVersion = resource.getStructureFhirVersionEnum();
            var adapter = AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(resource);
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, artifactVersion, checkArtifactVersion, forceArtifactVersion);

            // TODO: Handle duplicates
            gatherRequirements(adapter).forEach(dr -> libraryAdapter.addDataRequirement(dr));

            adapter.combineComponentsAndDependencies().stream()
                    // sometimes VS dependencies aren't FHIR resources
                    .filter(ra -> !StringUtils.isBlank(Canonicals.getResourceType(ra.getReference())))
                    .filter(ra -> {
                        try {
                            var resourceDef = repository
                                    .fhirContext()
                                    .getResourceDefinition(Canonicals.getResourceType(ra.getReference()));
                            return resourceDef != null;
                        } catch (DataFormatException e) {
                            if (e.getMessage().contains("1684")) {
                                return false;
                            } else {
                                throw new DataFormatException(e.getMessage());
                            }
                        }
                    })
                    .map(ra -> SearchHelper.searchRepositoryByCanonicalWithPaging(repository, ra.getReference()))
                    .map(searchBundle -> (IDomainResource) BundleHelper.getEntryResourceFirstRep(searchBundle))
                    .forEach(component -> recursiveGather(
                            component,
                            libraryAdapter,
                            repository,
                            capability,
                            include,
                            artifactVersion,
                            checkArtifactVersion,
                            forceArtifactVersion));
        }
    }

    protected List<? extends ICompositeType> gatherRequirements(KnowledgeArtifactAdapter adapter) {
        // TODO: Refactor R4MeasureDataRequirementsService
        return new ArrayList<>();
    }
}
