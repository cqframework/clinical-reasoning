package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.r4.r4KnowledgeArtifactVisitor;

public interface r4LibraryAdapter extends r4KnowledgeArtifactAdapter, IBaseLibraryAdapter {
    List<Attachment> getContent();
    Attachment addContent();
    IBase accept(r4KnowledgeArtifactVisitor visitor, Repository repository, Parameters operationParameters);
    Library get();
}
