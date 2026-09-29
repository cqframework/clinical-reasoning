package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.UsageContext
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter
import org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter

class UsageContextAdapter(usageContext: IBase) :
    BaseElementAdapter(FhirVersionEnum.R4, usageContext), IUsageContextAdapter {
    private val usageContext: UsageContext?

    init {
        require(usageContext is UsageContext) {
            "object passed as coding argument is not a UsageContext data type"
        }
        this.usageContext = usageContext
    }

    override fun get(): UsageContext? {
        return usageContext
    }

    override fun hasCode(): Boolean {
        return usageContext!!.hasCode()
    }

    override val code: ICodingAdapter?
        get() {
            if (usageContext == null || usageContext.code == null) return null
            return CodingAdapter(get()!!.code)
        }

    override fun setCode(code: ICodingAdapter?): IUsageContextAdapter {
        get()!!.setCode(code!!.get() as Coding?)
        return this
    }

    override fun hasValue(): Boolean {
        return usageContext!!.hasValue()
    }

    override fun hasValueCodeableConcept(): Boolean {
        return usageContext != null &&
            usageContext.hasValue() &&
            usageContext.value is CodeableConcept
    }

    override val valueCodeableConcept: ICodeableConceptAdapter?
        get() {
            if (!hasValueCodeableConcept()) return null
            val valueCodeableConcept = usageContext!!.valueCodeableConcept
            return CodeableConceptAdapter(valueCodeableConcept)
        }

    override fun equalsDeep(obj: IUsageContextAdapter?): Boolean {
        if (obj !is UsageContextAdapter) return false
        return get()!!.equalsDeep(obj.get())
    }
}
