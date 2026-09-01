package org.opencds.cqf.fhir.cr.crmi.changelog;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.cr.crmi.TransformProperties;
import org.opencds.cqf.fhir.utility.Canonicals;

public class ValueSetChild extends PageBase {

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
        private final String codeValue;
        private final String version;
        private final String display;
        // Null means the code's status was never established, not that it is active. Only
        // expansion.contains carries inactive; compose.include has no equivalent element.
        private final Boolean inactive;
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

        public String getCodeValue() {
            return codeValue;
        }

        public String getVersion() {
            return version;
        }

        public String getDisplay() {
            return display;
        }

        public Boolean getInactive() {
            return inactive;
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

        @SuppressWarnings("java:S107")
        Code(
                String id,
                String system,
                String code,
                String version,
                String display,
                Boolean inactive,
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
            this.codeValue = code;
            this.version = version;
            this.display = display;
            this.inactive = inactive;
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
                    this.codeValue,
                    this.version,
                    this.display,
                    this.inactive,
                    this.memberOid,
                    this.parentValueSetName,
                    this.parentValueSetTitle,
                    this.parentValueSetUrl,
                    this.operation);
        }

        // Name and OID for each code system used in eRSD, matched on a fragment of the system URL
        private record CodeSystemIdentity(String urlFragment, String name, String oid) {}

        private static final List<CodeSystemIdentity> CODE_SYSTEM_IDENTITIES = List.of(
                new CodeSystemIdentity("snomed", "SNOMEDCT", "2.16.840.1.113883.6.96"),
                new CodeSystemIdentity("loinc", "LOINC", "2.16.840.1.113883.6.1"),
                new CodeSystemIdentity("icd-10", "ICD10CM", "2.16.840.1.113883.6.90"),
                new CodeSystemIdentity("icd-9", "ICD9CM", "2.16.840.1.113883.6.103, 2.16.840.1.113883.6.104"),
                new CodeSystemIdentity("rxnorm", "RXNORM", "2.16.840.1.113883.6.88"),
                new CodeSystemIdentity("cvx", "CVX", "2.16.840.1.113883.12.292"));

        private static Optional<CodeSystemIdentity> identify(String systemUrl) {
            if (systemUrl == null) {
                return Optional.empty();
            }
            var lowered = systemUrl.toLowerCase(Locale.ROOT);
            return CODE_SYSTEM_IDENTITIES.stream()
                    .filter(identity -> lowered.contains(identity.urlFragment()))
                    .findFirst();
        }

        public static String getCodeSystemOid(String systemUrl) {
            return identify(systemUrl).map(CodeSystemIdentity::oid).orElse(null);
        }

        public static String getCodeSystemName(String systemUrl) {
            return identify(systemUrl).map(CodeSystemIdentity::name).orElse(null);
        }

        public Operation getOperation() {
            return this.operation;
        }

