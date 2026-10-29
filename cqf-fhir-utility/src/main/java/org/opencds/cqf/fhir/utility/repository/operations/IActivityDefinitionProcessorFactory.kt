package org.opencds.cqf.fhir.utility.repository.operations

import ca.uhn.fhir.repository.IRepository

fun interface IActivityDefinitionProcessorFactory {
    fun create(repository: IRepository?): IActivityDefinitionProcessor?
}
