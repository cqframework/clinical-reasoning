package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.DomainResource;
import org.hl7.fhir.r5.model.IdType;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.UrlType;
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

        getImplementationGuide().getDefinition().getResource().forEach(dr -> {
            addProfileReferences(references, dr.getReference().getReference());
        });
        return references;
    }

    @Override
    public List<IDependencyInfo> getDependencies(IRepository repository) {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        var artifactUrlExt = "http://hl7.org/fhir/StructureDefinition/artifact-url";

        for (var dr : getImplementationGuide().getDefinition().getResource()) {
            if (dr.hasReference() && dr.getReference().hasReference()) {
                if (dr.hasIsExample() && dr.getIsExample()) {
                    continue;
                }
                var refValue = dr.getReference().getReference();
                var refElement = dr.getReference().getReferenceElement();
                var refClass = fhirContext
                        .getResourceDefinition(refElement.getResourceType())
                        .newInstance()
                        .getClass();
                var read = repository.read(refClass, new IdType(refValue));
                if (read instanceof MetadataResource mr && (mr.hasUrl() || mr.hasUrlElement())) {
                    var url = mr.hasUrlElement() ? mr.getUrlElement() : new UrlType(mr.getUrl());
                    references.add(
                            new DependencyInfo(refValue, url.getValueAsString(), mr.getExtension(), url::setValue));
                } else if (read instanceof DomainResource domRes && domRes.getExtensionByUrl(artifactUrlExt) != null) {
                    // TODO: ensure this extension is accounted for during the gather step
                    var ext = domRes.getExtensionByUrl(artifactUrlExt);
                    var url = new UriType(ext.getValue().primitiveValue());
                    references.add(
                            new DependencyInfo(refValue, url.getValueAsString(), domRes.getExtension(), url::setValue));
                } else {
                    IAdapter.logger.warn("Unable to resolve dependency URL for reference: {}", refValue);
                }
            }
        }

        return references;
    }

    @Override
    public Map<String, ILibraryAdapter> retrieveReferencedLibraries(IRepository repository) {
        var libraries = new HashMap<String, ILibraryAdapter>();

        // Iterate through all resources in the IG definition
        for (var dr : getImplementationGuide().getDefinition().getResource()) {
            if (dr.hasReference() && dr.getReference().hasReference()) {
                var refElement = dr.getReference().getReferenceElement();

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

        return libraries;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ICompositeType & IBaseHasExtensions> List<T> getRelatedArtifact() {
        // Start with any relatedArtifacts from extensions (handled by base implementation)
        List<T> relatedArtifacts = new ArrayList<>(super.getRelatedArtifact());

        // Add IG dependencies from dependsOn element
        for (var dep : getImplementationGuide().getDependsOn()) {
            if (dep.hasUri()) {
                var relatedArtifact = new RelatedArtifact();
                relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
                relatedArtifact.setResource(dep.getUri() + (dep.hasVersion() ? "|" + dep.getVersion() : ""));
                if (dep.hasPackageId()) {
                    relatedArtifact.setDisplay("ImplementationGuide " + dep.getPackageId()
                            + (dep.hasVersion() ? ", " + dep.getVersion() : ""));
                }
                relatedArtifacts.add((T) relatedArtifact);
            }
        }

        // Add resources defined in the IG - these become composed-of dependencies
        for (var dr : getImplementationGuide().getDefinition().getResource()) {
            if (dr.hasReference() && dr.getReference().hasReference()) {
                // Skip examples
                if (dr.hasIsExample() && dr.getIsExample()) {
                    continue;
                }

                var refValue = dr.getReference().getReference();
                var relatedArtifact = new RelatedArtifact();
                relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);

                // Append version to the reference if IG has a version
                String resourceRef = refValue;
                if (getImplementationGuide().hasVersion() && !refValue.contains("|")) {
                    resourceRef = refValue + "|" + getImplementationGuide().getVersion();
                }
                relatedArtifact.setResource(resourceRef);

                // Add extension to track source package
                if (getImplementationGuide().hasUrl()) {
                    String packageUrl = getImplementationGuide().getUrl();
                    String packageVersion = getImplementationGuide().hasVersion()
                            ? getImplementationGuide().getVersion()
                            : null;

                    // Extract package ID - prefer explicit packageId field, otherwise extract from URL
                    String packageId;
                    if (getImplementationGuide().hasPackageId()) {
                        packageId = getImplementationGuide().getPackageId();
                    } else {
                        // Extract from URL (last segment after last slash)
                        packageId = packageUrl.substring(packageUrl.lastIndexOf('/') + 1);
                    }

                    // Create complex extension with packageId (required), version (optional), and uri (optional)
                    var extension = new org.hl7.fhir.r5.model.Extension();
                    extension.setUrl(org.opencds.cqf.fhir.utility.Constants.PACKAGE_SOURCE);

                    // Add required packageId sub-extension
                    var packageIdExt = new org.hl7.fhir.r5.model.Extension();
                    packageIdExt.setUrl("packageId");
                    packageIdExt.setValue(new org.hl7.fhir.r5.model.IdType(packageId));
                    extension.addExtension(packageIdExt);

                    // Add optional version sub-extension
                    if (packageVersion != null) {
                        var versionExt = new org.hl7.fhir.r5.model.Extension();
                        versionExt.setUrl("version");
                        versionExt.setValue(new org.hl7.fhir.r5.model.StringType(packageVersion));
                        extension.addExtension(versionExt);
                    }

                    // Add optional uri sub-extension
                    var uriExt = new org.hl7.fhir.r5.model.Extension();
                    uriExt.setUrl("uri");
                    uriExt.setValue(new org.hl7.fhir.r5.model.UriType(packageUrl));
                    extension.addExtension(uriExt);

                    relatedArtifact.addExtension(extension);
                }

                relatedArtifacts.add((T) relatedArtifact);
            }
        }

        return relatedArtifacts;
    }
}
