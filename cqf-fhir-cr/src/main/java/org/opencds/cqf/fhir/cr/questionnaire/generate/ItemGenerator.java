package org.opencds.cqf.fhir.cr.questionnaire.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.cr.questionnaire.generate.r4.ElementProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class ItemGenerator {
    protected static final Logger logger = LoggerFactory.getLogger(ItemGenerator.class);
    protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
    protected static final String ITEM_CREATION_ERROR = "An error occurred during item creation: %s";
    protected static final String CHILD_LINK_ID_FORMAT = "%s.%s";

    public static final List<String> INPUT_EXTENSION_LIST =
            Arrays.asList(Constants.CPG_INPUT_DESCRIPTION, Constants.CPG_FEATURE_EXPRESSION);

    // protected final Repository repository;
    protected final ExpressionProcessor expressionProcessor;
    protected final IElementProcessor elementProcessor;
    // protected final ExtensionResolver extensionResolver;

    public ItemGenerator(Repository repository) {
        // this.repository = repository;
        expressionProcessor = new ExpressionProcessor();
        elementProcessor = new ElementProcessor(repository);
        // this.extensionResolver = new ExtensionResolver(null, null, null, null);
    }

    public IBaseBackboneElement generate(IOperationRequest request, IBaseResource profile, int itemCount) {
        checkNotNull(profile);
        final String linkId = String.valueOf(itemCount + 1);
        try {
            var questionnaireItem = createQuestionnaireItem(request, profile, linkId);
            // extensionResolver.resolveExtensions(null, this.questionnaireItem.getExtension(), null);
            processElements(request, questionnaireItem, profile);
            return questionnaireItem;
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.error(message);
            return createErrorItem(request, linkId, message);
        }
    }
    
    protected void processElements(IOperationRequest request, IBaseBackboneElement item, IBaseResource profile) {        
        int childCount = request.getItems(item).size();
        var itemLinkId = request.resolvePathString(item, "linkId");
        var profileUrl = request.resolvePathString(profile, "url");
        var featureExpression = expressionProcessor.getCqfExpression(request, request.getExtensions(profile), Constants.CPG_FEATURE_EXPRESSION);
        IBaseResource caseFeature = null;
        if (featureExpression != null) {
            try {
                var results = expressionProcessor.getExpressionResult(request, featureExpression, itemLinkId);
                var result = results == null || results.isEmpty() ? null : results.get(0);
                if (result instanceof IBaseResource) {
                    caseFeature = (IBaseResource) result;
                }
            } catch (ResolveExpressionException e) {
                logger.error(e.getMessage());
            }
        }
        for (var element : getElementsWithNonNullElementType(request, profile)) {
            childCount++;
            var childLinkId = String.format(CHILD_LINK_ID_FORMAT, itemLinkId, childCount);
            var childItem = processElement(request, profileUrl, element, childLinkId, caseFeature);
            request.getModelResolver().setValue(item, "item", Collections.singletonList(childItem));
        }
    }

    protected IBaseBackboneElement processElement(IOperationRequest request, String profileUrl, ICompositeType element, String childLinkId, IBaseResource caseFeature) {
        try {
            return elementProcessor.processElement(request, element, profileUrl, childLinkId, caseFeature);
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.warn(message);
            return createErrorItem(request, childLinkId, message);
        }
    }

    @SuppressWarnings("unchecked")
    protected <E extends ICompositeType> List<E> getElementsWithNonNullElementType(IOperationRequest request, IBaseResource profile) {
        var differential = request.resolvePath(profile, "differential");
        final List<E> elements = request.resolvePathList(differential, "element")
            .stream().map(e -> (E) e).collect(Collectors.toList());
        return elements.stream()
                .filter(element -> getElementType(request, element) != null)
                .collect(Collectors.toList());
    }

    protected IBaseBackboneElement createErrorItem(IOperationRequest request, String linkId, String errorMessage) {
        return createQuestionnaireItemComponent(request, errorMessage, linkId, null, true);
    }

    protected String getElementType(IOperationRequest request, ICompositeType element) {
        var type = request.resolvePathList(element, "type");
        return type.isEmpty() ? null : request.resolvePathString(type.get(0), "code");
    }

    public IBaseBackboneElement createQuestionnaireItem(IOperationRequest request, IBaseResource profile, String linkId) {
        var url = request.resolvePathString(profile, "url");
        var type = request.resolvePathString(profile, "type");
        final String definition = String.format("%s#%s", url, type);
        String text = getProfileText(request, profile);
        var item = createQuestionnaireItemComponent(request, text, linkId, definition, false);
        request.getModelResolver().setValue(item, "extension", copyExtensions(request.getExtensions(profile)));
        return item;
    }

    protected List<IBaseExtension<?, ?>> copyExtensions(List<IBaseExtension<?, ?>> profileExtensions) {
        var extensions = new ArrayList<IBaseExtension<?, ?>>();
        profileExtensions.forEach(ext -> {
            if (INPUT_EXTENSION_LIST.contains(ext.getUrl())
                    && extensions.stream().noneMatch(e -> e.getUrl().equals(ext.getUrl()))) {
                extensions.add(ext);
            }
        });

        return extensions;
    }

    @SuppressWarnings("unchecked")
    protected String getProfileText(IOperationRequest request, IBaseResource profile) {
        var inputExt = request.getExtensions(profile).stream().filter(e -> e.getUrl().equals(Constants.CPG_INPUT_TEXT)).findFirst().orElse(null);
        if (inputExt != null) {
            return ((IPrimitiveType<String>) inputExt.getValue()).getValue();
        }
        var title = request.resolvePathString(profile, "title");
        if (title != null) {
            return title;
        }
        var url = request.resolvePathString(profile, "url");
        return url.substring(url.lastIndexOf("/") + 1);
    }

    protected IBaseBackboneElement createQuestionnaireItemComponent(IOperationRequest request, String text, String linkId, String definition, Boolean isDisplay) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(isDisplay ? org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.DISPLAY : org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.GROUP)
                        .setDefinition(definition)
                        .setLinkId(linkId)
                        .setText(text);
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(isDisplay ? org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DISPLAY : org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.GROUP)
                        .setDefinition(definition)
                        .setLinkId(linkId)
                        .setText(text);
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(isDisplay ? org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DISPLAY : org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.GROUP)
                        .setDefinition(definition)
                        .setLinkId(linkId)
                        .setText(text);
        
            default:
                throw new IllegalArgumentException(String.format(
                    "Unsupported version of FHIR: %s", request.getFhirVersion().getFhirVersionString()));
        }
    }
}
