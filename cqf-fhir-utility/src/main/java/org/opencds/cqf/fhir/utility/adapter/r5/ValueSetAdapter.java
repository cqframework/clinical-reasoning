package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

class ValueSetAdapter extends KnowledgeArtifactAdapter implements org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter {

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
    public IBaseResource get() {
        return this.valueSet;
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
    public void setVersion(String version) {
        this.getValueSet().setVersion(version);
    }

    @Override
    public List<DependencyInfo> getDependencies() {
        List<DependencyInfo> references = new ArrayList<>();
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
                        component.getValueSet().forEach(ct -> {
                            references.add(new DependencyInfo(referenceSource, ct.getValue(), ct.getExtension()));
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
        return this.valueSet.getApprovalDate();
    }

    @Override
    public ICompositeType getEffectivePeriod() {
        return new Period();
    }

    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return new ArrayList<>();
    }

    @Override
    public List<RelatedArtifact> getComponents() {
        return new ArrayList<>();
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        this.valueSet.setApprovalDate(approvalDate);
    }
}
