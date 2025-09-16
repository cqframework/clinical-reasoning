package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.cr.common.ExtensionBuilders.buildSdcLaunchContextExt;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.Helpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ItemGenerator {
    protected static final Logger logger = LoggerFactory.getLogger(ItemGenerator.class);
    protected static final String NO_PROFILE_ERROR = "No profile defined for input. Unable to generate item.";
    protected static final String ITEM_CREATION_ERROR = "An error occurred during item creation: %s";
    protected static final String CHILD_LINK_ID_FORMAT = "%s.%s";

    protected final IRepository repository;
    protected final CqlEngine engine;
    protected final ExpressionProcessor expressionProcessor;
    protected final ExtensionProcessor extensionProcessor;
    protected final ElementHasCaseFeature elementHasCaseFeature;
    protected final ItemTypeIsChoice itemTypeIsChoice;

    public ItemGenerator(IRepository repository) {
        this.repository = repository;
        engine = Engines.forRepository(this.repository);
        expressionProcessor = new ExpressionProcessor();
        extensionProcessor = new ExtensionProcessor();
        elementHasCaseFeature = new ElementHasCaseFeature();
        itemTypeIsChoice = new ItemTypeIsChoice(repository);
    }

    @Nullable
    public <T extends IBaseExtension<?, ?>> Pair<IBaseBackboneElement, List<T>> generate(GenerateRequest request) {
        final String linkId =
                String.valueOf(request.getQuestionnaireAdapter().getItem().size() + 1);
        try {
            int childCount = 0;
            var caseFeature = getFeatureExpression(request);
            var parentElements = getElements(request, null, null);
            var results = processElements(request, parentElements, childCount, linkId, caseFeature);
            var childItems = results.getLeft();
            var valueExtensions = results.getRight();
            if (childItems.isEmpty() && valueExtensions.isEmpty()) {
                return null;
            }
            var questionnaireItem = createQuestionnaireItem(request, linkId);
            if (!childItems.isEmpty()) {
                request.getModelResolver().setValue(questionnaireItem, "item", childItems);
            }
            if (!valueExtensions.isEmpty()) {
                request.getModelResolver().setValue(questionnaireItem, "extension", valueExtensions);
            }

            // If we have a caseFeature we need to include launchContext extensions and Item Population Context
            List<T> launchContextExts = new ArrayList<>();
            if (caseFeature != null) {
                var itemContextExt = questionnaireItem.addExtension();
                itemContextExt.setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
                itemContextExt.setValue(caseFeature.toExpressionType(request.getFhirVersion()));
                // Assume Patient for now - this should probably be the context of the Library if we can determine that
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
                                            && Arrays.stream(SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE.values())
                                                    .map(Object::toString)
                                                    .toList()
                                                    .contains(name);
                        })
                        .map(p -> request.resolvePathString(p, "name").toLowerCase())
                        .toList();
                inParameters.forEach(p -> launchContextExts.add(buildSdcLaunchContextExt(request.getFhirVersion(), p)));
            }
            return new ImmutablePair<>(questionnaireItem, launchContextExts);
        } catch (Exception ex) {
            final String message = ITEM_CREATION_ERROR.formatted(ex.getMessage());
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

    protected <E extends IBaseExtension<?, ?>> Pair<List<IBaseBackboneElement>, List<E>> processElements(
            GenerateRequest request,
            List<IElementDefinitionAdapter> elements,
            int childCount,
            String itemLinkId,
            CqfExpression caseFeature) {
        List<IBaseBackboneElement> items = new ArrayList<>();
        List<E> valueExtensions = new ArrayList<>();
        for (var element : elements) {
            if (element.hasExtension(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE)) {
                valueExtensions.add(element.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE));
            } else if (!element.hasDefaultOrFixedOrPattern()) {
                // elements with default values do not need to be generated
                // they will be pulled from the profile during extraction

                IBaseBackboneElement item;
                childCount++;
                var childLinkId = CHILD_LINK_ID_FORMAT.formatted(itemLinkId, childCount);
                item = processElement(request, caseFeature, element, childLinkId);
                if (item == null) {
                    childCount--;
                } else {
                    items.add(item);
                }
            }
        }
        return new ImmutablePair<>(items, valueExtensions);
    }

    protected IBaseBackboneElement processProfile(
            GenerateRequest request, IStructureDefinitionAdapter profileAdapter, String childLinkId) {
        // generate a new group item from the profile
        // if the generated group item has only 1 child return it
        throw new NotImplementedException("Nested profile generation is not currently supported.");
    }

    protected IStructureDefinitionAdapter getElementProfile(GenerateRequest request, String elementProfile) {
        try {
            return (IStructureDefinitionAdapter) request.getAdapterFactory()
                    .createKnowledgeArtifactAdapter((IDomainResource) searchRepositoryByCanonical(
                            request.getRepository(),
                            canonicalTypeForVersion(request.getFhirVersion(), elementProfile)));
        } catch (Exception e) {
            return null;
        }
    }

    protected IBaseBackboneElement processElement(
            GenerateRequest request, CqfExpression caseFeature, IElementDefinitionAdapter element, String childLinkId) {
        // if the element type has a profile we will generate a new group item
        // from that profile if we can find it in the repository
        IStructureDefinitionAdapter profileAdapter = null;
        var elementProfile = element.getTypeProfile();
        if (StringUtils.isNotBlank(elementProfile)) {
            profileAdapter = getElementProfile(request, elementProfile);
            if (profileAdapter != null) {
                return processProfile(request, profileAdapter, childLinkId);
            }
        }
        // if child elements exist process them first
        var childElements = getElements(request, element.getPath(), element.getSliceName());
        var results = processElements(request, childElements, 0, childLinkId, caseFeature);
        var childItems = results.getLeft();
        var valueExtensions = results.getRight();
        var item = createItemForElement(request, element, childLinkId, caseFeature, !childItems.isEmpty());
        if (!childItems.isEmpty()) {
            request.getModelResolver().setValue(item, "item", childItems);
        }
        if (!valueExtensions.isEmpty()) {
            request.getModelResolver().setValue(item, "extension", valueExtensions);
        }
        return item;
    }

    protected IBaseBackboneElement createItemForElement(
            GenerateRequest request,
            IElementDefinitionAdapter element,
            String childLinkId,
            CqfExpression caseFeature,
            Boolean isGroup) {
        try {
            var elementType = getElementType(request, element);
            final var itemType = Helpers.parseItemTypeForVersion(
                    request.getFhirVersion(), elementType, element.hasBinding(), isGroup);
            if (itemType == null) {
                return null;
            }
            final String definition = request.getProfileAdapter().getUrl() + "#" + element.getId();
            final var required = element.hasMin() && element.getMin() > 0;
            final var repeats = element.hasMax() && !element.getMax().equals("1");
            final var item = initializeQuestionnaireItem(
                    request, itemType, definition, childLinkId, getElementText(element), required, repeats);
            if (Helpers.isGroupItemType(itemType)) {
                return item;
            }
            if (Helpers.isChoiceItemType(itemType)) {
                itemTypeIsChoice.addProperties(element, item);
            }
            if (caseFeature != null) {
                elementHasCaseFeature.addProperties(request, caseFeature, request.getProfileAdapter(), element, item);
            }
            return item;
        } catch (Exception ex) {
            final String message = ITEM_CREATION_ERROR.formatted(ex.getMessage());
            logger.warn(message);
            return createErrorItem(request, childLinkId, message);
        }
    }

    protected List<IElementDefinitionAdapter> getElements(
            GenerateRequest request, String parentPath, String sliceName) {
        // Add all elements from the differential
        var elements = request.getDifferentialElements().stream()
                .filter(e -> filterElement(request, e, null, parentPath, sliceName, false))
                .collect(Collectors.toList());
        // Add elements from the snapshot that were not in the differential
        if (request.getSnapshotElements() != null) {
            elements.addAll(request.getProfileAdapter().getSnapshotElements().stream()
                    .filter(e -> filterElement(request, e, elements, parentPath, sliceName, request.getRequiredOnly()))
                    .toList());
        }
        return elements;
    }

    protected boolean filterElement(
            GenerateRequest request,
            IElementDefinitionAdapter element,
            List<IElementDefinitionAdapter> existingElements,
            String parentPath,
            String sliceName,
            boolean requiredOnly) {
        var path = element.getPath();
        if (elementExists(existingElements, path)) {
            return false;
        }
        if (element.hasDefaultOrFixedOrPattern()) {
            return false;
        }
        // filter out slicing definitions
        if (element.hasSlicing()) {
            return false;
        }
        if (notInPath(path, parentPath)) {
            return false;
        }
        if (sliceName != null) {
            return element.getId().contains(sliceName);
        }
        if (requiredOnly) {
            return element.isRequired();
        }
        if (request.getSupportedOnly()) {
            return element.getMustSupport();
        }

        return true;
    }

    protected boolean elementExists(List<IElementDefinitionAdapter> existingElements, String path) {
        return existingElements != null
                && !existingElements.isEmpty()
                && existingElements.stream().anyMatch(e -> e.getPath().equals(path));
    }

    protected boolean notInPath(String path, String parentPath) {
        var pathSplit = path.split("\\.");
        if (parentPath == null) {
            // grab only top level elements
            return pathSplit.length > 2;
        } else {
            // grab only the next level of elements
            var splitLength = parentPath.split("\\.").length + 1;
            return pathSplit.length > splitLength || !path.contains(parentPath + ".");
        }
    }

    protected <E extends ICompositeType> boolean isRequiredPath(GenerateRequest request, E element) {
        var min = request.resolvePath(element, "min", IPrimitiveType.class);
        return (min != null && (Integer) min.getValue() != 0);
    }

    protected IBaseBackboneElement createErrorItem(GenerateRequest request, String linkId, String errorMessage) {
        return createQuestionnaireItemComponent(request, errorMessage, linkId, null, true, false);
    }

    protected String getElementType(GenerateRequest request, IElementDefinitionAdapter element) {
        var type = element == null ? null : element.getTypeCode();
        if (type == null && element != null) {
            // Attempt to resolve the type from the Snapshot if it is available
            var path = element.getPath();
            var snapshot = request.getSnapshotElements() == null
                    ? null
                    : request.getSnapshotElements().stream()
                            .filter(e -> e.getPath().equals(path))
                            .findFirst()
                            .orElse(null);
            if (snapshot != null) {
                type = snapshot.getTypeCode();
            }
        }
        return type;
    }

    public IBaseBackboneElement createQuestionnaireItem(GenerateRequest request, String linkId) {
        var text = request.getProfileAdapter().hasTitle()
                ? request.getProfileAdapter().getTitle()
                : request.getProfileAdapter().getName();
        var item = createQuestionnaireItemComponent(
                request, text, linkId, request.getProfileAdapter().getUrl(), false, isRepeats(request));
        var definitionExtract = item.addExtension();
        definitionExtract.setUrl(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT);
        definitionExtract.setValue(canonicalTypeForVersion(
                request.getFhirVersion(), request.getProfileAdapter().getUrl()));
        return item;
    }

    protected Boolean isRepeats(GenerateRequest request) {
        // Determine whether the group should repeat based on the expected cardinality of
        // the Case Feature Expression result.
        var caseFeatureExtension = request.getProfileAdapter().getExtensionByUrl(Constants.CPG_FEATURE_EXPRESSION);
        if (caseFeatureExtension != null) {
            var expressionType = caseFeatureExtension.getValue();
            var expression = request.resolvePathString(expressionType, "expression");
            var reference = request.resolvePathString(expressionType, "reference");
            if (StringUtils.isNotBlank(expression) && StringUtils.isNotBlank(reference)) {
                var libraryCanonical = canonicalTypeForVersion(request.getFhirVersion(), reference);
                ILibraryAdapter library = null;
                try {
                    library = request.getAdapterFactory()
                            .createLibrary(SearchHelper.searchRepositoryByCanonical(repository, libraryCanonical));
                } catch (Exception e) {
                    logger.warn("Unable to find Library {} for Case Feature Expression {}", reference, expression);
                }
                if (library != null) {
                    var resultParam = library.getParameter().stream()
                            .filter(p -> expression.equals(request.resolvePathString(p, "name")))
                            .findFirst()
                            .orElse(null);
                    if (resultParam != null) {
                        var max = request.resolvePathString(resultParam, "max");
                        return StringUtils.isNotBlank(max) && !max.equals("1");
                    }
                }
            }
        }
        return false;
    }

    protected IBaseBackboneElement createQuestionnaireItemComponent(
            GenerateRequest request,
            String text,
            String linkId,
            String definition,
            Boolean isDisplay,
            Boolean isRepeats) {
        Object itemType =
                switch (request.getFhirVersion()) {
                    case R4 -> Boolean.TRUE.equals(isDisplay)
                            ? org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.DISPLAY
                            : org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType.GROUP;
                    case R5 -> Boolean.TRUE.equals(isDisplay)
                            ? org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.DISPLAY
                            : org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType.GROUP;
                    default -> null;
                };
        return initializeQuestionnaireItem(request, itemType, definition, linkId, text, false, isRepeats);
    }

    protected IBaseBackboneElement initializeQuestionnaireItem(
            GenerateRequest request,
            Object itemType,
            String definition,
            String linkId,
            String text,
            boolean required,
            boolean repeats) {
        return switch (request.getFhirVersion()) {
            case R4 -> new org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent()
                    .setType((org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType) itemType)
                    .setDefinition(definition)
                    .setLinkId(linkId)
                    .setText(text)
                    .setRequired(required)
                    .setRepeats(repeats);
            case R5 -> new org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent()
                    .setType((org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemType) itemType)
                    .setDefinition(definition)
                    .setLinkId(linkId)
                    .setText(text)
                    .setRequired(required)
                    .setRepeats(repeats);
            default -> null;
        };
    }

    protected String getElementText(IElementDefinitionAdapter element) {
        return element.hasLabel() ? element.getLabel() : getElementDescription(element);
    }

    protected String getElementDescription(IElementDefinitionAdapter element) {
        return element.hasShort() ? element.getShort() : element.getPath();
    }
}
