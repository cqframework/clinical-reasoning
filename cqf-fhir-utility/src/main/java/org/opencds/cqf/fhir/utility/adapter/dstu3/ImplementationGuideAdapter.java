package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ImplementationGuide;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public class ImplementationGuideAdapter extends KnowledgeArtifactAdapter implements IImplementationGuideAdapter {

    public ImplementationGuideAdapter(IDomainResource implementationGuide) {
        super(implementationGuide);
        if (!(implementationGuide instanceof ImplementationGuide)) {
            throw new IllegalArgumentException(
                    "resource passed as implementationGuide argument is not a ImplementationGuide resource");
        }
    }

    public ImplementationGuideAdapter(ImplementationGuide implementationGuide) {
        super(implementationGuide);
    }

    protected ImplementationGuide getImplementationGuide() {
        return (ImplementationGuide) resource;
    }

    @Override
    public ImplementationGuide get() {
        return (ImplementationGuide) resource;
    }

    @Override
    public ImplementationGuide copy() {
        return get().copy();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        getImplementationGuide().getPackage().forEach(p -> {
            p.getResource().forEach(pr -> {
                var source = pr.getSource();
                if (source instanceof UriType) {
                    addProfileReferences(references, ((UriType) source).getValue());
                } else if (source instanceof Reference) {
                    addProfileReferences(references, ((Reference) source).getReference());
                } else {
                    throw new UnprocessableEntityException(
                            "Package Resource Source must be instance of UriType or Reference");
                }
            });
        });

        return references;
    }

    @Override
    public List<IDependencyInfo> getDependencies(IRepository repository) {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        var artifactUrlExt = "http://hl7.org/fhir/StructureDefinition/artifact-url";

        for (var pkg : getImplementationGuide().getPackage()) {
            for (var dr : pkg.getResource()) {
                if (dr.hasSource()) {
                    if (dr.hasExample() && dr.getExample()) {
                        continue;
                    }
                    var refValue = dr.hasSourceReference()
                            ? dr.getSourceReference()
                            : new Reference(dr.getSource().primitiveValue());
                    var refElement = new IdType(refValue.getReference());
                    var refClass = fhirContext
                            .getResourceDefinition(refElement.getResourceType())
                            .newInstance()
                            .getClass();
                    var read = repository.read(refClass, new IdType(refValue.getReference()));
                    if (read instanceof MetadataResource mr && (mr.hasUrl() || mr.hasUrlElement())) {
                        var url = mr.hasUrlElement() ? mr.getUrlElement() : new UriType(mr.getUrl());
                        references.add(new DependencyInfo(
                                refValue.getReference(), url.getValueAsString(), mr.getExtension(), url::setValue));
                    } else if (read instanceof DomainResource domRes
                            && domRes.getExtensionByUrl(artifactUrlExt) != null) {
                        // TODO: ensure this extension is accounted for during the gather step
                        var ext = domRes.getExtensionByUrl(artifactUrlExt);
                        var url =
                                new org.hl7.fhir.r5.model.UriType(ext.getValue().primitiveValue());
                        references.add(new DependencyInfo(
                                refValue.getReference(), url.getValueAsString(), domRes.getExtension(), url::setValue));
                    } else {
                        IAdapter.logger.warn("Unable to resolve dependency URL for reference: {}", refValue);
                    }
                }
            }
        }

        return references;
    }

    @Override
    public Map<String, ILibraryAdapter> retrieveReferencedLibraries(IRepository repository) {
        var libraries = new HashMap<String, ILibraryAdapter>();

        // Iterate through all resources in the IG packages
        for (var pkg : getImplementationGuide().getPackage()) {
            for (var dr : pkg.getResource()) {
                if (dr.hasSource()) {
                    var refValue = dr.hasSourceReference()
                            ? dr.getSourceReference()
                            : new Reference(dr.getSource().primitiveValue());
                    var refElement = new IdType(refValue.getReference());

                    // Check if this is a Library resource
                    if ("Library".equals(refElement.getResourceType())) {
                        try {
                            var library = repository.read(Library.class, refElement);
                            if (library != null) {
                                var adapter = getAdapterFactory().createLibrary(library);
                                libraries.put(adapter.getName(), adapter);
                            }
                        } catch (Exception e) {
                            IAdapter.logger.warn("Unable to read Library resource: {}", refElement.getValue(), e);
                        }
                    }
                }
            }
        }

        return libraries;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifact() {
        // Start with any relatedArtifacts from extensions (handled by base implementation)
        List<T> relatedArtifacts = new ArrayList<>(super.getRelatedArtifact());

        // Add IG dependencies from dependency element (DSTU3)
        for (var dep : getImplementationGuide().getDependency()) {
            if (dep.hasUri()) {
                var relatedArtifact = new RelatedArtifact();
                relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
                relatedArtifact.setResource(new Reference(dep.getUri()));
                relatedArtifacts.add((T) relatedArtifact);
            }
        }

        // Add resources defined in the IG packages (DSTU3 structure)
        for (var pkg : getImplementationGuide().getPackage()) {
            for (var dr : pkg.getResource()) {
                if (dr.hasSource()) {
                    // Skip examples
                    if (dr.hasExample() && dr.getExample()) {
                        continue;
                    }

                    var refValue = dr.hasSourceReference()
                            ? dr.getSourceReference()
                            : new Reference(dr.getSource().primitiveValue());

                    var relatedArtifact = new RelatedArtifact();
                    relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);

                    // Append version to the reference if IG has a version
                    var resourceRef = new Reference(refValue.getReference());
                    if (getImplementationGuide().hasVersion()
                            && !refValue.getReference().contains("|")) {
                        resourceRef.setReference(refValue.getReference() + "|"
                                + getImplementationGuide().getVersion());
                    }
                    relatedArtifact.setResource(resourceRef);

                    // Add extension to track source package
                    if (getImplementationGuide().hasUrl()) {
                        String packageUrl = getImplementationGuide().getUrl();
                        String packageVersion = getImplementationGuide().hasVersion()
                                ? getImplementationGuide().getVersion()
                                : null;

                        // Extract package ID from URL (DSTU3 doesn't have explicit packageId field)
                        // Extract from URL (last segment after last slash)
                        String packageId = packageUrl.substring(packageUrl.lastIndexOf('/') + 1);

                        // Create complex extension with packageId (required), version (optional), and uri (optional)
                        var extension = new org.hl7.fhir.dstu3.model.Extension();
                        extension.setUrl(org.opencds.cqf.fhir.utility.Constants.PACKAGE_SOURCE);

                        // Add required packageId sub-extension
                        var packageIdExt = new org.hl7.fhir.dstu3.model.Extension();
                        packageIdExt.setUrl("packageId");
                        packageIdExt.setValue(new org.hl7.fhir.dstu3.model.IdType(packageId));
                        extension.addExtension(packageIdExt);

                        // Add optional version sub-extension
                        if (packageVersion != null) {
                            var versionExt = new org.hl7.fhir.dstu3.model.Extension();
                            versionExt.setUrl("version");
                            versionExt.setValue(new org.hl7.fhir.dstu3.model.StringType(packageVersion));
                            extension.addExtension(versionExt);
                        }

                        // Add optional uri sub-extension
                        var uriExt = new org.hl7.fhir.dstu3.model.Extension();
                        uriExt.setUrl("uri");
                        uriExt.setValue(new org.hl7.fhir.dstu3.model.UriType(packageUrl));
                        extension.addExtension(uriExt);

                        relatedArtifact.addExtension(extension);
                    }

                    relatedArtifacts.add((T) relatedArtifact);
                }
            }
        }

        return relatedArtifacts;
    }
}
