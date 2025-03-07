package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static java.util.Objects.nonNull;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;
import static org.opencds.cqf.fhir.utility.VersionUtilities.stringTypeForVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;
import org.opencds.cqf.fhir.cr.questionnaire.Helpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessItemWithContext extends ProcessItem {
    private static final Logger logger = LoggerFactory.getLogger(ProcessItemWithContext.class);

    public ProcessItemWithContext() {
        super();
    }

    public ProcessItemWithContext(ExpressionProcessor expressionProcessor) {
        super(expressionProcessor);
    }

    List<IBaseBackboneElement> processContextItem(PopulateRequest request, IBaseBackboneElement item) {
        var itemLinkId = request.getItemLinkId(item);
        IBaseResource profile = null;
        var definition = request.resolvePathString(item, "definition");
        if (StringUtils.isNotBlank(definition)) {
            var profileUrl = definition.split("#")[0];
            try {
                profile = searchRepositoryByCanonical(
                        request.getRepository(), canonicalTypeForVersion(request.getFhirVersion(), profileUrl));
            } catch (Exception e) {
                var message = String.format("No profile found for definition: %s", profileUrl);
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
        List<IBaseResource> populationContext;
        try {
            populationContext =
                    expressionProcessor.getExpressionResultForItem(request, contextExpression, itemLinkId).stream()
                            .map(r -> {
                                if (r instanceof IBaseResource baseResource) {
                                    return baseResource;
                                } else {
                                    var message = String.format(
                                            "Encountered error populating item (%s): Context value is expected to be a resource.",
                                            itemLinkId);
                                    logger.error(message);
                                    request.logException(message);
                                    return null;
                                }
                            })
                            // filtering nulls here to prevent unnecessary duplicate responseItems
                            .filter(r -> nonNull(r))
                            .collect(Collectors.toList());

        } catch (Exception e) {
            var message =
                    String.format("Encountered error evaluating expression: %s", contextExpression.getExpression());
            logger.error(message);
            request.logException(message);
            populationContext = new ArrayList<>();
        }
        if (populationContext.isEmpty()) {
            // We always want to return a responseItem even if we have nothing to populate
            populationContext.add(null);
        }
        return populationContext.stream()
                .map(context ->
                        processPopulationContext(request, item, contextExpression.getName(), context, profileAdapter))
                .collect(Collectors.toList());
    }

    IBaseBackboneElement processPopulationContext(
            PopulateRequest request,
            IBaseBackboneElement groupItem,
            String contextName,
            IBaseResource context,
            IStructureDefinitionAdapter profile) {
        final var contextItem = createResponseItem(request.getFhirVersion(), groupItem);
        request.getItems(groupItem).forEach(item -> {
            var childItems = request.getItems(item);
            if (!childItems.isEmpty()) {
                var childGroupItem = processPopulationContext(request, item, contextName, context, profile);
                request.getModelResolver().setValue(contextItem, "item", Collections.singletonList(childGroupItem));
            } else {
                try {
                    var processedSubItem = createResponseContextItem(request, item, contextName, context, profile);
                    request.getModelResolver()
                            .setValue(contextItem, "item", Collections.singletonList(processedSubItem));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    request.logException(e.getMessage());
                }
            }
        });
        return contextItem;
    }

    @SuppressWarnings("unchecked")
    IBaseBackboneElement createResponseContextItem(
            PopulateRequest request,
            IBaseBackboneElement item,
            String contextName,
            IBaseResource context,
            IStructureDefinitionAdapter profile) {
        if (request.resolveRawPath(item, "initial") != null) {
            return processItem(request, item);
        }
        final var responseItem = createResponseItem(request.getFhirVersion(), item);
        // if we have a definition use it to populate
        var definition = request.resolvePathString(item, "definition");
        if (StringUtils.isNotBlank(definition) && profile != null) {
            final var pathValue = getPathValue(request, context, definition, profile);
            if (pathValue != null) {
                final List<IBase> answerValue =
                        pathValue instanceof List ? (List<IBase>) pathValue : Arrays.asList((IBase) pathValue);
                if (answerValue != null && !answerValue.isEmpty()) {
                    addAuthorExtension(request, responseItem);
                }
                populateAnswer(request, responseItem, answerValue);
            }
        } else {
            var extension = request.getExtensionByUrl(item, Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION);
            // populate using expected initial expression extensions
            if (extension != null) {
                // pass the context resource(s) as a parameter to the evaluation
                request.addContextParameter("%" + contextName, context);
                populateAnswer(request, responseItem, getInitialValue(request, item, responseItem));
            }
        }
        return responseItem;
    }

    public Object getPathValue(
            IOperationRequest request, IBaseResource context, String definition, IStructureDefinitionAdapter profile) {
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

    private Object getSliceValue(
            IOperationRequest request,
            IStructureDefinitionAdapter profile,
            String path,
            String sliceName,
            ArrayList<?> pathList) {
        var filterElements = profile.getSliceElements(sliceName).stream()
                .filter(e -> e.hasDefaultOrFixedOrPattern())
                .collect(Collectors.toList());
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
