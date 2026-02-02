package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
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
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
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
    public IBaseResource createChangelog(IBaseResource source, IBaseResource target, Endpoint terminologyEndpoint) {
        logger.info("Unable to perform $create-changelog outside of HAPI context");
        return new Parameters();
    }

    public static class ChangeLog {
        public List<Page<?>> pages;
        public String manifestUrl;

        public ChangeLog(String url) {
            this.pages = new ArrayList<Page<?>>();
            this.manifestUrl = url;
        }

        public <T extends PageBase> Page<T> addPage(String url, T oldData, T newData) {
            var page = new Page<T>(url, oldData, newData);
            this.pages.add(page);
            return page;
        }

        public Page<ValueSetChild> addPage(ValueSet sourceResource, ValueSet targetResource, DiffCache cache)
                throws UnprocessableEntityException {
            if (sourceResource != null
                    && targetResource != null
                    && !sourceResource.getUrl().equals(targetResource.getUrl())) {
                throw new UnprocessableEntityException("URLs don't match");
            }
            // Map< [Code], [Object with code, version, system, etc.] >
            Map<String, Code> codeMap = new HashMap<String, Code>();
            // Map< [URL], Map <[Version], [Object with name, version, and other metadata] >>
            Map<String, Map<String, ValueSetChild.Leaf>> leafMetadataMap =
                    new HashMap<String, Map<String, ValueSetChild.Leaf>>();
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
            var url = sourceResource == null ? targetResource.getUrl() : sourceResource.getUrl();
            var page = new Page<ValueSetChild>(url, oldData, newData);
            this.pages.add(page);
            return page;
        }

        private Optional<String> getPriority(ValueSet valueSet) {
            return valueSet.getUseContext().stream()
                    .filter(uc -> uc.getCode().getSystem().equals(TransformProperties.usPHUsageContextType)
                            && uc.getCode().getCode().equals("priority"))
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
                    valueSet.getCompose().getInclude().forEach(concept -> {
                        if (concept.hasConcept()) {
                            var codeSystemName = ValueSetChild.Code.getCodeSystemName(concept.getSystem());
                            var codeSystemOid = ValueSetChild.Code.getCodeSystemOid(concept.getSystem());
                            var doesOidExistInList = leafData.codeSystems.stream()
                                    .anyMatch(nameAndOid ->
                                            nameAndOid.oid != null && nameAndOid.oid.equals(codeSystemOid));
                            if (!doesOidExistInList) {
                                leafData.codeSystems.add(
                                        new ValueSetChild.Leaf.NameAndOid(codeSystemName, codeSystemOid));
                            }
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
                if (valueSet.getExpansion().hasContains()) {
                    valueSet.getExpansion().getContains().forEach((cnt) -> {
                        if (!codeMap.containsKey(cnt.getCode())) {
                            var codeSystemName = ValueSetChild.Code.getCodeSystemName(cnt.getSystem());
                            var codeSystemOid = ValueSetChild.Code.getCodeSystemOid(cnt.getSystem());
                            var doesOidExistInList = leafData.codeSystems.stream()
                                    .anyMatch(nameAndOid ->
                                            nameAndOid.oid != null && nameAndOid.oid.equals(codeSystemOid));
                            if (!doesOidExistInList) {
                                leafData.codeSystems.add(
                                        new ValueSetChild.Leaf.NameAndOid(codeSystemName, codeSystemOid));
                            }
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
            }
        }

        private ValueSetChild.Leaf updateLeafMap(
                Map<String, Map<String, ValueSetChild.Leaf>> leafMap, ValueSet valueSet)
                throws UnprocessableEntityException {
            if (!valueSet.hasVersion()) {
                throw new UnprocessableEntityException("ValueSet " + valueSet.getUrl() + " does not have a version");
            }

            var versionedLeafMap = leafMap.get(valueSet.getUrl());
            ;
            if (!leafMap.containsKey(valueSet.getUrl())) {
                versionedLeafMap = new HashMap<String, ValueSetChild.Leaf>();
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
                throw new UnprocessableEntityException("URLs don't match");
            }
            var oldData = sourceResource == null
                    ? null
                    : new LibraryChild(
                            sourceResource.getName(),
                            sourceResource.getPurpose(),
                            sourceResource.getTitle(),
                            sourceResource.getIdPart(),
                            sourceResource.getVersion(),
                            sourceResource.getUrl(),
                            Optional.ofNullable((Period) sourceResource.getEffectivePeriod())
                                    .map(p -> p.getStart())
                                    .map(s -> s.toString())
                                    .orElse(null),
                            Optional.ofNullable(sourceResource.getApprovalDate())
                                    .map(s -> s.toString())
                                    .orElse(null),
                            sourceResource.getRelatedArtifact());
            var newData = targetResource == null
                    ? null
                    : new LibraryChild(
                            targetResource.getName(),
                            targetResource.getPurpose(),
                            targetResource.getTitle(),
                            targetResource.getIdPart(),
                            targetResource.getVersion(),
                            targetResource.getUrl(),
                            Optional.ofNullable((Period) targetResource.getEffectivePeriod())
                                    .map(p -> p.getStart())
                                    .map(s -> s.toString())
                                    .orElse(null),
                            Optional.ofNullable(targetResource.getApprovalDate())
                                    .map(s -> s.toString())
                                    .orElse(null),
                            targetResource.getRelatedArtifact());
            var url = sourceResource == null ? targetResource.getUrl() : sourceResource.getUrl();
            var page = new Page<LibraryChild>(url, oldData, newData);
            this.pages.add(page);
            return page;
        }

        public Page<PlanDefinitionChild> addPage(PlanDefinition sourceResource, PlanDefinition targetResource)
                throws UnprocessableEntityException {
            if (sourceResource != null
                    && targetResource != null
                    && !sourceResource.getUrl().equals(targetResource.getUrl())) {
                throw new UnprocessableEntityException("URLs don't match");
            }
            var oldData = sourceResource == null
                    ? null
                    : new PlanDefinitionChild(
                            sourceResource.getTitle(),
                            sourceResource.getIdPart(),
                            sourceResource.getVersion(),
                            sourceResource.getName(),
                            sourceResource.getUrl());
            var newData = targetResource == null
                    ? null
                    : new PlanDefinitionChild(
                            targetResource.getTitle(),
                            targetResource.getIdPart(),
                            targetResource.getVersion(),
                            targetResource.getName(),
                            targetResource.getUrl());
            var url = sourceResource == null ? targetResource.getUrl() : sourceResource.getUrl();
            var page = new Page<PlanDefinitionChild>(url, oldData, newData);
            this.pages.add(page);
            return page;
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
            var page = new Page<OtherChild>(url, oldData, newData);
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
                            for (final var ra : manifestOldData.relatedArtifacts) {
                                ((ValueSetChild) page.oldData)
                                        .leafValuesets.stream()
                                                .filter(leafValueSet -> leafValueSet.memberOid != null
                                                        && leafValueSet.memberOid.equals(
                                                                Canonicals.getIdPart(ra.value)))
                                                .forEach(leafValueSet -> {
                                                    updateConditions(ra, leafValueSet);
                                                    updatePriorities(ra, leafValueSet);
                                                });
                            }
                        }
                        if (page.newData instanceof ValueSetChild) {
                            for (final var ra : manifestNewData.relatedArtifacts) {
                                ((ValueSetChild) page.newData)
                                        .leafValuesets.stream()
                                                .filter(leafValueSet -> leafValueSet.memberOid != null
                                                        && leafValueSet.memberOid.equals(
                                                                Canonicals.getIdPart(ra.value)))
                                                .forEach(leafValueSet -> {
                                                    updateConditions(ra, leafValueSet);
                                                    updatePriorities(ra, leafValueSet);
                                                });
                            }
                        }
                    }
                }
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
            public T oldData;
            public T newData;
            public String url;
            public String resourceType;

            Page(String url, T oldData, T newData) {
                this.url = url;
                this.oldData = oldData;
                this.newData = newData;
                if (oldData != null && oldData.resourceType != null) {
                    this.resourceType = oldData.resourceType;
                } else if (newData != null && newData.resourceType != null) {
                    this.resourceType = newData.resourceType;
                }
            }

            public void addOperation(
                    String type, String path, Object currentValue, Object originalValue, ChangeLog parent) {
                if (type != null) {
                    switch (type) {
                        case "replace":
                            addReplaceOperation(type, path, currentValue, originalValue, parent);
                            break;
                        case "delete":
                            addDeleteOperation(type, path, null, originalValue, parent);
                            break;
                        case "insert":
                            addInsertOperation(type, path, currentValue, null, parent);
                            break;
                        default:
                            throw new UnprocessableEntityException(
                                    "Unknown type provided when adding an operation to the ChangeLog");
                    }
                } else {
                    throw new UnprocessableEntityException(
                            "Type must be provided when adding an operation to the ChangeLog");
                }
            }

            void addInsertOperation(
                    String type, String path, Object currentValue, Object originalValue, ChangeLog parent) {
                if (type != "insert") {
                    throw new UnprocessableEntityException("wrong type");
                }
                this.newData.addOperation(type, path, currentValue, originalValue, parent);
            }

            void addDeleteOperation(
                    String type, String path, Object currentValue, Object originalValue, ChangeLog parent) {
                if (type != "delete") {
                    throw new UnprocessableEntityException("wrong type");
                }
                this.oldData.addOperation(type, path, currentValue, originalValue, parent);
            }

            void addReplaceOperation(
                    String type, String path, Object currentValue, Object originalValue, ChangeLog parent) {
                if (type != "replace") {
                    throw new UnprocessableEntityException("wrong type");
                }
                this.oldData.addOperation(type, path, currentValue, null, parent);
                this.newData.addOperation(type, path, null, originalValue, parent);
            }
        }

        public static class ValueAndOperation {
            public String value;
            public Operation operation;

            public void setOperation(Operation operation) {
                if (operation != null) {
                    if (this.operation != null
                            && this.operation.type == operation.type
                            && this.operation.path == operation.path
                            && this.operation.newValue != operation.newValue) {
                        throw new UnprocessableEntityException("Multiple changes to the same element");
                    }
                    this.operation = operation;
                }
            }
        }

        public static class Operation {
            public String type;
            public String path;
            public Object newValue;
            public Object oldValue;

            Operation(String type, String path, IBase newValue, IBase original) {
                this.type = type;
                this.path = path;
                this.oldValue = original;
                this.newValue = newValue;
            }

            Operation(String type, String path, Object newValue, Object originalValue) {
                this.type = type;
                this.path = path;
                if (originalValue instanceof IPrimitiveType) {
                    this.oldValue = ((IPrimitiveType) originalValue).getValue();
                } else if (originalValue instanceof IBase) {
                    this.oldValue = originalValue;
                } else if (originalValue != null) {
                    this.oldValue = originalValue.toString();
                }
                if (newValue instanceof IPrimitiveType) {
                    this.newValue = ((IPrimitiveType) newValue).getValue();
                } else if (newValue instanceof IBase) {
                    this.newValue = newValue;
                } else if (newValue != null) {
                    this.newValue = newValue.toString();
                }
            }
        }

        public static class PageBase {
            public ValueAndOperation title = new ValueAndOperation();
            public ValueAndOperation id = new ValueAndOperation();
            public ValueAndOperation version = new ValueAndOperation();
            public ValueAndOperation name = new ValueAndOperation();
            public ValueAndOperation url = new ValueAndOperation();
            public String resourceType;

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

            public void addOperation(
                    String type, String path, Object currentValue, Object originalValue, ChangeLog parent) {
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
            public List<Code> codes = new ArrayList<>();
            public List<Leaf> leafValuesets = new ArrayList<>();
            public List<Operation> operations = new ArrayList<>();
            public ValueAndOperation priority = new ValueAndOperation();

            public static class Code {
                public String id;
                public String system;
                public String code;
                public String version;
                public String display;
                public String memberOid;
                public String codeSystemOid;
                public String codeSystemName;
                public String parentValueSetName;
                public String parentValueSetTitle;
                public String parentValueSetUrl;
                public Operation operation;

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
                                && this.operation.type == operation.type
                                && this.operation.path == operation.path
                                && this.operation.newValue != operation.newValue) {
                            throw new UnprocessableEntityException("Multiple changes to the same element");
                        }
                        this.operation = operation;
                    }
                }
            }

            public static class Leaf {
                public String memberOid;
                public String name;
                public String title;
                public String url;
                public List<NameAndOid> codeSystems = new ArrayList<NameAndOid>();
                public String status;
                public List<Code> conditions = new ArrayList<Code>();
                public ValueAndOperation priority = new ValueAndOperation();
                public Operation operation;

                public static class NameAndOid {
                    public String name;
                    public String oid;

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
                            this.codeSystems.stream().map(c -> c.copy()).collect(Collectors.toList());
                    copy.conditions =
                            this.conditions.stream().map(c -> c.copy()).collect(Collectors.toList());
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
                            .filter(cmp -> cmp.hasValueSet())
                            .flatMap(c -> c.getValueSet().stream())
                            .filter(vs -> vs.hasValue())
                            .map(vs -> vs.getValue())
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
                                    leafValuesets.add(latest.copy());
                                } else {
                                    var versionPart = Canonicals.getVersion(vs);
                                    var leaf = leafMetadataMap.get(urlPart).get(versionPart);
                                    // creating a new object because modifying it causes weirdness later
                                    leafValuesets.add(leaf.copy());
                                }
                            });
                }
                if (priority != null) {
                    this.priority.value = priority;
                }
            }

            @Override
            public void addOperation(
                    String type, String path, Object newValue, Object originalValue, ChangeLog parent) {
                if (type != null) {
                    super.addOperation(type, path, newValue, originalValue, parent);
                    var operation = new Operation(type, path, newValue, originalValue);
                    if (path.contains("compose")) {
                        // if the valuesets changed
                        List<String> urlsToCheck = List.of();
                        // default to the original operation for use with primitive types
                        List<Operation> updatedOperations = List.of(operation);
                        if (newValue instanceof IPrimitiveType && ((IPrimitiveType<String>) newValue).hasValue()) {
                            urlsToCheck = List.of(((IPrimitiveType<String>) newValue).getValue());
                        } else if (originalValue instanceof IPrimitiveType
                                && ((IPrimitiveType<String>) originalValue).hasValue()) {
                            urlsToCheck = List.of(((IPrimitiveType<String>) originalValue).getValue());
                        } else if (newValue instanceof ValueSet.ValueSetComposeComponent
                                && ((ValueSet.ValueSetComposeComponent) newValue)
                                        .getIncludeFirstRep()
                                        .hasValueSet()) {
                            urlsToCheck = ((ValueSet.ValueSetComposeComponent) newValue)
                                    .getInclude().stream()
                                            .filter(include -> include.hasValueSet())
                                            .flatMap(include -> include.getValueSet().stream())
                                            .filter(canonical -> canonical.hasValue())
                                            .map(canonical -> canonical.getValue())
                                            .collect(Collectors.toList());
                            updatedOperations = urlsToCheck.stream()
                                    .map(url -> new Operation(
                                            type, path, url, type.equals("replace") ? originalValue : null))
                                    .collect(Collectors.toList());
                        } else if (originalValue instanceof ValueSet.ValueSetComposeComponent
                                && ((ValueSet.ValueSetComposeComponent) originalValue)
                                        .getIncludeFirstRep()
                                        .hasValueSet()) {
                            urlsToCheck = ((ValueSet.ValueSetComposeComponent) originalValue)
                                    .getInclude().stream()
                                            .filter(include -> include.hasValueSet())
                                            .flatMap(include -> include.getValueSet().stream())
                                            .filter(canonical -> canonical.hasValue())
                                            .map(canonical -> canonical.getValue())
                                            .collect(Collectors.toList());
                            updatedOperations = urlsToCheck.stream()
                                    .map(url ->
                                            new Operation(type, path, type.equals("replace") ? newValue : null, url))
                                    .collect(Collectors.toList());
                        }
                        if (!urlsToCheck.isEmpty()) {
                            for (var i = 0; i < urlsToCheck.size(); i++) {
                                final var urlNotNull = Canonicals.getIdPart(urlsToCheck.get(i));
                                for (final var leafValueSet : this.leafValuesets) {
                                    if (leafValueSet.memberOid.equals(urlNotNull)) {
                                        leafValueSet.operation = updatedOperations.get(i);
                                    }
                                }
                            }
                        }
                    } else if (path.contains("expansion")) {
                        if (path.contains("expansion.contains[")) {
                            // if the codes themselves changed
                            String codeToCheck = null;
                            if (newValue instanceof IPrimitiveType || originalValue instanceof IPrimitiveType) {
                                codeToCheck = newValue instanceof IPrimitiveType
                                        ? ((IPrimitiveType<String>) newValue).getValue()
                                        : ((IPrimitiveType<String>) originalValue).getValue();
                            } else if (originalValue instanceof ValueSet.ValueSetExpansionContainsComponent) {
                                codeToCheck = ((ValueSet.ValueSetExpansionContainsComponent) originalValue).getCode();
                            }
                            updateCodeOperation(codeToCheck, operation);
                        } else if (newValue instanceof ValueSet.ValueSetExpansionComponent
                                || originalValue instanceof ValueSet.ValueSetExpansionComponent) {
                            var contains = newValue instanceof ValueSet.ValueSetExpansionComponent
                                    ? (ValueSet.ValueSetExpansionComponent) newValue
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
                    } else if (path.contains("useContext")) {
                        String priorityToCheck = null;
                        if (newValue instanceof UsageContext
                                && ((UsageContext) newValue)
                                        .getCode()
                                        .getSystem()
                                        .equals(TransformProperties.usPHUsageContextType)
                                && ((UsageContext) newValue).getCode().getCode().equals("priority")) {
                            priorityToCheck = ((UsageContext) newValue)
                                    .getValueCodeableConcept()
                                    .getCodingFirstRep()
                                    .getCode();
                        } else if (originalValue instanceof UsageContext
                                && ((UsageContext) originalValue)
                                        .getCode()
                                        .getSystem()
                                        .equals(TransformProperties.usPHUsageContextType)
                                && ((UsageContext) originalValue)
                                        .getCode()
                                        .getCode()
                                        .equals("priority")) {
                            priorityToCheck = ((UsageContext) originalValue)
                                    .getValueCodeableConcept()
                                    .getCodingFirstRep()
                                    .getCode();
                        }
                        if (priorityToCheck != null) {
                            this.priority.operation = operation;
                        }
                    } else {
                        this.operations.add(operation);
                    }
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
                                    code -> {
                                        code.setOperation(operation);
                                    },
                                    () -> {
                                        // drop unmatched operations in the base operations list
                                        this.operations.add(operation);
                                    });
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
            public RelatedArtifact fullRelatedArtifact;
            public List<codeableConceptWithOperation> conditions = new ArrayList<>();
            public codeableConceptWithOperation priority = new codeableConceptWithOperation(null);

            public static class codeableConceptWithOperation {
                public CodeableConcept value;
                public Operation operation;

                codeableConceptWithOperation(CodeableConcept e) {
                    this.value = e;
                }
            }

            RelatedArtifactUrlWithOperation(RelatedArtifact relatedArtifact) {
                if (relatedArtifact != null) {
                    this.value = relatedArtifact.getResource();
                    this.conditions = relatedArtifact.getExtensionsByUrl(TransformProperties.vsmCondition).stream()
                            .map(e -> new codeableConceptWithOperation((CodeableConcept) e.getValue()))
                            .collect(Collectors.toList());
                    var priorities = relatedArtifact.getExtensionsByUrl(TransformProperties.vsmPriority).stream()
                            .map(e -> (CodeableConcept) e.getValue())
                            .collect(Collectors.toList());
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
            public ValueAndOperation purpose = new ValueAndOperation();
            public ValueAndOperation effectiveStart = new ValueAndOperation();
            public ValueAndOperation releaseDate = new ValueAndOperation();
            public List<RelatedArtifactUrlWithOperation> relatedArtifacts = new ArrayList<>();

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

            private Optional<RelatedArtifactUrlWithOperation> getRelatedArtifactFromUrl(String target) {
                return this.relatedArtifacts.stream()
                        .filter(ra -> ra.value != null && ra.value.equals(target))
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
                            .ifPresent(condition -> {
                                condition.operation = newOperation;
                            });
                }
            }

            private void tryAddPriorityOperation(
                    Extension maybePriority, RelatedArtifactUrlWithOperation target, Operation newOperation) {
                if (maybePriority.getUrl().equals(TransformProperties.vsmPriority)) {
                    if (target.priority.value != null
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
                                            .getCode())) {
                        // priority will always be replace because:
                        // insert = an extension exists where it did not before, which is a replacement from "routine"
                        // to "emergent"
                        // delete = an extension does not exist where it did before, which is a replacement from
                        // "emergent" to "routine"
                        newOperation.type = "replace";
                        target.priority.operation = newOperation;
                    }
                    ;
                }
            }

            @Override
            public void addOperation(
                    String type, String path, Object currentValue, Object originalValue, ChangeLog parent) {
                if (type != null) {
                    super.addOperation(type, path, currentValue, originalValue, parent);
                    var newOperation = new Operation(type, path, currentValue, originalValue);
                    Optional<RelatedArtifactUrlWithOperation> operationTarget = Optional.ofNullable(null);
                    if (path != null && path.contains("elatedArtifact")) {
                        if (currentValue instanceof RelatedArtifact) {
                            operationTarget = getRelatedArtifactFromUrl(((RelatedArtifact) currentValue).getResource());
                        } else if (originalValue instanceof RelatedArtifact) {
                            operationTarget =
                                    getRelatedArtifactFromUrl(((RelatedArtifact) originalValue).getResource());
                        } else if (path.contains("[")) {
                            var matcher = Pattern.compile("relatedArtifact\\[(\\d+)\\]")
                                    .matcher(path);
                            if (matcher.find()) {
                                var relatedArtifactIndex = Integer.parseInt(matcher.group(1));
                                operationTarget = Optional.of(this.relatedArtifacts.get(relatedArtifactIndex));
                            }
                        }
                        if (operationTarget.isPresent()) {
                            if (path.contains("xtension[")) {
                                var matcher =
                                        Pattern.compile("xtension\\[(\\d+)\\]").matcher(path);
                                if (matcher.find()) {
                                    var extension = operationTarget
                                            .get()
                                            .fullRelatedArtifact
                                            .getExtension()
                                            .get(Integer.parseInt(matcher.group(1)));
                                    tryAddConditionOperation(extension, operationTarget.orElse(null), newOperation);
                                    tryAddPriorityOperation(extension, operationTarget.orElse(null), newOperation);
                                }
                            } else if (currentValue instanceof Extension) {
                                tryAddConditionOperation(
                                        (Extension) currentValue, operationTarget.orElse(null), newOperation);
                                tryAddPriorityOperation(
                                        (Extension) currentValue, operationTarget.orElse(null), newOperation);
                            } else if (originalValue instanceof Extension) {
                                tryAddConditionOperation(
                                        (Extension) originalValue, operationTarget.orElse(null), newOperation);
                                tryAddPriorityOperation(
                                        (Extension) originalValue, operationTarget.orElse(null), newOperation);
                            } else {
                                operationTarget.get().operation = newOperation;
                            }
                        }
                    } else if (path.equals("name")) {
                        this.name.setOperation(newOperation);
                    } else if (path.contains("purpose")) {
                        this.purpose.setOperation(newOperation);
                    } else if (path.equals("approvalDate")) {
                        this.releaseDate.setOperation(newOperation);
                    } else if (path.contains("effectivePeriod")) {
                        this.effectiveStart.setOperation(newOperation);
                    }
                }
            }
        }
    }
}
