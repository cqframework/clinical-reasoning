package org.opencds.cqf.cql.evaluator.fhir.adapter;

import org.opencds.cqf.cql.evaluator.fhir.visitor.KnowledgeArtifactVisitor;

public interface KnowledgeArtifactAdapter extends ResourceAdapter {
  void accept(KnowledgeArtifactVisitor visitor);
}
