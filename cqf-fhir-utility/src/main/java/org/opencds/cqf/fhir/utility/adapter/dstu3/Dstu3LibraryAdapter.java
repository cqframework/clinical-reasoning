package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;

public interface Dstu3LibraryAdapter extends Dstu3KnowledgeArtifactAdapter, IBaseLibraryAdapter {
    List<Attachment> getContent();
    Attachment addContent();
}
