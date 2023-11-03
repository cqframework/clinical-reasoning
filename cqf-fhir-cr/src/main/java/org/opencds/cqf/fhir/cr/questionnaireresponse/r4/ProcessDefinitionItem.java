package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
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
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

class ProcessDefinitionItem {
    protected static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionItem.class);
    private static final String PROPERTY_SETTING_ERROR_MESSAGE = "Error encountered attempting to set property (%s) on resource type (%s): %s";
    ProcessorHelper processorHelper;
    ModelResolver modelResolver;
    Repository repository;
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
        final Resource resource = (Resource) newValue(resourceType);
        resource.setId(new IdType(resourceType, processorHelper.getExtractId(questionnaireResponse) + "-" + item.getLinkId()));
        resolveMeta((DomainResource) resource, item.getDefinition());
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
        final Property subjectProperty = getSubjectProperty(resource);
        if (subjectProperty != null) {
            resource.setProperty(subjectProperty.getName(), subject);
        }
    }

    void setAuthorProperty(Resource resource, QuestionnaireResponse questionnaireResponse) {
        final Property authorProperty = getAuthorProperty(resource);
        if (authorProperty != null && questionnaireResponse.hasAuthor()) {
            resource.setProperty(authorProperty.getName(), questionnaireResponse.getAuthor());
        }
    }

    void setDateProperty(Resource resource, QuestionnaireResponse questionnaireResponse) {
        var dateProperties = getDateProperties(resource);
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
        final BaseRuntimeElementDefinition propertyDef = getPropertyDefinition(property);
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

    BaseRuntimeElementDefinition getPropertyDefinition(Property property) {
        return repository.fhirContext().getElementDefinition(
            property.getTypeCode().contains("|")
                ? property.getTypeCode().split("\\|")[0]
                : property.getTypeCode());
    }

    private Property getSubjectProperty(Resource resource) {
        Property property = resource.getNamedProperty("subject");
        if (property == null) {
            property = resource.getNamedProperty("patient");
        }

        return property;
    }

    private Property getAuthorProperty(Resource resource) {
        Property property = resource.getNamedProperty("recorder");
        if (property == null && resource.fhirType().equals(FHIRAllTypes.OBSERVATION.toCode())) {
            property = resource.getNamedProperty("performer");
        }
        return property;
    }

    private List<Property> getDateProperties(Resource resource) {
        List<Property> results = new ArrayList<>();
        results.add(resource.getNamedProperty("onset"));
        results.add(resource.getNamedProperty("issued"));
        results.add(resource.getNamedProperty("effective"));
        results.add(resource.getNamedProperty("recordDate"));
        return results.stream().filter(p -> p != null).collect(Collectors.toList());
    }

    private void resolveMeta(DomainResource resource, String definition) {
        var meta = new Meta();
        // Consider setting source and lastUpdated here?
        if (definition != null && !definition.isEmpty()) {
            meta.addProfile(definition.split("#")[0]);
            resource.setMeta(meta);
        }
    }

    private List<IBase> getExpressionResult(Expression expression, String itemLinkId) {
        if (expression == null || expression.getExpression().isEmpty()) {
            return null;
        }
        try {
            return libraryEngine.resolveExpression(
                patientId, new CqfExpression(expression, libraryUrl, null), parameters, bundle);
        } catch (Exception ex) {
            var message = String.format(
                "Error encountered evaluating expression (%s) for item (%s): %s",
                expression.getExpression(), itemLinkId, ex.getMessage());
            logger.error(message);
        }
        return null;
    }

    private String getDefinitionType(String definition) {
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(
                String.format("Unable to determine resource type from item definition: %s", definition));
        }
        return definition.split("#")[1];
    }

    private Base newValue(String type) {
        try {
            return (Base) Class.forName("org.hl7.fhir.r4.model." + type)
                .getConstructor()
                .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
