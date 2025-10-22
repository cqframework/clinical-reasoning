package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionRelatedActionComponent;
import org.hl7.fhir.dstu3.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ITriggerDefinitionAdapter;

public class PlanDefinitionActionAdapter extends BaseAdapter implements IPlanDefinitionActionAdapter {

    private final PlanDefinitionActionComponent action;

    public PlanDefinitionActionAdapter(IBase action) {
        super(FhirVersionEnum.DSTU3, action);
        if (!(action instanceof PlanDefinitionActionComponent)) {
            throw new IllegalArgumentException(
                    "object passed as action argument is not a PlanDefinitionActionComponent data type");
        }
        this.action = (PlanDefinitionActionComponent) action;
    }

    @Override
    public PlanDefinitionActionComponent get() {
        return action;
    }

    @Override
    public boolean hasId() {
        return get().hasId();
    }

    @Override
    public String getId() {
        return get().getId();
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
    public boolean hasDescription() {
        return get().hasDescription();
    }

    @Override
    public String getDescription() {
        return get().getDescription();
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
    public boolean hasPriority() {
        return false;
    }

    @Override
    public String getPriority() {
        return null;
    }

    @Override
    public boolean hasCode() {
        return get().hasCode();
    }

    @Override
    public ICodeableConceptAdapter getCode() {
        if (hasCode()) {
            return getAdapterFactory().createCodeableConcept(get().getCode().get(0));
        } else {
            return null;
        }
    }

    @Override
    public boolean hasDocumentation() {
        return get().hasDocumentation();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ICompositeType & IBaseHasExtensions> List<T> getDocumentation() {
        return get().getDocumentation().stream().map(d -> (T) d).toList();
    }

    @Override
    public boolean hasTrigger() {
        return get().hasTriggerDefinition();
    }

    @Override
    public List<ITriggerDefinitionAdapter> getTrigger() {
        return get().getTriggerDefinition().stream()
                .map(TriggerDefinitionAdapter::new)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<String> getTriggerType() {
        return get().getTriggerDefinition().stream()
                .map(t -> t.getType().toCode())
                .toList();
    }

    @Override
    public boolean hasCondition() {
        return get().hasCondition();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PlanDefinitionActionConditionComponent> getCondition() {
        return get().getCondition();
    }

    @Override
    public boolean hasInput() {
        return get().hasInput();
    }

    @Override
    public List<IDataRequirementAdapter> getInputDataRequirement() {
        return get().getInput().stream()
                .map(getAdapterFactory()::createDataRequirement)
                .toList();
    }

    @Override
    public boolean hasRelatedAction() {
        return get().hasRelatedAction();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<PlanDefinitionActionRelatedActionComponent> getRelatedAction() {
        return get().getRelatedAction();
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
    public boolean hasType() {
        return get().hasType();
    }

    @Override
    public ICodeableConceptAdapter getType() {
        return getAdapterFactory().createCodeableConcept(new CodeableConcept().addCoding(get().getType()));
    }

    @Override
    public boolean hasSelectionBehavior() {
        return get().hasSelectionBehavior();
    }

    @Override
    public String getSelectionBehavior() {
        if (hasSelectionBehavior()) {
            return get().getSelectionBehavior().toCode();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasDefinition() {
        return get().hasDefinition();
    }

    @Override
    public IPrimitiveType<String> getDefinition() {
        if (hasDefinition() && get().getDefinition().hasReference()) {
            return get().getDefinition().getReferenceElement_();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasAction() {
        return get().hasAction();
    }

    @Override
    public List<IPlanDefinitionActionAdapter> getAction() {
        return get().getAction().stream()
                .map(getAdapterFactory()::createPlanDefinitionAction)
                .toList();
    }

    @Override
    public IRequestActionAdapter newRequestAction() {
        return getAdapterFactory().createRequestAction(new RequestGroupActionComponent());
    }
}
