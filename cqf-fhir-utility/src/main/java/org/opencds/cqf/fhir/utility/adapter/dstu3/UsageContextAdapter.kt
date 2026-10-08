package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.CodeableConcept
import org.hl7.fhir.dstu3.model.Coding
import org.hl7.fhir.dstu3.model.UsageContext
import org.hl7.fhir.instance.model.api.IBase
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter
import org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter

class UsageContextAdapter(usageContext: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, usageContext), IUsageContextAdapter {
    private val usageContext: UsageContext

    init {
        require(usageContext is UsageContext) {
            "object passed as coding argument is not a UsageContext data type"
        }
        this.usageContext = usageContext
    }

    override fun get(): UsageContext {
        return usageContext
    }

    override fun hasCode(): Boolean {
        return usageContext.hasCode()
    }

    override val code: ICodingAdapter?
        get() {
            if (usageContext.code == null) return null
            return CodingAdapter(get().code)
        }

    override fun setCode(code: ICodingAdapter?): IUsageContextAdapter {
        get().code = code!!.get() as Coding?
        return this
    }

    override fun hasValue(): Boolean {
        return usageContext.hasValue()
    }

    override fun hasValueCodeableConcept(): Boolean {
        return usageContext.hasValue() && usageContext.value is CodeableConcept
    }

    override val valueCodeableConcept: ICodeableConceptAdapter?
        get() {
            if (!hasValueCodeableConcept()) return null
            val valueCodeableConcept = usageContext.valueCodeableConcept
            return CodeableConceptAdapter(valueCodeableConcept)
        }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun equalsDeep(obj: IUsageContextAdapter?): Boolean {
        if (obj !is UsageContextAdapter) return false
        return get().equalsDeep(obj.get())
    }
}
