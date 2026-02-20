package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ConceptSetComponent;
import org.opencds.cqf.fhir.cr.common.ArtifactDiffProcessor.DiffCache;
import org.opencds.cqf.fhir.cr.common.CreateChangelogProcessor.ChangeLog.ValueSetChild.Code;
import org.opencds.cqf.fhir.cr.crmi.TransformProperties;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateChangelogProcessor implements ICreateChangelogProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CreateChangelogProcessor.class);

    public CreateChangelogProcessor() {
        /* Empty as we will not perform create changelog outside HAPI context */
    }

    @Override
    public IBaseResource createChangelog(
            IBaseResource source, IBaseResource target, IBaseResource terminologyEndpoint) {
        logger.info("Unable to perform $create-changelog outside of HAPI context");
        return new Parameters();
    }

    public static class ChangeLog {
        private List<Page<?>> pages;
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

        public List<Page<?>> getPages() {
            return pages;
        }

        public void setPages(List<Page<?>> pages) {
            this.pages = pages;
        }

        public String getManifestUrl() {
            return manifestUrl;
        }

        public void setManifestUrl(String manifestUrl) {
            this.manifestUrl = manifestUrl;
        }

        public Page<ValueSetChild> addPage(ValueSet sourceResource, ValueSet targetResource, DiffCache cache)
                throws UnprocessableEntityException {
            if (sourceResource != null
                    && targetResource != null
                    && !sourceResource.getUrl().equals(targetResource.getUrl())) {
                throw new UnprocessableEntityException(URLS_DONT_MATCH);
            }
            // Map< [Code], [Object with code, version, system, etc.] >
            Map<String, Code> codeMap = new HashMap<>();
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
                DiffCache cache) {
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
                Map<String, Code> codeMap,
                Map<String, Map<String, ValueSetChild.Leaf>> leafMap,
                ValueSet valueSet,
                DiffCache cache,
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

        private void handleValueSetContains(Map<String, Code> codeMap, ValueSet valueSet, ValueSetChild.Leaf leafData) {
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
            var codeSystemName = Code.getCodeSystemName(system);
            var codeSystemOid = Code.getCodeSystemOid(system);
            var doesOidExistInList = leafData.codeSystems.stream()
                    .anyMatch(nameAndOid -> nameAndOid.oid != null && nameAndOid.oid.equals(codeSystemOid));
            if (!doesOidExistInList) {
                leafData.codeSystems.add(new ValueSetChild.Leaf.NameAndOid(codeSystemName, codeSystemOid));
            }
        }

        private ValueSetChild.Leaf updateLeafMap(
                Map<String, Map<String, ValueSetChild.Leaf>> leafMap, ValueSet valueSet)
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
                            null,
                            sourceResource.getIdElement().getIdPart(),
                            null,
                            null,
                            url,
                            sourceResource.fhirType());
            var newData = targetResource == null
                    ? null
                    : new OtherChild(
                            null,
                            targetResource.getIdElement().getIdPart(),
                            null,
                            null,
                            url,
                            targetResource.fhirType());
            var page = new Page<>(url, oldData, newData);
            this.pages.add(page);
            return page;
        }

        public Optional<Page<? extends PageBase>> getPage(String url) {
            return this.pages.stream()
                    .filter(p -> p.url != null && p.url.equals(url))
                    .findAny();
        }

        public void handleRelatedArtifacts() {
            var manifest = this.getPage(this.manifestUrl);
            if (manifest.isPresent()) {
                var specLibrary = manifest.get();
                var manifestOldData = (LibraryChild) specLibrary.oldData;
                var manifestNewData = (LibraryChild) specLibrary.newData;
                if (manifestNewData != null) {
                    for (final var page : this.pages) {
                        if (page.oldData instanceof ValueSetChild) {
                            updateConditionsAndPriorities(manifestOldData, (ValueSetChild) page.oldData);
                        }
                        if (page.newData instanceof ValueSetChild) {
                            updateConditionsAndPriorities(manifestNewData, (ValueSetChild) page.newData);
                        }
                    }
                }
            }
        }

        private void updateConditionsAndPriorities(LibraryChild manifestData, ValueSetChild pageData) {
            for (final var ra : manifestData.relatedArtifacts) {
                pageData.leafValueSets.stream()
                        .filter(leafValueSet -> leafValueSet.memberOid != null
                                && leafValueSet.memberOid.equals(Canonicals.getIdPart(ra.getValue())))
                        .forEach(leafValueSet -> {
                            updateConditions(ra, leafValueSet);
                            updatePriorities(ra, leafValueSet);
                        });
            }
        }

        private void updateConditions(RelatedArtifactUrlWithOperation ra, ChangeLog.ValueSetChild.Leaf leafValueSet) {
            ra.conditions.forEach(condition -> {
                if (condition.value != null) {
                    var c = leafValueSet.tryAddCondition(condition.value);
                    c.operation = condition.operation;
                }
            });
        }

        private void updatePriorities(RelatedArtifactUrlWithOperation ra, ChangeLog.ValueSetChild.Leaf leafValueSet) {
            if (ra.priority.value != null) {
                var coding = ra.priority.value.getCodingFirstRep();
                leafValueSet.priority.value = coding.getCode();
                leafValueSet.priority.operation = ra.priority.operation;
            }
        }

        public static class Page<T extends PageBase> {
            private final T oldData;
            private final T newData;
            private String url;
            private String resourceType;

            public T getOldData() {
                return oldData;
            }

            public T getNewData() {
                return newData;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getResourceType() {
                return resourceType;
            }

            public void setResourceType(String resourceType) {
                this.resourceType = resourceType;
            }

            Page(String url, T oldData, T newData) {
                this.url = url;
                this.oldData = oldData;
                this.newData = newData;
                if (oldData != null && oldData.getResourceType() != null) {
                    this.resourceType = oldData.getResourceType();
                } else if (newData != null && newData.getResourceType() != null) {
                    this.resourceType = newData.getResourceType();
                }
            }

            public void addOperation(String type, String path, Object currentValue, Object originalValue) {
                if (type != null) {
                    switch (type) {
                        case REPLACE -> addReplaceOperation(type, path, currentValue, originalValue);
                        case DELETE -> addDeleteOperation(type, path, originalValue);
                        case INSERT -> addInsertOperation(type, path, currentValue);
                        default -> throw new UnprocessableEntityException(
                                "Unknown type provided when adding an operation to the ChangeLog");
                    }
                } else {
                    throw new UnprocessableEntityException(
                            "Type must be provided when adding an operation to the ChangeLog");
                }
            }

            void addInsertOperation(String type, String path, Object currentValue) {
                if (!type.equals(INSERT)) {
                    throw new UnprocessableEntityException(WRONG_TYPE);
                }
                this.newData.addOperation(type, path, currentValue, null);
            }

            void addDeleteOperation(String type, String path, Object originalValue) {
                if (!type.equals(DELETE)) {
                    throw new UnprocessableEntityException(WRONG_TYPE);
                }
                this.oldData.addOperation(type, path, null, originalValue);
            }

            void addReplaceOperation(String type, String path, Object currentValue, Object originalValue) {
                if (!type.equals(REPLACE)) {
                    throw new UnprocessableEntityException(WRONG_TYPE);
                }
                this.oldData.addOperation(type, path, currentValue, null);
                this.newData.addOperation(type, path, null, originalValue);
            }
        }

        public static class ValueAndOperation {
            private String value;
            private Operation operation;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public Operation getOperation() {
                return operation;
            }

            public void setOperation(Operation operation) {
                if (operation != null) {
                    if (this.operation != null
                            && this.operation.type.equals(operation.type)
                            && this.operation.path.equals(operation.path)
                            && this.operation.newValue != operation.newValue) {
                        throw new UnprocessableEntityException("Multiple changes to the same element");
                    }
                    this.operation = operation;
                }
            }
        }

        public static class Operation {
            private String type;
            private String path;
            private Object newValue;
            private Object oldValue;

            Operation(String type, String path, Object newValue, Object originalValue) {
                this.type = type;
                this.path = path;
                if (originalValue instanceof IPrimitiveType<?> originalPrimitive) {
                    this.oldValue = originalPrimitive.getValue();
                } else if (originalValue instanceof IBase) {
                    this.oldValue = originalValue;
                } else if (originalValue != null) {
                    this.oldValue = originalValue.toString();
                }
                if (newValue instanceof IPrimitiveType<?> newPrimitive) {
                    this.newValue = newPrimitive.getValue();
                } else if (newValue instanceof IBase) {
                    this.newValue = newValue;
                } else if (newValue != null) {
                    this.newValue = newValue.toString();
                }
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public Object getNewValue() {
                return newValue;
            }

            public Object getOldValue() {
                return oldValue;
            }
        }

        public static class PageBase {
            private final ValueAndOperation title = new ValueAndOperation();
            private final ValueAndOperation id = new ValueAndOperation();
            private final ValueAndOperation version = new ValueAndOperation();
            private final ValueAndOperation name = new ValueAndOperation();

            public ValueAndOperation getTitle() {
                return title;
            }

            public ValueAndOperation getId() {
                return id;
            }

            public ValueAndOperation getVersion() {
                return version;
            }

            public ValueAndOperation getName() {
                return name;
            }

            public ValueAndOperation getUrl() {
                return url;
            }

            public String getResourceType() {
                return resourceType;
            }

            private final ValueAndOperation url = new ValueAndOperation();
            private final String resourceType;

            PageBase(String title, String id, String version, String name, String url, String resourceType) {
                if (!StringUtils.isEmpty(title)) {
                    this.title.value = title;
                }
                if (!StringUtils.isEmpty(id)) {
                    this.id.value = id;
                }
                if (!StringUtils.isEmpty(version)) {
                    this.version.value = version;
                }
                if (!StringUtils.isEmpty(name)) {
                    this.name.value = name;
                }
                if (!StringUtils.isEmpty(url)) {
                    this.url.value = url;
                }
                this.resourceType = resourceType;
            }

            public void addOperation(String type, String path, Object currentValue, Object originalValue) {
                if (type != null) {
                    var newOp = new Operation(type, path, currentValue, originalValue);
                    if (path.equals("id")) {
                        this.id.setOperation(newOp);
                    } else if (path.contains("title")) {
                        this.title.setOperation(newOp);
                    } else if (path.equals("version")) {
                        this.version.setOperation(newOp);
                    } else if (path.equals("name")) {
                        this.name.setOperation(newOp);
                    } else if (path.equals("url")) {
                        this.url.setOperation(newOp);
                    }
                }
            }
        }

        public static class ValueSetChild extends PageBase {
            private final List<Code> codes = new ArrayList<>();
            private final List<Leaf> leafValueSets = new ArrayList<>();
            private final List<Operation> operations = new ArrayList<>();
            private final ValueAndOperation priority = new ValueAndOperation();

            public List<Code> getCodes() {
                return codes;
            }

            public List<Leaf> getLeafValueSets() {
                return leafValueSets;
            }

            public List<Operation> getOperations() {
                return operations;
            }

            public ValueAndOperation getPriority() {
                return priority;
            }

            public static class Code {
                private final String id;
                private final String system;
                private final String code;
                private final String version;
                private final String display;
                private final String memberOid;
                private String codeSystemOid;
                private String codeSystemName;
                private final String parentValueSetName;
                private final String parentValueSetTitle;
                private final String parentValueSetUrl;
                private Operation operation;

                public String getId() {
                    return id;
                }

                public String getSystem() {
                    return system;
                }

                public String getCode() {
                    return code;
                }

                public String getVersion() {
                    return version;
                }

                public String getDisplay() {
                    return display;
                }

                public String getMemberOid() {
                    return memberOid;
                }

                public String getCodeSystemOid() {
                    return codeSystemOid;
                }

                public String getCodeSystemName() {
                    return codeSystemName;
                }

                public String getParentValueSetName() {
                    return parentValueSetName;
                }

                public String getParentValueSetTitle() {
                    return parentValueSetTitle;
                }

                public String getParentValueSetUrl() {
                    return parentValueSetUrl;
                }

                Code(
                        String id,
                        String system,
                        String code,
                        String version,
                        String display,
                        String memberOid,
                        String parentValueSetName,
                        String parentValueSetTitle,
                        String parentValueSetUrl,
                        Operation operation) {
                    this.id = id;
                    this.system = system;
                    if (system != null) {
                        this.codeSystemOid = getCodeSystemOid(system);
                        this.codeSystemName = getCodeSystemName(system);
                    }
                    this.code = code;
                    this.version = version;
                    this.display = display;
                    this.memberOid = memberOid;
                    this.operation = operation;
                    this.parentValueSetName = parentValueSetName;
                    this.parentValueSetTitle = parentValueSetTitle;
                    this.parentValueSetUrl = parentValueSetUrl;
                }

                public Code copy() {
                    return new Code(
                            this.id,
                            this.system,
                            this.code,
                            this.version,
                            this.display,
                            this.memberOid,
                            this.parentValueSetName,
                            this.parentValueSetTitle,
                            this.parentValueSetUrl,
                            this.operation);
                }

                public static String getCodeSystemOid(String systemUrl) {
                    if (systemUrl.contains("snomed")) {
                        return "2.16.840.1.113883.6.96";
                    } else if (systemUrl.contains("icd-10")) {
                        return "2.16.840.1.113883.6.90";
                    } else if (systemUrl.contains("icd-9")) {
                        return "2.16.840.1.113883.6.103, 2.16.840.1.113883.6.104";
                    } else if (systemUrl.contains("loinc")) {
                        return "2.16.840.1.113883.6.1";
                    } else {
                        return null;
                    }
                }

                public static String getCodeSystemName(String systemUrl) {
                    if (systemUrl.contains("snomed")) {
                        return "SNOMEDCT";
                    } else if (systemUrl.contains("icd-10")) {
                        return "ICD10CM";
                    } else if (systemUrl.contains("icd-9")) {
                        return "ICD9CM";
                    } else if (systemUrl.contains("loinc")) {
                        return "LOINC";
                    } else {
                        return null;
                    }
                }

                public Operation getOperation() {
                    return this.operation;
                }

                public void setOperation(Operation operation) {
                    if (operation != null) {
                        if (this.operation != null
                                && this.operation.type.equals(operation.type)
                                && this.operation.path.equals(operation.path)
                                && this.operation.newValue != operation.newValue) {
                            throw new UnprocessableEntityException("Multiple changes to the same element");
                        }
                        this.operation = operation;
                    }
                }
            }

            public static class Leaf {
                private final String memberOid;
                private final String name;
                private final String title;
                private final String url;
                private List<NameAndOid> codeSystems = new ArrayList<>();
                private String status;
                private List<Code> conditions = new ArrayList<>();
                private ValueAndOperation priority = new ValueAndOperation();
                private Operation operation;

                public String getMemberOid() {
                    return memberOid;
                }

                public String getName() {
                    return name;
                }

                public String getTitle() {
                    return title;
                }

                public String getUrl() {
                    return url;
                }

                public List<NameAndOid> getCodeSystems() {
                    return codeSystems;
                }

                public String getStatus() {
                    return status;
                }

                public List<Code> getConditions() {
                    return conditions;
                }

                public ValueAndOperation getPriority() {
                    return priority;
                }

                public Operation getOperation() {
                    return operation;
                }

                public static class NameAndOid {
                    private final String name;
                    private final String oid;

                    public String getName() {
                        return name;
                    }

                    public String getOid() {
                        return oid;
                    }

                    NameAndOid(String name, String oid) {
                        this.name = name;
                        this.oid = oid;
                    }

                    public NameAndOid copy() {
                        return new NameAndOid(this.name, this.oid);
                    }
                }

                Leaf(String memberOid, String name, String title, String url, PublicationStatus status) {
                    this.memberOid = memberOid;
                    this.name = name;
                    this.title = title;
                    this.url = url;
                    if (status != null) {
                        this.status = status.getDisplay();
                    }
                }

                public Leaf copy() {
                    var copy = new Leaf(this.memberOid, this.name, this.title, this.url, null);
                    copy.status = this.status;
                    copy.codeSystems =
                            this.codeSystems.stream().map(NameAndOid::copy).collect(Collectors.toList());
                    copy.conditions = this.conditions.stream().map(Code::copy).collect(Collectors.toList());
                    copy.priority = new ValueAndOperation();
                    copy.priority.value = this.priority.value;
                    copy.priority.operation = this.priority.operation;
                    copy.operation = this.operation;
                    return copy;
                }

                public ValueSetChild.Code tryAddCondition(CodeableConcept condition) {
                    var coding = condition.getCodingFirstRep();
                    var conditionName =
                            (coding.getDisplay() == null || coding.getDisplay().isBlank())
                                    ? condition.getText()
                                    : coding.getDisplay();
                    final var maybeExisting = this.conditions.stream()
                            .filter(code ->
                                    code.system.equals(coding.getSystem()) && code.code.equals(coding.getCode()))
                            .findAny();
                    if (maybeExisting.isEmpty()) {
                        final var newCondition = new ValueSetChild.Code(
                                coding.getId(),
                                coding.getSystem(),
                                coding.getCode(),
                                coding.getVersion(),
                                conditionName,
                                null,
                                null,
                                null,
                                null,
                                null);
                        this.conditions.add(newCondition);
                        return newCondition;
                    } else {
                        return maybeExisting.get();
                    }
                }
            }

            ValueSetChild(
                    String title,
                    String id,
                    String version,
                    String name,
                    String url,
                    List<ValueSet.ConceptSetComponent> compose,
                    List<ValueSet.ValueSetExpansionContainsComponent> contains,
                    Map<String, Code> codeMap,
                    Map<String, Map<String, Leaf>> leafMetadataMap,
                    String priority) {
                super(title, id, version, name, url, "ValueSet");
                if (contains != null) {
                    contains.forEach(contained -> {
                        if (contained.getCode() != null && codeMap.containsKey(contained.getCode())) {
                            this.codes.add(codeMap.get(contained.getCode()));
                        }
                    });
                }
                if (compose != null) {
                    compose.stream()
                            .filter(ConceptSetComponent::hasValueSet)
                            .flatMap(c -> c.getValueSet().stream())
                            .filter(PrimitiveType::hasValue)
                            .map(PrimitiveType::getValue)
                            .forEach(vs -> {
                                // sometimes the value set reference is unversioned - implying that the latest version
                                // should be used
                                // we need to make sure the diff operation only has the latest version in it, thereby we
                                // can get away with just having one url in the map and taking it
                                var urlPart = Canonicals.getUrl(vs);
                                if (Canonicals.getVersion(vs) == null) {
                                    // assume there is only the latest version
                                    var latest = leafMetadataMap
                                            .get(urlPart)
                                            .entrySet()
                                            .iterator()
                                            .next()
                                            .getValue();
                                    // creating a new object because modifying it causes weirdness later
                                    leafValueSets.add(latest.copy());
                                } else {
                                    var versionPart = Canonicals.getVersion(vs);
                                    var leaf = leafMetadataMap.get(urlPart).get(versionPart);
                                    // creating a new object because modifying it causes weirdness later
                                    leafValueSets.add(leaf.copy());
                                }
                            });
                }
                if (priority != null) {
                    this.priority.value = priority;
                }
            }

            @Override
            public void addOperation(String type, String path, Object newValue, Object originalValue) {
                if (type != null) {
                    super.addOperation(type, path, newValue, originalValue);
                    var operation = new Operation(type, path, newValue, originalValue);
                    if (path.contains("compose")) {
                        addOperationHandleCompose(type, path, newValue, originalValue, operation);
                    } else if (path.contains("expansion")) {
                        addOperationHandleExpansion(type, path, newValue, originalValue, operation);
                    } else if (path.contains("useContext")) {
                        addOperationHandleUseContext(newValue, originalValue, operation);
                    } else {
                        this.operations.add(operation);
                    }
                }
            }

            private void addOperationHandleCompose(
                    String type, String path, Object newValue, Object originalValue, Operation operation) {
                // if the valuesets changed
                List<String> urlsToCheck = List.of();
                // default to the original operation for use with primitive types
                List<Operation> updatedOperations = List.of(operation);
                if (newValue instanceof IPrimitiveType && ((IPrimitiveType<String>) newValue).hasValue()) {
                    urlsToCheck = List.of(((IPrimitiveType<String>) newValue).getValue());
                } else if (originalValue instanceof IPrimitiveType
                        && ((IPrimitiveType<String>) originalValue).hasValue()) {
                    urlsToCheck = List.of(((IPrimitiveType<String>) originalValue).getValue());
                } else if (newValue instanceof ValueSet.ValueSetComposeComponent newVSCC
                        && newVSCC.getIncludeFirstRep().hasValueSet()) {
                    urlsToCheck = newVSCC.getInclude().stream()
                            .filter(ConceptSetComponent::hasValueSet)
                            .flatMap(include -> include.getValueSet().stream())
                            .filter(PrimitiveType::hasValue)
                            .map(PrimitiveType::getValue)
                            .toList();
                    updatedOperations = urlsToCheck.stream()
                            .map(url -> new Operation(type, path, url, type.equals(REPLACE) ? originalValue : null))
                            .toList();
                } else if (originalValue instanceof ValueSet.ValueSetComposeComponent originalVSCC
                        && originalVSCC.getIncludeFirstRep().hasValueSet()) {
                    urlsToCheck = originalVSCC.getInclude().stream()
                            .filter(ConceptSetComponent::hasValueSet)
                            .flatMap(include -> include.getValueSet().stream())
                            .filter(PrimitiveType::hasValue)
                            .map(PrimitiveType::getValue)
                            .toList();
                    updatedOperations = urlsToCheck.stream()
                            .map(url -> new Operation(type, path, type.equals(REPLACE) ? newValue : null, url))
                            .toList();
                }
                handleUrlsToCheck(urlsToCheck, updatedOperations);
            }

            private void handleUrlsToCheck(List<String> urlsToCheck, List<Operation> updatedOperations) {
                if (!urlsToCheck.isEmpty()) {
                    for (var i = 0; i < urlsToCheck.size(); i++) {
                        final var urlNotNull = Canonicals.getIdPart(urlsToCheck.get(i));
                        for (final var leafValueSet : this.leafValueSets) {
                            if (leafValueSet.memberOid.equals(urlNotNull)) {
                                leafValueSet.operation = updatedOperations.get(i);
                            }
                        }
                    }
                }
            }

            private void addOperationHandleExpansion(
                    String type, String path, Object newValue, Object originalValue, Operation operation) {
                if (path.contains("expansion.contains[")) {
                    // if the codes themselves changed
                    String codeToCheck = getCodeToCheck(newValue, originalValue);
                    updateCodeOperation(codeToCheck, operation);
                } else if (newValue instanceof ValueSet.ValueSetExpansionComponent
                        || originalValue instanceof ValueSet.ValueSetExpansionComponent) {
                    var contains = newValue instanceof ValueSet.ValueSetExpansionComponent newVSEC
                            ? newVSEC
                            : (ValueSet.ValueSetExpansionComponent) originalValue;
                    contains.getContains().forEach(c -> {
                        Operation updatedOperation;
                        if (newValue instanceof ValueSet.ValueSetExpansionComponent) {
                            updatedOperation = new Operation(type, path, c.getCode(), null);
                        } else {
                            updatedOperation = new Operation(type, path, null, c.getCode());
                        }
                        updateCodeOperation(c.getCode(), updatedOperation);
                    });
                }
            }

            private static String getCodeToCheck(Object newValue, Object originalValue) {
                String codeToCheck = null;
                if (newValue instanceof IPrimitiveType || originalValue instanceof IPrimitiveType) {
                    codeToCheck = newValue instanceof IPrimitiveType
                            ? ((IPrimitiveType<String>) newValue).getValue()
                            : ((IPrimitiveType<String>) originalValue).getValue();
                } else if (originalValue instanceof ValueSet.ValueSetExpansionContainsComponent) {
                    codeToCheck = ((ValueSet.ValueSetExpansionContainsComponent) originalValue).getCode();
                }
                return codeToCheck;
            }

            private void addOperationHandleUseContext(Object newValue, Object originalValue, Operation operation) {
                String priorityToCheck = null;
                if (newValue instanceof UsageContext newUseContext
                        && newUseContext.getCode().getSystem().equals(TransformProperties.usPHUsageContextType)
                        && newUseContext.getCode().getCode().equals(TransformProperties.VSM_PRIORITY_CODE)) {
                    priorityToCheck = newUseContext
                            .getValueCodeableConcept()
                            .getCodingFirstRep()
                            .getCode();
                } else if (originalValue instanceof UsageContext originalUseContext
                        && originalUseContext.getCode().getSystem().equals(TransformProperties.usPHUsageContextType)
                        && originalUseContext.getCode().getCode().equals(TransformProperties.VSM_PRIORITY_CODE)) {
                    priorityToCheck = originalUseContext
                            .getValueCodeableConcept()
                            .getCodingFirstRep()
                            .getCode();
                }
                if (priorityToCheck != null) {
                    this.priority.operation = operation;
                }
            }

            private void updateCodeOperation(String codeToCheck, Operation operation) {
                if (codeToCheck != null) {
                    final String codeNotNull = codeToCheck;
                    this.codes.stream()
                            .filter(code -> code.code != null)
                            .filter(code -> code.code.equals(codeNotNull))
                            .findAny()
                            .ifPresentOrElse(
                                    code -> code.setOperation(operation),
                                    () ->
                                            // drop unmatched operations in the base operations list
                                            this.operations.add(operation));
                }
            }
        }

        public static class PlanDefinitionChild extends PageBase {
            PlanDefinitionChild(String title, String id, String version, String name, String url) {
                super(title, id, version, name, url, "PlanDefinition");
            }
        }

        public static class OtherChild extends PageBase {
            OtherChild(String title, String id, String version, String name, String url, String fhirType) {
                super(title, id, version, name, url, fhirType);
            }
        }

        public static class RelatedArtifactUrlWithOperation extends ValueAndOperation {
            private final RelatedArtifact fullRelatedArtifact;
            private List<CodeableConceptWithOperation> conditions = new ArrayList<>();
            private final CodeableConceptWithOperation priority = new CodeableConceptWithOperation(null);

            public RelatedArtifact getFullRelatedArtifact() {
                return fullRelatedArtifact;
            }

            public List<CodeableConceptWithOperation> getConditions() {
                return conditions;
            }

            public CodeableConceptWithOperation getPriority() {
                return priority;
            }

            public static class CodeableConceptWithOperation {
                private CodeableConcept value;
                private Operation operation;

                CodeableConceptWithOperation(CodeableConcept e) {
                    this.value = e;
                }

                public CodeableConcept getValue() {
                    return value;
                }

                public Operation getOperation() {
                    return operation;
                }
            }

            RelatedArtifactUrlWithOperation(RelatedArtifact relatedArtifact) {
                if (relatedArtifact != null) {
                    this.setValue(relatedArtifact.getResource());
                    this.conditions = relatedArtifact.getExtensionsByUrl(TransformProperties.vsmCondition).stream()
                            .map(e -> new CodeableConceptWithOperation((CodeableConcept) e.getValue()))
                            .toList();
                    var priorities = relatedArtifact.getExtensionsByUrl(TransformProperties.vsmPriority).stream()
                            .map(e -> (CodeableConcept) e.getValue())
                            .toList();
                    if (priorities.size() > 1) {
                        throw new UnprocessableEntityException("too many priorities");
                    } else if (priorities.size() == 1) {
                        this.priority.value = priorities.get(0);
                    } else {
                        this.priority.value = new CodeableConcept(
                                new Coding(TransformProperties.usPHUsageContext, "routine", "Routine"));
                    }
                }
                this.fullRelatedArtifact = relatedArtifact;
            }
        }

        public static class LibraryChild extends PageBase {
            private final ValueAndOperation purpose = new ValueAndOperation();
            private final ValueAndOperation effectiveStart = new ValueAndOperation();
            private final ValueAndOperation releaseDate = new ValueAndOperation();
            private final List<RelatedArtifactUrlWithOperation> relatedArtifacts = new ArrayList<>();

            LibraryChild(
                    String name,
                    String purpose,
                    String title,
                    String id,
                    String version,
                    String url,
                    String effectiveStart,
                    String releaseDate,
                    List<RelatedArtifact> relatedArtifacts) {
                super(title, id, version, name, url, "Library");
                if (!StringUtils.isEmpty(purpose)) {
                    this.purpose.value = purpose;
                }
                if (!StringUtils.isEmpty(effectiveStart)) {
                    this.effectiveStart.value = effectiveStart;
                }
                if (!StringUtils.isEmpty(releaseDate)) {
                    this.releaseDate.value = releaseDate;
                }
                if (!relatedArtifacts.isEmpty()) {
                    relatedArtifacts.forEach(ra -> this.relatedArtifacts.add(new RelatedArtifactUrlWithOperation(ra)));
                }
            }

            public ValueAndOperation getPurpose() {
                return purpose;
            }

            public ValueAndOperation getEffectiveStart() {
                return effectiveStart;
            }

            public ValueAndOperation getReleaseDate() {
                return releaseDate;
            }

            public List<RelatedArtifactUrlWithOperation> getRelatedArtifacts() {
                return relatedArtifacts;
            }

            private Optional<RelatedArtifactUrlWithOperation> getRelatedArtifactFromUrl(String target) {
                return this.relatedArtifacts.stream()
                        .filter(ra -> ra.getValue() != null && ra.getValue().equals(target))
                        .findAny();
            }

            private void tryAddConditionOperation(
                    Extension maybeCondition, RelatedArtifactUrlWithOperation target, Operation newOperation) {
                if (maybeCondition.getUrl().equals(TransformProperties.vsmCondition)) {
                    target.conditions.stream()
                            .filter(e -> e.value
                                            .getCodingFirstRep()
                                            .getSystem()
                                            .equals(((CodeableConcept) maybeCondition.getValue())
                                                    .getCodingFirstRep()
                                                    .getSystem())
                                    && e.value
                                            .getCodingFirstRep()
                                            .getCode()
                                            .equals(((CodeableConcept) maybeCondition.getValue())
                                                    .getCodingFirstRep()
                                                    .getCode()))
                            .findAny()
                            .ifPresent(condition -> condition.operation = newOperation);
                }
            }

            private void tryAddPriorityOperation(
                    Extension maybePriority, RelatedArtifactUrlWithOperation target, Operation newOperation) {
                if (maybePriority.getUrl().equals(TransformProperties.vsmPriority)
                        && (target.priority.value != null
                                && target.priority
                                        .value
                                        .getCodingFirstRep()
                                        .getSystem()
                                        .equals(((CodeableConcept) maybePriority.getValue())
                                                .getCodingFirstRep()
                                                .getSystem())
                                && target.priority
                                        .value
                                        .getCodingFirstRep()
                                        .getCode()
                                        .equals(((CodeableConcept) maybePriority.getValue())
                                                .getCodingFirstRep()
                                                .getCode()))) {
                    // priority will always be replace because:
                    // insert = an extension exists where it did not before, which is a replacement from "routine"
                    // to "emergent"
                    // delete = an extension does not exist where it did before, which is a replacement from
                    // "emergent" to "routine"
                    newOperation.type = REPLACE;
                    target.priority.operation = newOperation;
                }
            }

            @Override
            public void addOperation(String type, String path, Object currentValue, Object originalValue) {
                if (type != null) {
                    super.addOperation(type, path, currentValue, originalValue);
                    var newOperation = new Operation(type, path, currentValue, originalValue);
                    if (path != null && path.contains("elatedArtifact")) {
                        addOperationHandleRelatedArtifacts(path, currentValue, originalValue, newOperation);
                    } else if (path != null && path.equals("name")) {
                        this.getName().setOperation(newOperation);
                    } else if (path != null && path.contains("purpose")) {
                        this.purpose.setOperation(newOperation);
                    } else if (path != null && path.equals("approvalDate")) {
                        this.releaseDate.setOperation(newOperation);
                    } else if (path != null && path.contains("effectivePeriod")) {
                        this.effectiveStart.setOperation(newOperation);
                    }
                }
            }

            private void addOperationHandleRelatedArtifacts(
                    String path, Object currentValue, Object originalValue, Operation newOperation) {
                Optional<RelatedArtifactUrlWithOperation> operationTarget = Optional.empty();
                if (currentValue instanceof RelatedArtifact currentRelatedArtifact) {
                    operationTarget = getRelatedArtifactFromUrl(currentRelatedArtifact.getResource());
                } else if (originalValue instanceof RelatedArtifact originalRelatedArtifact) {
                    operationTarget = getRelatedArtifactFromUrl(originalRelatedArtifact.getResource());
                } else if (path.contains("[")) {
                    var matcher = Pattern.compile("relatedArtifact\\[(\\d+)]").matcher(path);
                    if (matcher.find()) {
                        var relatedArtifactIndex = Integer.parseInt(matcher.group(1));
                        operationTarget = Optional.of(this.relatedArtifacts.get(relatedArtifactIndex));
                    }
                }
                if (operationTarget.isPresent()) {
                    if (path.contains("xtension[")) {
                        var matcher = Pattern.compile("xtension\\[(\\d+)]").matcher(path);
                        if (matcher.find()) {
                            var extension = operationTarget
                                    .get()
                                    .fullRelatedArtifact
                                    .getExtension()
                                    .get(Integer.parseInt(matcher.group(1)));
                            tryAddConditionOperation(extension, operationTarget.orElse(null), newOperation);
                            tryAddPriorityOperation(extension, operationTarget.orElse(null), newOperation);
                        }
                    } else if (currentValue instanceof Extension currentExtension) {
                        tryAddConditionOperation(currentExtension, operationTarget.orElse(null), newOperation);
                        tryAddPriorityOperation(currentExtension, operationTarget.orElse(null), newOperation);
                    } else if (originalValue instanceof Extension originalExtension) {
                        tryAddConditionOperation(originalExtension, operationTarget.orElse(null), newOperation);
                        tryAddPriorityOperation(originalExtension, operationTarget.orElse(null), newOperation);
                    } else {
                        operationTarget.get().setOperation(newOperation);
                    }
                }
            }
        }
    }
}
