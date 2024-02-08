package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;

public interface r5KnowledgeArtifactAdapter extends IBaseKnowledgeArtifactAdapter {
  MetadataResource get();
  Period getEffectivePeriod();
  List<RelatedArtifact> getRelatedArtifact();
}
