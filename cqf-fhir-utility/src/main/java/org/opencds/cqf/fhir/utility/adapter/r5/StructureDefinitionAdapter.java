package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Expression;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.UsageContext;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;

public class StructureDefinitionAdapter extends ResourceAdapter implements IStructureDefinitionAdapter {

    public StructureDefinitionAdapter(IDomainResource structureDefinition) {
        super(structureDefinition);
        if (!(structureDefinition instanceof StructureDefinition)) {
            throw new IllegalArgumentException(
                    "resource passed as planDefinition argument is not a StructureDefinition resource");
        }
    }

    public StructureDefinitionAdapter(StructureDefinition structureDefinition) {
        super(structureDefinition);
    }

    protected StructureDefinition getStructureDefinition() {
        return (StructureDefinition) resource;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<UsageContext> getUseContext() {
        return get().getUseContext();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

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

        if (get().hasBaseDefinition()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    get().getBaseDefinition(),
                    get().getBaseDefinitionElement().getExtension(),
                    reference -> get().setBaseDefinition(reference)));
        }
        get().getExtensionsByUrl(Constants.CPG_ASSERTION_EXPRESSION).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));
        get().getExtensionsByUrl(Constants.CPG_FEATURE_EXPRESSION).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));
        get().getExtensionsByUrl(Constants.CPG_INFERENCE_EXPRESSION).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));
        get().getDifferential()
                .getElement()
                .forEach(element -> getDependenciesOfDifferential(element, references, referenceSource));

        return references;
    }

    private void getDependenciesOfDifferential(
            ElementDefinition element, List<IDependencyInfo> references, String referenceSource) {
        element.getType().forEach(type -> {
            type.getProfile()
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource, profile.getValueAsString(), profile.getExtension(), profile::setValue)));
            type.getTargetProfile()
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource, profile.getValueAsString(), profile.getExtension(), profile::setValue)));
        });
        if (element.getBinding().hasValueSet()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    element.getBinding().getValueSet(),
                    element.getBinding().getExtension(),
                    reference -> element.getBinding().setValueSet(reference)));
        }
    }

    @Override
    public StructureDefinition get() {
        return getStructureDefinition();
    }

    @Override
    public StructureDefinition copy() {
        return get().copy();
    }

    @Override
    public String getUrl() {
        return get().getUrl();
    }

    @Override
    public boolean hasUrl() {
        return get().hasUrl();
    }

    @Override
    public void setUrl(String url) {
        get().setUrl(url);
    }

    @Override
    public void setVersion(String version) {
        get().setVersion(version);
    }

    @Override
    public String getVersion() {
        return get().getVersion();
    }

    @Override
    public boolean hasVersion() {
        return get().hasVersion();
    }

    @Override
    public String getName() {
        return get().getName();
    }

    @Override
    public void setName(String name) {
        get().setName(name);
    }

    @Override
    public Date getApprovalDate() {
        return null;
    }

    @Override
    public Date getDate() {
        return get().getDate();
    }

    @Override
    public void setDate(Date approvalDate) {
        get().setDate(approvalDate);
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        get().setDateElement((DateTimeType) date);
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
        return get().getPurpose();
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
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
        return new ArrayList<>();
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
        get().setStatus(status);
    }

    @Override
    public String getStatus() {
        return get().getStatus() == null ? null : get().getStatus().toCode();
    }

    @Override
    public boolean getExperimental() {
        return get().getExperimental();
    }
}
