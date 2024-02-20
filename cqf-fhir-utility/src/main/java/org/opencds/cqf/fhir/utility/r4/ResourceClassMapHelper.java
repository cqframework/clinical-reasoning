package org.opencds.cqf.fhir.utility.r4;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.ChargeItemDefinition;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CompartmentDefinition;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.EffectEvidenceSynthesis;
import org.hl7.fhir.r4.model.EventDefinition;
import org.hl7.fhir.r4.model.Evidence;
import org.hl7.fhir.r4.model.EvidenceVariable;
import org.hl7.fhir.r4.model.ExampleScenario;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MessageDefinition;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.OperationDefinition;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.ResearchDefinition;
import org.hl7.fhir.r4.model.ResearchElementDefinition;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.RiskEvidenceSynthesis;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.model.TerminologyCapabilities;
import org.hl7.fhir.r4.model.TestScript;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ResourceClassMapHelper {
  public static final Map<ResourceType, Class<? extends IBaseResource>> resourceTypeToClass;
  static {
    Map<ResourceType, Class<? extends IBaseResource>> initializer = new HashMap<ResourceType, Class<? extends IBaseResource>>();
    initializer.put(ResourceType.ActivityDefinition, ActivityDefinition.class);
    initializer.put(ResourceType.CapabilityStatement, CapabilityStatement.class);
    initializer.put(ResourceType.ChargeItemDefinition, ChargeItemDefinition.class);
    initializer.put(ResourceType.CompartmentDefinition, CompartmentDefinition.class);
    initializer.put(ResourceType.ConceptMap, ConceptMap.class);
    initializer.put(ResourceType.EffectEvidenceSynthesis, EffectEvidenceSynthesis.class);
    initializer.put(ResourceType.EventDefinition, EventDefinition.class);
    initializer.put(ResourceType.Evidence, Evidence.class);
    initializer.put(ResourceType.EvidenceVariable, EvidenceVariable.class);
    initializer.put(ResourceType.ExampleScenario, ExampleScenario.class);
    initializer.put(ResourceType.GraphDefinition, GraphDefinition.class);
    initializer.put(ResourceType.ImplementationGuide, ImplementationGuide.class);
    initializer.put(ResourceType.Library, Library.class);
    initializer.put(ResourceType.Measure, Measure.class);
    initializer.put(ResourceType.MessageDefinition, MessageDefinition.class);
    initializer.put(ResourceType.NamingSystem, NamingSystem.class);
    initializer.put(ResourceType.OperationDefinition, OperationDefinition.class);
    initializer.put(ResourceType.PlanDefinition, PlanDefinition.class);
    initializer.put(ResourceType.Questionnaire, Questionnaire.class);
    initializer.put(ResourceType.ResearchDefinition, ResearchDefinition.class);
    initializer.put(ResourceType.ResearchElementDefinition, ResearchElementDefinition.class);
    initializer.put(ResourceType.RiskEvidenceSynthesis, RiskEvidenceSynthesis.class);
    initializer.put(ResourceType.SearchParameter, SearchParameter.class);
    initializer.put(ResourceType.StructureDefinition, StructureDefinition.class);
    initializer.put(ResourceType.StructureMap, StructureMap.class);
    initializer.put(ResourceType.TerminologyCapabilities, TerminologyCapabilities.class);
    initializer.put(ResourceType.TestScript, TestScript.class);
    initializer.put(ResourceType.ValueSet, ValueSet.class);
    initializer.put(ResourceType.CodeSystem, CodeSystem.class);
	resourceTypeToClass = initializer;
  }

  public static Class<? extends IBaseResource> getClass(String resourceType) throws UnprocessableEntityException {
    ResourceType type = null;
    try {
      type = ResourceType.fromCode(resourceType);
    } catch (FHIRException e) {
      throw new UnprocessableEntityException("Could not find resource type : " + resourceType);
    }
    Class<? extends IBaseResource> retval = resourceTypeToClass.get(type);
    if (retval == null) { 
      throw new UnprocessableEntityException(resourceType + " is not a valid KnowledgeArtifact resource type");
    }
    return retval;
  } 
}
