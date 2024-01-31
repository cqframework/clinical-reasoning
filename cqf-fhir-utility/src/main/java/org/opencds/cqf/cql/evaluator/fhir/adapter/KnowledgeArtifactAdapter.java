package org.opencds.cqf.cql.evaluator.fhir.adapter;

import org.opencds.cqf.cql.evaluator.fhir.visitor.KnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.api.Repository;

public interface KnowledgeArtifactAdapter {
  void accept(KnowledgeArtifactVisitor visitor, Repository theRepository);
}
