package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.Resources.getClassForTypeAndVersion

abstract class BaseTerminologyProvider(protected val fhirContext: FhirContext) :
    ITerminologyProvider {

    protected fun getAddressBase(address: String): String {
        return ITerminologyServerClient.getAddressBase(address, this.fhirContext)
    }

    override val codeSystemClass: Class<out IBaseResource>
        get() {
            return getClassForTypeAndVersion("CodeSystem", fhirContext.version.version)
        }

    override val valueSetClass: Class<out IBaseResource>
        get() {
            return getClassForTypeAndVersion("ValueSet", fhirContext.version.version)
        }
}
