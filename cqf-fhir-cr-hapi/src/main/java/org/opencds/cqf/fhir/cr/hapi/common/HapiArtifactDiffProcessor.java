package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.patch.FhirPatch;
import ca.uhn.fhir.parser.path.EncodeContextPath;
import ca.uhn.fhir.parser.path.EncodeContextPathElement;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UrlType;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetComposeComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor.DiffCache;
import org.opencds.cqf.fhir.cr.common.IArtifactDiffProcessor;
import org.opencds.cqf.fhir.cr.visitor.ExpandHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;
import org.springframework.beans.BeanWrapperImpl;

public class HapiArtifactDiffProcessor implements IArtifactDiffProcessor {

    protected final IRepository repository;

    public HapiArtifactDiffProcessor(IRepository repository) {
        this.repository = repository;
    }

    @Override
    public IBaseParameters getArtifactDiff(
            IBaseResource sourceResource,
            IBaseResource targetResource,
            Boolean compareComputable,
            Boolean compareExecutable,
            DiffCache cache,
            Endpoint terminologyEndpoint) {
        if (!(sourceResource instanceof MetadataResource theSourceLibrary)) {
            throw new UnprocessableEntityException("Source resource must exist and be a Knowledge Artifact type.");
        }

        if (!(targetResource instanceof MetadataResource theTargetLibrary)) {
            throw new UnprocessableEntityException("Target resource must exist and be a Knowledge Artifact type.");
        }
        if (sourceResource.getClass() != targetResource.getClass()) {
            throw new UnprocessableEntityException("Source and target resources must be of the same type.");
        }

        // setup
        var patch = new FhirPatch(repository.fhirContext());
        patch.setIncludePreviousValueInDiff(true);
        // ignore meta changes
        patch.addIgnorePath("*.meta");
        var libraryDiff = handleRelatedArtifactArrayElementsDiff(theSourceLibrary, theTargetLibrary, patch);

        // then check for references and add those to the base Parameters object
        if (cache == null) {
            cache = new DiffCache();
        }
        var sourceCanonical = theSourceLibrary.getUrl() + "|" + theSourceLibrary.getVersion();
        var targetCanonical = theTargetLibrary.getUrl() + "|" + theTargetLibrary.getVersion();
        cache.addSource(sourceCanonical, theSourceLibrary);
        cache.addTarget(targetCanonical, theTargetLibrary);
        cache.addDiff(sourceCanonical, targetCanonical, libraryDiff);
        checkForChangesInChildren(
                libraryDiff,
                theSourceLibrary,
                theTargetLibrary,
                repository,
                patch,
                cache,
                repository.fhirContext(),
                compareComputable,
                compareExecutable,
                terminologyEndpoint);
        return libraryDiff;
    }

