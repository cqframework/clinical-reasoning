package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;
import static org.opencds.cqf.fhir.utility.Constants.CPG_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT;
import static org.opencds.cqf.fhir.utility.Constants.SDC_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT;
import static org.opencds.cqf.fhir.utility.Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.stringTypeForVersion;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;
import org.opencds.cqf.fhir.cr.questionnaire.Helpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ItemProcessor.class);
    final ExpressionProcessor expressionProcessor;

    public ItemProcessor(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    public void processItem(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            List<IQuestionnaireResponseItemComponentAdapter> responseItem) {
        if (item.isContextItem()) {
            processContextItem(request, item, responseItem);
        } else {
            responseItem.add(item.newResponseItem());
            processSingleItem(request, item, responseItem);
        }
    }

    protected void processGroupItem(
            PopulateRequest request,
            IQuestionnaireResponseItemComponentAdapter responseItem,
            List<IQuestionnaireItemComponentAdapter> childItems) {
        childItems.forEach(childItem -> {
            var childResponseItem = new ArrayList<IQuestionnaireResponseItemComponentAdapter>();
            processItem(request, childItem, childResponseItem);
            responseItem.addItems(childResponseItem);
        });
    }

    protected List<IQuestionnaireResponseItemComponentAdapter> processSingleItem(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            List<IQuestionnaireResponseItemComponentAdapter> responseItems) {
        var responseItem = responseItems.get(0);
        var childItems = item.getItem().stream()
                .map(IQuestionnaireItemComponentAdapter.class::cast)
                .collect(Collectors.toList());
        if (item.isGroupItem()) {
            processGroupItem(request, responseItem, childItems);
        } else {
            request.setContextVariable(responseItem.get());
            var rawParams = request.getRawParameters();
            rawParams.put("%qitem", item.get());
            populateAnswer(request, responseItem, getInitialValue(request, item, responseItem, rawParams));
            if (!childItems.isEmpty()) {
                // children of non group items go under each answer
                var childResponseItems = childItems.stream()
                        .map(c -> processSingleItem(request, c, List.of(c.newResponseItem())))
                        .flatMap(Collection::stream)
                        .toList();
                var answers = responseItem.getAnswer();
                if (answers.isEmpty()) {
                    answers.add(responseItem.newAnswer(null));
                    responseItem.setAnswer(answers);
                }
                answers.forEach(a -> a.setItem(childResponseItems));
            }
        }
        return responseItems;
    }

    protected void processContextItem(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            List<IQuestionnaireResponseItemComponentAdapter> responseItem) {
        var itemLinkId = item.getLinkId();
        if (!item.isGroupItem() && item.hasExtension(SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT)) {
            throw new UnprocessableEntityException(
                    "Encountered Item Population Context extension on a non group item: {}", itemLinkId);
        }
        IBaseResource profile = null;
        var ctx = getContext(request, item);
        var definition = ctx.component1();
        var contextExpression = ctx.component2();

        if (StringUtils.isNotBlank(definition)) {
            var profileUrl = definition.split("#")[0];
            try {
                profile = searchRepositoryByCanonical(
                        request.getRepository(), canonicalTypeForVersion(request.getFhirVersion(), profileUrl));
            } catch (Exception e) {
                var message = "No profile found for definition: %s".formatted(profileUrl);
                logger.error(message);
                request.logException(message);
            }
        }
        final var profileAdapter = profile == null
                ? null
                : (IStructureDefinitionAdapter)
                        request.getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource) profile);
        List<IAdapter<?>> populationContext;
        if (contextExpression != null) {
            try {
                populationContext =
                        expressionProcessor
                                .getExpressionResultForItem(request, contextExpression, itemLinkId, null, null)
                                .stream()
                                // filtering nulls here to prevent unnecessary duplicate responseItems
                                .filter(Objects::nonNull)
                                .map(r -> request.getAdapterFactory().createBase(r))
                                .collect(Collectors.toList());
            } catch (Exception e) {
                logger.error(e.getMessage());
                request.logException(e.getMessage());
                populationContext = new ArrayList<>();
            }
        } else {
            populationContext = new ArrayList<>();
        }
        if (populationContext.isEmpty()) {
            // We always want to return a responseItem even if we have nothing to populate
            populationContext.add(null);
        }
        if (populationContext.size() > 1 && !item.getRepeats()) {
            throw new UnprocessableEntityException(
                    "Population context expression resulted in multiple values for a non repeating group: {}",
                    contextExpression.getExpression());
        }
        var contextName = contextExpression == null ? null : contextExpression.getName();
        var newResponseItems = populationContext.stream()
                .map(context -> processPopulationContext(request, item, contextName, context, profileAdapter))
                .toList();
        responseItem.addAll(newResponseItems);
    }

    protected kotlin.Pair<String, CqfExpression> getContext(
            PopulateRequest request, IQuestionnaireItemComponentAdapter item) {
        String definition;
        CqfExpression contextExpression;
        if (item.hasExtension(SDC_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT)
                || item.hasExtension(CPG_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT)) {
            var defPopExt = item.getExtensionByUrl(SDC_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT);
            if (defPopExt == null) {
                defPopExt = item.getExtensionByUrl(CPG_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT);
            }
            definition = defPopExt.getExtension().stream()
                    .map(IBaseExtension.class::cast)
                    .filter(e -> "definition".equals(e.getUrl()))
                    .map(IBaseExtension::getValue)
                    .map(IPrimitiveType.class::cast)
                    .map(IPrimitiveType::getValueAsString)
                    .findFirst()
                    .orElse(null);
            contextExpression = defPopExt.getExtension().stream()
                    .map(IBaseExtension.class::cast)
                    .filter(e -> "expression".equals(e.getUrl()))
                    .map(e -> CqfExpression.of(
                            request.getAdapterFactory().createBase(e.getValue()), request.getReferencedLibraries()))
                    .findFirst()
                    .orElse(null);
        } else {
            definition = item.getDefinition();
            contextExpression = expressionProcessor.getCqfExpression(
                    request, item.getExtension(), SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        }
        return new kotlin.Pair<>(definition, contextExpression);
    }

    protected void populateAnswer(
            PopulateRequest request, IQuestionnaireResponseItemComponentAdapter responseItem, List<IBase> answerValue) {
        if (answerValue == null || answerValue.isEmpty()) {
            return;
        }
        var answers = new ArrayList<IQuestionnaireResponseItemAnswerComponentAdapter>();
        for (var value : answerValue) {
            answers.add(responseItem.newAnswer(transformValueToItem(request.getFhirVersion(), value)));
        }
        responseItem.setAnswer(answers);
    }

    protected List<IBase> getInitialValue(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            IQuestionnaireResponseItemComponentAdapter responseItem,
            Map<String, Object> rawParameters) {
        List<IBase> results;
        var expression = expressionProcessor.getItemInitialExpression(request, item);
        if (expression != null) {
            var itemLinkId = item.getLinkId();
            try {
                results = expressionProcessor.getExpressionResultForItem(
                        request, expression, itemLinkId, null, rawParameters);
                if (results != null && !results.isEmpty()) {
                    addAuthorExtension(request, responseItem);
                }
            } catch (Exception e) {
                var message = "Encountered error evaluating initial expression for item %s: %s"
                        .formatted(itemLinkId, e.getMessage());
                logger.error(message);
                request.logException(message);
                results = new ArrayList<>();
            }
        } else {
            results = item.getInitial().stream().map(IBase.class::cast).toList();
        }
        return results;
    }

    protected void addAuthorExtension(PopulateRequest request, IQuestionnaireResponseItemComponentAdapter item) {
        item.addExtension(buildReferenceExt(request.getFhirVersion(), QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION, false));
    }

    protected IQuestionnaireResponseItemComponentAdapter processPopulationContext(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            String contextName,
            IAdapter<?> context,
            IStructureDefinitionAdapter profile) {
        if (item.hasItem()) {
            final var contextItem = item.newResponseItem();
            item.getItem().stream()
                    .map(IQuestionnaireItemComponentAdapter.class::cast)
                    .forEach(childItem -> {
                        var childItems = childItem.getItem();
                        if (!childItems.isEmpty()) {
                            var childItemWithChildren =
                                    processPopulationContext(request, childItem, contextName, context, profile);
                            contextItem.addItem(childItemWithChildren);
                        } else {
                            try {
                                var processedSubItem =
                                        createResponseContextItem(request, childItem, contextName, context, profile);
                                contextItem.addItem(processedSubItem);
                            } catch (Exception e) {
                                logger.error(e.getMessage());
                                request.logException(e.getMessage());
                            }
                        }
                    });
            return contextItem;
        } else {
            try {
                return createResponseContextItem(request, item, contextName, context, profile);
            } catch (Exception e) {
                logger.error(e.getMessage());
                request.logException(e.getMessage());
                return item.newResponseItem();
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected IQuestionnaireResponseItemComponentAdapter createResponseContextItem(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            String contextName,
            IAdapter<?> context,
            IStructureDefinitionAdapter profile) {
        final var responseItem = item.newResponseItem();
        if (item.hasInitial()) {
            return processSingleItem(request, item, List.of(responseItem)).get(0);
        }
        request.setContextVariable(responseItem.get());
        // if we have a definition and no initial expression use the definition to populate
        var definition = item.getDefinition();
        var initialExpressionExt = item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
        if (StringUtils.isNotBlank(definition) && profile != null && context != null && initialExpressionExt == null) {
            final var pathValue = getPathValue(request, context, definition, profile);
            if (pathValue != null) {
                var answerValue = pathValue instanceof List ? (List<IBase>) pathValue : List.of((IBase) pathValue);
                if (!answerValue.isEmpty()) {
                    addAuthorExtension(request, responseItem);
                }
                populateAnswer(request, responseItem, answerValue);
            }
        } else {
            // populate using expected initial expression extensions
            if (initialExpressionExt != null) {
                List<IBase> initialValue = null;
                if (context != null) {
                    // pass the context resource(s) as a parameter to the evaluation
                    var rawParams = request.getRawParameters();
                    rawParams.put("%" + contextName, context.get());
                    rawParams.put("%qitem", item.get());
                    initialValue = getInitialValue(request, item, responseItem, rawParams);
                }
                populateAnswer(request, responseItem, initialValue);
            }
        }
        return responseItem;
    }

    protected Object getPathValue(
            IOperationRequest request, IAdapter<?> context, String definition, IStructureDefinitionAdapter profile) {
        Object pathValue = null;
        var elementId = definition.split("#")[1];
        var sliceName = Helpers.getSliceName(elementId);
        var element = profile.getElement(elementId);
        var elementPath = element.getPath();
        var answerType = element.getTypeCode();
        var path = elementPath.substring(elementPath.indexOf(".") + 1).replace("[x]", "");
        if (StringUtils.isNotBlank(sliceName)) {
            path = path.split("\\.")[0];
        }
        pathValue = context.resolvePath(path);
        if (pathValue instanceof ArrayList<?> pathList) {
            if (elementId.contains(":")) {
                pathValue = getSliceValue(request, profile, path, sliceName, pathList);
            } else {
                pathValue = (pathList.get(0));
            }
        }
        // Ensure resource id's include the resource type
        if (pathValue instanceof IIdType idType
                && path.equals("id")
                && context.get() instanceof IBaseResource contextResource) {
            pathValue = idType.withResourceType(contextResource.fhirType());
        }

        if (pathValue != null
                && !((IBase) pathValue).fhirType().equals(answerType)
                && pathValue instanceof IPrimitiveType<?> stringPath) {
            pathValue = stringTypeForVersion(request.getFhirVersion(), stringPath.getValueAsString());
        }

        return pathValue;
    }

    protected Object getSliceValue(
            IOperationRequest request,
            IStructureDefinitionAdapter profile,
            String path,
            String sliceName,
            List<?> pathList) {
        var filterElements = profile.getSliceElements(sliceName).stream()
                .filter(IElementDefinitionAdapter::hasDefaultOrFixedOrPattern)
                .toList();
        return pathList.stream()
                .map(v -> (IBase) v)
                .filter(value -> {
                    for (var filterElement : filterElements) {
                        var filterSplit = filterElement.getPath().split("\\.");
                        var sliceIndex = -1;
                        for (int i = 0; i < filterSplit.length; i++) {
                            if (filterSplit[i].equals(path)) {
                                sliceIndex = i;
                            }
                        }
                        var filterPath = filterSplit[sliceIndex + 1];
                        var filterValue = filterElement.resolvePath(value, filterPath);
                        var filter = filterElement.getDefaultOrFixedOrPattern();
                        if (filter instanceof IPrimitiveType<?> filterString
                                && filterValue instanceof IPrimitiveType<?> valueString
                                && filterString.getValueAsString().equals(valueString.getValueAsString())) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }
}
