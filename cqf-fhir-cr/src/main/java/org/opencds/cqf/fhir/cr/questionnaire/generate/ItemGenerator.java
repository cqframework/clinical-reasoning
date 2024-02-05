package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.opencds.cqf.fhir.cr.questionnaire.common.ResolveExpressionException;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemGenerator {
    protected static final Logger logger = LoggerFactory.getLogger(ItemGenerator.class);
    protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
    protected static final String NO_BASE_DEFINITION_ERROR =
            "An error occurred search for base definition with url (%s): %s";
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

    public IBaseBackboneElement generate(GenerateRequest request, IBaseResource profile) {
        checkNotNull(profile);
        final String linkId =
                String.valueOf(request.getItems(request.getQuestionnaire()).size() + 1);
        try {
            var questionnaireItem = createQuestionnaireItem(request, profile, linkId);
            processExtensions(request, questionnaireItem, profile);
            processElements(request, questionnaireItem, profile);
            return questionnaireItem;
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.error(message);
            return createErrorItem(request, linkId, message);
        }
    }

    protected void processExtensions(
            GenerateRequest request, IBaseBackboneElement questionnaireItem, IBaseResource profile) {
        extensionProcessor.processExtensionsInList(request, questionnaireItem, profile, INPUT_EXTENSION_LIST);
    }

    protected CqfExpression getFeatureExpression(GenerateRequest request, IBaseResource profile) {
        return expressionProcessor.getCqfExpression(
                request, request.getExtensions(profile), Constants.CPG_FEATURE_EXPRESSION);
    }

    protected List<IBase> getFeatureExpressionResults(
            GenerateRequest request, CqfExpression featureExpression, String itemLinkId)
            throws ResolveExpressionException {
        return expressionProcessor.getExpressionResultForItem(request, featureExpression, itemLinkId);
    }

    protected void processElements(GenerateRequest request, IBaseBackboneElement item, IBaseResource profile) {
        int childCount = request.getItems(item).size();
        var itemLinkId = request.getItemLinkId(item);
        var profileUrl = request.resolvePathString(profile, "url");
        var featureExpression = getFeatureExpression(request, profile);
        IBaseResource caseFeature = null;
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
        for (var element : getElementsWithNonNullElementType(request, profile)) {
            childCount++;
            var childLinkId = String.format(CHILD_LINK_ID_FORMAT, itemLinkId, childCount);
            var childItem = processElement(request, profileUrl, element, childLinkId, caseFeature);
            request.getModelResolver().setValue(item, "item", Collections.singletonList(childItem));
        }
    }

    protected IBaseBackboneElement processElement(
            GenerateRequest request,
            String profileUrl,
            ICompositeType element,
            String childLinkId,
            IBaseResource caseFeature) {
        try {
            return elementProcessor.processElement(request, element, profileUrl, childLinkId, caseFeature);
        } catch (Exception ex) {
            final String message = String.format(ITEM_CREATION_ERROR, ex.getMessage());
            logger.warn(message);
            return createErrorItem(request, childLinkId, message);
        }
    }

    @SuppressWarnings("unchecked")
    protected <E extends ICompositeType> List<E> getElementsWithNonNullElementType(
            GenerateRequest request, IBaseResource profile) {
        List<E> elements = new ArrayList<>();
        var differential = request.resolvePath(profile, "differential");
        elements.addAll(request.resolvePathList(differential, "element").stream()
                .map(e -> (E) e)
                .collect(Collectors.toList()));
        // var snapshot = getProfileSnapshot(request, profile);
        // if (snapshot != null) {
        //     if (request.getRequiredOnly()) {
        //         // only top level required elements and any required elements under them
        //         elements.addAll(request.resolvePathList(snapshot, "element").stream()
        //                 .filter(e -> {
        //                     var path = request.resolvePathString(e, "path");
        //                     var min = request.resolvePath(e, "min", IPrimitiveType.class);
        //                     return min == null ? false : (Integer) min.getValue() > 0;
        //                 })
        //                 .map(e -> (E) e)
        //                 .collect(Collectors.toList()));
        //     } else {
        //         elements.addAll(request.resolvePathList(snapshot, "element").stream()
        //                 .map(e -> (E) e)
        //                 .collect(Collectors.toList()));
        //     }
        // }
        if (request.getSupportedOnly()) {
            elements = elements.stream()
                    .filter(e -> {
                        var mustSupport = Boolean.FALSE;
                        var mustSupportElement = request.resolvePath(e, "mustSupport", IBaseBooleanDatatype.class);
                        if (mustSupportElement != null) {
                            mustSupport = mustSupportElement.getValue();
                        }
                        return mustSupport;
                    })
                    .collect(Collectors.toList());
        }
        return elements.stream()
                .filter(element -> getElementType(request, element) != null)
                .collect(Collectors.toList());
    }

    // protected IBase getProfileSnapshot(GenerateRequest request, IBaseResource profile) {
    //     var snapshot = request.resolvePath(profile, "snapshot");
    //     if (snapshot == null) {
    //         // Grab the snapshot from the baseDefinition
    //         var baseUrl = request.resolvePath(profile, "baseDefinition", IPrimitiveType.class);
    //         if (baseUrl != null) {
    //             IBaseResource baseProfile = null;
    //             try {
    //                 baseProfile = searchRepositoryByCanonical(repository, baseUrl);
    //             } catch (Exception e) {
    //                 logger.error(NO_BASE_DEFINITION_ERROR, baseUrl.getValueAsString(), e);
    //             }
    //             if (baseProfile != null) {
    //                 snapshot = request.resolvePath(baseProfile, "snapshot");
    //             }
    //         }
    //     }
    //     if (snapshot == null) {
    //         // Grab the snapshot from the type definition
    //         // Dstu3 is a code, should still cast to IPrimitiveType<String>?
    //         var type = request.resolvePathString(profile, "type");
    //         var definition = repository.fhirContext().getResourceDefinition(request.getFhirVersion(), type);
    //         var typeProfile = definition == null ? null : definition.toProfile(null);
    //         if (typeProfile != null) {
    //             snapshot = request.resolvePath(typeProfile, "snapshot");
    //         }
    //     }
    //     return snapshot;
    // }

    protected IBaseBackboneElement createErrorItem(GenerateRequest request, String linkId, String errorMessage) {
        return createQuestionnaireItemComponent(request, errorMessage, linkId, null, true);
    }

    protected String getElementType(GenerateRequest request, ICompositeType element) {
        var type = request.resolvePathList(element, "type");
        return type.isEmpty() ? null : request.resolvePathString(type.get(0), "code");
    }

    public IBaseBackboneElement createQuestionnaireItem(GenerateRequest request, IBaseResource profile, String linkId) {
        var url = request.resolvePathString(profile, "url");
        var type = request.resolvePathString(profile, "type");
        final String definition = String.format("%s#%s", url, type);
        String text = getProfileText(request, profile);
        var item = createQuestionnaireItemComponent(request, text, linkId, definition, false);
        return item;
    }

    @SuppressWarnings("unchecked")
    protected String getProfileText(GenerateRequest request, IBaseResource profile) {
        var inputExt = request.getExtensions(profile).stream()
                .filter(e -> e.getUrl().equals(Constants.CPG_INPUT_TEXT))
                .findFirst()
                .orElse(null);
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
