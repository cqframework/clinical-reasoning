package org.opencds.cqf.fhir.utility.behavior

import ca.uhn.fhir.context.FhirContext

interface FhirContextUser {
    val fhirContext: FhirContext
}
