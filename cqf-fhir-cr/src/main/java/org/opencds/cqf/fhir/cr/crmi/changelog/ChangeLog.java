package org.opencds.cqf.fhir.cr.crmi.changelog;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
        // Map< [Code], [Object with code, version, system, etc.] >
        Map<String, ValueSetChild.Code> codeMap = new HashMap<>();
        // Map< [URL], Map <[Version], [Object with name, version, and other metadata] >>
        Map<String, Map<String, ValueSetChild.Leaf>> leafMetadataMap = new HashMap<>();
        updateCodeMapAndLeafMetadataMap(codeMap, leafMetadataMap, sourceResource, cache);
        updateCodeMapAndLeafMetadataMap(codeMap, leafMetadataMap, targetResource, cache);
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
                        codeMap,
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
                        codeMap,
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
        valueSet.getCompose().getInclude().forEach(concept -> {
            if (concept.hasConcept()) {
                updateLeafData(concept.getSystem(), leafData);
                mapConceptSetToCodeMap(
                        codeMap,
                        concept,
                        Canonicals.getIdPart(valueSet.getUrl()),
                        valueSet.getName(),
                        valueSet.getTitle(),
                        valueSet.getUrl());
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
        var code = new ValueSetChild.Code(id, system, codeValue, version, display, source, name, title, url, null);
        codeMap.put(codeValue, code);
    }

    // can this be done with a fhir operation? tx server work?
    private void mapConceptSetToCodeMap(
            Map<String, ValueSetChild.Code> codeMap,
            ValueSet.ConceptSetComponent concept,
            String source,
            String name,
            String title,
            String url) {
        var system = concept.getSystem();
        var id = concept.getId();
        var version = concept.getVersion();
        concept.getConcept().stream()
                .filter(ValueSet.ConceptReferenceComponent::hasCode)
                .forEach(conceptReference -> {
                    if (!codeMap.containsKey(conceptReference.getCode())) {
                        var code = new ValueSetChild.Code(
                                id,
                                system,
                                conceptReference.getCode(),
                                version,
                                conceptReference.getDisplay(),
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
                    if (page.getOldData() instanceof ValueSetChild oldValueSet) {
                        updateConditionsAndPriorities(manifestOldData, oldValueSet);
                    }
                    if (page.getNewData() instanceof ValueSetChild newValueSet) {
                        updateConditionsAndPriorities(manifestNewData, newValueSet);
                    }
                }
            }
        }
    }

    private void updateConditionsAndPriorities(LibraryChild manifestData, ValueSetChild pageData) {
        for (final var ra : manifestData.getRelatedArtifacts()) {
            pageData.getLeafValueSets().stream()
                    .filter(leafValueSet -> leafValueSet.getMemberOid() != null
                            && leafValueSet.getMemberOid().equals(Canonicals.getIdPart(ra.getValue())))
                    .forEach(leafValueSet -> {
                        updateConditions(ra, leafValueSet);
                        updatePriorities(ra, leafValueSet);
                    });
        }
    }

    private void updateConditions(RelatedArtifactUrlWithOperation ra, ValueSetChild.Leaf leafValueSet) {
        ra.getConditions().forEach(condition -> {
            if (condition.getValue() != null) {
                var c = leafValueSet.tryAddCondition(condition.getValue());
                c.setOperation(condition.getOperation());
            }
        });
    }

    private void updatePriorities(RelatedArtifactUrlWithOperation ra, ValueSetChild.Leaf leafValueSet) {
        if (ra.getPriority().getValue() != null) {
            var coding = ra.getPriority().getValue().getCodingFirstRep();
            leafValueSet.getPriority().setValue(coding.getCode());
            leafValueSet.getPriority().setOperation(ra.getPriority().getOperation());
        }
    }
}
