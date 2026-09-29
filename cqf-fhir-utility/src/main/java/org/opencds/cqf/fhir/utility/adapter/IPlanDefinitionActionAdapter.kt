package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.Constants.CqfApplicabilityBehavior

interface IPlanDefinitionActionAdapter : IAdapter<IBase> {
    fun hasId(): Boolean

    val id: String?

    fun hasTitle(): Boolean

    val title: String?

    fun hasDescription(): Boolean

    val description: String?

    fun hasTextEquivalent(): Boolean

    val textEquivalent: String?

    fun hasPriority(): Boolean

    val priority: String?

    fun hasCode(): Boolean

    val code: ICodeableConceptAdapter?

    fun hasDocumentation(): Boolean

    fun <T> getDocumentation(): MutableList<T?>? where T : ICompositeType, T : IBaseHasExtensions

    fun hasTrigger(): Boolean

    val trigger: MutableList<ITriggerDefinitionAdapter?>?

    val triggerType: MutableList<String?>?

    fun hasCondition(): Boolean

    fun <T : IBaseBackboneElement> getCondition(): MutableList<T?>?

    fun hasInput(): Boolean

    val inputDataRequirement: MutableList<IDataRequirementAdapter?>?

    fun hasRelatedAction(): Boolean

    fun <T : IBaseBackboneElement> getRelatedAction(): MutableList<T?>?

    fun hasTiming(): Boolean

    val timing: IBaseDatatype?

    fun hasType(): Boolean

    val type: ICodeableConceptAdapter?

    // These will need to be overridden starting with R6 when this is introduced as an element on
    // action
    fun hasApplicabilityBehavior(): Boolean {
        return hasExtension(Constants.CQF_APPLICABILITY_BEHAVIOR)
    }

    val applicabilityBehavior: Constants.CqfApplicabilityBehavior
        // These will need to be overridden starting with R6 when this is introduced as an element
        // on action
        get() {
            val extension =
                getExtensionByUrl<IBaseExtension<*, *>>(Constants.CQF_APPLICABILITY_BEHAVIOR)
            if (extension != null && extension.value is IPrimitiveType<*>) {
                try {
                    return CqfApplicabilityBehavior.valueOf(
                        (extension.value as IPrimitiveType<*>).valueAsString.uppercase()
                    )
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException(
                        "Encountered invalid value for applicabilityBehavior extension ${(extension.value as IPrimitiveType<*>).valueAsString}.  Expected `all` or `any`."
                    )
                }
            }
            return CqfApplicabilityBehavior.ALL
        }

    fun hasSelectionBehavior(): Boolean

    val selectionBehavior: String?

    fun hasDefinition(): Boolean

    val definition: IPrimitiveType<String?>?

    fun hasAction(): Boolean

    val action: MutableList<IPlanDefinitionActionAdapter?>?

    fun newRequestAction(): IRequestActionAdapter?
}
