package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;

public interface IExtractProcessor extends IOperationProcessor {
    IBaseBundle extract(ExtractRequest request);

    List<IBaseResource> processItems(ExtractRequest request);
}
