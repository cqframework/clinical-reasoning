package org.opencds.cqf.fhir.cr.crmi.changelog;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor;
import org.opencds.cqf.fhir.cr.crmi.TransformProperties;
import org.opencds.cqf.fhir.utility.Canonicals;

@SuppressWarnings("rawtypes")
public class ChangeLog {

    private List<Page> pages;
    private String manifestUrl;
    public static final String URLS_DONT_MATCH = "URLs don't match";
    public static final String WRONG_TYPE = "wrong type";
    public static final String REPLACE = "replace";
    public static final String INSERT = "insert";
    public static final String DELETE = "delete";
    // Conditions and priorities are compared between sides rather than diffed, so their operations have no
    // FhirPatch path - this stands in for one.
    private static final String CONDITION_PATH = "condition";
    private static final String PRIORITY_PATH = "priority";

    public ChangeLog(String url) {
        this.pages = new ArrayList<>();
        this.manifestUrl = url;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }

    public String getManifestUrl() {
        return manifestUrl;
    }

    public void setManifestUrl(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    public Page<ValueSetChild> addPage(
            ValueSet sourceResource, ValueSet targetResource, ArtifactDiffProcessor.DiffCache cache)
            throws UnprocessableEntityException {
        if (sourceResource != null
                && targetResource != null
                && !sourceResource.getUrl().equals(targetResource.getUrl())) {
            throw new UnprocessableEntityException(URLS_DONT_MATCH);
        }
        // Map< [Code], [Object with code, version, system, etc.] > - one per side.
        //
        // One code map per side. Allows each side to correctly record details of a given code.
        // Prevents a single logical change from being reported twice, which could not be distinguished from a contradiction.
        Map<String, ValueSetChild.Code> sourceCodeMap = new HashMap<>();
        Map<String, ValueSetChild.Code> targetCodeMap = new HashMap<>();
        // Map< [URL], Map <[Version], [Object with name, version, and other metadata] >>
        Map<String, Map<String, ValueSetChild.Leaf>> leafMetadataMap = new HashMap<>();
        updateCodeMapAndLeafMetadataMap(sourceCodeMap, leafMetadataMap, sourceResource, cache);
        updateCodeMapAndLeafMetadataMap(targetCodeMap, leafMetadataMap, targetResource, cache);
        var oldData = sourceResource == null
                ? null
                : new ValueSetChild(
                        sourceResource.getTitle(),
                        sourceResource.getIdPart(),
                        sourceResource.getVersion(),
                        sourceResource.getName(),
                        sourceResource.getUrl(),
                        sourceResource.getCompose().getInclude(),
                        sourceResource.getExpansion().getContains(),
                        sourceCodeMap,
                        leafMetadataMap,
                        getPriority(sourceResource).orElse(null));
        var newData = targetResource == null
                ? null
                : new ValueSetChild(
                        targetResource.getTitle(),
                        targetResource.getIdPart(),
                        targetResource.getVersion(),
                        targetResource.getName(),
                        targetResource.getUrl(),
                        targetResource.getCompose().getInclude(),
                        targetResource.getExpansion().getContains(),
                        targetCodeMap,
                        leafMetadataMap,
                        getPriority(targetResource).orElse(null));
        var url = getPageUrl(sourceResource, targetResource);
        var page = new Page<>(url, oldData, newData);
        this.pages.add(page);
        return page;
    }

    public String getPageUrl(MetadataResource source, MetadataResource target) {
        if (source == null) {
            return target.getUrl();
        }
        return source.getUrl();
    }

    private Optional<String> getPriority(ValueSet valueSet) {
        return valueSet.getUseContext().stream()
                .filter(uc -> uc.getCode().getSystem().equals(TransformProperties.usPHUsageContextType)
                        && uc.getCode().getCode().equals(TransformProperties.VSM_PRIORITY_CODE))
                .findAny()
                .map(uc -> uc.getValueCodeableConcept().getCodingFirstRep().getCode());
    }

    private void updateCodeMapAndLeafMetadataMap(
            Map<String, ValueSetChild.Code> codeMap,
            Map<String, Map<String, ValueSetChild.Leaf>> leafMap,
            ValueSet valueSet,
            ArtifactDiffProcessor.DiffCache cache) {
        if (valueSet != null) {
            var leafData = updateLeafMap(leafMap, valueSet);
            if (valueSet.getCompose().hasInclude()) {
                handleValueSetInclude(codeMap, leafMap, valueSet, cache, leafData);
            }
            if (valueSet.getExpansion().hasContains()) {
                handleValueSetContains(codeMap, valueSet, leafData);
            }
        }
    }

    private void handleValueSetInclude(
            Map<String, ValueSetChild.Code> codeMap,
            Map<String, Map<String, ValueSetChild.Leaf>> leafMap,
            ValueSet valueSet,
            ArtifactDiffProcessor.DiffCache cache,
            ValueSetChild.Leaf leafData) {
        // compose.include carries no code system version in practice and has no element at all for a
        // code's active status, so fall back to what the ValueSet's expansion recorded per code. Without
        // the version the changelog cannot show where a repin's insert/delete pairs came from
        var expansionDetails = collectExpansionDetailsByCode(valueSet);
        valueSet.getCompose().getInclude().forEach(concept -> {
            if (concept.hasConcept()) {
                updateLeafData(concept.getSystem(), leafData);
                mapConceptSetToCodeMap(
                        codeMap,
                        concept,
                        Canonicals.getIdPart(valueSet.getUrl()),
                        valueSet.getName(),
                        valueSet.getTitle(),
                        valueSet.getUrl(),
                        expansionDetails);
            }
            if (concept.hasValueSet()) {
                concept.getValueSet().stream()
                        .map(vs -> cache.getResource(vs.getValue()).map(v -> (ValueSet) v))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(vs -> {
                            updateLeafMap(leafMap, vs);
                            updateCodeMapAndLeafMetadataMap(codeMap, leafMap, vs, cache);
                        });
            }
        });
    }

    private void handleValueSetContains(
            Map<String, ValueSetChild.Code> codeMap, ValueSet valueSet, ValueSetChild.Leaf leafData) {
        valueSet.getExpansion().getContains().forEach(cnt -> {
            if (!codeMap.containsKey(cnt.getCode())) {
                updateLeafData(cnt.getSystem(), leafData);
                mapExpansionContainsToCodeMap(
                        codeMap,
                        cnt,
                        Canonicals.getIdPart(valueSet.getUrl()),
                        valueSet.getName(),
                        valueSet.getTitle(),
                        valueSet.getUrl());
            }
        });
    }

    private static void updateLeafData(String system, ValueSetChild.Leaf leafData) {
        var codeSystemName = ValueSetChild.Code.getCodeSystemName(system);
        var codeSystemOid = ValueSetChild.Code.getCodeSystemOid(system);
        var doesOidExistInList = leafData.getCodeSystems().stream()
                .anyMatch(nameAndOid ->
                        nameAndOid.getOid() != null && nameAndOid.getOid().equals(codeSystemOid));
        if (!doesOidExistInList) {
            leafData.getCodeSystems().add(new ValueSetChild.Leaf.NameAndOid(codeSystemName, codeSystemOid));
        }
    }

    private ValueSetChild.Leaf updateLeafMap(Map<String, Map<String, ValueSetChild.Leaf>> leafMap, ValueSet valueSet)
            throws UnprocessableEntityException {
        if (!valueSet.hasVersion()) {
            throw new UnprocessableEntityException("ValueSet " + valueSet.getUrl() + " does not have a version");
        }

        var versionedLeafMap = leafMap.get(valueSet.getUrl());

        if (!leafMap.containsKey(valueSet.getUrl())) {
            versionedLeafMap = new HashMap<>();
            leafMap.put(valueSet.getUrl(), versionedLeafMap);
        }

        var leaf = versionedLeafMap.get(valueSet.getVersion());
        if (!versionedLeafMap.containsKey(valueSet.getVersion())) {
            leaf = new ValueSetChild.Leaf(
                    Canonicals.getIdPart(valueSet.getUrl()),
                    valueSet.getName(),
                    valueSet.getTitle(),
                    valueSet.getUrl(),
                    valueSet.getStatus());
            versionedLeafMap.put(valueSet.getVersion(), leaf);
        }
        return leaf;
    }

    private void mapExpansionContainsToCodeMap(
            Map<String, ValueSetChild.Code> codeMap,
            ValueSet.ValueSetExpansionContainsComponent containsComponent,
            String source,
            String name,
            String title,
            String url) {
        var system = containsComponent.getSystem();
        var id = containsComponent.getId();
        var version = containsComponent.getVersion();
        var codeValue = containsComponent.getCode();
        var display = containsComponent.getDisplay();
        var inactive = containsComponent.hasInactive() ? containsComponent.getInactive() : null;
        var code = new ValueSetChild.Code(
                id, system, codeValue, version, display, inactive, source, name, title, url, null);
        codeMap.put(codeValue, code);
    }

    // What the expansion recorded about a code that compose.include cannot express.
    private record ExpansionDetail(String version, Boolean inactive) {}

    // Collects the code system version and active status the expansion recorded for each code.
    private Map<String, ExpansionDetail> collectExpansionDetailsByCode(ValueSet valueSet) {
        if (!valueSet.getExpansion().hasContains()) {
            return Map.of();
        }
        Map<String, ExpansionDetail> detailsByCode = new HashMap<>();
        valueSet.getExpansion().getContains().forEach(contained -> {
            if (!contained.hasCode()) {
                return;
            }
            // First entry to state a thing wins, per field. Taking the first entry wholesale would let a
            // contains entry with no version block a later one that has it.
            detailsByCode.compute(
                    contained.getCode(),
                    (k, existing) -> new ExpansionDetail(
                            firstNonNull(
                                    existing == null ? null : existing.version(),
                                    contained.hasVersion() ? contained.getVersion() : null),
                            firstNonNull(
                                    existing == null ? null : existing.inactive(),
                                    contained.hasInactive() ? contained.getInactive() : null)));
        });
        return detailsByCode;
    }

    private static <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private void mapConceptSetToCodeMap(
            Map<String, ValueSetChild.Code> codeMap,
            ValueSet.ConceptSetComponent concept,
            String source,
            String name,
            String title,
            String url,
            Map<String, ExpansionDetail> expansionDetails) {
        var system = concept.getSystem();
        var id = concept.getId();
        var version = concept.getVersion();
        concept.getConcept().stream()
                .filter(ValueSet.ConceptReferenceComponent::hasCode)
                .forEach(conceptReference -> {
                    if (!codeMap.containsKey(conceptReference.getCode())) {
                        var detail = expansionDetails.get(conceptReference.getCode());
                        var code = new ValueSetChild.Code(
                                id,
                                system,
                                conceptReference.getCode(),
                                version == null || version.isBlank()
                                        ? (detail == null ? null : detail.version())
                                        : version,
                                conceptReference.getDisplay(),
                                detail == null ? null : detail.inactive(),
                                source,
                                name,
                                title,
                                url,
                                null);
                        codeMap.put(conceptReference.getCode(), code);
                    }
                });
    }

    public Page<LibraryChild> addPage(Library sourceResource, Library targetResource)
            throws UnprocessableEntityException {
        if (sourceResource != null
                && targetResource != null
                && !sourceResource.getUrl().equals(targetResource.getUrl())) {
            throw new UnprocessableEntityException(URLS_DONT_MATCH);
        }
        var oldData = getLibraryChild(sourceResource);
        var newData = getLibraryChild(targetResource);
        var url = getPageUrl(sourceResource, targetResource);
        var page = new Page<>(url, oldData, newData);
        this.pages.add(page);
        return page;
    }

    private static LibraryChild getLibraryChild(Library library) {
        return library == null
                ? null
                : new LibraryChild(
                        library.getName(),
                        library.getPurpose(),
                        library.getTitle(),
                        library.getIdPart(),
                        library.getVersion(),
                        library.getUrl(),
                        Optional.ofNullable(library.getEffectivePeriod())
                                .map(Period::getStart)
                                .map(Date::toString)
                                .orElse(null),
                        Optional.ofNullable(library.getApprovalDate())
                                .map(Date::toString)
                                .orElse(null),
                        library.getRelatedArtifact());
    }

    public Page<PlanDefinitionChild> addPage(PlanDefinition sourceResource, PlanDefinition targetResource)
            throws UnprocessableEntityException {
        if (sourceResource != null
                && targetResource != null
                && !sourceResource.getUrl().equals(targetResource.getUrl())) {
            throw new UnprocessableEntityException(URLS_DONT_MATCH);
        }
        var oldData = getPlanDefinitionChild(sourceResource);
        var newData = getPlanDefinitionChild(targetResource);
        var url = getPageUrl(sourceResource, targetResource);
        var page = new Page<>(url, oldData, newData);
        this.pages.add(page);
        return page;
    }

    private static PlanDefinitionChild getPlanDefinitionChild(PlanDefinition resource) {
        return resource == null
                ? null
                : new PlanDefinitionChild(
                        resource.getTitle(),
                        resource.getIdPart(),
                        resource.getVersion(),
                        resource.getName(),
                        resource.getUrl());
    }

    public Page<OtherChild> addPage(IBaseResource sourceResource, IBaseResource targetResource, String url)
            throws UnprocessableEntityException {
        var oldData = sourceResource == null
                ? null
                : new OtherChild(
                        null, sourceResource.getIdElement().getIdPart(), null, null, url, sourceResource.fhirType());
        var newData = targetResource == null
                ? null
                : new OtherChild(
                        null, targetResource.getIdElement().getIdPart(), null, null, url, targetResource.fhirType());
        var page = new Page<>(url, oldData, newData);
        this.pages.add(page);
        return page;
    }

    public Optional<Page> getPage(String url) {
        return this.pages.stream()
                .filter(p -> p.getUrl() != null && p.getUrl().equals(url))
                .findAny();
    }

    public void handleRelatedArtifacts() {
        var manifest = this.getPage(this.manifestUrl);
        if (manifest.isPresent()) {
            var specLibrary = manifest.get();
            var manifestOldData = (LibraryChild) specLibrary.getOldData();
            var manifestNewData = (LibraryChild) specLibrary.getNewData();
            if (manifestNewData != null) {
                for (final var page : this.pages) {
                    var oldValueSet = page.getOldData() instanceof ValueSetChild old ? old : null;
                    var newValueSet = page.getNewData() instanceof ValueSetChild latest ? latest : null;

                    // Whether a condition or priority changed is a property of the pair of sides, so
                    // both sides have to be known before either is populated.
                    var oldStated = statedForLeaves(manifestOldData, oldValueSet);
                    var newStated = statedForLeaves(manifestNewData, newValueSet);

                    // A value set present in only one release already says so through its own insert or
                    // delete, so a null operation type leaves its conditions and priorities unmarked.
                    var onBothSides = oldValueSet != null && newValueSet != null;
                    addConditionsAndPriorities(manifestOldData, oldValueSet, newStated, onBothSides ? DELETE : null);
                    addConditionsAndPriorities(manifestNewData, newValueSet, oldStated, onBothSides ? INSERT : null);
                }
            }
        }
    }

    /** What one side's manifest states for the leaves that side holds, to mark the other side against. */
    private record StatedForLeaves(Set<String> conditionKeys, Map<String, String> priorityByLeafOid) {}

    /** Reads only; nothing is modified. Called for both sides before either is populated. */
    private static StatedForLeaves statedForLeaves(LibraryChild manifest, ValueSetChild side) {
        Set<String> conditionKeys = new HashSet<>();
        Map<String, String> priorityByLeafOid = new HashMap<>();
        if (manifest == null || side == null) {
            return new StatedForLeaves(conditionKeys, priorityByLeafOid);
        }
        for (final var relatedArtifact : manifest.getRelatedArtifacts()) {
            for (final var leaf : leavesFor(side, relatedArtifact)) {
                for (final var condition : conditionsOf(relatedArtifact)) {
                    conditionKeys.add(conditionKey(leaf, condition));
                }
                var priority = priorityCodeOf(relatedArtifact);
                if (priority != null) {
                    priorityByLeafOid.put(leaf.getMemberOid(), priority);
                }
            }
        }
        return new StatedForLeaves(conditionKeys, priorityByLeafOid);
    }

    /**
     * Adds the manifest's conditions and priorities to one side, each already marked.
     *
     * @param otherSide what the other side states, so a change can be recognised as it is added
     * @param operationType what being absent from the other side means here, or null to mark nothing
     */
    private void addConditionsAndPriorities(
            LibraryChild manifest, ValueSetChild side, StatedForLeaves otherSide, String operationType) {
        if (manifest == null || side == null) {
            return;
        }
        for (final var relatedArtifact : manifest.getRelatedArtifacts()) {
            for (final var leaf : leavesFor(side, relatedArtifact)) {
                for (final var condition : conditionsOf(relatedArtifact)) {
                    var statedOnOtherSide = otherSide.conditionKeys().contains(conditionKey(leaf, condition));
                    leaf.tryAddCondition(
                            condition, statedOnOtherSide ? null : conditionOperation(condition, operationType));
                }
                updatePriority(relatedArtifact, leaf, otherSide.priorityByLeafOid(), operationType);
            }
        }
    }

    /**
     * This operation is derived from comparing the sides, and has no FhirPatch path of its own. This
     * avoids reporting positional changes in the collection of conditions.
     */
    private static Operation conditionOperation(CodeableConcept condition, String operationType) {
        if (operationType == null) {
            return null;
        }
        return new Operation(
                operationType, CONDITION_PATH, condition.getCodingFirstRep().getCode(), null);
    }

    private static List<CodeableConcept> conditionsOf(RelatedArtifactUrlWithOperation relatedArtifact) {
        return relatedArtifact.getConditions().stream()
                .map(RelatedArtifactUrlWithOperation.CodeableConceptWithOperation::getValue)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * The leaves on this side that the given manifest entry describes. The manifest lists every leaf in
     * the program, a page holds only the ones its grouper composes. Both identify a leaf by the OID
     * in its canonical, so that is what they are matched on.
     */
    private static List<ValueSetChild.Leaf> leavesFor(
            ValueSetChild side, RelatedArtifactUrlWithOperation manifestEntry) {
        var leafOid = Canonicals.getIdPart(manifestEntry.getValue());
        return side.getLeafValueSets().stream()
                .filter(leaf ->
                        leaf.getMemberOid() != null && leaf.getMemberOid().equals(leafOid))
                .toList();
    }

    /** A condition should be identified by leaf, system, and code. Never by its position. */
    private static String conditionKey(ValueSetChild.Leaf leaf, CodeableConcept condition) {
        return leaf.getMemberOid() + "|"
                + condition.getCodingFirstRep().getSystem() + "|"
                + condition.getCodingFirstRep().getCode();
    }

    /**
     * A leaf's priority, marked when the other side states a different one.
     *
     * Priority sits in the same relatedArtifact extension collection as conditions and had the same
     * defect: diff reports that collection positionally, it flagged nearly every leaf as a result.
     */
    private void updatePriority(
            RelatedArtifactUrlWithOperation relatedArtifact,
            ValueSetChild.Leaf leaf,
            Map<String, String> otherSidePriorities,
            String operationType) {
        var priority = priorityCodeOf(relatedArtifact);
        if (priority == null) {
            return;
        }
        leaf.getPriority().setValue(priority);
        var otherSidePriority = otherSidePriorities.get(leaf.getMemberOid());
        if (operationType != null && !Objects.equals(priority, otherSidePriority)) {
            leaf.getPriority().setOperation(new Operation(REPLACE, PRIORITY_PATH, priority, otherSidePriority));
        }
    }

    private static String priorityCodeOf(RelatedArtifactUrlWithOperation relatedArtifact) {
        var value = relatedArtifact.getPriority().getValue();
        return value == null ? null : value.getCodingFirstRep().getCode();
    }
}
