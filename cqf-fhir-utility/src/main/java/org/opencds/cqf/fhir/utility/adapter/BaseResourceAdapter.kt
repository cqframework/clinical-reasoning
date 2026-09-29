package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.BaseRuntimeElementDefinition
import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Constants

abstract class BaseResourceAdapter
protected constructor(protected open val resource: IBaseResource) :
    BaseAdapter(FhirContext.forCached(resource.structureFhirVersionEnum!!)), IResourceAdapter {

    protected val elementDefinition: BaseRuntimeElementDefinition<*>?
        get() = fhirContext()!!.getElementDefinition(this.resource.javaClass)

    override fun get(): IBaseResource? {
        return resource
    }

    override fun setId(id: IIdType?) {
        setValue(get(), "id", id)
    }

    override fun setValue(path: String, value: Any?) {
        setValue(get(), path, value)
    }

    companion object {
        @JvmStatic
        protected val LIBRARY_TYPES =
            mutableSetOf(
                "logic-library",
                "model-definition",
                "asset-collection",
                "module-definition",
            )

        @JvmStatic
        protected val REFERENCE_EXTENSIONS =
            mutableSetOf(
                Constants.QUESTIONNAIRE_UNIT_VALUE_SET,
                Constants.QUESTIONNAIRE_REFERENCE_PROFILE,
                Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE,
                Constants.SDC_QUESTIONNAIRE_SUB_QUESTIONNAIRE,
                Constants.CQFM_INPUT_PARAMETERS,
                Constants.CQF_EXPANSION_PARAMETERS,
                Constants.CQF_CQL_OPTIONS,
            )

        @JvmStatic
        protected val EXPRESSION_EXTENSIONS =
            mutableSetOf(
                Constants.VARIABLE_EXTENSION,
                Constants.SDC_QUESTIONNAIRE_CANDIDATE_EXPRESSION,
                Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION,
                Constants.SDC_QUESTIONNAIRE_CALCULATED_EXPRESSION,
                Constants.CQF_EXPRESSION,
            )

        @JvmStatic
        protected val CANONICAL_EXTENSIONS =
            mutableSetOf(
                Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS,
                Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS,
            )
    }
}
