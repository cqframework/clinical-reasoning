package org.opencds.cqf.fhir.cr.questionnaire.generate;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;

public interface IGenerateProcessor extends IOperationProcessor {
    IBaseResource generate(String id);

    IBaseResource generate(GenerateRequest request, String id);

    /**
     * Generates a Questionnaire item from a StructureDefinition Profile and returns
     * a Pair containing the Questionnaire item and the url of the supporting CQL Library if present
     * @param request GenerateRequest
     * @return Pair
     */
    <T extends IBaseExtension<?, ?>> Pair<IQuestionnaireItemComponentAdapter, List<T>> generateItem(
            GenerateRequest request);
}
