package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Constants.CqfApplicabilityBehavior;

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

    <T extends IBaseBackboneElement> List<T> getCondition();

    boolean hasInput();

    List<IDataRequirementAdapter> getInputDataRequirement();

    boolean hasRelatedAction();

    <T extends IBaseBackboneElement> List<T> getRelatedAction();

    boolean hasTiming();

    IBaseDatatype getTiming();

    boolean hasType();

    ICodeableConceptAdapter getType();

    // These will need to be overridden starting with R6 when this is introduced as an element on action
    default boolean hasApplicabilityBehavior() {
        return hasExtension(Constants.CQF_APPLICABILITY_BEHAVIOR);
    }

    // These will need to be overridden starting with R6 when this is introduced as an element on action
    default CqfApplicabilityBehavior getApplicabilityBehavior() {
        var extension = getExtensionByUrl(Constants.CQF_APPLICABILITY_BEHAVIOR);
        if (extension != null && extension.getValue() instanceof IPrimitiveType<?> primitiveType) {
            try {
                return CqfApplicabilityBehavior.valueOf(
                        primitiveType.getValueAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Encountered invalid value for applicabilityBehavior extension %s.  Expected `all` or `any`."
                                .formatted(primitiveType.getValueAsString()));
            }
        }
        return CqfApplicabilityBehavior.ALL;
    }

    boolean hasSelectionBehavior();

    String getSelectionBehavior();

    boolean hasDefinition();

    IPrimitiveType<String> getDefinition();

    boolean hasAction();

    List<IPlanDefinitionActionAdapter> getAction();

    IRequestActionAdapter newRequestAction();
}
