package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

class ValueSetAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter {

    private ValueSet valueSet;

    public ValueSetAdapter(ValueSet valueSet) {
        super(valueSet);

        if (!valueSet.fhirType().equals("ValueSet")) {
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
    public void setId(IIdType id) {
        this.getValueSet().setId(id);
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
                        this.valueSet.getCompose().getInclude().stream(),
                        this.valueSet.getCompose().getExclude().stream())
                .forEach(component -> {
                    if (component.hasValueSet()) {
                        component.getValueSet().forEach(uri -> {
                            references.add(new DependencyInfo(referenceSource, uri.getValue(), uri.getExtension()));
                        });
                    }
                    if (component.hasSystem()) {
                        references.add(new DependencyInfo(
                                referenceSource,
                                component.getSystem(),
                                component.getSystemElement().getExtension()));
                    }
                });

        // TODO: Ideally this would use the $data-requirements code
        return references;
    }

    @Override
    public Date getApprovalDate() {
        return null;
    }

    @Override
    public void setApprovalDate(Date date) {
        // do nothing;
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
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        this.getValueSet().setDateElement((DateTimeType) date);
    }

    @Override
    public Period getEffectivePeriod() {
        return new Period();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getComponents() {
        return this.getRelatedArtifactsOfType("composed-of");
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
        relatedArtifacts.stream().map(ra -> {
            try {
                return (RelatedArtifact) ra;
            } catch (ClassCastException e) {
                throw new UnprocessableEntityException(
                        "All related artifacts must be of type " + RelatedArtifact.class.getName());
            }
        });
        // do nothing
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        // do nothing
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus type;
        try {
            type = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.getValueSet().setStatus(type);
    }
}
