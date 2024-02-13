package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.List;
import org.hl7.fhir.dstu3.model.Attachment;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;

public interface Dstu3LibraryAdapter extends Dstu3KnowledgeArtifactAdapter, IBaseLibraryAdapter {
    List<Attachment> getContent();
    Attachment addContent();
}
