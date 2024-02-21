package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.r5.r5KnowledgeArtifactVisitor;

public interface r5LibraryAdapter extends r5KnowledgeArtifactAdapter, IBaseLibraryAdapter {
    List<Attachment> getContent();

    Attachment addContent();

    IBase accept(r5KnowledgeArtifactVisitor visitor, Repository repository, Parameters operationParameters);

    Library get();
}
