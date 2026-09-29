package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.hl7.fhir.r4.model.DataRequirement
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementCodeFilterAdapter

class DataRequirementCodeFilterAdapter(codeFilter: IBase) :
    BaseElementAdapter(FhirVersionEnum.R4, codeFilter), IDataRequirementCodeFilterAdapter {
    private val codeFilter: DataRequirement.DataRequirementCodeFilterComponent

    init {
        require(codeFilter is DataRequirement.DataRequirementCodeFilterComponent) {
            "object passed as codeFilter argument is not a DataRequirementCodeFilterComponent data type"
        }
        this.codeFilter = codeFilter
    }

    override fun get(): DataRequirement.DataRequirementCodeFilterComponent {
        return codeFilter
    }

    override fun hasCode(): Boolean {
        return get().hasCode()
    }

    override val code: MutableList<ICodingAdapter?>
        get() {
            return get().code.map { coding -> CodingAdapter(coding) }.toMutableList()
        }

    override fun hasPath(): Boolean {
        return get().hasPath()
    }

    override val path: String?
        get() {
            return get().path
        }

    override fun hasValueSet(): Boolean {
        return get().hasValueSet()
    }

    override val valueSet: IPrimitiveType<String?>?
        get() {
            return get().valueSetElement
        }
}
