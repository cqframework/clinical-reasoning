package org.opencds.cqf.fhir.utility.repository.operations

import ca.uhn.fhir.repository.IRepository

interface IQuestionnaireResponseProcessorFactory {
    fun create(repository: IRepository?): IQuestionnaireResponseProcessor?
}
