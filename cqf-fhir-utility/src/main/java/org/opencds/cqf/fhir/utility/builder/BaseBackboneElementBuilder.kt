package org.opencds.cqf.fhir.utility.builder

import ca.uhn.fhir.context.FhirVersionEnum
import java.util.UUID
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.opencds.cqf.fhir.utility.FhirVersions.forClass
import org.opencds.cqf.fhir.utility.Resources

abstract class BaseBackboneElementBuilder<
    SELF : BaseBackboneElementBuilder<SELF, T>,
    T : IBaseBackboneElement,
> protected constructor(private val resourceClass: Class<T>) {

    protected var id: String? = UUID.randomUUID().toString()
        private set

    private var extensions: MutableList<Pair<String?, CodeableConceptSettings?>?>? = null
    private var modifierExtensions: MutableList<Pair<String?, CodeableConceptSettings?>?>? = null

    protected constructor(resourceClass: Class<T>, id: String) : this(resourceClass) {
        this.id = id
    }

    open fun build(): T {
        val backboneElement: T = Resources.newBackboneElement(resourceClass)

        when (forClass<T?>(resourceClass)) {
            FhirVersionEnum.DSTU3 -> initializeDstu3(backboneElement)
            FhirVersionEnum.R4 -> initializeR4(backboneElement)
            FhirVersionEnum.R5 -> initializeR5(backboneElement)
            else ->
                throw IllegalArgumentException(
                    "ResourceBuilder.initializeResource does not support FHIR version ${forClass(resourceClass).fhirVersionString}"
                )
        }

        return backboneElement
    }

    private fun addExtension(extension: Pair<String?, CodeableConceptSettings?>?) {
        if (this.extensions == null) {
            this.extensions = mutableListOf()
        }
        this.extensions!!.add(extension)
    }

    private fun getExtensions(): MutableList<Pair<String?, CodeableConceptSettings?>?> {
        if (extensions == null) {
            return mutableListOf()
        }
        return extensions!!
    }

    private fun addModifierExtension(modifierExtension: Pair<String?, CodeableConceptSettings?>?) {
        if (this.modifierExtensions == null) {
            this.modifierExtensions = mutableListOf()
        }
        this.modifierExtensions!!.add(modifierExtension)
    }

    private fun getModifierExtensions(): MutableList<Pair<String?, CodeableConceptSettings?>?> {
        if (modifierExtensions == null) {
            return mutableListOf()
        }
        return modifierExtensions!!
    }

    fun withId(id: String): SELF {
        this.id = id

        return self()
    }

    fun withExtension(extension: Pair<String?, CodeableConceptSettings?>): SELF {

        addExtension(extension)

        return self()
    }

    fun withModifierExtension(modifierExtension: Pair<String?, CodeableConceptSettings?>): SELF {

        addModifierExtension(modifierExtension)

        return self()
    }

    protected fun self(): SELF {
        return this as SELF
    }

    protected open fun initializeDstu3(resource: T?) {
        getExtensions().forEach { extensionSetting ->
            extensionSetting!!.second!!.codingSettings.forEach { coding ->
                val codeableConcept =
                    org.hl7.fhir.dstu3.model
                        .CodeableConcept()
                        .addCoding(
                            org.hl7.fhir.dstu3.model
                                .Coding()
                                .setSystem(coding!!.system)
                                .setCode(coding.code)
                                .setDisplay(coding.display)
                        )
                val extension = resource!!.addExtension()
                extension.url = extensionSetting.first
                extension.value = codeableConcept
            }
        }

        getModifierExtensions().forEach { extensionSetting ->
            extensionSetting!!.second!!.codingSettings.forEach { coding ->
                val codeableConcept =
                    org.hl7.fhir.dstu3.model
                        .CodeableConcept()
                        .addCoding(
                            org.hl7.fhir.dstu3.model
                                .Coding()
                                .setSystem(coding!!.system)
                                .setCode(coding.code)
                                .setDisplay(coding.display)
                        )
                val modifierExtension = resource!!.addModifierExtension()
                modifierExtension.url = extensionSetting.first
                modifierExtension.value = codeableConcept
            }
        }
    }

    protected open fun initializeR4(resource: T?) {
        getExtensions().forEach { extensionSetting ->
            extensionSetting!!.second!!.codingSettings.forEach { coding ->
                val codeableConcept =
                    org.hl7.fhir.r4.model
                        .CodeableConcept()
                        .addCoding(
                            org.hl7.fhir.r4.model
                                .Coding()
                                .setSystem(coding!!.system)
                                .setCode(coding.code)
                                .setDisplay(coding.display)
                        )
                val extension = resource!!.addExtension()
                extension.url = extensionSetting.first
                extension.value = codeableConcept
            }
        }

        getModifierExtensions().forEach { extensionSetting ->
            extensionSetting!!.second!!.codingSettings.forEach { coding: CodingSettings? ->
                val codeableConcept =
                    org.hl7.fhir.r4.model
                        .CodeableConcept()
                        .addCoding(
                            org.hl7.fhir.r4.model
                                .Coding()
                                .setSystem(coding!!.system)
                                .setCode(coding.code)
                                .setDisplay(coding.display)
                        )
                val modifierExtension = resource!!.addModifierExtension()
                modifierExtension.url = extensionSetting.first
                modifierExtension.value = codeableConcept
            }
        }
    }

    protected open fun initializeR5(resource: T?) {
        getExtensions().forEach { extensionSetting ->
            extensionSetting!!.second!!.codingSettings.forEach { coding ->
                val codeableConcept =
                    org.hl7.fhir.r5.model
                        .CodeableConcept()
                        .addCoding(
                            org.hl7.fhir.r5.model
                                .Coding()
                                .setSystem(coding!!.system)
                                .setCode(coding.code)
                                .setDisplay(coding.display)
                        )
                val extension = resource!!.addExtension()
                extension.url = extensionSetting.first
                extension.value = codeableConcept
            }
        }

        getModifierExtensions().forEach { extensionSetting ->
            extensionSetting!!.second!!.codingSettings.forEach { coding ->
                val codeableConcept =
                    org.hl7.fhir.r5.model
                        .CodeableConcept()
                        .addCoding(
                            org.hl7.fhir.r5.model
                                .Coding()
                                .setSystem(coding!!.system)
                                .setCode(coding.code)
                                .setDisplay(coding.display)
                        )
                val modifierExtension = resource!!.addModifierExtension()
                modifierExtension.url = extensionSetting.first
                modifierExtension.value = codeableConcept
            }
        }
    }
}
