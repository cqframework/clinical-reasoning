package org.opencds.cqf.fhir.utility.adapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;

public interface IBaseKnowledgeArtifactAdapter extends ResourceAdapter {

  IBaseResource get();

  default IIdType getId() {
    return this.get().getIdElement();
  }
  default void setId(IIdType id) {
    this.get().setId(id);
  }

  String getName();

  void setName(String name);

  String getUrl();

  void setUrl(String url);

  String getVersion();

  void setVersion(String version);

  List<DependencyInfo> getDependencies();
  Date getApprovalDate();
  ICompositeType getEffectivePeriod();
  List<? extends ICompositeType> getRelatedArtifact();
  List<? extends ICompositeType> getComponents();
//   <T extends Enum<T>> List<? extends ICompositeType> getRelatedArtifactsOfType(T relatedArtifactType);
//   IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters);
  String releaseLabelUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
  String releaseDescriptionUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
  String valueSetConditionUrl = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-condition";
  String valueSetPriorityUrl = "http://aphl.org/fhir/vsm/StructureDefinition/vsm-valueset-priority";
  String valueSetPriorityCode = "priority";
  String valueSetConditionCode = "focus";
  List<String> preservedExtensionUrls = Collections.unmodifiableList(new ArrayList<String>(Arrays.asList(
        valueSetPriorityUrl,
        valueSetConditionUrl
    )));
    String usPhContextTypeUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context-type";
	String contextTypeUrl = "http://terminology.hl7.org/CodeSystem/usage-context-type";
	String contextUrl = "http://hl7.org/fhir/us/ecr/CodeSystem/us-ph-usage-context";
  String isOwnedUrl = "http://hl7.org/fhir/StructureDefinition/crmi-isOwned";
}