package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RequestGroup.ActionConditionKind;
import org.hl7.fhir.r4.model.RequestGroup.ActionRelationshipType;
import org.hl7.fhir.r4.model.RequestGroup.ActionSelectionBehavior;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionConditionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionRelatedActionComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestPriority;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;

public class RequestActionAdapter extends BaseAdapter implements IRequestActionAdapter {

    private final RequestGroupActionComponent requestAction;

    public RequestActionAdapter(IBase requestAction) {
        super(FhirVersionEnum.R4, requestAction);
        if (!(requestAction instanceof RequestGroupActionComponent)) {
            throw new IllegalArgumentException(
                    "element passed as action argument is not a RequestGroupActionComponent Element");
        }
        this.requestAction = (RequestGroupActionComponent) requestAction;
    }

    @Override
    public RequestGroupActionComponent get() {
        return requestAction;
    }

    @Override
    public String getId() {
        return get().getId();
    }

    @Override
    public IRequestActionAdapter setId(String id) {
        get().setId(id);
        return this;
    }

    @Override
    public boolean hasTitle() {
        return get().hasTitle();
    }

    @Override
    public String getTitle() {
        return get().getTitle();
    }

    @Override
    public IRequestActionAdapter setTitle(String title) {
        get().setTitle(title);
        return this;
    }

    @Override
    public boolean hasDescription() {
        return get().hasDescription();
    }

    @Override
    public String getDescription() {
        return get().getDescription();
    }

    @Override
    public IRequestActionAdapter setDescription(String description) {
        get().setDescription(description);
        return this;
    }

    @Override
    public boolean hasTextEquivalent() {
        return get().hasTextEquivalent();
    }

    @Override
    public String getTextEquivalent() {
        return get().getTextEquivalent();
    }

    @Override
    public IRequestActionAdapter setTextEquivalent(String text) {
        get().setTextEquivalent(text);
        return this;
    }

    @Override
    public boolean hasPriority() {
        return get().hasPriority();
    }

    @Override
    public String getPriority() {
        return get().getPriority().toCode();
    }

    @Override
    public IRequestActionAdapter setPriority(String priority) {
        get().setPriority(RequestPriority.fromCode(priority));
        return this;
    }

    @Override
    public boolean hasCode() {
        return get().hasCode();
    }

    @Override
    public ICodeableConceptAdapter getCode() {
        return getAdapterFactory().createCodeableConcept(get().getCode().get(0));
    }

    @Override
    public IRequestActionAdapter setCode(ICodeableConceptAdapter code) {
        get().setCode(code == null ? null : List.of((CodeableConcept) code.get()));
        return this;
    }

    @Override
    public boolean hasDocumentation() {
        return get().hasDocumentation();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getDocumentation() {
        return get().getDocumentation();
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> IRequestActionAdapter setDocumentation(
            List<T> documentation) {
        get().setDocumentation(
                        documentation.stream().map(RelatedArtifact.class::cast).toList());
        return this;
    }

    @Override
    public boolean hasCondition() {
        return get().hasCondition();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RequestGroupActionConditionComponent> getCondition() {
        return get().getCondition();
    }

    @Override
    public void addCondition(IBaseBackboneElement element) {
        if (element instanceof PlanDefinitionActionConditionComponent condition) {
            get().addCondition(new RequestGroupActionConditionComponent()
                    .setKind(ActionConditionKind.fromCode(condition.getKind().toCode()))
                    .setExpression(condition.getExpression()));
        }
    }

    @Override
    public boolean hasRelatedAction() {
        return get().hasRelatedAction();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RequestGroupActionRelatedActionComponent> getRelatedAction() {
        return get().getRelatedAction();
    }

    @Override
    public void addRelatedAction(IBaseBackboneElement element) {
        if (element instanceof PlanDefinitionActionRelatedActionComponent relatedAction) {
            get().addRelatedAction(new RequestGroupActionRelatedActionComponent()
                    .setActionId(relatedAction.getActionId())
                    .setRelationship(ActionRelationshipType.fromCode(
                            relatedAction.getRelationship().toCode()))
                    .setOffset(relatedAction.getOffset()));
        }
    }

    @Override
    public boolean hasTiming() {
        return get().hasTiming();
    }

    @Override
    public IBaseDatatype getTiming() {
        return get().getTiming();
    }

    @Override
    public IRequestActionAdapter setTiming(IBaseDatatype timing) {
        get().setTiming((Type) timing);
        return this;
    }

    @Override
    public boolean hasType() {
        return get().hasType();
    }

    @Override
    public ICodeableConceptAdapter getType() {
        return new CodeableConceptAdapter(get().getType());
    }

    @Override
    public IRequestActionAdapter setType(ICodeableConceptAdapter type) {
        get().setType((CodeableConcept) type.get());
        return this;
    }

    @Override
    public boolean hasSelectionBehavior() {
        return get().hasSelectionBehavior();
    }

    @Override
    public String getSelectionBehavior() {
        return get().getSelectionBehavior().toCode();
    }

    @Override
    public IRequestActionAdapter setSelectionBehavior(String behavior) {
        get().setSelectionBehavior(ActionSelectionBehavior.fromCode(behavior));
        return this;
    }

    @Override
    public boolean hasResource() {
        return get().hasResource();
    }

    @Override
    public Reference getResource() {
        return get().getResource();
    }

    @Override
    public IRequestActionAdapter setResource(IBaseReference resource) {
        get().setResource((Reference) resource);
        return this;
    }

    @Override
    public boolean hasAction() {
        return get().hasAction();
    }

    @Override
    public List<IRequestActionAdapter> getAction() {
        return get().getAction().stream().map(RequestActionAdapter::new).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void addAction(IBaseBackboneElement element) {
        if (element instanceof RequestGroupActionComponent action) {
            get().addAction(action);
        }
    }

    @Override
    public IRequestActionAdapter setAction(List<IRequestActionAdapter> actions) {
        get().setAction(
                        actions == null
                                ? null
                                : actions.stream()
                                        .map(IRequestActionAdapter::get)
                                        .map(RequestGroupActionComponent.class::cast)
                                        .toList());
        return this;
    }
}
