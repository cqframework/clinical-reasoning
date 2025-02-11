package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static org.opencds.cqf.fhir.cr.common.ItemValueTransformer.transformValueToResource;
import static org.opencds.cqf.fhir.utility.Resources.getClassForTypeAndVersion;
import static org.opencds.cqf.fhir.utility.Resources.newBase;
import static org.opencds.cqf.fhir.utility.Resources.newBaseForVersion;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

import ca.uhn.fhir.context.BaseRuntimeChildDatatypeDefinition;
import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.RuntimeChildChoiceDefinition;
import ca.uhn.fhir.context.RuntimeChildExtension;
import ca.uhn.fhir.context.RuntimeChildPrimitiveDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeChildPrimitiveEnumerationDatatypeDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDefinitionItem {
    protected static final String ID_PATH = "id";
    protected static final String DEFINITION_PATH = "definition";
    protected static final String VALUE_PATH = "value";
    protected static final Logger logger = LoggerFactory.getLogger(ProcessDefinitionItem.class);
    final ExpressionProcessor expressionProcessor;

    public ProcessDefinitionItem() {
        this(new ExpressionProcessor());
    }

    public ProcessDefinitionItem(ExpressionProcessor expressionProcessor) {
        this.expressionProcessor = expressionProcessor;
    }

    public IBaseResource processDefinitionItem(ExtractRequest request, ItemPair item) {
        // Definition-based extraction -
        // http://build.fhir.org/ig/HL7/sdc/extraction.html#definition-based-extraction

        var linkId = request.getItemLinkId(item.getResponseItem());
        if (StringUtils.isBlank(linkId)) {
            linkId = "Questionnaire.root";
        }
        var definitionProfile = getDefinitionProfile(request, item);
        var valueExtensions = getValueExtensions(request, item);
        var definition = getDefinition(request, item);
        var profileUrl = definitionProfile.right == null ? definition : definitionProfile.right;
        var profile = getProfile(request, profileUrl);
        var resourceType = getResourceType(linkId, definitionProfile, definition, profile);
        var extractResource = getExtractResource();
        var isCreatedResource = extractResource == null;
        var resource = isCreatedResource
                ? (IBaseResource) newBaseForVersion(resourceType, request.getFhirVersion())
                : extractResource;
        processResource(request, resource, profile, isCreatedResource, item, valueExtensions);
        return resource;
    }

    protected String getResourceType(
            String linkId,
            ImmutablePair<String, String> definitionProfile,
            String definition,
            Optional<IStructureDefinitionAdapter> profile) {
        var resourceType = definitionProfile.left;
        if (StringUtils.isEmpty(resourceType)) {
            if (profile.isPresent()) {
                resourceType = profile.get().getType();
            } else if (definitionProfile.right != null) {
                var split = definitionProfile.right.split("\\/");
                resourceType = split[split.length - 1];
            } else {
                if (definition == null) {
                    throw new IllegalArgumentException(
                            String.format("Unable to retrieve definition for item: %s", linkId));
                }
                resourceType = getDefinitionType(definition);
            }
        }
        return resourceType;
    }

    protected <E extends IBaseExtension<?, ?>> E getExtractExtension(ExtractRequest request, ItemPair item) {
        var url = Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT;
        E ext = request.getExtensionByUrl(getExtensionElement(request, item, url), url);
        if (ext != null) {
            return ext;
        }
        var deprecatedUrl = Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT;
        return request.getExtensionByUrl(getExtensionElement(request, item, deprecatedUrl), deprecatedUrl);
    }

    protected <E extends IBaseExtension<?, ?>> List<E> getValueExtensions(ExtractRequest request, ItemPair item) {
        var url = Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE;
        return request.getExtensionsByUrl(getExtensionElement(request, item, url), url);
    }

    protected IBase getExtensionElement(ExtractRequest request, ItemPair item, String url) {
        // First, check the Questionnaire.item
        // Second, check the QuestionnaireResponse.item
        // Third, check the Questionnaire
        IBase element;
        if (request.hasExtension(item.getItem(), url)) {
            element = item.getItem();
        } else if (request.hasExtension(item.getResponseItem(), url)) {
            element = item.getResponseItem();
        } else {
            element = request.getQuestionnaire();
        }
        return element;
    }

    @SuppressWarnings("unchecked")
    protected ImmutablePair<String, String> getDefinitionProfile(ExtractRequest request, ItemPair item) {
        String resourceType = null;
        String profile = null;
        var extractExtension = getExtractExtension(request, item);
        if (extractExtension != null) {
            var extValue = extractExtension.getValue();
            if (extValue instanceof IPrimitiveType) {
                var stringValue = ((IPrimitiveType<String>) extValue).getValueAsString();
                if (stringValue.contains("/")) {
                    profile = stringValue;
                } else {
                    resourceType = stringValue;
                }
            }
        }
        return new ImmutablePair<>(resourceType, profile);
    }

    protected IBaseResource getExtractResource() {
        // Not currently implemented
        // If the item has a definition with a path of 'id' then attempt to resolve that resource and update it
        // Entry request needs to be PUT instead of POST
        return null;
    }

    @SuppressWarnings("squid:S1905")
    protected void processResource(
            ExtractRequest request,
            IBaseResource resource,
            Optional<IStructureDefinitionAdapter> profile,
            boolean isCreatedResource,
            ItemPair item,
            List<IBaseExtension<?, ?>> valueExtensions) {
        var resourceDefinition = request.getFhirContext().getElementDefinition(resource.getClass());
        if (isCreatedResource) {
            var id = request.getExtractId();
            var linkId = request.getItemLinkId(item.getResponseItem());
            if (StringUtils.isNotBlank(linkId)) {
                id = id.concat(String.format("-%s", linkId));
            }
            // casting here to identify the signature
            resource.setId((IIdType) Ids.newId(request.getFhirVersion(), id));
            resolveMeta(resource, profile);
        }
        valueExtensions.forEach(valueExt -> processValueExtension(request, resource, profile, valueExt));
        List<IBaseBackboneElement> responseItems;
        List<IBaseBackboneElement> questionnaireItems;
        if (item.getResponseItem() != null
                && request.getItems(item.getResponseItem()).isEmpty()) {
            responseItems = Arrays.asList(item.getResponseItem());
            questionnaireItems = Arrays.asList(item.getItem());
        } else {
            responseItems = request.getItems(
                    item.getResponseItem() == null ? request.getQuestionnaireResponse() : item.getResponseItem());
            questionnaireItems = request.getItems(item.getItem() == null ? request.getQuestionnaire() : item.getItem());
        }
        processItems(
                request,
                resourceDefinition,
                profile,
                resource,
                new ImmutablePair<>(responseItems, questionnaireItems),
                false,
                StringUtils.EMPTY);
        processDefaultItems(request, resource, profile);
    }

    protected void processDefaultItems(
            ExtractRequest request, IBaseResource resource, Optional<IStructureDefinitionAdapter> profile) {
        // If we have the profile go through each differential element and add any default values
        if (profile.isPresent()) {
            var defaultElements = profile.get().getDifferentialElements().stream()
                    .filter(e -> e.hasDefaultOrFixedOrPattern())
                    .collect(Collectors.toList());
            defaultElements.forEach(e -> {
                var value = e.getDefaultOrFixedOrPattern();
                if (value != null) {
                    var path = getPath(e);
                    var idSplit = e.getId().split(":");
                    // Ignore child slices, they are handled while processing the parent item
                    if (!(idSplit.length > 1 && idSplit[1].contains("."))) {
                        request.getModelResolver().setValue(resource, path, value);
                    }
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    protected void processValueExtension(
            ExtractRequest request,
            IBaseResource resource,
            Optional<IStructureDefinitionAdapter> profile,
            IBaseExtension<?, ?> valueExt) {
        var definitionExt = request.getExtensionByUrl(valueExt, DEFINITION_PATH);
        var fixedValueExt = request.getExtensionByUrl(valueExt, "fixed-value");
        var expressionExt = request.getExtensionByUrl(valueExt, "expression");
        if (definitionExt != null && (expressionExt != null || fixedValueExt != null)) {
            var definition = ((IPrimitiveType<String>) definitionExt.getValue()).getValueAsString();
            var value = fixedValueExt != null
                    ? fixedValueExt.getValue()
                    : expressionProcessor.getExpressionResult(request, CqfExpression.of(expressionExt, null));
            if (value != null) {
                var path = getPathAdapter(request, profile, definition);
                request.getModelResolver().setValue(resource, path.left, value);
            }
        }
    }

    private ImmutablePair<String, IStructureDefinitionAdapter> getPathAdapter(
            ExtractRequest request, Optional<IStructureDefinitionAdapter> profile, String definition) {
        var split = definition.split("#");
        var canonical = split[0];
        var id = split[1];
        // TODO: check if profile url matches canonical
        var adapter = profile.orElseGet(() -> getProfile(request, canonical).orElse(null));
        var path = getPath(adapter, id);
        return new ImmutablePair<>(path, adapter);
    }

    protected Optional<IStructureDefinitionAdapter> getProfile(ExtractRequest request, String definition) {
        if (StringUtils.isNotBlank(definition)) {
            var canonical =
                    canonicalTypeForVersion(request.getFhirVersion(), definition.split("#")[0]);
            try {
                return Optional.of((IStructureDefinitionAdapter)
                        request.getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource)
                                searchRepositoryByCanonical(request.getRepository(), canonical)));
            } catch (Exception e) {
                logger.error(
                        String.format("Encountered error retrieving profile %s: %s", canonical, e.getMessage()), e);
            }
        }
        return Optional.empty();
    }

    protected void processItems(
            ExtractRequest request,
            BaseRuntimeElementDefinition<?> resourceDefinition,
            Optional<IStructureDefinitionAdapter> profile,
            IBase resource,
            ImmutablePair<List<IBaseBackboneElement>, List<IBaseBackboneElement>> items,
            boolean isNestedRepeating,
            String parentPath) {
        var responseItems = items.left;
        var questionnaireItems = items.right;
        responseItems.forEach(childItem -> {
            var itemPair = new ItemPair(request.getQuestionnaireItem(childItem, questionnaireItems), childItem);
            processChildItem(request, resourceDefinition, profile, resource, itemPair, isNestedRepeating, parentPath);
        });
    }

    @SuppressWarnings("unchecked")
    protected boolean getIsRepeating(ExtractRequest request, IBaseBackboneElement questionnaireItem) {
        var repeats = request.resolvePath(questionnaireItem, "repeats");
        return repeats != null && ((IPrimitiveType<Boolean>) repeats).getValue();
    }

    protected String stripTypeFromPath(String path) {
        // First element is always the resource type, so it can be ignored
        return path.substring(path.indexOf(".") + 1);
    }

    protected String getPath(IElementDefinitionAdapter element) {
        return stripTypeFromPath(element.getPath());
    }

    protected String getPath(IStructureDefinitionAdapter profile, String id) {
        var path = id;
        if (profile != null) {
            var element = profile.getElement(id);
            if (element != null) {
                path = element.getPath();
            }
        }
        return stripTypeFromPath(path);
    }

    protected void processChildItem(
            ExtractRequest request,
            BaseRuntimeElementDefinition<?> resourceDefinition,
            Optional<IStructureDefinitionAdapter> profile,
            IBase parent,
            ItemPair itemPair,
            boolean isNestedRepeating,
            String parentPath) {
        var definition = getDefinition(request, itemPair);
        var children = request.getItems(itemPair.getResponseItem());
        var repeats = getIsRepeating(request, itemPair.getItem());
        if (StringUtils.isBlank(definition)) {
            processItems(
                    request,
                    resourceDefinition,
                    profile,
                    parent,
                    new ImmutablePair<>(children, request.getItems(itemPair.getItem())),
                    repeats,
                    parentPath);
            return;
        }
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(String.format(
                    "Invalid definition encountered for item %s", request.getItemLinkId(itemPair.getResponseItem())));
        }
        var pathAdapter = getPathAdapter(request, profile, definition);
        var path = pathAdapter.left;
        var adapter = pathAdapter.right;
        var identifiers = path.split("\\.");
        var propertyDefs = getPropertyDefinitions(request, resourceDefinition, adapter, identifiers);
        if (!children.isEmpty()) {
            var prop = identifiers[identifiers.length - 1];
            var element = repeats ? null : getElement(request, parent, path);
            if (element == null) {
                if (propertyDefs.get(prop) instanceof BaseRuntimeChildDatatypeDefinition datatypeDef) {
                    element = newBase(datatypeDef.getDatatype());
                } else if (adapter != null) {
                    var elementDef = adapter.getElementByPath(path);
                    element = newBaseForVersion(elementDef.getTypeCode(), request.getFhirVersion());
                } else if (propertyDefs.get(prop) instanceof RuntimeChildChoiceDefinition choiceDef) {
                    element = newBase(getChoices(choiceDef).get(0));
                }
            }
            processItems(
                    request,
                    resourceDefinition,
                    profile,
                    element,
                    new ImmutablePair<>(children, request.getItems(itemPair.getItem())),
                    repeats,
                    path);
            request.getModelResolver().setValue(parent, prop, Arrays.asList(element));
        } else {
            var answers = request.resolvePathList(itemPair.getResponseItem(), "answer", IBaseBackboneElement.class);
            processItem(
                    request,
                    parent,
                    isNestedRepeating,
                    parentPath,
                    answers,
                    repeats,
                    adapter,
                    path,
                    identifiers,
                    propertyDefs);
        }
    }

    @SuppressWarnings("squid:S107")
    protected void processItem(
            ExtractRequest request,
            IBase parent,
            boolean isNestedRepeating,
            String parentPath,
            List<IBaseBackboneElement> answers,
            boolean repeats,
            IStructureDefinitionAdapter profile,
            String path,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs) {
        if (path.contains(":")) {
            processSliceItem(request, profile, parent, answers, identifiers, propertyDefs);
        } else if (identifiers.length > 1 && (isNestedRepeating || repeats)) {
            processRepeatingWithNested(request, parent, isNestedRepeating, answers, identifiers, propertyDefs, profile);
        } else {
            var answerPath = StringUtils.isBlank(parentPath) ? path : path.replace(parentPath + ".", "");
            answers.stream().forEach(answer -> {
                var answerValue = request.resolvePath(answer, VALUE_PATH);
                if (answerValue != null) {
                    setAnswerValue(request, parent, propertyDefs.get(answerPath), answerPath, answerValue, profile);
                }
            });
        }
    }

    protected void processRepeatingWithNested(
            ExtractRequest request,
            IBase parent,
            boolean isNestedRepeating,
            List<IBaseBackboneElement> answers,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs,
            IStructureDefinitionAdapter profile) {
        var parentProperty = identifiers[0];
        var childProperty = getChildProperty(identifiers, 1);
        var isChildList = propertyDefs.get(identifiers[identifiers.length - 1]).isMultipleCardinality();
        // If we are within a repeating item or the child element is multi cardinality we will use the parent
        // element
        // otherwise we will create a new value for each answer and add that to the parent
        var useParent = isNestedRepeating || isChildList;
        answers.stream().forEach(answer -> {
            var answerValue = request.resolvePath(answer, VALUE_PATH);
            if (answerValue != null) {
                var parentValue = useParent
                        ? parent
                        : newBase(
                                ((BaseRuntimeChildDatatypeDefinition) propertyDefs.get(parentProperty)).getDatatype());
                setAnswerValue(
                        request, parentValue, propertyDefs.get(childProperty), childProperty, answerValue, profile);
                if (!useParent) {
                    setAnswerValue(
                            request, parent, propertyDefs.get(parentProperty), parentProperty, parentValue, profile);
                }
            }
        });
    }

    protected void processSliceItem(
            ExtractRequest request,
            IStructureDefinitionAdapter profile,
            IBase parent,
            List<IBaseBackboneElement> answers,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs) {
        if (profile == null) {
            throw new IllegalArgumentException(String.format(
                    "Unable to parse slice element without a profile for definition: %s",
                    String.join(".", identifiers)));
        }
        var sliceIndex = -1;
        for (int i = 0; i < identifiers.length; i++) {
            if (identifiers[i].contains(":")) {
                sliceIndex = i;
            }
        }
        var sliceName = sliceIndex == -1 ? null : identifiers[sliceIndex];
        if (sliceName == null) {
            return;
        }
        var slicePath = sliceName.split(":")[0];
        var slicePropertyDef = propertyDefs.get(sliceName);
        var sliceClass = slicePropertyDef instanceof BaseRuntimeChildDatatypeDefinition def
                ? def.getDatatype()
                : getClassForTypeAndVersion("Extension", request.getFhirVersion());
        var sliceElements = profile.getSliceElements(sliceName);
        var answerPath = getChildProperty(identifiers, sliceIndex + 1);
        var extensionUrl = getExtensionUrl(profile, sliceName);
        answers.stream().forEach(answer -> {
            var answerValue = request.resolvePath(answer, VALUE_PATH);
            if (answerValue != null) {
                var sliceValue = newBase(sliceClass);
                setAnswerValue(request, sliceValue, propertyDefs.get(answerPath), answerPath, answerValue, profile);
                for (var slice : sliceElements) {
                    var sliceElementPath =
                            slice.getId().replace(String.format("%s.%s.", profile.getType(), sliceName), "");
                    var sliceElementValue = slice.getDefaultOrFixedOrPattern();
                    setAnswerValue(
                            request,
                            sliceValue,
                            propertyDefs.get(sliceElementPath),
                            sliceElementPath,
                            sliceElementValue,
                            profile);
                }
                if (slicePath.equals("extension")) {
                    setAnswerValue(request, sliceValue, propertyDefs.get("url"), "url", extensionUrl, profile);
                }
                setAnswerValue(request, parent, propertyDefs.get(sliceName), slicePath, sliceValue, profile);
            }
        });
    }

    protected IBase getExtensionUrl(IStructureDefinitionAdapter profile, String sliceName) {
        IBase retValue = null;
        var sliceElement = profile.getElement(profile.getType() + "." + sliceName);
        if (sliceElement != null) {
            var type = sliceElement.getType().stream().findFirst();
            if (type.isPresent()) {
                retValue = sliceElement.resolvePathList(type.get(), "profile").stream()
                        .findFirst()
                        .orElse(null);
            }
        }
        return retValue;
    }

    protected String getChildProperty(String[] identifiers, int startIndex) {
        var childProperty = identifiers[startIndex];
        for (int i = startIndex + 1; i < identifiers.length; i++) {
            childProperty = childProperty.concat("." + identifiers[i]);
        }
        return childProperty;
    }

    protected void setAnswerValue(
            ExtractRequest request,
            IBase parent,
            BaseRuntimeChildDefinition pathDefinition,
            String answerPath,
            IBase answerValue,
            IStructureDefinitionAdapter profile) {
        try {
            request.getModelResolver()
                    .setValue(
                            parent,
                            answerPath,
                            transformAnswer(request, pathDefinition, answerValue, answerPath, profile));
        } catch (Exception e) {
            if (pathDefinition instanceof RuntimeChildPrimitiveDatatypeDefinition
                    && answerValue instanceof IPrimitiveType) {
                var newValue = (IPrimitiveType<?>)
                        newBase(((RuntimeChildPrimitiveDatatypeDefinition) pathDefinition).getDatatype());
                newValue.setValueAsString(((IPrimitiveType<?>) answerValue).getValueAsString());
                request.getModelResolver().setValue(parent, answerPath, newValue);
            }
        }
    }

    protected HashMap<String, BaseRuntimeChildDefinition> getPropertyDefinitions(
            ExtractRequest request,
            BaseRuntimeElementDefinition<?> resourceDefinition,
            IStructureDefinitionAdapter adapter,
            String[] identifiers) {
        var props = new HashMap<String, BaseRuntimeChildDefinition>();
        var targetDef = resourceDefinition;
        for (int i = 0; i < identifiers.length; i++) {
            var def = targetDef.getChildByName(identifiers[i].split(":")[0]);
            props.put(identifiers[i], def);
            if (i < identifiers.length - 1) {
                if (def instanceof RuntimeChildExtension) {
                    targetDef = request.getFhirContext()
                            .getElementDefinition(getClassForTypeAndVersion("Extension", request.getFhirVersion()));
                } else if (def instanceof RuntimeChildChoiceDefinition choiceDef) {
                    var elementDef = adapter == null ? null : adapter.getElementByPath(identifiers[i]);
                    if (elementDef == null) {
                        targetDef = choiceDef.getChildElementDefinitionByDatatype(
                                choiceDef.getChoices().get(0));
                    } else {
                        targetDef = choiceDef.getChildByName(identifiers[i].replace("[x]", elementDef.getTypeCode()));
                    }
                } else if (def instanceof BaseRuntimeChildDatatypeDefinition datatypeDef) {
                    targetDef = request.getFhirContext().getElementDefinition(datatypeDef.getDatatype());
                }
            }
        }
        return props;
    }

    protected IBase getElement(ExtractRequest request, IBase parent, String path) {
        var elementPath = path.split("\\.")[0];
        var value = request.resolveRawPath(parent, elementPath);
        return (IBase) (value instanceof ArrayList ? ((ArrayList<?>) value).get(0) : value);
    }

    protected List<Class<? extends IBase>> getChoices(BaseRuntimeChildDefinition pathDefinition) {
        return pathDefinition instanceof RuntimeChildChoiceDefinition def ? def.getChoices() : new ArrayList<>();
    }

    protected Object transformAnswer(
            ExtractRequest request,
            BaseRuntimeChildDefinition pathDefinition,
            IBase answerValue,
            String answerPath,
            IStructureDefinitionAdapter profile) {
        var pathElement =
                profile == null ? null : profile.getElementByPath(answerPath.split(":")[0]);
        var answerType = pathElement == null ? null : pathElement.getTypeCode();
        if (answerType != null && !answerValue.fhirType().equals(answerType)) {
            var newAnswerValue = newBaseForVersion(answerType, request.getFhirVersion());
            request.getModelResolver().setValue(newAnswerValue, VALUE_PATH, answerValue);
            answerValue = newAnswerValue;
        } else {
            // Check if answer type matches path types available and transform if necessary
            if (!(pathDefinition instanceof RuntimeChildPrimitiveEnumerationDatatypeDefinition)
                    && ((pathDefinition instanceof RuntimeChildChoiceDefinition
                                    && !getChoices(pathDefinition).contains(answerValue.getClass()))
                            || (pathDefinition instanceof BaseRuntimeChildDatatypeDefinition def
                                    && !def.getDatatype().equals(answerValue.getClass())))) {
                answerValue = transformValueToResource(request.getFhirVersion(), answerValue);
            }
        }
        return pathDefinition != null && pathDefinition.isMultipleCardinality()
                ? Arrays.asList(answerValue)
                : answerValue;
    }

    protected String getDefinition(ExtractRequest request, ItemPair itemPair) {
        var definition = request.resolvePathString(itemPair.getItem(), DEFINITION_PATH);
        if (definition == null) {
            definition = request.resolvePathString(itemPair.getResponseItem(), DEFINITION_PATH);
        }
        return definition;
    }

    protected String getDefinitionType(String definition) {
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(
                    String.format("Unable to determine resource type from item definition: %s", definition));
        }
        return definition.split("#")[1];
    }

    protected void resolveMeta(IBaseResource resource, Optional<IStructureDefinitionAdapter> profile) {
        var meta = resource.getMeta();
        // Consider setting source and lastUpdated here?
        if (profile.isPresent()) {
            meta.addProfile(profile.get().getCanonical());
        }
    }
}
