package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildSdcLaunchContextExt;
import static org.opencds.cqf.fhir.utility.VersionUtilities.codeTypeForVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemGenerator {
    protected static final Logger logger = LoggerFactory.getLogger(ItemGenerator.class);
    protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
    protected static final String ITEM_CREATION_ERROR = "An error occurred during item creation: %s";
    protected static final String CHILD_LINK_ID_FORMAT = "%s.%s";

    protected final Repository repository;
    protected final CqlEngine engine;
    protected final IElementProcessor elementProcessor;
    protected final ExpressionProcessor expressionProcessor;
    protected final ExtensionProcessor extensionProcessor;

    public ItemGenerator(Repository repository) {
        this(repository, IElementProcessor.createProcessor(repository));
    }

    public ItemGenerator(Repository repository, IElementProcessor elementProcessor) {
        this.repository = repository;
        engine = Engines.forRepository(this.repository);
        this.elementProcessor = elementProcessor;
        expressionProcessor = new ExpressionProcessor();
        extensionProcessor = new ExtensionProcessor();
    }

    public <T extends IBaseExtension<?, ?>> Pair<IBaseBackboneElement, List<T>> generate(GenerateRequest request) {
        final String linkId =
                String.valueOf(request.getItems(request.getQuestionnaire()).size() + 1);
        try {
            var questionnaireItem = createQuestionnaireItem(request, linkId);
            int childCount = request.getItems(questionnaireItem).size();
            var caseFeature = getFeatureExpression(request);
            var parentElements = getElements(request, null, null);
            processElements(request, questionnaireItem, parentElements, childCount, linkId, caseFeature);
            // If we have a caseFeature we need to include launchContext extensions and Item Population Context
            List<T> launchContextExts = new ArrayList<>();
            if (caseFeature != null) {
                var itemContextExt = questionnaireItem.addExtension();
                itemContextExt.setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
                itemContextExt.setValue(caseFeature.toExpressionType(request.getFhirVersion()));
                // Assume Patient
                launchContextExts.add(buildSdcLaunchContextExt(request.getFhirVersion(), "patient"));
                var featureLibrary = request.getAdapterFactory()
                        .createLibrary(SearchHelper.searchRepositoryByCanonical(
                                repository,
                                VersionUtilities.canonicalTypeForVersion(
                                        request.getFhirVersion(), caseFeature.getLibraryUrl())));
                // Add any other in parameters that match launch context codes
                var inParameters = featureLibrary.getParameter().stream()
                        .filter(p -> {
                            var name = request.resolvePathString(p, "name").toUpperCase();
                            return (name.equals("PRACTITIONER"))
                                    || request.resolvePathString(p, "use").equals("in")
                                            && Arrays.asList(SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE.values()).stream()
                                                    .map(Object::toString)
                                                    .collect(Collectors.toList())
                                                    .contains(name);
                        })
                        .map(p -> request.resolvePathString(p, "name").toLowerCase())
                        .collect(Collectors.toList());
                inParameters.forEach(p -> launchContextExts.add(buildSdcLaunchContextExt(request.getFhirVersion(), p)));
            }
            return new ImmutablePair<>(questionnaireItem, launchContextExts);
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.error(message);
            return new ImmutablePair<>(createErrorItem(request, linkId, message), new ArrayList<>());
        }
    }

    protected CqfExpression getFeatureExpression(GenerateRequest request) {
        var expression = expressionProcessor.getCqfExpression(
                request, request.getExtensions(request.getProfile()), Constants.CPG_FEATURE_EXPRESSION);
        if (expression != null) {
            expression.setName(request.getProfileAdapter().getName());
        }
        return expression;
    }

    protected List<IBase> getFeatureExpressionResults(
            GenerateRequest request, CqfExpression featureExpression, String itemLinkId) {
        return expressionProcessor.getExpressionResultForItem(request, featureExpression, itemLinkId);
    }

    protected <E extends ICompositeType> void processElements(
            GenerateRequest request,
            IBaseBackboneElement item,
            List<E> elements,
            int childCount,
            String itemLinkId,
            CqfExpression caseFeature) {
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
            CqfExpression caseFeature,
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
        if (elementExists(request, existingElements, path)) {
            return false;
        }

        // filter out slicing definitions
        if (request.resolvePath(element, "slicing") != null) {
            return false;
        }
        if (notInPath(path, parentPath)) {
            return false;
        }

        if (sliceName != null && !request.resolvePathString(element, "id").contains(sliceName)) {
            return false;
        }
        if (Boolean.TRUE.equals(requiredOnly) && !isRequiredPath(request, element)) {
            return false;
        }
        if (Boolean.TRUE.equals(request.getSupportedOnly())) {
            var mustSupportElement = request.resolvePath(element, "mustSupport", IBaseBooleanDatatype.class);
            if (mustSupportElement == null || mustSupportElement.getValue().equals(Boolean.FALSE)) {
                return false;
            }
        }
        return true;
    }

    protected <E extends ICompositeType> boolean elementExists(
            GenerateRequest request, List<E> existingElements, String path) {
        return existingElements != null
                && !existingElements.isEmpty()
                && existingElements.stream().anyMatch(e -> path.equals(request.resolvePathString(e, "path")));
    }

    protected boolean notInPath(String path, String parentPath) {
        var pathSplit = path.split("\\.");
        if (parentPath == null) {
            // grab only top level elements
            if (pathSplit.length > 2) {
                return true;
            }
        } else {
            // grab only the next level of elements
            var splitLength = parentPath.split("\\.").length + 1;
            if (pathSplit.length > splitLength || !path.contains(parentPath + ".")) {
                return true;
            }
        }
        return false;
    }

    protected <E extends ICompositeType> boolean isRequiredPath(GenerateRequest request, E element) {
        var min = request.resolvePath(element, "min", IPrimitiveType.class);
        return (min != null && (Integer) min.getValue() != 0);
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
        var text = request.getProfileAdapter().hasTitle()
                ? request.getProfileAdapter().getTitle()
                : request.getProfileAdapter().getName();
        var item = createQuestionnaireItemComponent(
                request, text, linkId, request.getProfileAdapter().getUrl(), false);
        var extractContext = item.addExtension();
        extractContext.setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT);
        extractContext.setValue(codeTypeForVersion(
                request.getFhirVersion(), request.getProfileAdapter().getType()));
        return item;
    }

    protected IBaseBackboneElement createQuestionnaireItemComponent(
            GenerateRequest request, String text, String linkId, String definition, Boolean isDisplay) {
        switch (request.getFhirVersion()) {
            case R4:
                return new org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(
                                Boolean.TRUE.equals(isDisplay)
                                        ? org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DISPLAY
                                        : org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.GROUP)
                        .setDefinition(definition)
                        .setLinkId(linkId)
                        .setText(text);
            case R5:
                return new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent()
                        .setType(
                                Boolean.TRUE.equals(isDisplay)
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
