package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.ResultSeverityEnum
import kotlin.RuntimeException
import kotlin.collections.MutableMap
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.ImplementationGuide

class ResourceValidator : IResourceValidator {
    protected var repo: IRepository?
    protected var context: FhirContext?
    protected var validator: FhirValidator? = null
    protected var profiles: MutableMap<String?, ValidationProfile?>

    /**
     * Creates a ResourceValidator with an externally-configured FhirValidator. Use this constructor
     * when you want to provide your own pre-configured validator (e.g., with custom validation
     * support chain, terminology services, etc.).
     *
     * @param validator the pre-configured FhirValidator to use
     * @param profiles optional validation profiles for filtering error messages
     */
    constructor(validator: FhirValidator, profiles: MutableMap<String?, ValidationProfile?>?) {
        this.validator = validator
        this.profiles = profiles ?: mutableMapOf()
        this.repo = null
        this.context = null
    }

    /**
     * Creates a ResourceValidator that bootstraps its own FhirValidator based on the provided
     * FhirContext and profiles.
     *
     * @param context the FhirContext to use
     * @param profiles validation profiles to load from the repository
     * @param repo the repository to load ImplementationGuide resources from
     */
    constructor(
        context: FhirContext?,
        profiles: MutableMap<String?, ValidationProfile?>?,
        repo: IRepository?,
    ) {
        this.repo = repo
        this.context = context
        this.profiles = profiles ?: mutableMapOf()
        setValidator()
    }

    /**
     * Creates a ResourceValidator that bootstraps its own FhirValidator based on the provided FHIR
     * version and profiles.
     *
     * @param version the FHIR version to use
     * @param profiles validation profiles to load from the repository
     * @param repo the repository to load ImplementationGuide resources from
     */
    constructor(
        version: FhirVersionEnum?,
        profiles: MutableMap<String?, ValidationProfile?>?,
        repo: IRepository?,
    ) {
        this.repo = repo
        this.context = FhirContext.forCached(version)
        this.profiles = profiles ?: mutableMapOf()
        setValidator()
    }

    protected fun setValidator() {
        if (this.profiles.isEmpty()) {
            this.validator = this.context!!.newValidator()
        } else {
            val supportChain = ValidationSupportChain()
            supportChain.addValidationSupport(DefaultProfileValidationSupport(this.context))
            supportChain.addValidationSupport(CommonCodeSystemsTerminologyService(this.context))
            supportChain.addValidationSupport(
                InMemoryTerminologyServerValidationSupport(this.context)
            )

            val profileSupport = PrePopulatedValidationSupport(this.context)
            for (profile in this.profiles.entries) {
                val ig =
                    this.repo!!.read(
                        ImplementationGuide::class.java,
                        IdType("ImplementationGuide", profile.value!!.name),
                    )
                if (ig == null) {
                    continue
                }
                for (resourceComponent in ig.definition.resource) {
                    if (
                        listOf("CodeSystem", "StructureDefinition", "ValueSet")
                            .contains(
                                resourceComponent.reference.reference
                                    .split("/")
                                    .dropLastWhile { it.isEmpty() }[0]
                            )
                    ) {
                        try {
                            val resource =
                                this.repo!!.read(
                                    IBaseResource::class.java,
                                    IdType(resourceComponent.reference.reference),
                                )
                            if (resource != null) {
                                profileSupport.addResource(resource)
                            }
                        } catch (e: Exception) {
                            // TODO: Log exception
                        }
                    }
                }
            }

            supportChain.addValidationSupport(profileSupport)

            this.validator =
                this.context!!
                    .newValidator()
                    .registerValidatorModule(FhirInstanceValidator(supportChain))
        }
    }

    override fun validate(resource: IBaseResource?): IBaseResource? {
        return this.validate(resource, false)
    }

    override fun validate(resource: IBaseResource?, error: Boolean?): IBaseResource? {
        val validationResult = this.validator!!.validateWithResult(resource)
        val errors =
            validationResult.messages.filter { m ->
                m.severity.compareTo(ResultSeverityEnum.ERROR) > -1 &&
                    this.profiles.entries
                        .flatMap { p -> p.value!!.getIgnoreKeys() }
                        .none { s -> m.message.contains(s!!) }
            }

        if (errors.isEmpty()) {
            return resource
        }

        if (true == error) {
            val messages = errors.map { obj -> obj.message }
            val issues = messages.joinToString("; ")
            throw RuntimeException(
                "Unable to validate resource. The following problems were found: $issues"
            )
        } else {
            return validationResult.toOperationOutcome()
        }
    }
}
