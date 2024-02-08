package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;

public interface r4LibraryAdapter extends r4KnowledgeArtifactAdapter, IBaseLibraryAdapter {
    List<Attachment> getContent();
    Attachment addContent();
}
