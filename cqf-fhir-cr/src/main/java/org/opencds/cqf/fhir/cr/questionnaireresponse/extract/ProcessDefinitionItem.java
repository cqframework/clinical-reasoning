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
import ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.common.ExpressionProcessor;
import org.opencds.cqf.fhir.cr.common.ICqlOperationRequest;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.FhirPathCache;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "UnstableApiUsage"})
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

        var linkId = item.getResponseItem() == null
                ? "Questionnaire.root"
                : item.getResponseItem().getLinkId();
        var definitionProfile = getDefinitionProfile(request, item);
        var definition = getDefinition(item);
        var profileUrl = definitionProfile.right == null ? definition : definitionProfile.right;
        var profile = getProfile(request, profileUrl);
        var resourceType = getResourceType(linkId, definitionProfile, definition, profile);
        var extractResource = getExtractResource();
        var isCreatedResource = extractResource == null;
        var resource = isCreatedResource
                ? (IBaseResource) newBaseForVersion(resourceType, request.getFhirVersion())
                : extractResource;
        var resourceAdapter = request.getAdapterFactory().createResource(resource);
        processResource(request, resourceAdapter, profile, isCreatedResource, item);
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
                var split = definitionProfile.right.split("/");
                resourceType = split[split.length - 1];
            } else {
                if (definition == null) {
                    throw new IllegalArgumentException("Unable to retrieve definition for item: %s".formatted(linkId));
                }
                resourceType = getDefinitionType(definition);
            }
        }
        return resourceType;
    }

    protected <E extends IBaseExtension<?, ?>> E getExtractExtension(ExtractRequest request, ItemPair item) {
        var url = Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT;
        var extElement = getExtensionElement(request, item, url);
        E ext = extElement == null ? null : extElement.getExtensionByUrl(url);
        if (ext != null) {
            return ext;
        }
        var deprecatedUrl = Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT;
        var depElement = getExtensionElement(request, item, deprecatedUrl);
        return depElement == null ? null : depElement.getExtensionByUrl(deprecatedUrl);
    }

    protected <E extends IBaseExtension<?, ?>> List<E> getValueExtensions(ExtractRequest request, ItemPair item) {
        var url = Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT_VALUE;
        var extElement = getExtensionElement(request, item, url);
        return extElement == null ? Collections.emptyList() : extElement.getExtensionsByUrl(url);
    }

    protected IAdapter<?> getExtensionElement(ExtractRequest request, ItemPair item, String url) {
        // First, check the Questionnaire.item
        // Second, check the QuestionnaireResponse.item
        // Third, check the Questionnaire
        IAdapter<?> element;
        if (item.getItem() != null && item.getItem().hasExtension(url)) {
            element = item.getItem();
        } else if (item.getResponseItem() != null && item.getResponseItem().hasExtension(url)) {
            element = item.getResponseItem();
        } else {
            element = request.getQuestionnaireAdapter();
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
            IResourceAdapter resource,
            Optional<IStructureDefinitionAdapter> profile,
            boolean isCreatedResource,
            ItemPair item) {
        var resourceDefinition =
                request.getFhirContext().getElementDefinition(resource.get().getClass());
        if (isCreatedResource) {
            var id = request.getExtractId();
            var linkId = item.getResponseItem() == null
                    ? null
                    : item.getResponseItem().getLinkId();
            if (StringUtils.isNotBlank(linkId)) {
                id = id.concat("-%s".formatted(linkId));
            }
            // casting here to identify the signature
            resource.setId((IIdType) Ids.newId(request.getFhirVersion(), id));
            resolveMeta(resource, profile);
        }
        getValueExtensions(request, item)
                .forEach(valueExt -> processValueExtension(request, resource, profile, valueExt));
        List<? extends IItemComponentAdapter> responseItems;
        List<? extends IItemComponentAdapter> questionnaireItems;
        if (item.getResponseItem() != null && !item.getResponseItem().hasItem()) {
            responseItems = List.of(item.getResponseItem());
            questionnaireItems = List.of(item.getItem());
        } else {
            responseItems = item.getResponseItem() != null
                    ? item.getResponseItem().getItem()
                    : request.getQuestionnaireResponseAdapter().getItem();
            questionnaireItems = item.getItem() != null
                    ? item.getItem().getItem()
                    : request.getQuestionnaireAdapter() != null
                            ? request.getQuestionnaireAdapter().getItem()
                            : List.of();
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
            ExtractRequest request, IResourceAdapter resource, Optional<IStructureDefinitionAdapter> profile) {
        // If we have the profile go through each differential element and add any default values
        if (profile.isPresent()) {
            var defaultElements = profile.get().getDifferentialElements().stream()
                    .filter(IElementDefinitionAdapter::hasDefaultOrFixedOrPattern)
                    .toList();
            defaultElements.forEach(e -> {
                var value = e.getDefaultOrFixedOrPattern();
                if (value != null) {
                    var path = getPath(e);
                    var idSplit = e.getId().split(":");
                    // Ignore child slices, they are handled while processing the parent item
                    if (!(idSplit.length > 1 && idSplit[1].contains("."))) {
                        resource.setValue(path, value);
                    }
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    protected void processValueExtension(
            ExtractRequest request,
            IResourceAdapter resource,
            Optional<IStructureDefinitionAdapter> profile,
            IBaseExtension<?, ?> valueExt) {
        var definitionExt = resource.getExtensionByUrl(valueExt, DEFINITION_PATH);
        var fixedValueExt = resource.getExtensionByUrl(valueExt, "fixed-value");
        var expressionExt = resource.getExtensionByUrl(valueExt, "expression");
        if (definitionExt != null && (expressionExt != null || fixedValueExt != null)) {
            var definition = ((IPrimitiveType<String>) definitionExt.getValue()).getValueAsString();
            var value = fixedValueExt != null
                    ? fixedValueExt.getValue()
                    : getExpressionResult(request, CqfExpression.of(expressionExt, request.getReferencedLibraries()));
            if (value != null) {
                var path = getPathAdapter(request, profile, definition);
                resource.setValue(path.left, value);
            }
        }
    }

    protected List<IBase> getExpressionResult(ICqlOperationRequest request, CqfExpression expression) {
        // Constructing a CQL Library for each fhirpath expression is extremely inefficient
        // Using the HAPI FHIRPath engine instead
        // This assumes the expressions are simple and do not need any extra variables defined
        if (expression.getLanguage().equals("text/fhirpath")) {
            var fhirPath =
                    FhirPathCache.cachedForContext(request.getRepository().fhirContext());
            return fhirPath.evaluate(request.getContextVariable(), expression.getExpression(), IBase.class);
        } else {
            return expressionProcessor.getExpressionResult(request, expression);
        }
    }

    protected ImmutablePair<String, IStructureDefinitionAdapter> getPathAdapter(
            ExtractRequest request, Optional<IStructureDefinitionAdapter> profile, String definition) {
        var split = definition.split("#");
        var canonical = split[0];
        var id = split[1];
        var adapter = profile.filter(p -> canonical.equals(p.getCanonical()))
                .orElseGet(() -> getProfile(request, canonical, true).orElseGet(() -> profile.orElse(null)));
        var path = getPath(adapter, id);
        return new ImmutablePair<>(path, adapter);
    }

    protected Optional<IStructureDefinitionAdapter> getProfile(ExtractRequest request, String definition) {
        return getProfile(request, definition, false);
    }

    protected Optional<IStructureDefinitionAdapter> getProfile(
            ExtractRequest request, String definition, boolean optional) {
        if (StringUtils.isNotBlank(definition)) {
            var canonical =
                    canonicalTypeForVersion(request.getFhirVersion(), definition.split("#")[0]);
            try {
                return Optional.of((IStructureDefinitionAdapter) request.getAdapterFactory()
                        .createKnowledgeArtifactAdapter((IDomainResource) searchRepositoryByCanonical(
                                request.getRepository(),
                                canonical,
                                request.getFhirContext()
                                        .getResourceDefinition("StructureDefinition")
                                        .getImplementingClass())));
            } catch (Exception e) {
                if (optional) {
                    logger.debug("Unable to resolve optional profile {}: {}", canonical, e.getMessage());
                } else {
                    logger.error("Encountered error retrieving profile %s: %s".formatted(canonical, e.getMessage()), e);
                }
            }
        }
        return Optional.empty();
    }

    protected void processItems(
            ExtractRequest request,
            BaseRuntimeElementDefinition<?> resourceDefinition,
            Optional<IStructureDefinitionAdapter> profile,
            IAdapter<?> adapter,
            ImmutablePair<List<? extends IItemComponentAdapter>, List<? extends IItemComponentAdapter>> items,
            boolean isNestedRepeating,
            String parentPath) {
        var responseItems = items.left;
        var questionnaireItems = items.right;
        responseItems.forEach(childItem -> {
            var itemPair = new ItemPair(request.getQuestionnaireItem(childItem, questionnaireItems), childItem);
            processChildItem(request, resourceDefinition, profile, adapter, itemPair, isNestedRepeating, parentPath);
        });
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
        // Extension slice identity is required when constructing the nested Extension.
        if (profile != null && !id.contains(".extension:")) {
            var element = profile.getElement(id);
            if (element != null) {
                path = element.getPath();
            }
        }
        return stripTypeFromPath(path);
    }

    @SuppressWarnings("unchecked")
    protected void processChildItem(
            ExtractRequest request,
            BaseRuntimeElementDefinition<?> resourceDefinition,
            Optional<IStructureDefinitionAdapter> profile,
            IAdapter<?> parent,
            ItemPair itemPair,
            boolean isNestedRepeating,
            String parentPath) {
        var definition = getDefinition(itemPair);
        var children = itemPair.getResponseItem().getItem();
        var repeats = itemPair.getItem() != null && itemPair.getItem().getRepeats();
        if (StringUtils.isBlank(definition)) {
            processItems(
                    request,
                    resourceDefinition,
                    profile,
                    parent,
                    new ImmutablePair<>(children, itemPair.getItem().getItem()),
                    repeats,
                    parentPath);
            return;
        }
        if (!definition.contains("#")) {
            throw new IllegalArgumentException("Invalid definition encountered for item %s"
                    .formatted(itemPair.getResponseItem().getLinkId()));
        }
        var pathAdapter = getPathAdapter(request, profile, definition);
        var path = pathAdapter.left;
        var adapter = pathAdapter.right;
        var identifiers = path.split("\\.");
        if (hasMultipleSlices(identifiers)) {
            if (hasExtractionAnswer(itemPair.getResponseItem())) {
                throw new UnprocessableEntityException("Extraction of multiple nested slices is not supported");
            }
            return;
        }
        var propertyDefs = getPropertyDefinitions(request, resourceDefinition, adapter, identifiers);
        if (!children.isEmpty()) {
            var prop = identifiers[identifiers.length - 1];
            var element = repeats ? null : getElement(parent, path);
            if (element == null) {
                var propDef = propertyDefs.get(prop);
                if (propDef instanceof BaseRuntimeChildDatatypeDefinition datatypeDef) {
                    element = newBase(datatypeDef.getDatatype());
                } else if (propDef instanceof RuntimeChildResourceBlockDefinition blockDef) {
                    element = newBase(
                            blockDef.getChildByName(blockDef.getElementName()).getImplementingClass());
                } else if (propDef instanceof RuntimeChildExtension) {
                    element = newBaseForVersion("Extension", request.getFhirVersion());
                } else if (adapter != null) {
                    var elementDef = adapter.getElementByPath(path);
                    element = newBaseForVersion(elementDef.getTypeCode(), request.getFhirVersion());
                } else if (propertyDefs.get(prop) instanceof RuntimeChildChoiceDefinition choiceDef) {
                    element = newBase(getChoices(choiceDef).get(0));
                } else {
                    throw new UnprocessableEntityException(String.format(
                            "Unable to determine type for item: %s",
                            itemPair.getResponseItem().getLinkId()));
                }
            }
            var elementAdapter = request.getAdapterFactory().createBase(element);
            if (element instanceof IBaseExtension<?, ?> extension && adapter != null) {
                var url = getExtensionUrl(adapter, path);
                if (url instanceof IPrimitiveType<?> primitive) {
                    extension.setUrl(primitive.getValueAsString());
                }
                var sliceId = adapter.getType() + "." + path;
                for (var slice : getSliceDefaults(adapter, sliceId)) {
                    var childPath = slice.getId().substring(sliceId.length() + 1);
                    setAnswerValue(
                            request,
                            elementAdapter,
                            propertyDefs.get(childPath),
                            childPath,
                            slice.getDefaultOrFixedOrPattern(),
                            adapter,
                            stripTypeFromPath(slice.getId()));
                }
            }
            processItems(
                    request,
                    resourceDefinition,
                    profile,
                    elementAdapter,
                    new ImmutablePair<>(
                            children,
                            itemPair.getItem() == null
                                    ? null
                                    : itemPair.getItem().getItem()),
                    repeats,
                    path);
            parent.setValue(prop.split(":")[0], List.of(element));
        } else {
            processItem(
                    request,
                    parent,
                    isNestedRepeating,
                    parentPath,
                    itemPair.getResponseItem().getAnswer(),
                    repeats,
                    adapter,
                    path,
                    identifiers,
                    propertyDefs,
                    stripTypeFromPath(definition.split("#")[1]));
        }
    }

    @SuppressWarnings("squid:S107")
    protected void processItem(
            ExtractRequest request,
            IAdapter<?> parent,
            boolean isNestedRepeating,
            String parentPath,
            List<IQuestionnaireResponseItemAnswerComponentAdapter> answers,
            boolean repeats,
            IStructureDefinitionAdapter profile,
            String path,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs) {
        processItem(
                request,
                parent,
                isNestedRepeating,
                parentPath,
                answers,
                repeats,
                profile,
                path,
                identifiers,
                propertyDefs,
                path);
    }

    @SuppressWarnings("squid:S107")
    protected void processItem(
            ExtractRequest request,
            IAdapter<?> parent,
            boolean isNestedRepeating,
            String parentPath,
            List<IQuestionnaireResponseItemAnswerComponentAdapter> answers,
            boolean repeats,
            IStructureDefinitionAdapter profile,
            String path,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs,
            String targetPath) {
        var localPath = StringUtils.isBlank(parentPath) ? path : StringUtils.removeStart(path, parentPath + ".");
        if (localPath.contains(":")) {
            processSliceItem(request, profile, parent, answers, identifiers, propertyDefs, parentPath);
        } else if (identifiers.length > 1 && (isNestedRepeating || repeats)) {
            processRepeatingWithNested(
                    request, parent, isNestedRepeating, answers, identifiers, propertyDefs, profile, targetPath);
        } else {
            var answerPath = StringUtils.isBlank(parentPath) ? path : path.replace(parentPath + ".", "");
            answers.forEach(answer -> {
                var answerValue = answer.getValue();
                if (answerValue != null) {
                    setAnswerValue(
                            request,
                            parent,
                            propertyDefs.get(identifiers[identifiers.length - 1]),
                            answerPath,
                            answerValue,
                            profile,
                            targetPath);
                }
            });
        }
    }

    protected void processRepeatingWithNested(
            ExtractRequest request,
            IAdapter<?> parent,
            boolean isNestedRepeating,
            List<IQuestionnaireResponseItemAnswerComponentAdapter> answers,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs,
            IStructureDefinitionAdapter profile) {
        processRepeatingWithNested(
                request,
                parent,
                isNestedRepeating,
                answers,
                identifiers,
                propertyDefs,
                profile,
                String.join(".", identifiers));
    }

    protected void processRepeatingWithNested(
            ExtractRequest request,
            IAdapter<?> parent,
            boolean isNestedRepeating,
            List<IQuestionnaireResponseItemAnswerComponentAdapter> answers,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs,
            IStructureDefinitionAdapter profile,
            String targetPath) {
        var parentProperty = identifiers[0];
        var childProperty = getChildProperty(identifiers, 1);
        var isChildList = propertyDefs.get(identifiers[identifiers.length - 1]).isMultipleCardinality();
        // If we are within a repeating item or the child element is multi cardinality we will use the parent
        // element
        // otherwise we will create a new value for each answer and add that to the parent
        var useParent = isNestedRepeating || isChildList;
        answers.forEach(answer -> {
            var answerValue = answer.getValue();
            if (answerValue != null) {
                var parentValue = useParent
                        ? parent
                        : request.getAdapterFactory().createBase(newChildValue(propertyDefs.get(parentProperty)));
                setAnswerValue(
                        request,
                        parentValue,
                        propertyDefs.get(identifiers[identifiers.length - 1]),
                        childProperty,
                        answerValue,
                        profile,
                        targetPath);
                if (!useParent) {
                    setAnswerValue(
                            request, parent, propertyDefs.get(parentProperty), parentProperty, parentValue.get(), null);
                }
            }
        });
    }

    protected void processSliceItem(
            ExtractRequest request,
            IStructureDefinitionAdapter profile,
            IAdapter<?> parent,
            List<IQuestionnaireResponseItemAnswerComponentAdapter> answers,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs) {
        processSliceItem(request, profile, parent, answers, identifiers, propertyDefs, "");
    }

    protected void processSliceItem(
            ExtractRequest request,
            IStructureDefinitionAdapter profile,
            IAdapter<?> parent,
            List<IQuestionnaireResponseItemAnswerComponentAdapter> answers,
            String[] identifiers,
            HashMap<String, BaseRuntimeChildDefinition> propertyDefs,
            String parentPath) {
        if (profile == null) {
            throw new IllegalArgumentException("Unable to parse slice element without a profile for definition: %s"
                    .formatted(String.join(".", identifiers)));
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
        var sliceId = profile.getType() + "." + String.join(".", Arrays.copyOf(identifiers, sliceIndex + 1));
        var sliceElements = getSliceDefaults(profile, sliceId);
        if (answers.stream().noneMatch(answer -> answer.getValue() != null)) {
            return;
        }
        if (hasMultipleSlices(identifiers)) {
            throw new UnprocessableEntityException("Extraction of multiple nested slices is not supported");
        }
        var answerPath =
                sliceIndex + 1 == identifiers.length ? "value[x]" : getChildProperty(identifiers, sliceIndex + 1);
        var extensionUrl = getExtensionUrl(profile, stripTypeFromPath(sliceId));
        var answerElement = resolveAnswerElement(
                request,
                profile,
                sliceIndex + 1 == identifiers.length
                        ? String.join(".", identifiers) + ".value[x]"
                        : String.join(".", identifiers));
        var sliceParentIndex = sliceIndex;
        answers.forEach(answer -> {
            var answerValue = answer.getValue();
            if (answerValue != null) {
                var sliceValue = request.getAdapterFactory().createBase(newBase(sliceClass));
                for (var slice : sliceElements) {
                    var sliceElementPath = slice.getId().substring(sliceId.length() + 1);
                    var sliceElementValue = slice.getDefaultOrFixedOrPattern();
                    setAnswerValue(
                            request,
                            sliceValue,
                            propertyDefs.get(sliceElementPath),
                            sliceElementPath,
                            sliceElementValue,
                            profile,
                            stripTypeFromPath(slice.getId()));
                }
                var answerDefinition = sliceParentIndex + 1 == identifiers.length
                        ? request.getFhirContext()
                                .getElementDefinition(sliceValue.get().getClass())
                                .getChildByName("value[x]")
                        : propertyDefs.get(identifiers[identifiers.length - 1]);
                setResolvedAnswerValue(request, sliceValue, answerDefinition, answerPath, answerValue, answerElement);
                if (slicePath.equals("extension")) {
                    setAnswerValue(request, sliceValue, propertyDefs.get("url"), "url", extensionUrl, null);
                }
                var sliceParent = parent;
                var start = StringUtils.isBlank(parentPath) ? 0 : parentPath.split("\\.").length;
                for (int i = start; i < sliceParentIndex; i++) {
                    var property = identifiers[i];
                    var container = getElement(sliceParent, property);
                    if (container == null) {
                        container = newChildValue(propertyDefs.get(property));
                        setAnswerValue(request, sliceParent, propertyDefs.get(property), property, container, null);
                    }
                    sliceParent = request.getAdapterFactory().createBase(container);
                }
                setAnswerValue(request, sliceParent, propertyDefs.get(sliceName), slicePath, sliceValue.get(), null);
            }
        });
    }

    protected IBase getExtensionUrl(IStructureDefinitionAdapter profile, String sliceName) {
        var profiles = getExtensionProfiles(profile, profile.getType() + "." + sliceName);
        return profiles.size() == 1 ? profiles.get(0) : null;
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
            IAdapter<?> parent,
            BaseRuntimeChildDefinition pathDefinition,
            String answerPath,
            IBase answerValue,
            IStructureDefinitionAdapter profile) {
        setAnswerValue(request, parent, pathDefinition, answerPath, answerValue, profile, answerPath);
    }

    protected void setAnswerValue(
            ExtractRequest request,
            IAdapter<?> parent,
            BaseRuntimeChildDefinition pathDefinition,
            String answerPath,
            IBase answerValue,
            IStructureDefinitionAdapter profile,
            String targetPath) {
        setResolvedAnswerValue(
                request,
                parent,
                pathDefinition,
                answerPath,
                answerValue,
                resolveAnswerElement(request, profile, targetPath));
    }

    private void setResolvedAnswerValue(
            ExtractRequest request,
            IAdapter<?> parent,
            BaseRuntimeChildDefinition pathDefinition,
            String answerPath,
            IBase answerValue,
            IElementDefinitionAdapter answerElement) {
        if (answerValue == null) {
            return;
        }
        try {
            parent.setValue(
                    answerPath, transformAnswer(request, pathDefinition, answerValue, answerElement, answerPath));
        } catch (Exception e) {
            if (pathDefinition instanceof RuntimeChildPrimitiveDatatypeDefinition definition
                    && answerValue instanceof IPrimitiveType<?> type) {
                var newValue = (IPrimitiveType<?>) newBase(definition.getDatatype());
                newValue.setValueAsString(type.getValueAsString());
                parent.setValue(answerPath, newValue);
            } else {
                logger.warn(
                        "Unable to assign answer type {} to extraction path {}", answerValue.fhirType(), answerPath);
            }
        }
    }

    private boolean hasMultipleSlices(String[] identifiers) {
        return Arrays.stream(identifiers).filter(id -> id.contains(":")).count() > 1;
    }

    private boolean hasExtractionAnswer(IItemComponentAdapter item) {
        if (item instanceof IQuestionnaireResponseItemComponentAdapter response
                && response.getAnswer().stream()
                        .anyMatch(answer -> answer.getValue() != null
                                || answer.getItem().stream().anyMatch(this::hasExtractionAnswer))) {
            return true;
        }
        return item.getItem().stream().anyMatch(this::hasExtractionAnswer);
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
                } else if (def instanceof RuntimeChildResourceBlockDefinition blockDef) {
                    targetDef = blockDef.getChildByName(blockDef.getElementName());
                }
            }
        }
        return props;
    }

    protected IBase getElement(IAdapter<?> parent, String path) {
        var elementPath = path.split("\\.")[0];
        var value = parent.resolvePathList(elementPath);
        return value.isEmpty() ? null : value.get(0);
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
        return transformAnswer(
                request, pathDefinition, answerValue, resolveAnswerElement(request, profile, answerPath), answerPath);
    }

    protected Object transformAnswer(
            ExtractRequest request,
            BaseRuntimeChildDefinition pathDefinition,
            IBase answerValue,
            IElementDefinitionAdapter pathElement) {
        return transformAnswer(request, pathDefinition, answerValue, pathElement, null);
    }

    private Object transformAnswer(
            ExtractRequest request,
            BaseRuntimeChildDefinition pathDefinition,
            IBase answerValue,
            IElementDefinitionAdapter pathElement,
            String answerPath) {
        var answerTypes = pathElement == null
                ? List.<String>of()
                : pathElement.getType().stream()
                        .map(type -> pathElement.resolvePathString(type, "code"))
                        .filter(Objects::nonNull)
                        .toList();
        if (pathElement != null
                && answerPath != null
                && !answerPath.contains("[x]")
                && pathElement.getPath().endsWith("[x]")) {
            var field = pathElement.getPath().substring(pathElement.getPath().lastIndexOf('.') + 1);
            var alias = answerPath.substring(answerPath.lastIndexOf('.') + 1);
            answerTypes = answerTypes.stream()
                    .filter(type ->
                            field.replace("[x]", StringUtils.capitalize(type)).equals(alias))
                    .toList();
        }
        if (!answerTypes.isEmpty() && !answerTypes.contains(answerValue.fhirType())) {
            var converted = transformValueToResource(request.getFhirVersion(), answerValue);
            if (converted != null && answerTypes.contains(converted.fhirType())) {
                answerValue = converted;
            } else if (answerTypes.size() == 1) {
                var newAnswerValue = request.getAdapterFactory()
                        .createBase(newBaseForVersion(answerTypes.get(0), request.getFhirVersion()));
                newAnswerValue.setValue(VALUE_PATH, answerValue);
                answerValue = newAnswerValue.get();
            } else {
                throw new IllegalArgumentException("Answer does not match any allowed target type: " + answerTypes);
            }
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
                ? Collections.singletonList(answerValue)
                : answerValue;
    }

    private IBase newChildValue(BaseRuntimeChildDefinition definition) {
        if (definition instanceof RuntimeChildResourceBlockDefinition block) {
            return newBase(block.getChildByName(block.getElementName()).getImplementingClass());
        }
        return newBase(((BaseRuntimeChildDatatypeDefinition) definition).getDatatype());
    }

    private IElementDefinitionAdapter getTypedElement(IStructureDefinitionAdapter profile, String id) {
        // A differential can change cardinality/binding without repeating the snapshot's type.
        return Stream.concat(profile.getDifferentialElements().stream(), profile.getSnapshotElements().stream())
                .filter(e -> !e.getType().isEmpty())
                .filter(e -> e.getId().equals(id)
                        || (e.getType().stream()
                                .filter(type -> e.resolvePathString(type, "code") != null)
                                .anyMatch(type -> e.getId()
                                        .replace("[x]", StringUtils.capitalize(e.resolvePathString(type, "code")))
                                        .equals(id))))
                .findFirst()
                .orElse(null);
    }

    private IElementDefinitionAdapter resolveAnswerElement(
            ExtractRequest request, IStructureDefinitionAdapter profile, String targetPath) {
        if (profile == null) {
            return null;
        }
        var id = profile.getType() + "." + targetPath;
        var element = getTypedElement(profile, id);
        if (element != null) {
            return element;
        }
        // Resolve each enclosing Extension's own profile; never look up its value at the resource root.
        for (int dot = targetPath.lastIndexOf('.'); dot > 0; dot = targetPath.lastIndexOf('.', dot - 1)) {
            var parent = getTypedElement(profile, profile.getType() + "." + targetPath.substring(0, dot));
            if (parent == null || !"Extension".equals(parent.getTypeCode())) {
                continue;
            }
            var references = getExtensionProfiles(profile, profile.getType() + "." + targetPath.substring(0, dot));
            if (references.size() == 1 && references.get(0) instanceof IPrimitiveType<?> canonical) {
                var extensionProfile =
                        getProfile(request, canonical.getValueAsString(), true).orElse(null);
                if (extensionProfile != null && "Extension".equals(extensionProfile.getType())) {
                    return resolveAnswerElement(request, extensionProfile, targetPath.substring(dot + 1));
                }
            }
        }
        return null;
    }

    private List<IBase> getExtensionProfiles(IStructureDefinitionAdapter profile, String id) {
        return Stream.concat(profile.getDifferentialElements().stream(), profile.getSnapshotElements().stream())
                .filter(e -> id.equals(e.getId()))
                .flatMap(e -> e.getType().stream()
                        .filter(type -> "Extension".equals(e.resolvePathString(type, "code")))
                        .map(type -> {
                            var value = e.resolvePath(type, "profile");
                            if (value instanceof IBase base) {
                                return List.of(base);
                            }
                            return value instanceof List<?> values
                                    ? values.stream()
                                            .filter(IBase.class::isInstance)
                                            .map(IBase.class::cast)
                                            .toList()
                                    : List.<IBase>of();
                        }))
                .filter(profiles -> !profiles.isEmpty())
                .findFirst()
                .orElse(List.of());
    }

    private List<IElementDefinitionAdapter> getSliceDefaults(IStructureDefinitionAdapter profile, String sliceId) {
        // Load snapshot defaults first so explicit differential values replace them by exact identity.
        var elements = new LinkedHashMap<String, IElementDefinitionAdapter>();
        Stream.concat(profile.getSnapshotElements().stream(), profile.getDifferentialElements().stream())
                .filter(e -> e.getId().startsWith(sliceId + ".") && e.hasDefaultOrFixedOrPattern())
                .forEach(e -> elements.put(e.getId(), e));
        return new ArrayList<>(elements.values());
    }

    protected String getDefinition(ItemPair itemPair) {
        return itemPair.getItem() != null && itemPair.getItem().hasDefinition()
                ? itemPair.getItem().getDefinition()
                : itemPair.getResponseItem() != null
                        ? itemPair.getResponseItem().getDefinition()
                        : null;
    }

    protected String getDefinitionType(String definition) {
        if (!definition.contains("#")) {
            throw new IllegalArgumentException(
                    "Unable to determine resource type from item definition: %s".formatted(definition));
        }
        return definition.split("#")[1];
    }

    protected void resolveMeta(IResourceAdapter resource, Optional<IStructureDefinitionAdapter> profile) {
        var meta = resource.get().getMeta();
        // Consider setting source and lastUpdated here?
        profile.ifPresent(iStructureDefinitionAdapter -> meta.addProfile(iStructureDefinitionAdapter.getCanonical()));
    }
}
