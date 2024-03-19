package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

class PlanDefinitionAdapter extends ResourceAdapter implements KnowledgeArtifactAdapter {

    private PlanDefinition planDefinition;

    public PlanDefinitionAdapter(IDomainResource planDefinition) {
        super(planDefinition);

        if (!(planDefinition instanceof PlanDefinition)) {
            throw new IllegalArgumentException(
                    "resource passed as planDefinition argument is not a PlanDefinition resource");
        }

        this.planDefinition = (PlanDefinition) planDefinition;
    }

    public PlanDefinitionAdapter(PlanDefinition planDefinition) {
        super(planDefinition);
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
    public PlanDefinition copy() {
        return this.get().copy();
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
    public boolean hasUrl() {
        return this.getPlanDefinition().hasUrl();
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
        final String referenceSource = this.hasVersion()
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
        references.addAll(this.getRelatedArtifact().stream()
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .collect(Collectors.toList()));

        // library[]
        List<Reference> libraries = this.planDefinition.getLibrary();
        for (Reference ref : libraries) {
            // TODO: Account for reference.identifier?
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource,
                    ref.getReference(),
                    ref.getExtension(),
                    (reference) -> ref.setReference(reference));
            references.add(dependency);
        }
        // action[]
        this.planDefinition.getAction().forEach(action -> {
            action.getTriggerDefinition().stream().map(t -> t.getEventData()).forEach(eventData -> {
                // trigger[].dataRequirement[].profile[]
                eventData.getProfile().forEach(profile -> {
                    references.add(new DependencyInfo(
                            referenceSource,
                            profile.getValue(),
                            profile.getExtension(),
                            (reference) -> profile.setValue(reference)));
                });
                // trigger[].dataRequirement[].codeFilter[].valueSet
                eventData.getCodeFilter().stream()
                        .filter(cf -> cf.hasValueSet())
                        .forEach(cf -> {
                            references.add(dependencyFromDataRequirementCodeFilter(cf));
                        });
            });
            Stream.concat(action.getInput().stream(), action.getOutput().stream())
                    .forEach(inputOrOutput -> {
                        // ..input[].profile[]
                        // ..output[].profile[]
                        inputOrOutput.getProfile().forEach(profile -> {
                            references.add(new DependencyInfo(
                                    referenceSource,
                                    profile.getValue(),
                                    profile.getExtension(),
                                    (reference) -> profile.setValue(reference)));
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
                            referenceSource,
                            ((UriType) ext.getValue()).getValue(),
                            ext.getExtension(),
                            (reference) -> ((UriType) ext.getValue()).setValue(reference)));
                });
        // TODO: Ideally use $data-requirements code

        return references;
    }

    private DependencyInfo dependencyFromDataRequirementCodeFilter(DataRequirementCodeFilterComponent cf) {
        var vs = cf.getValueSet();
        if (vs instanceof StringType) {
            return new DependencyInfo(
                    this.planDefinition.getUrl(),
                    ((StringType) vs).getValue(),
                    vs.getExtension(),
                    (reference) -> ((StringType) vs).setValue(reference));
        } else if (vs instanceof Reference) {
            return new DependencyInfo(
                    this.planDefinition.getUrl(),
                    ((Reference) vs).getReference(),
                    vs.getExtension(),
                    (reference) -> ((Reference) vs).setReference(reference));
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
    public boolean hasRelatedArtifact() {
        return this.getPlanDefinition().hasRelatedArtifact();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getPlanDefinition().getRelatedArtifact();
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
        this.getPlanDefinition()
                .setRelatedArtifact(relatedArtifacts.stream()
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

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be " + Period.class.getName());
        }
        this.getPlanDefinition().setEffectivePeriod((Period) effectivePeriod);
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus status;
        try {
            status = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.getPlanDefinition().setStatus(status);
    }

    @Override
    public String getStatus() {
        return this.getPlanDefinition().getStatus() == null
                ? null
                : this.getPlanDefinition().getStatus().toCode();
    }

    @Override
    public boolean getExperimental() {
        return this.getPlanDefinition().getExperimental();
    }

    @Override
    public void setExtension(List<IBaseExtension<?, ?>> extensions) {
        this.get().setExtension(extensions.stream().map(e -> (Extension) e).collect(Collectors.toList()));
    }
}
