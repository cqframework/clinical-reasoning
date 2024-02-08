package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;

public interface Dstu3KnowledgeArtifactAdapter extends IBaseKnowledgeArtifactAdapter {
  MetadataResource get();
  Period getEffectivePeriod();
  List<RelatedArtifact> getRelatedArtifact();
}
