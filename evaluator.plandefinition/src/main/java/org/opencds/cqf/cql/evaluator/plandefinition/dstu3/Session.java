package org.opencds.cqf.cql.evaluator.plandefinition.dstu3;

import java.util.Collection;

import org.hl7.fhir.dstu3.model.CarePlan;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;

class Session {
    public final String patientId;
    public CarePlan carePlan;
    public final Collection<IBaseResource> requestResources;
    public final String practitionerId;
    public final String organizationId;
    public final String userType;
    public final String userLanguage;
    public final String userTaskContext;
    public final String setting;
    public final String settingContext;
    public final String prefetchDataKey;
    public final String encounterId;
    public final IBaseParameters parameters;
    public final IBaseParameters prefetchData;
    public final IBaseResource contentEndpoint;
    public final IBaseResource terminologyEndpoint;
    public final IBaseResource dataEndpoint;
    public final IBaseBundle bundle;
    public final IBaseBundle prefetchDataData;
    public final DataRequirement prefetchDataDescription;
    public final Boolean useServerData;
  
    public Session(Collection<IBaseResource> requestResources, CarePlan carePlan, String patientId, String encounterId,
        String practitionerId, String organizationId, String userType, String userLanguage, String userTaskContext,
        String setting, String settingContext, IBaseParameters parameters,
        IBaseParameters prefetchData, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint,
        IBaseResource dataEndpoint, IBaseBundle bundle, Boolean useServerData,
        IBaseBundle prefetchDataData, DataRequirement prefetchDataDescription, String prefetchDataKey) {
  
      this.patientId = patientId;
      this.carePlan = carePlan;
      this.requestResources = requestResources;
      this.encounterId = encounterId;
      this.practitionerId = practitionerId;
      this.organizationId = organizationId;
      this.userType = userType;
      this.userLanguage = userLanguage;
      this.userTaskContext = userTaskContext;
      this.setting = setting;
      this.settingContext = settingContext;
      this.parameters = parameters;
      this.contentEndpoint = contentEndpoint;
      this.terminologyEndpoint = terminologyEndpoint;
      this.dataEndpoint = dataEndpoint;
      this.bundle = bundle;
      this.useServerData = useServerData;
      this.prefetchDataData = prefetchDataData;
      this.prefetchDataDescription = prefetchDataDescription;
      this.prefetchData = prefetchData;
      this.prefetchDataKey = prefetchDataKey;
    }

    public void setCarePlan(CarePlan carePlan) {
		this.carePlan = carePlan;
	}
}
  