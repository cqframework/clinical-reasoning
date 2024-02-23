package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.visitor.dstu3.dstu3KnowledgeArtifactVisitor;

class PlanDefinitionAdapter extends KnowledgeArtifactAdapter implements dstu3PlanDefinitionAdapter {

    private PlanDefinition planDefinition;

    public PlanDefinitionAdapter(PlanDefinition planDefinition) {
        super(planDefinition);

        if (!planDefinition.fhirType().equals("PlanDefinition")) {
            throw new IllegalArgumentException(
                    "resource passed as planDefinition argument is not a PlanDefinition resource");
        }

        this.planDefinition = planDefinition;
    }

    @Override
    public IBase accept(dstu3KnowledgeArtifactVisitor visitor, Repository repository, Parameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

    protected PlanDefinition getPlanDefinition() {
        return this.planDefinition;
    }

    @Override
    public PlanDefinition get() {
        return this.planDefinition;
    }

    @Override
    public IIdType getId() {
        return this.getPlanDefinition().getIdElement();
    }

    @Override
    public void setId(IIdType id) {
        this.getPlanDefinition().setId(id);
    }

    @Override
    public String getName() {
        return this.getPlanDefinition().getName();
    }

    @Override
    public void setName(String name) {
        this.getPlanDefinition().setName(name);
    }

    @Override
    public String getUrl() {
        return this.getPlanDefinition().getUrl();
    }

    @Override
    public void setUrl(String url) {
        this.getPlanDefinition().setUrl(url);
    }

    @Override
    public String getVersion() {
        return this.getPlanDefinition().getVersion();
    }

    @Override
    public void setVersion(String version) {
        this.getPlanDefinition().setVersion(version);
    }

    @Override
    public List<DependencyInfo> getDependencies() {
        List<DependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getPlanDefinition().hasVersion()
                ? this.getPlanDefinition().getUrl() + "|"
                        + this.getPlanDefinition().getVersion()
                : this.getPlanDefinition().getUrl();
        /*
         https://build.fhir.org/ig/HL7/crmi-ig/distribution.html#package-and-data-requirements
         relatedArtifact[].resource
         library[]
         action[]..trigger[].dataRequirement[].profile[]
         action[]..trigger[].dataRequirement[].codeFilter[].valueSet
         action[]..condition[].expression.reference
         action[]..input[].profile[]
         action[]..input[].codeFilter[].valueSet
         action[]..output[].profile[]
         action[]..output[].codeFilter[].valueSet
         action[]..definitionCanonical
         action[]..dynamicValue[].expression.reference
         extension[cpg-partOf]
        */

        // relatedArtifact[].resource
        references.addAll(getRelatedArtifactReferences(this.planDefinition, this.planDefinition.getRelatedArtifact()));

        // library[]
        List<Reference> libraries = this.planDefinition.getLibrary();
        for (Reference ref : libraries) {
            // TODO: Account for reference.identifier?
            DependencyInfo dependency = new DependencyInfo(referenceSource, ref.getReference(), ref.getExtension());
            references.add(dependency);
        }
        // action[]
        this.planDefinition.getAction().forEach(action -> {
            action.getTriggerDefinition().stream().map(t -> t.getEventData()).forEach(eventData -> {
                // trigger[].dataRequirement[].profile[]
                eventData.getProfile().forEach(profile -> {
                    references.add(new DependencyInfo(referenceSource, profile.getValue(), profile.getExtension()));
                });
                // trigger[].dataRequirement[].codeFilter[].valueSet
                eventData.getCodeFilter().stream().filter(cf -> cf.hasValueSet()).forEach(cf -> {
                    references.add(dependencyFromDataRequirementCodeFilter(cf));
                });
            });
            Stream.concat(action.getInput().stream(), action.getOutput().stream())
                    .forEach(inputOrOutput -> {
                        // ..input[].profile[]
                        // ..output[].profile[]
                        inputOrOutput.getProfile().forEach(profile -> {
                            references.add(
                                    new DependencyInfo(referenceSource, profile.getValue(), profile.getExtension()));
                        });
                        // input[].codeFilter[].valueSet
                        // output[].codeFilter[].valueSet
                        inputOrOutput.getCodeFilter().forEach(cf -> {
                            references.add(dependencyFromDataRequirementCodeFilter(cf));
                        });
                    });
        });
        this.getPlanDefinition().getExtension().stream()
                .filter(ext -> ext.getUrl().contains("cpg-partOf"))
                .filter(ext -> ext.hasValue())
                .findAny()
                .ifPresent(ext -> {
                    references.add(new DependencyInfo(
                            referenceSource, ((UriType) ext.getValue()).getValue(), ext.getExtension()));
                });
        // TODO: Ideally use $data-requirements code

        return references;
    }

    private DependencyInfo dependencyFromDataRequirementCodeFilter(DataRequirementCodeFilterComponent cf) {
        var vs = cf.getValueSet();
        if (vs instanceof StringType) {
            return new DependencyInfo(this.planDefinition.getUrl(), ((StringType) vs).getValue(), vs.getExtension());
        } else if (vs instanceof Reference) {
            return new DependencyInfo(this.planDefinition.getUrl(), ((Reference) vs).getReference(), vs.getExtension());
        }
        return null;
    }

    @Override
    public Date getApprovalDate() {
        return this.getPlanDefinition().getApprovalDate();
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        this.getPlanDefinition().setApprovalDate(approvalDate);
    }

    @Override
    public Period getEffectivePeriod() {
        return this.getPlanDefinition().getEffectivePeriod();
    }

    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getPlanDefinition().getRelatedArtifact();
    }
}
