package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION;
import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildReferenceExt;
import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToItem;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.stringTypeForVersion;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;
import org.opencds.cqf.fhir.cr.questionnaire.Helpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
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

    public ItemProcessor() {
        this(new ExpressionProcessor());
    }

    public ItemProcessor(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    public List<IQuestionnaireResponseItemComponentAdapter> processItem(
            PopulateRequest request, IQuestionnaireItemComponentAdapter item) {
        var populationContextExt = item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        return populationContextExt != null
                ? processContextItem(request, item)
                : List.of(processSingleItem(request, item));
    }

    protected IQuestionnaireResponseItemComponentAdapter processSingleItem(
            PopulateRequest request, IQuestionnaireItemComponentAdapter item) {
        final var responseItem = item.newResponseItem();
        var childItems = item.getItem().stream()
                .map(IQuestionnaireItemComponentAdapter.class::cast)
                .toList();
        if (item.isGroupItem()) {
            final List<IQuestionnaireResponseItemComponentAdapter> groupChildItems = new ArrayList<>();
            childItems.forEach(childItem -> {
                groupChildItems.addAll(processItem(request, childItem));
            });
            responseItem.setItem(groupChildItems);
        } else {
            request.setContextVariable(responseItem.get());
            var rawParams = request.getRawParameters();
            rawParams.put("%qitem", item.get());
            populateAnswer(request, responseItem, getInitialValue(request, item, responseItem, rawParams));
            if (!childItems.isEmpty()) {
                // children of non group items go under each answer
                var childResponseItems = childItems.stream()
                        .map(c -> processSingleItem(request, c))
                        .toList();
                var answers = responseItem.getAnswer();
                if (answers.isEmpty()) {
                    answers.add(responseItem.newAnswer(null));
                    responseItem.setAnswer(answers);
                }
                answers.forEach(a -> a.setItem(childResponseItems));
            }
        }
        return responseItem;
    }

    protected List<IQuestionnaireResponseItemComponentAdapter> processContextItem(
            PopulateRequest request, IQuestionnaireItemComponentAdapter item) {
        var itemLinkId = item.getLinkId();
        if (!item.isGroupItem()) {
            throw new UnprocessableEntityException(
                    "Encountered Item Population Context extension on a non group item: {}", itemLinkId);
        }
        IBaseResource profile = null;
        var definition = item.getDefinition();
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
        final CqfExpression contextExpression = expressionProcessor.getCqfExpression(
                request, item.getExtension(), Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
        List<Object> populationContext;
        try {
            populationContext =
                    expressionProcessor
                            .getExpressionResultForItem(request, contextExpression, itemLinkId, null, null)
                            .stream()
                            .map(r -> {
                                if (r.fhirType().equals("Tuple")) {
                                    return new Tuple()
                                            .withElements(request.getAdapterFactory()
                                                    .createTuple(r)
                                                    .getProperties());
                                }
                                return r;
                            })
                            // filtering nulls here to prevent unnecessary duplicate responseItems
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error(e.getMessage());
            request.logException(e.getMessage());
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
        return populationContext.stream()
                .map(context ->
                        processPopulationContext(request, item, contextExpression.getName(), context, profileAdapter))
                .collect(Collectors.toList());
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
            results = item.getInitial().stream()
                    .map(i -> request.resolvePath(i, "value", IBase.class))
                    .collect(Collectors.toList());
        }
        return results;
    }

    protected void addAuthorExtension(PopulateRequest request, IQuestionnaireResponseItemComponentAdapter item) {
        item.addExtension(buildReferenceExt(request.getFhirVersion(), QUESTIONNAIRE_RESPONSE_AUTHOR_EXTENSION, false));
    }

    protected IQuestionnaireResponseItemComponentAdapter processPopulationContext(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter groupItem,
            String contextName,
            Object context,
            IStructureDefinitionAdapter profile) {
        final var contextItem = groupItem.newResponseItem();
        groupItem.getItem().stream()
                .map(IQuestionnaireItemComponentAdapter.class::cast)
                .forEach(item -> {
                    var childItems = item.getItem();
                    if (!childItems.isEmpty()) {
                        var childGroupItem = processPopulationContext(request, item, contextName, context, profile);
                        contextItem.addItem(childGroupItem);
                    } else {
                        try {
                            var processedSubItem =
                                    createResponseContextItem(request, item, contextName, context, profile);
                            contextItem.addItem(processedSubItem);
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                            request.logException(e.getMessage());
                        }
                    }
                });
        return contextItem;
    }

    @SuppressWarnings("unchecked")
    protected IQuestionnaireResponseItemComponentAdapter createResponseContextItem(
            PopulateRequest request,
            IQuestionnaireItemComponentAdapter item,
            String contextName,
            Object context,
            IStructureDefinitionAdapter profile) {
        if (item.hasInitial()) {
            return processSingleItem(request, item);
        }
        final var responseItem = item.newResponseItem();
        request.setContextVariable(responseItem.get());
        // if we have a definition use it to populate
        var definition = item.getDefinition();
        if (StringUtils.isNotBlank(definition) && profile != null) {
            final var pathValue = getPathValue(request, context, definition, profile);
            if (pathValue != null) {
                final List<IBase> answerValue =
                        pathValue instanceof List ? (List<IBase>) pathValue : List.of((IBase) pathValue);
                if (!answerValue.isEmpty()) {
                    addAuthorExtension(request, responseItem);
                }
                populateAnswer(request, responseItem, answerValue);
            }
        } else {
            var extension = item.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
            // populate using expected initial expression extensions
            if (extension != null) {
                // pass the context resource(s) as a parameter to the evaluation
                var rawParams = request.getRawParameters();
                rawParams.put("%" + contextName, context);
                rawParams.put("%qitem", item.get());
                populateAnswer(request, responseItem, getInitialValue(request, item, responseItem, rawParams));
            }
        }
        return responseItem;
    }

    protected Object getPathValue(
            IOperationRequest request, Object context, String definition, IStructureDefinitionAdapter profile) {
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
        pathValue = request.getModelResolver().resolvePath(context, path);
        if (pathValue instanceof ArrayList<?> pathList) {
            if (elementId.contains(":")) {
                pathValue = getSliceValue(request, profile, path, sliceName, pathList);
            } else {
                pathValue = (pathList.get(0));
            }
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
            ArrayList<?> pathList) {
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
                        var filterValue = request.resolvePath(value, filterPath);
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
