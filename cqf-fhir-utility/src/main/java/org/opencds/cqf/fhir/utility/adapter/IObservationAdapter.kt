package org.opencds.cqf.fhir.utility.adapter

import java.util.*
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.VersionUtilities.dateTimeTypeForVersion
import org.opencds.cqf.fhir.utility.VersionUtilities.instantTypeForVersion

interface IObservationAdapter : IResourceAdapter {
    fun setBasedOn(basedOn: MutableList<IBaseReference?>?): IObservationAdapter {
        setValue("basedOn", basedOn)
        return this
    }

    fun setPartOf(partOf: MutableList<IBaseReference?>?): IObservationAdapter {
        setValue("partOf", partOf)
        return this
    }

    fun setDerivedFrom(derivedFrom: MutableList<IBaseReference?>?): IObservationAdapter {
        setValue("derivedFrom", derivedFrom)
        return this
    }

    fun setStatus(status: String?): IObservationAdapter {
        setValue("status", status)
        return this
    }

    val status: String?
        get() = resolvePathString("status")

    fun setCode(codeableConcept: ICompositeType?): IObservationAdapter {
        setValue("code", codeableConcept)
        return this
    }

    val code: ICodeableConceptAdapter?
        get() {
            val code = resolvePath("code", IBase::class.java)
            return if (code == null) null else adapterFactory!!.createCodeableConcept(code)
        }

    fun setCategory(category: MutableList<ICompositeType?>?): IObservationAdapter {
        setValue("category", category)
        return this
    }

    fun setSubject(subject: IBaseReference?): IObservationAdapter {
        setValue("subject", subject)
        return this
    }

    fun setEncounter(encounter: IBaseReference?): IObservationAdapter {
        setValue("encounter", encounter)
        return this
    }

    fun setEffective(effective: String?): IObservationAdapter {
        return setEffective(dateTimeTypeForVersion(fhirVersion()!!, effective))
    }

    fun setEffective(effective: IBaseDatatype?): IObservationAdapter {
        if (effective is IPrimitiveType<*> && effective.value is Date) {
            setValue("effectiveDateTime", effective)
        }
        return this
    }

    fun setEffectivePeriod(period: ICompositeType?): IObservationAdapter {
        setValue(get(), "effectivePeriod", period)
        return this
    }

    fun setIssued(issued: String?): IObservationAdapter {
        return setIssued(instantTypeForVersion(fhirVersion()!!, issued))
    }

    fun setIssued(issued: IBaseDatatype?): IObservationAdapter {
        if (issued is IPrimitiveType<*> && issued.fhirType() == "instant" && issued.value is Date) {
            setValue("issued", issued)
        }
        return this
    }

    fun setPerformer(performer: MutableList<IBaseReference?>?): IObservationAdapter {
        setValue("performer", performer)
        return this
    }

    fun setValue(value: IBaseDatatype?): IObservationAdapter {
        setValue("value", value)
        return this
    }
}
