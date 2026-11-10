package org.opencds.cqf.fhir.utility.repository.operations

import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.monad.Either3

interface IPlanDefinitionProcessor {
    fun <C : IPrimitiveType<String?>, R : IBaseResource> apply(
        planDefinition: Either3<C, IIdType, R>?,
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
        planDefinition: Either3<C, IIdType, R>?,
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

    fun <C : IPrimitiveType<String?>, R : IBaseResource> applyR5(
        planDefinition: Either3<C, IIdType, R>?,
        subjectId: String?,
        encounterId: String?,
        practitionerId: String?,
        organizationId: String?,
        userType: IBaseDatatype?,
        userLanguage: IBaseDatatype?,
        userTaskContext: IBaseDatatype?,
        setting: IBaseDatatype?,
        settingContext: IBaseDatatype?,
    ): IBaseBundle?

    fun <C : IPrimitiveType<String?>, R : IBaseResource> applyR5(
        planDefinition: Either3<C, IIdType, R>?,
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
    ): IBaseBundle?
}
