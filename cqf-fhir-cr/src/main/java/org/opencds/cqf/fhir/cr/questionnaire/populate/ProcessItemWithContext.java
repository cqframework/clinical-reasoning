package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static java.util.Objects.nonNull;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

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
        final List<IBaseResource> populationContext =
                expressionProcessor.getExpressionResultForItem(request, contextExpression, itemLinkId).stream()
                        .map(r -> {
                            if (r instanceof IBaseResource) {
                                return (IBaseResource) r;
                            } else {
                                var message = String.format(
                                        "Encountered error populating item (%s): Context value is expected to be a resource.",
                                        itemLinkId);
                                logger.error(message);
                                request.logException(message);
                                return null;
                            }
                        })
                        .filter(r -> nonNull(r))
                        .collect(Collectors.toList());
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
                populateAnswer(request, responseItem, getInitialValue(request, item));
            }
        }
        return responseItem;
    }

    public Object getPathValue(
            IOperationRequest request, IBaseResource context, String definition, IStructureDefinitionAdapter profile) {
        Object pathValue = null;
        var elementId = definition.split("#")[1];
        var pathSplit = elementId.split("\\.");
        if (pathSplit.length > 2) {
            pathValue = getNestedPath(request, context, profile, elementId, pathSplit);
        } else {
            var path = pathSplit[pathSplit.length - 1].replace("[x]", "");
            pathValue = request.resolveRawPath(context, path);
        }
        return pathValue;
    }

    private Object getNestedPath(
            IOperationRequest request,
            Object pathValue,
            IStructureDefinitionAdapter profile,
            String elementId,
            String[] pathSplit) {
        String slice = null;
        for (int i = 1; i < pathSplit.length; i++) {
            if (pathValue instanceof List && !((List<?>) pathValue).isEmpty()) {
                if (slice != null && ((List<?>) pathValue).size() > 1) {
                    pathValue = getSliceValue(request, profile, pathValue, elementId, pathSplit, slice, i);
                } else {
                    pathValue = ((List<?>) pathValue).get(0);
                }
            }
            slice = pathSplit[i].contains(":") ? pathSplit[i].substring(pathSplit[i].indexOf(":") + 1) : null;
            pathValue = request.resolveRawPath(
                    pathValue, pathSplit[i].replace("[x]", "").replace(":" + slice, ""));
        }
        return pathValue;
    }

    @SuppressWarnings("unchecked")
    private Object getSliceValue(
            IOperationRequest request,
            IStructureDefinitionAdapter profile,
            Object pathValue,
            String elementId,
            String[] pathSplit,
            String slice,
            int i) {
        final var sliceName = slice;
        final var filterIndex = i;
        final var pathValues = ((List<?>) pathValue);
        var filterElements = profile.getDifferentialElements().stream()
                .filter(e -> {
                    var id = request.resolvePathString(e, "id");
                    return !id.equals(elementId)
                            && request.resolveRawPath(e, "sliceName") == null
                            && id.contains(sliceName);
                })
                .collect(Collectors.toList());
        var filterValues = new ArrayList<>();
        filterElements.forEach(e -> {
            var path = request.resolvePathString(e, "path");
            var elementPath = elementId.replace(":" + sliceName, "");
            var filterPath = path.replace(elementPath.substring(0, elementPath.indexOf(pathSplit[filterIndex])), "");
            var filterValue = request.resolvePath(e, "fixed");
            pathValues.stream().forEach(v -> {
                var value = request.resolvePath((IBase) v, filterPath);
                if (value instanceof IPrimitiveType && filterValue instanceof IPrimitiveType) {
                    if (((IPrimitiveType<String>) value)
                            .getValueAsString()
                            .equals(((IPrimitiveType<String>) filterValue).getValueAsString())) {
                        filterValues.add(v);
                    }
                } else if (value.equals(filterValue)) {
                    filterValues.add(v);
                }
            });
        });
        pathValue = filterValues.isEmpty() ? null : filterValues.get(0);
        return pathValue;
    }
}
