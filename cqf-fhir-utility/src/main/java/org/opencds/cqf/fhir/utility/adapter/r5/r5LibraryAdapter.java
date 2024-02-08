package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.List;
import org.hl7.fhir.r5.model.Attachment;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;

public interface r5LibraryAdapter extends r5KnowledgeArtifactAdapter, IBaseLibraryAdapter {
    List<Attachment> getContent();
    Attachment addContent();
}
