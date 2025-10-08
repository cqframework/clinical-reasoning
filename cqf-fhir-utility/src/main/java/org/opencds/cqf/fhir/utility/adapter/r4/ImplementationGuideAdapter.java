package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UrlType;
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

        // TODO: is the dependsOn element needed?

        getImplementationGuide().getDefinition().getResource().forEach(dr -> {
            if (dr.hasReference() && dr.getReference().hasReference()) {
                var ref = dr.getReference().hasReferenceElement()
                        ? dr.getReference().getReferenceElement()
                        : new IdType(dr.getReference().getReference());
                var dep = new DependencyInfo(
                        getImplementationGuide().getUrl(), ref.getValueAsString(), null, ref::setValue);
                references.add(dep);
            }
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
                if (dr.hasExampleCanonicalType()
                        || (dr.hasExampleBooleanType()
                                && dr.getExampleBooleanType().booleanValue())) {
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
}
