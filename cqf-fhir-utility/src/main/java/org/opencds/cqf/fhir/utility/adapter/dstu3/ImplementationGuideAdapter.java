package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.ImplementationGuide;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter;

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
                    var refValue = dr.hasSourceReference() ? dr.getSourceReference() : new Reference(dr.getSource().primitiveValue());
                    var refElement = new IdType(refValue.primitiveValue());
                    var refClass = fhirContext
                        .getResourceDefinition(refElement.getResourceType())
                        .newInstance()
                        .getClass();
                    var read = repository.read(refClass, new IdType(refValue.primitiveValue()));
                    if (read instanceof MetadataResource mr && (mr.hasUrl() || mr.hasUrlElement())) {
                        var url = mr.hasUrlElement() ? mr.getUrlElement() : new UriType(mr.getUrl());
                        references.add(
                            new DependencyInfo(refValue.getReference(), url.getValueAsString(), mr.getExtension(), url::setValue));
                    } else if (read instanceof DomainResource domRes && domRes.getExtensionByUrl(artifactUrlExt) != null) {
                        // TODO: ensure this extension is accounted for during the gather step
                        var ext = domRes.getExtensionByUrl(artifactUrlExt);
                        var url = new org.hl7.fhir.r5.model.UriType(ext.getValue().primitiveValue());
                        references.add(
                            new DependencyInfo(refValue.getReference(), url.getValueAsString(), domRes.getExtension(), url::setValue));
                    } else {
                        IAdapter.logger.warn("Unable to resolve dependency URL for reference: {}", refValue);
                    }
                }
            }
        }

        return references;
    }
}
