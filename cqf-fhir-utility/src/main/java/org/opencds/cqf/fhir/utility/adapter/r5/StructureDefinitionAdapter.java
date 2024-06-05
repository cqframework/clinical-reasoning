package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;

public class StructureDefinitionAdapter extends ResourceAdapter implements KnowledgeArtifactAdapter {

    private StructureDefinition structureDefinition;

    public StructureDefinitionAdapter(StructureDefinition structureDefinition) {
        super(structureDefinition);
        this.structureDefinition = structureDefinition;
    }

    protected StructureDefinition getStructureDefinition() {
        return this.structureDefinition;
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getStructureDefinition().hasVersion()
                ? this.getStructureDefinition().getUrl() + "|"
                        + this.getStructureDefinition().getVersion()
                : this.getStructureDefinition().getUrl();
        /*
           extension[].url
           modifierExtension[].url
           baseDefinition
           differential.element[].type.code
           differential.element[].type.profile[]
           differential.element[].type.targetProfile[]
           differential.element[].binding.valueSet
           differential.element[].extension[].url
           differential.element[].modifierExtension[].url
           extension[cpg-inferenceExpression].reference
           extension[cpg-assertionExpression].reference
           extension[cpg-featureExpression].reference
        */

        var libraryExtensions = structureDefinition.getExtensionsByUrl(Constants.CQF_LIBRARY);
        for (var libraryExt : libraryExtensions) {
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource,
                    ((CanonicalType) libraryExt.getValue()).asStringValue(),
                    libraryExt.getExtension(),
                    (reference) -> libraryExt.setValue(new CanonicalType(reference)));
            references.add(dependency);
        }

        return references;
    }

    @Override
    public StructureDefinition get() {
        return this.structureDefinition;
    }

    @Override
    public StructureDefinition copy() {
        return this.get().copy();
    }

    @Override
    public String getUrl() {
        return this.get().getUrl();
    }

    @Override
    public boolean hasUrl() {
        return this.get().hasUrl();
    }

    @Override
    public void setUrl(String url) {
        this.get().setUrl(url);
    }

    @Override
    public void setVersion(String version) {
        this.get().setVersion(version);
    }

    @Override
    public String getVersion() {
        return this.get().getVersion();
    }

    @Override
    public boolean hasVersion() {
        return this.get().hasVersion();
    }

    @Override
    public String getName() {
        return this.get().getName();
    }

    @Override
    public void setName(String name) {
        this.get().setName(name);
    }

    @Override
    public Date getApprovalDate() {
        return null;
    }

    @Override
    public Date getDate() {
        return this.get().getDate();
    }

    @Override
    public void setDate(Date approvalDate) {
        this.get().setDate(approvalDate);
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        this.get().setDateElement((DateTimeType) date);
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        // do nothing
    }

    @Override
    public Period getEffectivePeriod() {
        return null;
    }

    @Override
    public String getPurpose() {
        return this.get().getPurpose();
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be a valid " + Period.class.getName());
        }
        // do nothing
    }

    @Override
    public boolean hasRelatedArtifact() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return new ArrayList<>();
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
        return this.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts)
            throws UnprocessableEntityException {
        // do nothing
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus status;
        try {
            status = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.get().setStatus(status);
    }

    @Override
    public String getStatus() {
        return this.get().getStatus() == null ? null : this.get().getStatus().toCode();
    }

    @Override
    public boolean getExperimental() {
        return this.get().getExperimental();
    }

    @Override
    public void setExtension(List<IBaseExtension<?, ?>> extensions) {
        this.get().setExtension(extensions.stream().map(e -> (Extension) e).collect(Collectors.toList()));
    }
}
