package org.opencds.cqf.fhir.cr.questionnaireresponse.r4.defintionbasedextraction;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.questionnaireresponse.r4.ProcessParameters;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;
import java.util.List;

public class ProcessDefinitionItem {
    // Definition-based extraction -
    // http://build.fhir.org/ig/HL7/sdc/extraction.html#definition-based-extraction

    protected static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionItem.class);
    private static final String EXPRESSION_EVALUATION_ERROR_MESSAGE = "Error encountered evaluating expression (%s) for item (%s): %s";
    private final ResourceFactory resourceFactory = new ResourceFactory();
    String libraryUrl;
    LibraryEngine libraryEngine;
    String patientId;
    IBaseParameters parameters;
    IBaseBundle bundle;
     public void process(ProcessParameters processParameters) {
        maybeProcessItemWithExtractionContext(processParameters);
        final Resource resource = resourceFactory.makeResource(processParameters);
        processParameters.addToResources(resource);
    }

    // ROSIE TODO: this method is currently not doing anything => can we delete or should we keep?
    void maybeProcessItemWithExtractionContext(ProcessParameters processParameters) {
        final Extension itemExtractionContext = getItemExtractionContext(processParameters);
        if (itemExtractionContext != null) {
            final Expression contextExpression = (Expression) itemExtractionContext.getValue();
            final List<IBase> context = getExpressionResult(contextExpression, processParameters.getItem().getLinkId());
            if (context != null && !context.isEmpty()) {
                // TODO: edit context instead of creating new resources
            }
        }
    }

    @Nullable
    Extension getItemExtractionContext(ProcessParameters processParameters) {
        final String contextExtension = Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT;
        if (processParameters.getItem().hasExtension(contextExtension)) {
            return processParameters.getItem().getExtensionByUrl(contextExtension);
        }
        return processParameters.getQuestionnaireResponse().getExtensionByUrl(contextExtension);
    }

    private List<IBase> getExpressionResult(Expression expression, String itemLinkId) {
        if (expression == null || expression.getExpression().isEmpty()) {
            return null;
        }
        try {
            return libraryEngine.resolveExpression(
                patientId, new CqfExpression(expression, libraryUrl, null), parameters, bundle);
        } catch (Exception ex) {
            logger.error(String.format(
                EXPRESSION_EVALUATION_ERROR_MESSAGE,
                expression.getExpression(), itemLinkId, ex.getMessage())
            );
        }
        return null;
    }
}
