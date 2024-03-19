package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

class ValueSetAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter {

    private ValueSet valueSet;

    public ValueSetAdapter(ValueSet valueSet) {
        super(valueSet);

        if (!(valueSet instanceof ValueSet)) {
            throw new IllegalArgumentException("resource passed as valueSet argument is not a ValueSet resource");
        }

        this.valueSet = valueSet;
    }

    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

    protected ValueSet getValueSet() {
        return this.valueSet;
    }

    @Override
    public ValueSet get() {
        return this.valueSet;
    }

    @Override
    public ValueSet copy() {
        return this.get().copy();
    }

    @Override
    public String getName() {
        return this.getValueSet().getName();
    }

    @Override
    public void setName(String name) {
        this.getValueSet().setName(name);
    }

    @Override
    public boolean hasUrl() {
        return this.getValueSet().hasUrl();
    }

    @Override
    public String getUrl() {
        return this.getValueSet().getUrl();
    }

    @Override
    public void setUrl(String url) {
        this.getValueSet().setUrl(url);
    }

    @Override
    public String getVersion() {
        return this.getValueSet().getVersion();
    }

    @Override
    public boolean hasVersion() {
        return this.getValueSet().hasVersion();
    }

    @Override
    public void setVersion(String version) {
        this.getValueSet().setVersion(version);
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getValueSet().hasVersion()
                ? this.getValueSet().getUrl() + "|" + this.getValueSet().getVersion()
                : this.getValueSet().getUrl();

        /*
          compose.include[].valueSet
          compose.include[].system
          compose.exclude[].valueSet
          compose.exclude[].system
        */
        Stream.concat(
                        this.getValueSet().getCompose().getInclude().stream(),
                        this.getValueSet().getCompose().getExclude().stream())
                .forEach(component -> {
                    if (component.hasValueSet()) {
                        component.getValueSet().forEach(ct -> {
                            references.add(new DependencyInfo(
                                    referenceSource,
                                    ct.getValue(),
                                    ct.getExtension(),
                                    (reference) -> ct.setValue(reference)));
                        });
                    }
                    if (component.hasSystem()) {
                        references.add(new DependencyInfo(
                                referenceSource,
                                component.getSystem(),
                                component.getSystemElement().getExtension(),
                                (reference) -> component.setSystem(reference)));
                    }
                });

        // TODO: Ideally this would use the $data-requirements code
        return references;
    }

    @Override
    public Date getApprovalDate() {
        return this.getValueSet().getApprovalDate();
    }

    @Override
    public Date getDate() {
        return this.getValueSet().getDate();
    }

    @Override
    public void setDate(Date date) {
        this.getValueSet().setDate(date);
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) throws UnprocessableEntityException {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        this.getValueSet().setDateElement((DateTimeType) date);
    }

    @Override
    public Period getEffectivePeriod() {
        return this.getValueSet().getEffectivePeriod();
    }

    @Override
    public boolean hasRelatedArtifact() {
        return this.getValueSet().hasRelatedArtifact();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getValueSet().getRelatedArtifact();
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts) {
        this.getValueSet()
                .setRelatedArtifact(relatedArtifacts.stream()
                        .map(ra -> (RelatedArtifact) ra)
                        .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getComponents() {
        return this.getRelatedArtifactsOfType("composed-of");
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        this.getValueSet().setApprovalDate(approvalDate);
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be org.hl7.fhir.r5.model.Period");
        }
        this.getValueSet().setEffectivePeriod((Period) effectivePeriod);
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
    public void setStatus(String statusCodeString) {
        PublicationStatus status;
        try {
            status = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.getValueSet().setStatus(status);
    }

    @Override
    public String getStatus() {
        return this.getValueSet().getStatus() == null
                ? null
                : this.getValueSet().getStatus().toCode();
    }

    @Override
    public boolean getExperimental() {
        return this.getValueSet().getExperimental();
    }

    @Override
    public void setExtension(List<IBaseExtension<?, ?>> extensions) {
        this.get().setExtension(extensions.stream().map(e -> (Extension) e).collect(Collectors.toList()));
    }
}
