package org.opencds.cqf.fhir.utility.builder

import org.hl7.fhir.instance.model.api.IDomainResource

abstract class BaseDomainResourceBuilder<SELF, T : IDomainResource> : BaseResourceBuilder<SELF, T> {
    private var extension: MutableList<Pair<String?, CodeableConceptSettings?>?>? = null
    private var modifierExtension: MutableList<Pair<String?, CodeableConceptSettings?>?>? = null

    protected constructor(resourceClass: Class<T>) : super(resourceClass)

    protected constructor(resourceClass: Class<T>, id: String) : super(resourceClass, id)

    private fun addExtension(extension: Pair<String?, CodeableConceptSettings?>?) {
        if (this.extension == null) {
            this.extension = mutableListOf()
        }
        this.extension!!.add(extension)
    }

    private val extensions: MutableList<Pair<String?, CodeableConceptSettings?>?>
        get() {
            if (extension == null) {
                return mutableListOf()
            }
            return extension!!
        }

    private fun addModifierExtension(modifierExtension: Pair<String?, CodeableConceptSettings?>?) {
        if (this.modifierExtension == null) {
            this.modifierExtension = mutableListOf()
        }
        this.modifierExtension!!.add(modifierExtension)
    }

    private val modifierExtensions: MutableList<Pair<String?, CodeableConceptSettings?>?>
        get() {
            if (modifierExtension == null) {
                return mutableListOf()
            }
            return modifierExtension!!
        }

    fun withExtension(extension: Pair<String?, CodeableConceptSettings?>): SELF {

        addExtension(extension)

        return self()
    }

    fun withModifierExtension(modifierExtension: Pair<String?, CodeableConceptSettings?>): SELF {

        addModifierExtension(modifierExtension)

        return self()
    }

    override fun self(): SELF {
        return this as SELF
    }

    override fun initializeDstu3(resource: T?) {
        super.initializeDstu3(resource)

        this.extensions.forEach { extensionSetting ->
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

        this.modifierExtensions.forEach { extensionSetting ->
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

    override fun initializeR4(resource: T?) {
        super.initializeR4(resource)

        this.extensions.forEach { extensionSetting ->
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

        this.modifierExtensions.forEach { extensionSetting ->
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
                val modifierExtension = resource!!.addModifierExtension()
                modifierExtension.url = extensionSetting.first
                modifierExtension.value = codeableConcept
            }
        }
    }

    override fun initializeR5(resource: T?) {
        super.initializeR5(resource)

        this.extensions.forEach { extensionSetting ->
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

        this.modifierExtensions.forEach { extensionSetting ->
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