        public void setOperation(Operation operation) {
            if (operation == null) {
                return;
            }
            // Each side now has its own Code, the only way one instance sees two operations at the
            // same type and path is the whole-expansion branch of addOperationHandleExpansion: it
            // fans over each contains entry using the same path, and two entries sharing a code
            // value then resolve to this Code twice, carrying the same value.
            if (this.operation != null
                    && this.operation.getType().equals(operation.getType())
                    && this.operation.getPath().equals(operation.getPath())
                    && !Objects.equals(this.operation.getNewValue(), operation.getNewValue())) {
                throw new UnprocessableEntityException(
                        "Multiple changes to the same code element: path=%s".formatted(operation.getPath()));
            }
            this.operation = operation;
        }
    }

    public static class Leaf {

        private final String memberOid;
        private final String name;
        private final String title;
        private final String url;
        private List<Leaf.NameAndOid> codeSystems = new ArrayList<>();
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

        public List<Leaf.NameAndOid> getCodeSystems() {
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

            public Leaf.NameAndOid copy() {
                return new Leaf.NameAndOid(this.name, this.oid);
            }
        }

        Leaf(String memberOid, String name, String title, String url, Enumerations.PublicationStatus status) {
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
                    this.codeSystems.stream().map(Leaf.NameAndOid::copy).collect(Collectors.toList());
            copy.conditions = this.conditions.stream().map(Code::copy).collect(Collectors.toList());
            copy.priority = new ValueAndOperation();
            copy.priority.setValue(this.priority.getValue());
            copy.priority.setOperation(this.priority.getOperation());
            copy.operation = this.operation;
            return copy;
        }

        /**
         * operation describes how this condition differs from the other side, or null when it does not
         */
        public Code tryAddCondition(CodeableConcept condition, Operation operation) {
            var coding = condition.getCodingFirstRep();
            var conditionName =
                    (coding.getDisplay() == null || coding.getDisplay().isBlank())
                            ? condition.getText()
                            : coding.getDisplay();
            final var maybeExisting = this.conditions.stream()
                    .filter(code -> code.system.equals(coding.getSystem()) && code.codeValue.equals(coding.getCode()))
                    .findAny();
            if (maybeExisting.isEmpty()) {
                final var newCondition = new Code(
                        coding.getId(),
                        coding.getSystem(),
                        coding.getCode(),
                        coding.getVersion(),
                        conditionName,
                        null,
                        null,
                        null,
                        null,
                        null,
                        operation);
                this.conditions.add(newCondition);
                return newCondition;
            } else {
                return maybeExisting.get();
            }
        }
    }

    @SuppressWarnings("java:S107")
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
                    .filter(ValueSet.ConceptSetComponent::hasValueSet)
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
            this.priority.setValue(priority);
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

    @SuppressWarnings("unchecked")
    private void addOperationHandleCompose(
            String type, String path, Object newValue, Object originalValue, Operation operation) {
        // if the valuesets changed
        List<String> urlsToCheck = List.of();
        // default to the original operation for use with primitive types
        List<Operation> updatedOperations = List.of(operation);
        if (newValue instanceof IPrimitiveType && ((IPrimitiveType<String>) newValue).hasValue()) {
            urlsToCheck = List.of(((IPrimitiveType<String>) newValue).getValue());
        } else if (originalValue instanceof IPrimitiveType && ((IPrimitiveType<String>) originalValue).hasValue()) {
            urlsToCheck = List.of(((IPrimitiveType<String>) originalValue).getValue());
        } else if (newValue instanceof ValueSet.ValueSetComposeComponent newVSCC
                && newVSCC.getIncludeFirstRep().hasValueSet()) {
            urlsToCheck = newVSCC.getInclude().stream()
                    .filter(ValueSet.ConceptSetComponent::hasValueSet)
                    .flatMap(include -> include.getValueSet().stream())
                    .filter(PrimitiveType::hasValue)
                    .map(PrimitiveType::getValue)
                    .toList();
            updatedOperations = urlsToCheck.stream()
                    .map(url -> new Operation(type, path, url, type.equals(ChangeLog.REPLACE) ? originalValue : null))
                    .toList();
        } else if (originalValue instanceof ValueSet.ValueSetComposeComponent originalVSCC
                && originalVSCC.getIncludeFirstRep().hasValueSet()) {
            urlsToCheck = originalVSCC.getInclude().stream()
                    .filter(ValueSet.ConceptSetComponent::hasValueSet)
                    .flatMap(include -> include.getValueSet().stream())
                    .filter(PrimitiveType::hasValue)
                    .map(PrimitiveType::getValue)
                    .toList();
            updatedOperations = urlsToCheck.stream()
                    .map(url -> new Operation(type, path, type.equals(ChangeLog.REPLACE) ? newValue : null, url))
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
        var primitive = asPrimitive(newValue) != null ? asPrimitive(newValue) : asPrimitive(originalValue);
        if (primitive != null) {
            // Only a string primitive can name a code. expansion.contains also holds a boolean
            // (inactive), and blind-casting that to String threw, aborting the entire changelog the
            // first time a code was retired. Anything non-string leaves the code unidentified, which
            // the caller already handles.
            if (primitive.getValue() instanceof String stringValue) {
                codeToCheck = stringValue;
            }
        } else if (originalValue instanceof ValueSet.ValueSetExpansionContainsComponent originalVSECC) {
            codeToCheck = originalVSECC.getCode();
        }
        return codeToCheck;
    }

    private static IPrimitiveType<?> asPrimitive(Object value) {
        return value instanceof IPrimitiveType<?> primitive ? primitive : null;
    }

    private void addOperationHandleUseContext(Object newValue, Object originalValue, Operation operation) {
        String priorityToCheck = null;
        if (newValue instanceof UsageContext newUseContext
                && newUseContext.getCode().getSystem().equals(TransformProperties.usPHUsageContextType)
                && newUseContext.getCode().getCode().equals(TransformProperties.VSM_PRIORITY_CODE)) {
            priorityToCheck =
                    newUseContext.getValueCodeableConcept().getCodingFirstRep().getCode();
        } else if (originalValue instanceof UsageContext originalUseContext
                && originalUseContext.getCode().getSystem().equals(TransformProperties.usPHUsageContextType)
                && originalUseContext.getCode().getCode().equals(TransformProperties.VSM_PRIORITY_CODE)) {
            priorityToCheck = originalUseContext
                    .getValueCodeableConcept()
                    .getCodingFirstRep()
                    .getCode();
        }
        if (priorityToCheck != null) {
            this.priority.setOperation(operation);
        }
    }

    private void updateCodeOperation(String codeToCheck, Operation operation) {
        if (codeToCheck != null) {
            final String codeNotNull = codeToCheck;
            this.codes.stream()
                    .filter(code -> code.codeValue != null)
                    .filter(code -> code.codeValue.equals(codeNotNull))
                    .findAny()
                    .ifPresentOrElse(
                            code -> code.setOperation(operation),
                            () ->
                                    // drop unmatched operations in the base operations list
                                    this.operations.add(operation));
        }
    }
}
