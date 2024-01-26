package org.opencds.cqf.cql.evaluator.fhir.adapter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;

import java.util.List;

/**
 * This interface exposes common functionality across all FHIR Library versions.
 */
public interface PlanDefinitionAdapter extends KnowledgeArtifactAdapter {

  IBaseResource get();

  IIdType getId();

  void setId(IIdType id);

  String getName();

  void setName(String name);

  String getUrl();

  void setUrl(String url);

  String getVersion();

  void setVersion(String version);

  List<DependencyInfo> getDependencies();
}
