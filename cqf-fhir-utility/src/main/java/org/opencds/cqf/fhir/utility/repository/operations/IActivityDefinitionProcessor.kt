package org.opencds.cqf.fhir.utility.repository.operations

import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.monad.Either3

interface IActivityDefinitionProcessor {
    fun <C : IPrimitiveType<String?>, R : IBaseResource> apply(
        activityDefinition: Either3<C, IIdType, R>?,
        subjectId: String?,
        encounterId: String?,
        practitionerId: String?,
        organizationId: String?,
        userType: IBaseDatatype?,
        userLanguage: IBaseDatatype?,
        userTaskContext: IBaseDatatype?,
        setting: IBaseDatatype?,
        settingContext: IBaseDatatype?,
    ): IBaseResource?

    fun <C : IPrimitiveType<String?>, R : IBaseResource> apply(
        activityDefinition: Either3<C, IIdType, R>?,
        subjectId: String?,
        encounterId: String?,
        practitionerId: String?,
        organizationId: String?,
        userType: IBaseDatatype?,
        userLanguage: IBaseDatatype?,
        userTaskContext: IBaseDatatype?,
        setting: IBaseDatatype?,
        settingContext: IBaseDatatype?,
        parameters: IBaseParameters?,
        useServerData: Boolean,
        data: IBaseBundle?,
        dataEndpoint: IBaseResource?,
        contentEndpoint: IBaseResource?,
        terminologyEndpoint: IBaseResource?,
    ): IBaseResource?
}