    private static void checkForChangesInChildren(
            Parameters baseDiff,
            MetadataResource sourceBase,
            MetadataResource targetBase,
            IRepository repository,
            FhirPatch patch,
            DiffCache cache,
            FhirContext ctx,
            boolean compareComputable,
            boolean compareExecutable,
            Endpoint terminologyEndpoint)
            throws UnprocessableEntityException {
        // get the references in both the source and target
        var targetReferences = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4)
                .createKnowledgeArtifactAdapter(targetBase)
                .getRelatedArtifact()
                .stream()
                .map(ra -> (RelatedArtifact) ra)
                .toList();
        var sourceReferences = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4)
                .createKnowledgeArtifactAdapter(sourceBase)
                .getRelatedArtifact()
                .stream()
                .map(ra -> (RelatedArtifact) ra)
                .toList();
        var combinedReferenceList =
                extractAdditionsAndDeletions(sourceReferences, targetReferences, RelatedArtifact.class);
        if (!combinedReferenceList.getSourceMatches().isEmpty()) {
            for (int i = 0; i < combinedReferenceList.getSourceMatches().size(); i++) {
                var sourceCanonical =
                        combinedReferenceList.getSourceMatches().get(i).getResource();
                var targetCanonical =
                        combinedReferenceList.getTargetMatches().get(i).getResource();
                boolean diffNotAlreadyComputedAndPresent =
                        baseDiff.getParameter(Canonicals.getUrl(targetCanonical)) == null;
                if (diffNotAlreadyComputedAndPresent) {
                    var source = checkOrUpdateResourceCache(
                            sourceCanonical, cache, repository, true, ctx, terminologyEndpoint, compareExecutable);
                    var target = checkOrUpdateResourceCache(
                            targetCanonical, cache, repository, false, ctx, terminologyEndpoint, compareExecutable);
                    checkOrUpdateDiffCache(
                                    sourceCanonical,
                                    targetCanonical,
                                    source,
                                    target,
                                    patch,
                                    cache,
                                    ctx,
                                    compareComputable,
                                    compareExecutable)
                            .ifPresentOrElse(
                                    diffToAppend -> {
                                        var component = baseDiff.addParameter();
                                        component.setName(Canonicals.getUrl(sourceCanonical));
                                        component.setResource(diffToAppend);
                                        // check for changes in the children of those as well
                                        checkForChangesInChildren(
                                                diffToAppend,
                                                source,
                                                target,
                                                repository,
                                                patch,
                                                cache,
                                                ctx,
                                                compareComputable,
                                                compareExecutable,
                                                terminologyEndpoint);
                                    },
                                    () -> {
                                        if (target == null) {
                                            var component = baseDiff.addParameter();
                                            component.setName(Canonicals.getUrl(sourceCanonical));
                                            component.setValue(new StringType("Target could not be retrieved"));
                                        } else if (source == null) {
                                            var component = baseDiff.addParameter();
                                            component.setName(Canonicals.getUrl(targetCanonical));
                                            component.setValue(new StringType("Source could not be retrieved"));
                                        }
                                    });
                }
            }
        }
        for (var addition : combinedReferenceList.getInsertions()) {
            if (addition.hasResource()) {
                boolean diffNotAlreadyComputedAndPresent =
                        baseDiff.getParameter(Canonicals.getUrl(addition.getResource())) == null;
                if (diffNotAlreadyComputedAndPresent) {
                    var targetResource = checkOrUpdateResourceCache(
                            addition.getResource(),
                            cache,
                            repository,
                            false,
                            ctx,
                            terminologyEndpoint,
                            compareExecutable);
                    checkOrUpdateDiffCache(
                                    null,
                                    addition.getResource(),
                                    null,
                                    targetResource,
                                    patch,
                                    cache,
                                    ctx,
                                    compareComputable,
                                    compareExecutable)
                            .ifPresent(diffToAppend -> {
                                var component = baseDiff.addParameter();
                                component.setName(Canonicals.getUrl(addition.getResource()));
                                component.setResource(diffToAppend);
                            });
                }
            }
        }
        for (var deletion : combinedReferenceList.getDeletions()) {
            if (deletion.hasResource()) {
                boolean diffNotAlreadyComputedAndPresent =
                        baseDiff.getParameter(Canonicals.getUrl(deletion.getResource())) == null;
                if (diffNotAlreadyComputedAndPresent) {
                    var sourceResource = checkOrUpdateResourceCache(
                            deletion.getResource(),
                            cache,
                            repository,
                            true,
                            ctx,
                            terminologyEndpoint,
                            compareExecutable);
                    checkOrUpdateDiffCache(
                                    deletion.getResource(),
                                    null,
                                    sourceResource,
                                    null,
                                    patch,
                                    cache,
                                    ctx,
                                    compareComputable,
                                    compareExecutable)
                            .ifPresent(diffToAppend -> {
                                var component = baseDiff.addParameter();
                                component.setName(Canonicals.getUrl(deletion.getResource()));
                                component.setResource(diffToAppend);
                            });
                }
            }
        }
    }

    private static MetadataResource checkOrUpdateResourceCache(
            String url,
            DiffCache cache,
            IRepository repository,
            boolean isSource,
            FhirContext context,
            Endpoint terminologyEndpoint,
            boolean needsExpandedValueSets)
            throws UnprocessableEntityException {
        var resource = cache.getResource(url).orElse(null);
        if (resource == null) {
            try {
                resource = retrieveResourcesByCanonical(url, repository);
            } catch (ResourceNotFoundException e) {
                // ignore
            }
            if (resource != null) {
                if (resource instanceof ValueSet valueSet && needsExpandedValueSets) {
                    try {
                        tryExpandValueSet(valueSet, context, terminologyEndpoint, repository);
                    } catch (Exception e) {
                        throw new UnprocessableEntityException("Could not expand ValueSet: " + e.getMessage());
                    }
                }
                if (isSource) {
                    cache.addSource(url, resource);
                } else {
                    cache.addTarget(url, resource);
                }
            }
        }
        return resource;
    }

    private static void tryExpandValueSet(
            ValueSet vset, FhirContext context, Endpoint terminologyEndpoint, IRepository repository) {
        // Only update if ValueSet has changed since last expansion
        if (wasValueSetChangedSinceLastExpansion(vset)) {
            var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
            var ts = new TerminologyServerClient(context);
            var expandHelper = new ExpandHelper(repository, ts);
            var endpointAdapter = Optional.ofNullable(terminologyEndpoint).map(factory::createEndpoint);
            var valueSetAdapter = (IValueSetAdapter) factory.createKnowledgeArtifactAdapter(vset);
            var parametersAdapter = factory.createParameters(new Parameters());
            parametersAdapter.addParameter("url", new UrlType(vset.getUrl()));
            parametersAdapter.addParameter("valueSetVersion", new StringType(vset.getVersion()));
            expandHelper.expandValueSet(
                    valueSetAdapter, parametersAdapter, endpointAdapter, new ArrayList(), new ArrayList(), new Date());
        }
    }

    private static boolean wasValueSetChangedSinceLastExpansion(ValueSet valueSet) {
        Optional<Date> lastExpanded =
                Optional.ofNullable(valueSet.getExpansion()).map(ValueSetExpansionComponent::getTimestamp);
        Optional<Date> lastUpdated = Optional.ofNullable(valueSet.getMeta()).map(Meta::getLastUpdated);
        // if lastExpanded after lastUpdated then we know the VS was NOT changed since the last expansion
        return !(lastExpanded.isPresent()
                && lastUpdated.isPresent()
                && (lastExpanded.get().after(lastUpdated.get())));
    }

    private static MetadataResource retrieveResourcesByCanonical(String reference, IRepository repository)
            throws ResourceNotFoundException {
        var referencedResourceBundle = SearchHelper.searchRepositoryByCanonicalWithPaging(repository, reference);
        var referencedResource = IKnowledgeArtifactAdapter.findLatestVersion(referencedResourceBundle);
        if (referencedResource.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Resource for Canonical '%s' not found.", reference));
        }
        return (MetadataResource) referencedResource.get();
    }

    private static Optional<Parameters> checkOrUpdateDiffCache(
            String sourceCanonical,
            String targetCanonical,
            MetadataResource source,
            MetadataResource target,
            FhirPatch patch,
            DiffCache cache,
            FhirContext ctx,
            boolean compareComputable,
            boolean compareExecutable) {
        var retval = cache.getDiff(
                sourceCanonical == null ? "empty" : sourceCanonical,
                targetCanonical == null ? "empty" : targetCanonical);
        if (sourceCanonical == null && target != null) {
            try {
                var empty = target.getClass()
                        .getDeclaredConstructor((Class<?>[]) null)
                        .newInstance((Object[]) null);
                retval = (Parameters) patch.diff(empty, target);
                cache.addDiff("empty", targetCanonical, retval);
            } catch (NoSuchMethodException
                    | IllegalAccessException
                    | InstantiationException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | SecurityException e) {
                // TODO: add OperationOutcome to bundle
                e.printStackTrace();
            }
        } else if (targetCanonical == null && source != null) {
            try {
                var empty = source.getClass()
                        .getDeclaredConstructor((Class<?>[]) null)
                        .newInstance((Object[]) null);
                retval = (Parameters) patch.diff(source, empty);
                cache.addDiff(sourceCanonical, "empty", retval);
            } catch (InstantiationException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | SecurityException e) {
                // TODO: add OperationOutcome to bundle
                e.printStackTrace();
            }
        } else if (retval == null && source != null && target != null) {
            if (source instanceof Library || source instanceof PlanDefinition) {
                retval = handleRelatedArtifactArrayElementsDiff(source, target, patch);
            } else if (source instanceof ValueSet) {
                retval = advancedValueSetDiff(source, target, patch, compareComputable, compareExecutable);
            } else {
                retval = (Parameters) patch.diff(source, target);
            }
            cache.addDiff(sourceCanonical, targetCanonical, retval);
        }
        return Optional.ofNullable(retval);
    }

    private static Parameters advancedValueSetDiff(
            MetadataResource sourceValueSet,
            MetadataResource targetValueSet,
            FhirPatch patch,
            boolean compareComputable,
            boolean compareExecutable) {
        var updateSource = (ValueSet) sourceValueSet.copy();
        var updateTarget = (ValueSet) targetValueSet.copy();
        var composeIncludeProcessed = extractAdditionsAndDeletions(
                updateSource.getCompose().getInclude(),
                updateTarget.getCompose().getInclude(),
                ConceptSetComponent.class);
        var expansionContainsProcessed = extractAdditionsAndDeletions(
                updateSource.getExpansion().getContains(),
                updateTarget.getExpansion().getContains(),
                ValueSetExpansionContainsComponent.class);
        if (compareComputable) {
            // only generate changes for compose.inlude elements if compareComputable is true
            updateSource.getCompose().setInclude(composeIncludeProcessed.getSourceMatches());
            updateTarget.getCompose().setInclude(composeIncludeProcessed.getTargetMatches());
        } else {
            // don't generate any Parameters
            updateSource.getCompose().setInclude(new ArrayList<>());
            updateTarget.getCompose().setInclude(new ArrayList<>());
        }
        if (compareExecutable) {
            // only generate changes for compose.inlude elements if compareExecutable is true
            updateSource.getExpansion().setContains(expansionContainsProcessed.getSourceMatches());
            updateTarget.getExpansion().setContains(expansionContainsProcessed.getTargetMatches());
        } else {
            // don't generate any Parameters
            updateSource.getExpansion().setContains(new ArrayList<>());
            updateTarget.getExpansion().setContains(new ArrayList<>());
        }
        // first check for ancillary differences between the otherwise matching array elements
        var vsDiff = (Parameters) patch.diff(updateSource, updateTarget);

        // then append the insert / delete entries
        if (compareComputable) {
            composeIncludeProcessed.appendInsertOperations(
                    vsDiff, patch, updateTarget.getCompose().getInclude().size());
            composeIncludeProcessed.appendDeleteOperations(
                    vsDiff, patch, updateTarget.getCompose().getInclude().size());
            composeIncludeProcessed.reorderArrayElements(sourceValueSet, targetValueSet);
        }
        if (compareExecutable) {
            expansionContainsProcessed.appendInsertOperations(
                    vsDiff, patch, updateTarget.getExpansion().getContains().size());
            expansionContainsProcessed.appendDeleteOperations(
                    vsDiff, patch, updateTarget.getExpansion().getContains().size());
            expansionContainsProcessed.reorderArrayElements(sourceValueSet, targetValueSet);
        }
        return vsDiff;
    }

    private static <T> AdditionsAndDeletions<T> extractAdditionsAndDeletions(
            List<T> source, List<T> target, Class<T> t) {
        List<T> sourceCopy = new ArrayList<>(source);
        List<T> targetCopy = new ArrayList<>(target);
        // this is n^2 with Lists but can be nlog(n) if we use TreeSets
        // check for matches and additions
        List<T> insertions = new ArrayList<>();
        List<T> deletions = new ArrayList<>();
        List<T> sourceMatches = new ArrayList<>();
        List<T> targetMatches = new ArrayList<>();
        targetCopy.forEach(targetObj -> {
            Optional<T> isInSource = sourceCopy.stream()
                    .filter(sourceObj -> {
                        if (sourceObj instanceof RelatedArtifact sourceRA
                                && targetObj instanceof RelatedArtifact targetRA) {
                            return relatedArtifactEquals(sourceRA, targetRA);
                        } else if (sourceObj instanceof ConceptSetComponent sourceCSC
                                && targetObj instanceof ConceptSetComponent targetCSC) {
                            return conceptSetEquals(sourceCSC, targetCSC);
                        } else if (sourceObj instanceof ValueSetExpansionContainsComponent sourceVSECC
                                && targetObj instanceof ValueSetExpansionContainsComponent targetVSECC) {
                            return valueSetContainsEquals(sourceVSECC, targetVSECC);
                        } else if (sourceObj instanceof Extension sourceExt
                                && targetObj instanceof Extension targetExt) {
                            return extensionEquals(sourceExt, targetExt);
                        } else {
                            return false;
                        }
                    })
                    .findAny();
            if (isInSource.isPresent()) {
                sourceMatches.add(isInSource.get());
                targetMatches.add(targetObj);
                sourceCopy.remove(isInSource.get());
            } else {
                insertions.add(targetObj);
            }
        });
        // check for deletions
        sourceCopy.forEach(sourceObj -> {
            boolean isInTarget = targetCopy.stream().anyMatch(targetObj -> {
                if (sourceObj instanceof RelatedArtifact sourceRA && targetObj instanceof RelatedArtifact targetRA) {
                    return relatedArtifactEquals(sourceRA, targetRA);
                } else if (sourceObj instanceof ConceptSetComponent sourceCSC
                        && targetObj instanceof ConceptSetComponent targetCSC) {
                    return conceptSetEquals(sourceCSC, targetCSC);
                } else if (sourceObj instanceof ValueSetExpansionContainsComponent sourceVSECC
                        && targetObj instanceof ValueSetExpansionContainsComponent targetVSECC) {
                    return valueSetContainsEquals(sourceVSECC, targetVSECC);
                } else if (sourceObj instanceof Extension sourceExt && targetObj instanceof Extension targetExt) {
                    return extensionEquals(sourceExt, targetExt);
                } else {
                    return false;
                }
            });
            if (!isInTarget) {
                deletions.add(sourceObj);
            }
        });
        return new AdditionsAndDeletions<>(sourceMatches, targetMatches, insertions, deletions, t);
    }

    private static boolean relatedArtifactEquals(RelatedArtifact ref1, RelatedArtifact ref2) {
        return Objects.equals(Canonicals.getUrl(ref1.getResource()), Canonicals.getUrl(ref2.getResource()))
                && ref1.getType() == ref2.getType();
    }

    private static boolean conceptSetEquals(ConceptSetComponent ref1, ConceptSetComponent ref2) {
        // consider any includes which share at least 1 URL
        if (ref1.hasValueSet() && ref2.hasValueSet()) {
            var ref1Urls = ref1.getValueSet().stream()
                    .map(CanonicalType::getValue)
                    .map(Canonicals::getUrl)
                    .toList();
            var intersect = ref2.getValueSet().stream()
                    .map(CanonicalType::getValue)
                    .map(Canonicals::getUrl)
                    .filter(ref1Urls::contains)
                    .toList();
            return !intersect.isEmpty();
        } else if (!ref1.hasValueSet() && !ref2.hasValueSet()) {
            return ref1.getSystem().equals(ref2.getSystem());
        } else {
            // if one conceptSet has a value set but not the other then they can't be updates of each other
            return false;
        }
    }

    private static boolean valueSetContainsEquals(
            ValueSetExpansionContainsComponent ref1, ValueSetExpansionContainsComponent ref2) {
        return ref1.getSystem().equals(ref2.getSystem()) && ref1.getCode().equals(ref2.getCode());
    }

    private static boolean extensionEquals(Extension source, Extension target) {
        if (!source.getValue().getClass().equals(target.getValue().getClass())) {
            return false;
        }
        if (source.getValue() instanceof IPrimitiveType sourcePrimitive) {
            return (sourcePrimitive.getValue())
                    .equals((((IPrimitiveType) target.getValue()).getValue()));
        } else if (source.getValue() instanceof CodeableConcept sourceCodableConcept) {
            return sourceCodableConcept
                            .getCodingFirstRep()
                            .getSystem()
                            .equals(((CodeableConcept) target.getValue())
                                    .getCodingFirstRep()
                                    .getSystem())
                    && ((CodeableConcept) source.getValue())
                            .getCodingFirstRep()
                            .getCode()
                            .equals(((CodeableConcept) target.getValue())
                                    .getCodingFirstRep()
                                    .getCode());
        } else {
            return source.equals(target);
        }
    }

    private static Parameters handleRelatedArtifactArrayElementsDiff(
            MetadataResource sourceLibrary, MetadataResource targetLibrary, FhirPatch patch) {
        var updateSource =
                IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(sourceLibrary);
        var updateTarget =
                IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(targetLibrary);

        // separate into replacements/insertions/deletions
        var processedRelatedArtifacts = extractAdditionsAndDeletions(
                updateSource.getRelatedArtifact(), updateTarget.getRelatedArtifact(), RelatedArtifact.class);

        // handle relatedArtifact extensions separately
        var updateOperations = diffWithExtensions(updateSource, updateTarget, processedRelatedArtifacts, patch);

        // append insertions/deletions
        processedRelatedArtifacts.appendInsertOperations(
                updateOperations,
                patch,
                processedRelatedArtifacts.getSourceMatches().size());
        processedRelatedArtifacts.appendDeleteOperations(
                updateOperations,
                patch,
                processedRelatedArtifacts.getTargetMatches().size());

        // reorder elements to match the path indexes, this should only be temporarily persisted in the cache
        processedRelatedArtifacts.reorderArrayElements(sourceLibrary, targetLibrary);
        return updateOperations;
    }

    private static Parameters diffWithExtensions(
            IKnowledgeArtifactAdapter updateSource,
            IKnowledgeArtifactAdapter updateTarget,
            AdditionsAndDeletions<RelatedArtifact> processedRelatedArtifacts,
            FhirPatch patch) {
        updateSource.setRelatedArtifact(processedRelatedArtifacts.getSourceMatches());
        updateTarget.setRelatedArtifact(processedRelatedArtifacts.getTargetMatches());

        // get the diff of the RelatedArtifacts without Extensions
        var updateOperations = (Parameters) patch.diff(
                removeRelatedArtifactExtensions(updateSource), removeRelatedArtifactExtensions(updateTarget));

        for (var i = 0; i < processedRelatedArtifacts.getSourceMatches().size(); i++) {
            var processedExtensions = extractAdditionsAndDeletions(
                    processedRelatedArtifacts.getSourceMatches().get(i).getExtension(),
                    processedRelatedArtifacts.getTargetMatches().get(i).getExtension(),
                    Extension.class);
            // create Libraries with an empty relatedArtifact and add extensions because patch.diff only takes resources
            var source = new Library();
            source.addRelatedArtifact().setExtension(processedExtensions.getSourceMatches());
            var target = new Library();
            target.addRelatedArtifact().setExtension(processedExtensions.getTargetMatches());
            var extensionDiff = (Parameters) patch.diff(source, target);
            processedExtensions.appendInsertOperations(
                    extensionDiff, patch, processedExtensions.getSourceMatches().size());
            processedExtensions.appendDeleteOperations(
                    extensionDiff, patch, processedExtensions.getTargetMatches().size());
            // fix the path with the right indexes and remove the resourceType
            fixRelatedArtifactExtensionDiffPaths(
                    extensionDiff.getParameter(), i);
            updateOperations.getParameter().addAll(extensionDiff.getParameter());
            processedExtensions.reorderArrayElements(
                    processedRelatedArtifacts.getSourceMatches().get(i),
                    processedRelatedArtifacts.getTargetMatches().get(i));
        }
        return updateOperations;
    }

    private static void fixRelatedArtifactExtensionDiffPaths(
            List<ParametersParameterComponent> parameters, int relatedArtifactIndex) {
        for (ParametersParameterComponent parameter : parameters) {
            Optional<ParametersParameterComponent> path = parameter.getPart().stream()
                    .filter(ParametersParameterComponent::hasName)
                    .filter(part -> part.getName().equals("path"))
                    .findFirst();
            if (path.isPresent()) {
                var pathString = ((StringType) path.get().getValue()).getValue();
                var newIndex = "relatedArtifact[" + relatedArtifactIndex
                        + "]"; // Replace with your desired string
                var indexedPathString = pathString.replaceAll("relatedArtifact\\[([^\\]]+)\\]", newIndex);
                path.get().setValue(new StringType(indexedPathString));
            }
        }
    }

    private static IDomainResource removeRelatedArtifactExtensions(IKnowledgeArtifactAdapter resource) {
        var retval = resource.copy();
        IAdapterFactory.forFhirVersion(FhirVersionEnum.R4)
                .createKnowledgeArtifactAdapter(retval)
                .getRelatedArtifact()
                .forEach(ra -> ((RelatedArtifact) ra).setExtension(new ArrayList<>()));
        return retval;
    }

    private static class AdditionsAndDeletions<T> {
        private List<T> sourceMatches;
        private List<T> targetMatches;
        private List<T> insertions;
        private List<T> deletions;
        private Class<T> t;

        public AdditionsAndDeletions(
                List<T> sourceMatches, List<T> targetMatches, List<T> additions, List<T> deletions, Class<T> t) {
            this.sourceMatches = sourceMatches;
            this.targetMatches = targetMatches;
            this.insertions = additions;
            this.deletions = deletions;
            this.t = t;
        }

        public List<T> getSourceMatches() {
            return this.sourceMatches;
        }

        public List<T> getTargetMatches() {
            return this.targetMatches;
        }

        public List<T> getInsertions() {
            return this.insertions;
        }

        public List<T> getDeletions() {
            return this.deletions;
        }

        public void appendInsertOperations(Parameters baseParameters, FhirPatch patch, int startIndex)
                throws UnprocessableEntityException {
            prepareForComparison(baseParameters, patch, startIndex, true, this.insertions);
        }

        public void appendDeleteOperations(Parameters baseParameters, FhirPatch patch, int startIndex)
                throws UnprocessableEntityException {
            prepareForComparison(baseParameters, patch, startIndex, false, this.deletions);
        }

        public void reorderArrayElements(MetadataResource sourceResource, MetadataResource targetResource) {
            var sourceAdapter =
                    IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(sourceResource);
            var targetAdapter =
                    IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(targetResource);
            if (this.t.isAssignableFrom(RelatedArtifact.class)) {
                var sourceRelatedArtifacts =
                        (List<RelatedArtifact>) Stream.concat(this.sourceMatches.stream(), this.deletions.stream())
                                .collect(Collectors.toList());
                var targetRelatedArtifacts =
                        (List<RelatedArtifact>) Stream.concat(this.targetMatches.stream(), this.insertions.stream())
                                .collect(Collectors.toList());
                sourceAdapter.setRelatedArtifact(sourceRelatedArtifacts);
                targetAdapter.setRelatedArtifact(targetRelatedArtifacts);
            } else if (this.t.isAssignableFrom(ConceptSetComponent.class)
                    && (sourceResource instanceof ValueSet && targetResource instanceof ValueSet)) {
                var sourceComposeInclude =
                        (List<ConceptSetComponent>) Stream.concat(this.sourceMatches.stream(), this.deletions.stream())
                                .collect(Collectors.toList());
                var targetComposeInclude =
                        (List<ConceptSetComponent>) Stream.concat(this.targetMatches.stream(), this.insertions.stream())
                                .collect(Collectors.toList());
                ((ValueSet) sourceResource).getCompose().setInclude(sourceComposeInclude);
                ((ValueSet) targetResource).getCompose().setInclude(targetComposeInclude);
            } else if (this.t.isAssignableFrom(ValueSetExpansionContainsComponent.class)
                    && (sourceResource instanceof ValueSet && targetResource instanceof ValueSet)) {
                var sourceExpansionContains = (List<ValueSetExpansionContainsComponent>)
                        Stream.concat(this.sourceMatches.stream(), this.deletions.stream())
                                .collect(Collectors.toList());
                var targetExpansionContains = (List<ValueSetExpansionContainsComponent>)
                        Stream.concat(this.targetMatches.stream(), this.insertions.stream())
                                .collect(Collectors.toList());
                ((ValueSet) sourceResource).getExpansion().setContains(sourceExpansionContains);
                ((ValueSet) targetResource).getExpansion().setContains(targetExpansionContains);
            }
        }

        public void reorderArrayElements(RelatedArtifact sourceElement, RelatedArtifact targetElement) {
            if (this.t.isAssignableFrom(Extension.class)) {
                var sourceExtensions =
                        (List<Extension>) Stream.concat(this.sourceMatches.stream(), this.deletions.stream())
                                .collect(Collectors.toList());
                var targetExtensions =
                        (List<Extension>) Stream.concat(this.targetMatches.stream(), this.insertions.stream())
                                .collect(Collectors.toList());
                sourceElement.setExtension(sourceExtensions);
                targetElement.setExtension(targetExtensions);
            }
        }
        /**
         *
         * @param baseParameters base diff to append to
         * @param patch patch instance which performs the diff
         * @param startIndex where the start numbering the operations
         * @param insertOrDelete true = insert, false = delete
         * @param resourcesToAdd list of insertions or deletions
         * @throws UnprocessableEntityException
         */
        private void prepareForComparison(
                Parameters baseParameters,
                FhirPatch patch,
                int startIndex,
                boolean insertOrDelete,
                List<T> resourcesToAdd)
                throws UnprocessableEntityException {
            if (!resourcesToAdd.isEmpty()) {
                MetadataResource empty;
                MetadataResource hasNewResources;
                if (this.t.isAssignableFrom(RelatedArtifact.class)) {
                    empty = new Library();
                    hasNewResources = new Library();
                    ((Library) hasNewResources).setRelatedArtifact((List<RelatedArtifact>) resourcesToAdd);
                } else if (this.t.isAssignableFrom(ConceptSetComponent.class)) {
                    empty = new ValueSet();
                    ((ValueSet) empty).setCompose(new ValueSetComposeComponent().setInclude(new ArrayList<>()));
                    hasNewResources = new ValueSet();
                    ((ValueSet) hasNewResources)
                            .setCompose(new ValueSetComposeComponent()
                                    .setInclude((List<ConceptSetComponent>) resourcesToAdd));
                } else if (this.t.isAssignableFrom(ValueSetExpansionContainsComponent.class)) {
                    empty = new ValueSet();
                    ((ValueSet) empty).setExpansion(new ValueSetExpansionComponent().setContains(new ArrayList<>()));
                    hasNewResources = new ValueSet();
                    ((ValueSet) hasNewResources)
                            .setExpansion(new ValueSetExpansionComponent()
                                    .setContains((List<ValueSetExpansionContainsComponent>) resourcesToAdd));
                } else if (this.t.isAssignableFrom(Extension.class)) {
                    empty = new Library();
                    ((Library) empty).addRelatedArtifact();
                    hasNewResources = new Library();
                    var newRA = ((Library) hasNewResources).addRelatedArtifact();
                    newRA.setExtension((List<Extension>) resourcesToAdd);
                } else {
                    throw new UnprocessableEntityException("Could not process object");
                }
                if (insertOrDelete) {
                    appendInsertOperations(baseParameters, empty, hasNewResources, patch, startIndex);
                } else {
                    // swap source and target for deletions
                    appendDeleteOperations(baseParameters, hasNewResources, empty, patch, startIndex);
                }
            }
        }

        private void appendInsertOperations(
                Parameters baseParameters,
                IBaseResource source,
                IBaseResource target,
                FhirPatch patch,
                int startIndex) {
            var insertions = (Parameters) patch.diff(source, target);
            fixInsertPathIndexes(insertions.getParameter(), startIndex);
            baseParameters.getParameter().addAll(insertions.getParameter());
        }

        private static void appendDeleteOperations(
                Parameters baseParameters,
                IBaseResource source,
                IBaseResource target,
                FhirPatch patch,
                int startIndex) {
            var deletions = (Parameters) patch.diff(source, target);
            fixDeletePathIndexesAndAddValues(deletions.getParameter(), startIndex, source);
            baseParameters.getParameter().addAll(deletions.getParameter());
        }

        private static void fixDeletePathIndexesAndAddValues(
            List<ParametersParameterComponent> parameters, int newStart, IBaseResource sourceResource) {
            for (int i = 0; i < parameters.size(); i++) {
                ParametersParameterComponent parameter = parameters.get(i);
                Optional<ParametersParameterComponent> path = parameter.getPart().stream()
                    .filter(ParametersParameterComponent::hasName)
                    .filter(part -> part.getName().equals("path"))
                    .findFirst();
                if (path.isPresent()) {

                    var pathString = ((StringType) path.get().getValue()).getValue();
                    var e = new EncodeContextPath(pathString);
                    var noBase = removeBase(e);
                    try {
                        Optional.ofNullable((new BeanWrapperImpl(sourceResource).getPropertyValue(noBase)))
                            .ifPresent(oldVal -> {
                                if (oldVal instanceof Type) {
                                    parameter.addPart().setName("previousValue").setValue((Type) oldVal);
                                } else if (oldVal instanceof ValueSetExpansionContainsComponent) {
                                    var exp = (ValueSetExpansionContainsComponent) oldVal;
                                    parameter.addPart().setName("previousValue").setValue(exp.getCodeElement());
                                } else if (oldVal instanceof ConceptSetComponent) {
                                    var cmp = (ConceptSetComponent) oldVal;
                                    cmp.getConcept().forEach(ref -> {
                                        parameter
                                            .addPart()
                                            .setName("previousValue")
                                            .setValue(ref.getCodeElement());
                                    });
                                    cmp.getValueSet().forEach(vs -> {
                                        parameter
                                            .addPart()
                                            .setName("previousValue")
                                            .setValue(vs);
                                    });
                                } else if (oldVal instanceof RelatedArtifact ra) {
                                    parameter.addPart().setName("previousValue").setValue(ra.getResourceElement());
                                }
                            });

                    } catch (Exception err) {
                        throw new UnprocessableEntityException(
                            "Error while following changelog path to extract context:" + err.getMessage());
                    }
                    var newIndex = "[" + String.valueOf(i + newStart) + "]"; // Replace with your desired string
                    var result = pathString.replaceAll("\\[([^\\]]+)\\]", newIndex);
                    path.get().setValue(new StringType(result));
                }
            }
        }

        private static String removeBase(EncodeContextPath path) {
            return path.getPath().subList(1, path.getPath().size()).stream()
                .map(EncodeContextPathElement::toString)
                .collect(Collectors.joining("."));
        }

        private static void fixInsertPathIndexes(List<ParametersParameterComponent> parameters, int newStart) {
            int opCounter = 0;
            for (ParametersParameterComponent parameter : parameters) {
                Optional<ParametersParameterComponent> index = parameter.getPart().stream()
                    .filter(part -> part.getName().equals("index"))
                    .findFirst();
                Optional<ParametersParameterComponent> value = parameter.getPart().stream()
                    .filter(part -> part.getName().equals("value"))
                    .findFirst();
                Optional<ParametersParameterComponent> path = parameter.getPart().stream()
                    .filter(part -> part.getName().equals("path"))
                    .findFirst();
                if (path.isPresent()) {
                    var pathString = ((StringType) path.get().getValue()).getValue();
                    var e = new EncodeContextPath(pathString);
                    var elementName = e.getLeafElementName();
                    // for contains / include, we want to update the second last index and the
                    if (((elementName.equals("contains")
                        || elementName.equals("include")
                        || elementName.equals("relatedArtifact")) && (index.isPresent() && value.isEmpty())) || (elementName.equals("relatedArtifact") && index.isPresent())) {
                        index.get().setValue(new IntegerType(opCounter + newStart));
                        opCounter += 1;
                    }
                    if ((pathString.contains("expansion.contains") || pathString.contains("compose.include")) && value.isPresent()) {
                        var newIndex = "[" + (opCounter - 1 + newStart) + "]";
                        var result = pathString.replaceAll("\\[([^\\]]+)\\]", newIndex);
                        path.get().setValue(new StringType(result));
                    }
                }
            }
        }
    }
}
