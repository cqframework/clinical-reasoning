package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.hl7.fhir.r4.model.DataRequirement
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementCodeFilterAdapter

class DataRequirementAdapter(compositeType: IBase) :
    BaseElementAdapter(FhirVersionEnum.R4, compositeType), IDataRequirementAdapter {
    private val dataRequirement: DataRequirement

    init {
        require(compositeType is DataRequirement) {
            "object passed as dataRequirement argument is not a DataRequirement data type"
        }
        this.dataRequirement = compositeType
    }

    override fun get(): DataRequirement {
        return dataRequirement
    }

    override fun hasId(): Boolean {
        return get().hasId()
    }

    override val id: String?
        get() {
            return get().id
        }

    override fun hasType(): Boolean {
        return get().hasType()
    }

    override val type: String?
        get() {
            return get().type
        }

    override fun hasProfile(): Boolean {
        return get().hasProfile()
    }

    override val profile: MutableList<IPrimitiveType<String?>?>
        get() {
            return get().profile.map { p -> p as IPrimitiveType<String?>? }.toMutableList()
        }

    override fun hasCodeFilter(): Boolean {
        return get().hasCodeFilter()
    }

    override val codeFilter: MutableList<IDataRequirementCodeFilterAdapter?>
        get() {
            return get()
                .codeFilter
                .map { codeFilter -> DataRequirementCodeFilterAdapter(codeFilter) }
                .toMutableList()
        }
}
