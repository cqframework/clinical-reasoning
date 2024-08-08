package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class KnowledgeArtifactAdapter extends ResourceAdapter
        implements org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter {
    MetadataResource adaptedResource;

    public KnowledgeArtifactAdapter(IDomainResource resource) {
        super(resource);
        if (!(resource instanceof MetadataResource)) {
            throw new IllegalArgumentException(
                    "resource passed as resource argument is not a MetadataResource resource");
        }
        adaptedResource = (MetadataResource) resource;
    }

    public KnowledgeArtifactAdapter(MetadataResource resource) {
        super(resource);
        adaptedResource = resource;
    }

    @Override
    public MetadataResource get() {
        return adaptedResource;
    }

    @Override
    public MetadataResource copy() {
        return get().copy();
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter.super.setDateElement(date);
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be a valid " + Period.class.getName());
        }
        org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter.super.setEffectivePeriod(effectivePeriod);
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);
        return references;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifactsOfType(String codeString) {
        RelatedArtifactType type;
        try {
            type = RelatedArtifactType.fromCode(codeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid related artifact code");
        }
        return getRelatedArtifact().stream()
                .map(ra -> (RelatedArtifact) ra)
                .filter(ra -> ra.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public String getStatus() {
        return get().getStatus() == null ? null : get().getStatus().toCode();
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus status;
        try {
            status = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        get().setStatus(status);
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts)
            throws UnprocessableEntityException {
        org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter.super.setRelatedArtifact(relatedArtifacts.stream()
                .map(ra -> {
                    try {
                        return (RelatedArtifact) ra;
                    } catch (ClassCastException e) {
                        throw new UnprocessableEntityException(
                                "All related artifacts must be of type " + RelatedArtifact.class.getName());
                    }
                })
                .collect(Collectors.toList()));
    }
}
