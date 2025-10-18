package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface IPlanDefinitionActionAdapter extends IAdapter<IBase> {
    boolean hasId();

    String getId();

    boolean hasTitle();

    String getTitle();

    boolean hasDescription();

    String getDescription();

    boolean hasTextEquivalent();

    String getTextEquivalent();

    boolean hasPriority();

    String getPriority();

    boolean hasCode();

    ICodeableConceptAdapter getCode();

    boolean hasDocumentation();

    <T extends ICompositeType & IBaseHasExtensions> List<T> getDocumentation();

    boolean hasTrigger();

    List<ITriggerDefinitionAdapter> getTrigger();

    List<String> getTriggerType();

    boolean hasCondition();

    List<IBaseBackboneElement> getCondition();

    boolean hasInput();

    List<IDataRequirementAdapter> getInputDataRequirement();

    boolean hasRelatedAction();

    List<IBaseBackboneElement> getRelatedAction();

    boolean hasTiming();

    IBaseDatatype getTiming();

    boolean hasType();

    ICodeableConceptAdapter getType();

    boolean hasSelectionBehavior();

    String getSelectionBehavior();

    boolean hasDefinition();

    IPrimitiveType<String> getDefinition();

    boolean hasAction();

    List<IPlanDefinitionActionAdapter> getAction();

    IRequestActionAdapter newRequestAction();
}
