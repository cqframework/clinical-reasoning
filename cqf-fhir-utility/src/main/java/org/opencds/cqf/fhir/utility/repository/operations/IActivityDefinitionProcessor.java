package org.opencds.cqf.fhir.utility.repository.operations;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.monad.Either3;

public interface IActivityDefinitionProcessor {
    <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> activityDefinition,
            String subjectId,
            String encounterId,
            String practitionerId,
            String organizationId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext);

    <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> activityDefinition,
            String subjectId,
            String encounterId,
            String practitionerId,
            String organizationId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint);
}
