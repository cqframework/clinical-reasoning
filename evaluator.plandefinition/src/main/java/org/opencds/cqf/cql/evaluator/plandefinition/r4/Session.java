package org.opencds.cqf.cql.evaluator.plandefinition.r4;

import java.util.Collection;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DataRequirement;

class Session {
    public final String patientId;
    public final Boolean containResources;
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
    public IBaseParameters parameters, prefetchData;
    public IBaseResource contentEndpoint, terminologyEndpoint, dataEndpoint;
    public IBaseBundle bundle, prefetchDataData;
    public DataRequirement prefetchDataDescription;
    public Boolean useServerData;
  
    public Session(Boolean containResources, Collection<IBaseResource> requestResources, String patientId, String encounterId,
        String practitionerId, String organizationId, String userType, String userLanguage, String userTaskContext,
        String setting, String settingContext, IBaseParameters parameters,
        IBaseParameters prefetchData, IBaseResource contentEndpoint, IBaseResource terminologyEndpoint,
        IBaseResource dataEndpoint, IBaseBundle bundle, Boolean useServerData,
        IBaseBundle prefetchDataData, DataRequirement prefetchDataDescription, String prefetchDataKey) {
  
      this.patientId = patientId;
      this.containResources = containResources;
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
  }
  