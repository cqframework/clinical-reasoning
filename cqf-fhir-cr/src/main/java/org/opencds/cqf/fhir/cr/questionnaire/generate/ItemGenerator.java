package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ExtensionProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.Helpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
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
    protected final ExpressionProcessor expressionProcessor;
    protected final ExtensionProcessor extensionProcessor;
    protected final ElementHasCaseFeature elementHasCaseFeature;
    protected final ItemTypeIsChoice itemTypeIsChoice;

    public ItemGenerator(IRepository repository) {
        this(
                repository,
                new ExpressionProcessor(),
                new ExtensionProcessor(),
                new ElementHasCaseFeature(),
                new ItemTypeIsChoice(repository));
    }

    public ItemGenerator(
            IRepository repository,
            ExpressionProcessor expressionProcessor,
            ExtensionProcessor extensionProcessor,
            ElementHasCaseFeature elementHasCaseFeature,
            ItemTypeIsChoice itemTypeIsChoice) {
        this.repository = repository;
        this.expressionProcessor = expressionProcessor;
        this.extensionProcessor = extensionProcessor;
        this.elementHasCaseFeature = elementHasCaseFeature;
        this.itemTypeIsChoice = itemTypeIsChoice;
    }

    @Nullable
    public <T extends IBaseExtension<?, ?>> Pair<IQuestionnaireItemComponentAdapter, List<T>> generate(
            GenerateRequest request) {
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
                questionnaireItem.addItems(childItems);
            }
            if (!valueExtensions.isEmpty()) {
                valueExtensions.forEach(questionnaireItem::addExtension);
            }

            // If we have a caseFeature we need to include launchContext extensions and Item Population Context
            List<T> launchContextExts = new ArrayList<>();
            if (caseFeature != null) {
                var itemContextExt = questionnaireItem.addExtension();
                itemContextExt.setUrl(Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT);
                itemContextExt.setValue(caseFeature.toExpressionType(request.getFhirVersion()));
                // Assume Patient for now - this should probably be the context of the Library if we can determine that
                launchContextExts.add(buildSdcLaunchContextExt(request, "patient", "Patient"));
                var featureLibrary = request.getAdapterFactory()
                        .createLibrary(SearchHelper.searchRepositoryByCanonical(
                                repository,
                                VersionUtilities.canonicalTypeForVersion(
                                        request.getFhirVersion(), caseFeature.getLibraryUrl())));
                // Add any other in parameters that with a type of Resource
                featureLibrary.getParameter().stream()
                        .filter(p -> request.resolvePathString(p, "use").equals("in"))
                        .filter(p -> request.getFHIRTypes().contains(request.resolvePathString(p, "type")))
                        .map(p -> new ImmutablePair<String, String>(
                                request.resolvePathString(p, "name"), request.resolvePathString(p, "type")))
                        .forEach(p -> launchContextExts.add(buildSdcLaunchContextExt(request, p.left, p.right)));
            }
            return new ImmutablePair<>(questionnaireItem, launchContextExts);
        } catch (Exception ex) {
            final String message = ITEM_CREATION_ERROR.formatted(ex.getMessage());
            logger.error(message);
            return new ImmutablePair<>(createErrorItem(request, linkId, message), new ArrayList<>());
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends IBaseExtension<?, ?>> T buildSdcLaunchContextExt(
            GenerateRequest request, String code, String type) {
        var system = "http://hl7.org/fhir/uv/sdc/CodeSystem/launchContext";
        var display = "";
        switch (code.toLowerCase()) {
            case "patient":
                display = "Patient";
                break;
            case "encounter":
                display = "Encounter";
                break;
            case "location":
                display = "Location";
                break;
            case "practitioner", "user":
                code = "user";
                display = "User";
                break;
            case "study":
                display = "ResearchStudy";
                break;
            case "clinical":
                display = "Clinical";
                break;

            default:
                display = code;
                system = "http://example.org/fhir/uv/sdc/CodeSystem/additionalLaunchContext";
                break;
        }
        var fhirVersion = request.getFhirVersion();
        var ext = ((IBaseHasExtensions) Resources.newBaseForVersion("Extension", fhirVersion)).addExtension();
        ext.setUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        var nameExt = ((IBaseHasExtensions) ext).addExtension();
        nameExt.setUrl("name");
        var nameCoding = request.getAdapterFactory().createCoding(Resources.newBaseForVersion("Coding", fhirVersion));
        nameCoding.setDisplay(display);
        nameCoding.setCode(code);
        nameCoding.setSystem(system);
        nameExt.setValue((IBaseDatatype) nameCoding.get());
        var typeExt = ((IBaseHasExtensions) ext).addExtension();
        typeExt.setUrl("type");
        typeExt.setValue(
                ((IPrimitiveType<String>) Resources.newBaseForVersion("CodeType", fhirVersion)).setValue(type));
        return (T) ext;
    }

    protected CqfExpression getFeatureExpression(GenerateRequest request) {
        var expression = expressionProcessor.getCqfExpression(
                request, request.getExtensions(request.getProfile()), Constants.CPG_FEATURE_EXPRESSION);
        if (expression != null) {
            expression.setName(request.getProfileAdapter().getName());
        }
        return expression;
    }

    protected <E extends IBaseExtension<?, ?>> Pair<List<IQuestionnaireItemComponentAdapter>, List<E>> processElements(
            GenerateRequest request,
            List<IElementDefinitionAdapter> elements,
            int childCount,
            String itemLinkId,
            CqfExpression caseFeature) {
        List<IQuestionnaireItemComponentAdapter> items = new ArrayList<>();
        List<E> valueExtensions = new ArrayList<>();
        for (var element : elements) {
            if (element.hasExtension(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE)) {
                valueExtensions.add(element.getExtensionByUrl(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE));
            } else if (!element.hasDefaultOrFixedOrPattern()) {
                // elements with default values do not need to be generated
                // they will be pulled from the profile during extraction

                IQuestionnaireItemComponentAdapter item;
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

    protected IQuestionnaireItemComponentAdapter processProfile(
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

    protected IQuestionnaireItemComponentAdapter processElement(
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
            item.addItems(childItems);
        }
        if (valueExtensions != null && !valueExtensions.isEmpty()) {
            valueExtensions.forEach(item::addExtension);
        }
        return item;
    }

    protected IQuestionnaireItemComponentAdapter createItemForElement(
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
            if (item.isGroupItem()) {
                return item;
            }
            if (item.isChoiceItem()) {
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

    protected IQuestionnaireItemComponentAdapter createErrorItem(
            GenerateRequest request, String linkId, String errorMessage) {
        return initializeQuestionnaireItem(request, "display", errorMessage, linkId, errorMessage, false, false);
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

    public IQuestionnaireItemComponentAdapter createQuestionnaireItem(GenerateRequest request, String linkId) {
        var text = request.getProfileAdapter().hasTitle()
                ? request.getProfileAdapter().getTitle()
                : request.getProfileAdapter().getName();
        var item = initializeQuestionnaireItem(
                request, "group", request.getProfileAdapter().getUrl(), linkId, text, false, isRepeats(request));
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

    protected IQuestionnaireItemComponentAdapter initializeQuestionnaireItem(
            GenerateRequest request,
            String itemType,
            String definition,
            String linkId,
            String text,
            boolean required,
            boolean repeats) {
        return request.getAdapterFactory()
                .createQuestionnaireItem()
                .setType(itemType)
                .setDefinition(definition)
                .setLinkId(linkId)
                .setText(text)
                .setRequired(required)
                .setRepeats(repeats);
    }

    protected String getElementText(IElementDefinitionAdapter element) {
        return element.hasLabel() ? element.getLabel() : getElementDescription(element);
    }

    protected String getElementDescription(IElementDefinitionAdapter element) {
        return element.hasShort() ? element.getShort() : element.getPath();
    }
}
