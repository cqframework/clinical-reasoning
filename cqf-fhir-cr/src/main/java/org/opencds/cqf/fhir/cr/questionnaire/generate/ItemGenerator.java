package org.opencds.cqf.fhir.cr.questionnaire.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.CqfExpression;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemGenerator {
    protected static final Logger logger = LoggerFactory.getLogger(ItemGenerator.class);
    protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
    protected static final String ITEM_CREATION_ERROR = "An error occurred during item creation: %s";
    protected static final String CHILD_LINK_ID_FORMAT = "%s.%s";

    public static final List<String> INPUT_EXTENSION_LIST =
            Arrays.asList(Constants.CPG_INPUT_DESCRIPTION, Constants.CPG_FEATURE_EXPRESSION);

    protected final Repository repository;
    protected final IElementProcessor elementProcessor;
    protected final ExpressionProcessor expressionProcessor;
    protected final ExtensionProcessor extensionProcessor;

    public ItemGenerator(Repository repository) {
        this(repository, IElementProcessor.createProcessor(repository));
    }

    public ItemGenerator(Repository repository, IElementProcessor elementProcessor) {
        this.repository = repository;
        this.elementProcessor = elementProcessor;
        expressionProcessor = new ExpressionProcessor();
        extensionProcessor = new ExtensionProcessor();
    }

    public IBaseBackboneElement generate(GenerateRequest request) {
        final String linkId =
                String.valueOf(request.getItems(request.getQuestionnaire()).size() + 1);
        try {
            var questionnaireItem = createQuestionnaireItem(request, linkId);
            processExtensions(request, questionnaireItem);
            int childCount = request.getItems(questionnaireItem).size();
            var caseFeature = getCaseFeature(request, linkId);
            var parentElements = getElements(request, null, null);
            processElements(request, questionnaireItem, parentElements, childCount, linkId, caseFeature);
            return questionnaireItem;
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.error(message);
            return createErrorItem(request, linkId, message);
        }
    }

    protected void processExtensions(GenerateRequest request, IBaseBackboneElement questionnaireItem) {
        extensionProcessor.processExtensionsInList(
                request, questionnaireItem, request.getProfile(), INPUT_EXTENSION_LIST);
    }

    protected IBaseResource getCaseFeature(GenerateRequest request, String itemLinkId) {
        IBaseResource caseFeature = null;
        var featureExpression = getFeatureExpression(request);
        if (featureExpression != null) {
            try {
                var results = getFeatureExpressionResults(request, featureExpression, itemLinkId);
                var result = results == null || results.isEmpty() ? null : results.get(0);
                if (result instanceof IBaseResource) {
                    caseFeature = (IBaseResource) result;
                }
            } catch (ResolveExpressionException e) {
                logger.error(e.getMessage());
            }
        }
        return caseFeature;
    }

    protected CqfExpression getFeatureExpression(GenerateRequest request) {
        return expressionProcessor.getCqfExpression(
                request, request.getExtensions(request.getProfile()), Constants.CPG_FEATURE_EXPRESSION);
    }

    protected List<IBase> getFeatureExpressionResults(
            GenerateRequest request, CqfExpression featureExpression, String itemLinkId)
            throws ResolveExpressionException {
        return expressionProcessor.getExpressionResultForItem(request, featureExpression, itemLinkId);
    }

    protected <E extends ICompositeType> void processElements(
            GenerateRequest request,
            IBaseBackboneElement item,
            List<E> elements,
            int childCount,
            String itemLinkId,
            IBaseResource caseFeature) {
        for (var element : elements) {
            childCount++;
            var childLinkId = String.format(CHILD_LINK_ID_FORMAT, itemLinkId, childCount);
            var childElements = getElements(
                    request,
                    request.resolvePathString(element, "path"),
                    request.resolvePathString(element, "sliceName"));
            // if child elements exist ignore the type and create a group
            var elementType = getElementType(request, element);
            var childItem =
                    processElement(request, element, elementType, childLinkId, caseFeature, !childElements.isEmpty());
            if (childElements.isEmpty()) {
                request.getModelResolver().setValue(item, "item", Collections.singletonList(childItem));
            } else {
                processElements(request, childItem, childElements, 0, childLinkId, caseFeature);
                request.getModelResolver().setValue(item, "item", Collections.singletonList(childItem));
            }
        }
    }

    protected IBaseBackboneElement processElement(
            GenerateRequest request,
            ICompositeType element,
            String elementType,
            String childLinkId,
            IBaseResource caseFeature,
            Boolean isGroup) {
        try {
            return elementProcessor.processElement(request, element, elementType, childLinkId, caseFeature, isGroup);
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.warn(message);
            return createErrorItem(request, childLinkId, message);
        }
    }

    @SuppressWarnings("unchecked")
    protected <E extends ICompositeType> List<E> getElements(
            GenerateRequest request, String parentPath, String sliceName) {
        List<E> elements = new ArrayList<>();
        // Add all elements from the differential
        if (request.getDifferentialElements() != null) {
            elements.addAll(request.getDifferentialElements().stream()
                    .map(e -> (E) e)
                    .filter(e -> filterElement(request, e, null, parentPath, sliceName, false))
                    .collect(Collectors.toList()));
        }
        // Add elements from the snapshot that were not in the differential
        if (request.getSnapshotElements() != null) {
            elements.addAll(request.getSnapshotElements().stream()
                    .map(e -> (E) e)
                    .filter(e -> filterElement(request, e, elements, parentPath, sliceName, request.getRequiredOnly()))
                    .collect(Collectors.toList()));
        }
        return elements;
    }

    protected <E extends ICompositeType> Boolean filterElement(
            GenerateRequest request,
            E element,
            List<E> existingElements,
            String parentPath,
            String sliceName,
            Boolean requiredOnly) {
        var path = request.resolvePathString(element, "path");
        if (existingElements != null && !existingElements.isEmpty()) {
            if (existingElements.stream().anyMatch(e -> path.equals(request.resolvePathString(e, "path")))) {
                return false;
            }
        }
        // filter out slicing definitions
        if (request.resolvePath(element, "slicing") != null) {
            return false;
        }
        var pathSplit = path.split("\\.");
        if (parentPath == null) {
            // grab only top level elements
            if (pathSplit.length > 2) {
                return false;
            }
        } else {
            // grab only the next level of elements
            var splitLength = parentPath.split("\\.").length + 1;
            if (pathSplit.length > splitLength || !path.contains(parentPath + ".")) {
                return false;
            }
        }
        if (sliceName != null && !request.resolvePathString(element, "id").contains(sliceName)) {
            return false;
        }
        if (requiredOnly) {
            var min = request.resolvePath(element, "min", IPrimitiveType.class);
            if (min == null || (Integer) min.getValue() == 0) {
                return false;
            }
        }
        if (request.getSupportedOnly()) {
            var mustSupportElement = request.resolvePath(element, "mustSupport", IBaseBooleanDatatype.class);
            if (mustSupportElement == null || mustSupportElement.getValue().equals(Boolean.FALSE)) {
                return false;
            }
        }
        return true;
    }

    protected IBaseBackboneElement createErrorItem(GenerateRequest request, String linkId, String errorMessage) {
        return createQuestionnaireItemComponent(request, errorMessage, linkId, null, true);
    }

    protected String resolveElementType(GenerateRequest request, ICompositeType element) {
        var typeList = request.resolvePathList(element, "type");
        return typeList.isEmpty() ? null : request.resolvePathString(typeList.get(0), "code");
    }

    protected String getElementType(GenerateRequest request, ICompositeType element) {
        var type = resolveElementType(request, element);
        if (type == null) {
            // Attempt to resolve the type from the Snapshot if it is available
            var path = request.resolvePathString(element, "path");
            var snapshot = request.getSnapshotElements() == null
                    ? null
                    : request.getSnapshotElements().stream()
                            .filter(e -> path.equals(request.resolvePathString(e, "path")))
                            .findFirst()
                            .orElse(null);
            if (snapshot != null) {
                type = resolveElementType(request, snapshot);
            }
        }
        return type;
    }

    public IBaseBackboneElement createQuestionnaireItem(GenerateRequest request, String linkId) {
        var url = request.resolvePathString(request.getProfile(), "url");
        var type = request.resolvePathString(request.getProfile(), "type");
        final String definition = String.format("%s#%s", url, type);
        String text = getProfileText(request);
        var item = createQuestionnaireItemComponent(request, text, linkId, definition, false);
        return item;
    }

    @SuppressWarnings("unchecked")
    protected String getProfileText(GenerateRequest request) {
        var inputExt = request.getExtensions(request.getProfile()).stream()
                .filter(e -> e.getUrl().equals(Constants.CPG_INPUT_TEXT))
                .findFirst()
                .orElse(null);
        if (inputExt != null) {
            return ((IPrimitiveType<String>) inputExt.getValue()).getValue();
        }
        var title = request.resolvePathString(request.getProfile(), "title");
        if (title != null) {
            return title;
        }
        var url = request.resolvePathString(request.getProfile(), "url");
        return url.substring(url.lastIndexOf("/") + 1);
    }

    protected IBaseBackboneElement createQuestionnaireItemComponent(
            GenerateRequest request, String text, String linkId, String definition, Boolean isDisplay) {
        switch (request.getFhirVersion()) {
            case DSTU3:
                return new org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(
                                isDisplay
                                        ? org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.DISPLAY
                                        : org.hl7.fhir.dstu3.model.Questionnaire.QuestionnaireItemType.GROUP)
                        .setDefinition(definition)
                        .setLinkId(linkId)
                        .setText(text);
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(
                                isDisplay
                                        ? org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DISPLAY
                                        : org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.GROUP)
                        .setDefinition(definition)
                        .setLinkId(linkId)
                        .setText(text);
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(
                                isDisplay
                                        ? org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DISPLAY
                                        : org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.GROUP)
                        .setDefinition(definition)
                        .setLinkId(linkId)
                        .setText(text);

            default:
                return null;
        }
    }
}
