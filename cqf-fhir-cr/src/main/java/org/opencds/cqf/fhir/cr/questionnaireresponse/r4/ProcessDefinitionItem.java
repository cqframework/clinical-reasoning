package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

class ProcessDefinitionItem {
    protected static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionItem.class);
    private static final String PROPERTY_SETTING_ERROR_MESSAGE = "Error encountered attempting to set property (%s) on resource type (%s): %s";
    private static final String EXPRESSION_EVALUATION_ERROR_MESSAGE = "Error encountered evaluating expression (%s) for item (%s): %s";
    private static final String INVALID_RESOURCE_TYPE_ERROR_MESSAGE = "Unable to determine resource type from item definition: %s";
    ProcessorHelper processorHelper;
    ModelResolver modelResolver;
    PropertyHelper propertyHelper;
    String libraryUrl;
    LibraryEngine libraryEngine;
    String patientId;
    IBaseParameters parameters;
    IBaseBundle bundle;
     void process(
        QuestionnaireResponseItemComponent item,
        QuestionnaireResponse questionnaireResponse,
        List<IBaseResource> resources,
        Reference subject)
    {
        // Definition-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#definition-based-extraction
        final String contextExtension = Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT;
        final Extension itemExtractionContext = item.hasExtension(contextExtension)
            ? item.getExtensionByUrl(contextExtension)
            : questionnaireResponse.getExtensionByUrl(contextExtension);
        if (itemExtractionContext != null) {
            final Expression contextExpression = (Expression) itemExtractionContext.getValue();
            final List<IBase> context = getExpressionResult(contextExpression, item.getLinkId());
            if (context != null && !context.isEmpty()) {
                // TODO: edit context instead of creating new resources
            }
        }
        final Resource resource = getResourceWithStuff(questionnaireResponse, item, subject);
        resources.add(resource);
    }

    private Resource createBaseResource(QuestionnaireResponse questionnaireResponse, QuestionnaireResponseItemComponent item) {
        final String resourceType = getDefinitionType(item.getDefinition());
        final Resource resource = (Resource) processorHelper.newValue(resourceType);
        resource.setId(new IdType(resourceType, processorHelper.getExtractId(questionnaireResponse) + "-" + item.getLinkId()));
        resource.setMeta(getMeta(item.getDefinition()));
        return resource;
    }

    private Resource getResourceWithStuff(
        QuestionnaireResponse questionnaireResponse,
        QuestionnaireResponseItemComponent item,
        Reference subject
    ) {
        final Resource resource = createBaseResource(questionnaireResponse, item);
        setSubjectProperty(resource, subject);
        setAuthorProperty(resource, questionnaireResponse);
        setDateProperty(resource, questionnaireResponse);
        item.getItem().forEach(childItem -> {
            if (childItem.hasDefinition()) {
                final String resourceType = getDefinitionType(item.getDefinition());
                var definition = childItem.getDefinition().split("#");
                var path = definition[1].replace(resourceType + ".", "");
                // First element is always the resource type, so it can be ignored
                var answerValue = childItem.getAnswerFirstRep().getValue();
                if (answerValue != null) {
                    modelResolver.setValue(resource, path, answerValue);
                }
            }
        });
        return resource;
    }

    void setSubjectProperty(Resource resource, Reference subject) {
        final Property subjectProperty = propertyHelper.getSubjectProperty(resource);
        if (subjectProperty != null) {
            resource.setProperty(subjectProperty.getName(), subject);
        }
    }

    void setAuthorProperty(Resource resource, QuestionnaireResponse questionnaireResponse) {
        final Property authorProperty = propertyHelper.getAuthorProperty(resource);
        if (authorProperty != null && questionnaireResponse.hasAuthor()) {
            resource.setProperty(authorProperty.getName(), questionnaireResponse.getAuthor());
        }
    }

    void setDateProperty(Resource resource, QuestionnaireResponse questionnaireResponse) {
        final List<Property> dateProperties = propertyHelper.getDateProperties(resource);
        if (dateProperties != null && !dateProperties.isEmpty() && questionnaireResponse.hasAuthored()) {
            dateProperties.forEach(property -> {
                try {
                    setPropertyValueWithModelResolver(property, resource, questionnaireResponse);
                } catch (Exception ex) {
                    logger.error(String.format(
                        PROPERTY_SETTING_ERROR_MESSAGE,
                        property.getName(), resource.fhirType(), ex.getMessage())
                    );
                }
            });
        }
    }

    void setPropertyValueWithModelResolver(Property property, Resource resource, QuestionnaireResponse questionnaireResponse)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final BaseRuntimeElementDefinition propertyDef = propertyHelper.getPropertyDefinition(property);
        if (propertyDef != null) {
            modelResolver.setValue(
                resource,
                property.getName(),
                propertyDef
                    .getImplementingClass()
                    .getConstructor(Date.class)
                    .newInstance(questionnaireResponse.getAuthored()));
        }
    }

    private Meta getMeta(String definition) {
        final Meta meta = new Meta();
        // Consider setting source and lastUpdated here?
        if (definition != null && !definition.isEmpty()) {
            meta.addProfile(definition.split("#")[0]);
        }
        return meta;
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

    private String getDefinitionType(String definition) {
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(String.format(INVALID_RESOURCE_TYPE_ERROR_MESSAGE, definition));
        }
        return definition.split("#")[1];
    }
}
