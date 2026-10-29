package org.opencds.cqf.fhir.utility.repository.operations

import ca.uhn.fhir.repository.IRepository

interface IPlanDefinitionProcessorFactory {
    fun create(repository: IRepository?): IPlanDefinitionProcessor?
}
