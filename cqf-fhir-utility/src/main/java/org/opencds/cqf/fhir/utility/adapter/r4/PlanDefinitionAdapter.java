package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public class PlanDefinitionAdapter extends ResourceAdapter implements KnowledgeArtifactAdapter {

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
    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
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
    public boolean hasVersion() {
        return this.getPlanDefinition().hasVersion();
    }

    @Override
    public void setVersion(String version) {
        this.getPlanDefinition().setVersion(version);
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getPlanDefinition().hasVersion()
                ? this.getPlanDefinition().getUrl() + "|"
                        + this.getPlanDefinition().getVersion()
                : this.getPlanDefinition().getUrl();
        /*
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
        references.addAll(this.getRelatedArtifact().stream()
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .collect(Collectors.toList()));

        // library[]
        List<CanonicalType> libraries = this.getPlanDefinition().getLibrary();
        for (CanonicalType ct : libraries) {
            DependencyInfo dependency = new DependencyInfo(referenceSource, ct.getValue(), ct.getExtension());
            references.add(dependency);
        }
        // action[]
        this.planDefinition.getAction().forEach(action -> {
            action.getTrigger().stream().flatMap(t -> t.getData().stream()).forEach(eventData -> {
                // trigger[].dataRequirement[].profile[]
                eventData.getProfile().forEach(profile -> {
                    references.add(new DependencyInfo(referenceSource, profile.getValue(), profile.getExtension()));
                });
                // trigger[].dataRequirement[].codeFilter[].valueSet
                eventData.getCodeFilter().stream()
                        .filter(cf -> cf.hasValueSet())
                        .forEach(cf -> {
                            references.add(new DependencyInfo(referenceSource, cf.getValueSet(), cf.getExtension()));
                        });
            });
            // condition[].expression.reference
            action.getCondition().stream()
                    .filter(c -> c.hasExpression())
                    .map(c -> c.getExpression())
                    .filter(e -> e.hasReference())
                    .forEach(expression -> {
                        references.add(new DependencyInfo(
                                referenceSource, expression.getReference(), expression.getExtension()));
                    });
            // dynamicValue[].expression.reference
            action.getDynamicValue().stream()
                    .filter(dv -> dv.hasExpression())
                    .map(dv -> dv.getExpression())
                    .filter(e -> e.hasReference())
                    .forEach(expression -> {
                        references.add(new DependencyInfo(
                                referenceSource, expression.getReference(), expression.getExtension()));
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
                        inputOrOutput.getCodeFilter().stream()
                                .filter(cf -> cf.hasValueSet())
                                .forEach(cf -> {
                                    references.add(
                                            new DependencyInfo(referenceSource, cf.getValueSet(), cf.getExtension()));
                                });
                    });
        });
        this.getPlanDefinition().getExtension().stream()
                .filter(ext -> ext.getUrl().contains("cpg-partOf"))
                .filter(ext -> ext.hasValue())
                .findAny()
                .ifPresent(ext -> {
                    references.add(new DependencyInfo(
                            referenceSource, ((CanonicalType) ext.getValue()).getValue(), ext.getExtension()));
                });
        // TODO: Ideally use $data-requirements code

        return references;
    }

    @Override
    public Date getApprovalDate() {
        return this.getPlanDefinition().getApprovalDate();
    }

    @Override
    public Date getDate() {
        return this.getPlanDefinition().getDate();
    }

    @Override
    public void setDate(Date date) {
        this.getPlanDefinition().setDate(date);
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        this.getPlanDefinition().setDateElement((DateTimeType) date);
    }

    @Override
    public Period getEffectivePeriod() {
        return this.getPlanDefinition().getEffectivePeriod();
    }

    @Override
    public void setApprovalDate(Date date) {
        this.getPlanDefinition().setApprovalDate(date);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getPlanDefinition().getRelatedArtifact();
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts) {
        this.getPlanDefinition()
                .setRelatedArtifact(relatedArtifacts.stream()
                        .map(ra -> (RelatedArtifact) ra)
                        .collect(Collectors.toList()));
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
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be org.hl7.fhir.r4.model.Period");
        }
        this.getPlanDefinition().setEffectivePeriod((Period) effectivePeriod);
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus type;
        try {
            type = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.getPlanDefinition().setStatus(type);
    }
}
