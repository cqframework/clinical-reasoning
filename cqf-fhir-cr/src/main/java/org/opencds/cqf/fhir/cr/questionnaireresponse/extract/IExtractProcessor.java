package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IExtractProcessor {
    IBaseBundle extract(ExtractRequest request);

    List<IBaseResource> processItems(ExtractRequest request);
}
