package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cql.ClassInstanceHelper.convertToFhirR4;
import static org.opencds.cqf.fhir.cql.ClassInstanceHelper.getId;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.cql.engine.runtime.Value;
import org.opencds.cqf.cql.engine.runtime.Vocabulary;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.CqlExpressionValue;
import org.opencds.cqf.fhir.cr.measure.common.SupportingEvidenceDef;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

/**
 * R4SupportingEvidenceExtension appends Supporting Evidence Criteria Results to MeasureReport.
 * Appends to MeasureReport.Group.Population.Extension as defined on Measure Resource.
 */
public class R4SupportingEvidenceExtension {

    private static final R4DateHelper DATE_HELPER = new R4DateHelper();
    private static final FhirTypeConverter TYPE_CONVERTER = new FhirTypeConverterFactory().create(FhirVersionEnum.R4);
    private static final int MAX_DEPTH = 25;

    // Primitive annotation URLs (match your examples)
    private static final String EXT_DATA_ABSENT_REASON = "http://hl7.org/fhir/StructureDefinition/data-absent-reason";
    private static final String EXT_CQF_CQL_TYPE = "http://hl7.org/fhir/StructureDefinition/cqf-cqlType";
    private static final String EXT_CQF_IS_EMPTY_LIST = "http://hl7.org/fhir/StructureDefinition/cqf-isEmptyList";

    private R4SupportingEvidenceExtension() {}

    /**
     * Produces ONE cqf-supportingEvidence extension PER SupportingEvidenceDef, and adds
     * each to reportPopulation.extension.
     *
     * Each supportingEvidence extension contains:
     * - name (required)
     * - description (optional)
     * - code (optional CodeableConcept)
     * - value (0..*) as repeated nested extensions holding the result(s)
     */
    public static void addSupportingEvidenceExtensions(
            MeasureReport.MeasureReportGroupPopulationComponent reportPopulation,
            List<SupportingEvidenceDef> supportingEvidenceDefs) {

        if (reportPopulation == null) {
            return;
        }
        buildSupportingEvidenceExtensions(supportingEvidenceDefs).forEach(reportPopulation::addExtension);
    }

    /**
     * Report-level variant, used for the entries synthesized for expressions the measure does not
     * declare.
     */
    public static void addSupportingEvidenceExtensions(
            MeasureReport report, List<SupportingEvidenceDef> supportingEvidenceDefs) {

        if (report == null) {
            return;
        }
        buildSupportingEvidenceExtensions(supportingEvidenceDefs).forEach(report::addExtension);
    }

    private static List<Extension> buildSupportingEvidenceExtensions(
            List<SupportingEvidenceDef> supportingEvidenceDefs) {

        if (supportingEvidenceDefs == null || supportingEvidenceDefs.isEmpty()) {
            return List.of();
        }

        List<Extension> extensions = new ArrayList<>();
        for (SupportingEvidenceDef def : supportingEvidenceDefs) {
            Extension seExt = buildSupportingEvidenceExtension(def);
            if (seExt != null) {
                extensions.add(seExt);
            }
        }
        return extensions;
    }

    private static Extension buildSupportingEvidenceExtension(SupportingEvidenceDef def) {
        if (def == null) {
            return null;
        }

        // Name is required by SD (min=1)
        String name = firstNonBlank(def.getName(), def.getExpression());
        if (name == null) {
            return null;
        }

        // URL comes from the definition (you are setting to systemUrl now)
        Extension seExt = new Extension().setUrl(def.getSystemUrl());

        // ---- name slice (required) ----
        // You asked for valueCode in JSON -> use CodeType so it serializes as valueCode
        seExt.addExtension(new Extension("name", new CodeType(name)));

        // ---- description slice (optional) ----
        String desc = def.getExpressionDescription();
        if (desc != null && !desc.isBlank()) {
            seExt.addExtension(new Extension("description", new StringType(desc)));
        }

        // ---- code slice (optional) ----
        CodeableConcept codeConcept = conceptDefToConcept(def.getCode());
        if (codeConcept != null && codeConcept.hasCoding()) {
            seExt.addExtension(new Extension("code", codeConcept));
        }

        // ---- value slice(s) ----
        Object exprValue = resolveExpressionValue(def);
        addValues(seExt, exprValue);

        return seExt;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    /**
     * Pick the data you want to render for this SupportingEvidenceDef.
     *
     * Primary: subjectResources for the expression (if present).
     * Fallback: if a single key exists, use it.
     */
    @Nullable
    private static Object resolveExpressionValue(SupportingEvidenceDef def) {
        Map<String, Set<Object>> subjectResources = def.getSubjectResources();
        if (subjectResources == null || subjectResources.isEmpty()) {
            return null;
        }

        String expr = def.getExpression();
        if (expr != null) {
            Set<Object> direct = subjectResources.get(expr);
            if (direct != null) {
                return direct;
            }
        }

        if (subjectResources.size() == 1) {
            return subjectResources.values().iterator().next();
        }

        return null;
    }

    /**
     * Adds repeated "value" nested extensions under the supportingEvidence extension.
     *
     * NEW BEHAVIOR (per your request):
     * - if result is null -> add one "value" slice with _valueBoolean extensions:
     *      data-absent-reason=unknown and cqf-cqlType=System.Any
     * - if result is empty list/set -> add one "value" slice with _valueBoolean extensions:
     *      cqf-isEmptyList=true and cqf-cqlType=List<System.Any>
     *
     * Otherwise:
     * - one "value" slice per leaf
     */
    private static void addValues(Extension supportingEvidenceExt, Object value) {

        // MUST classify before flattening
        ValueKind kind = classifyValue(value);

        if (kind == ValueKind.NULL_RESULT) {
            supportingEvidenceExt.addExtension(buildNullValueSliceExt("System.Any"));
            return;
        }

        if (kind == ValueKind.EMPTY_LIST) {
            supportingEvidenceExt.addExtension(buildEmptyListValueSliceExt("List<System.Any>"));
            return;
        }

        // NORMAL encoding path
        List<Object> leaves = collectLeaves(value);

        // If flattening produced nothing, treat as EMPTY_LIST (not null)
        if (leaves.isEmpty()) {
            supportingEvidenceExt.addExtension(buildEmptyListValueSliceExt("List<System.Any>"));
            return;
        }

        for (Object leaf : leaves) {
            Extension valueExt = new Extension("value");
            encodeLeafIntoValue(valueExt, leaf);
            supportingEvidenceExt.addExtension(valueExt);
        }
    }

    private enum ValueKind {
        NULL_RESULT,
        EMPTY_LIST,
        NORMAL
    }

    /**
     * Distinguish:
     * - NULL_RESULT: true null OR iterable containing only nulls
     * - EMPTY_LIST: iterable/map with no entries
     * - NORMAL: everything else
     */
    private static ValueKind classifyValue(Object value) {
        var wrapper = CqlExpressionValue.ofRaw(null, value, null);
        if (wrapper.isNull()) {
            return ValueKind.NULL_RESULT;
        }

        if (wrapper.isIterable()) {
            boolean sawAny = false;
            boolean sawNonNull = false;

            for (Object o : wrapper.asIterable()) {
                sawAny = true;
                if (o != null) {
                    sawNonNull = true;
                    break;
                }
            }

            if (!sawAny) {
                return ValueKind.EMPTY_LIST;
            }
            if (!sawNonNull) {
                // Iterable exists but all elements are null => treat as NULL_RESULT
                return ValueKind.NULL_RESULT;
            }
            return ValueKind.NORMAL;
        }

        if (wrapper.isMap()) {
            return wrapper.isEmpty() ? ValueKind.EMPTY_LIST : ValueKind.NORMAL;
        }

        return ValueKind.NORMAL;
    }

    private static Extension buildNullValueSliceExt(String cqlType) {
        Extension valueSlice = new Extension("value");
        valueSlice.setValue(buildNullMarkerBoolean(cqlType));
        return valueSlice;
    }

    private static Extension buildEmptyListValueSliceExt(String cqlType) {
        Extension valueSlice = new Extension("value");
        valueSlice.setValue(buildEmptyListMarkerBoolean(cqlType));
        return valueSlice;
    }

    /**
     * Collect leaves without destroying Interval/Tuple structure.
     * Keep CqlType as leaf; encode later (don't stringify here).
     */
    private static List<Object> collectLeaves(Object value) {
        List<Object> leaves = new ArrayList<>();
        collectLeavesInto(value, leaves, 0);
        return leaves;
    }

    private static void collectLeavesInto(Object value, List<Object> out, int depth) {
        if (depth > MAX_DEPTH) {
            out.add("[max-depth]");
            return;
        }

        // PRESERVE null leaf (do not drop it)
        if (value == null) {
            out.add(null);
            return;
        }

        // Preserve Interval and Tuple as atomic values
        if (asInterval(value) != null || value instanceof Tuple) {
            out.add(value);
            return;
        }

        var wrapper = CqlExpressionValue.ofRaw(null, value, null);

        // Flatten lists & sets
        if (wrapper.isIterable()) {
            for (Object item : wrapper.asIterable()) {
                collectLeavesInto(item, out, depth + 1);
            }
            return;
        }

        // Optional: flatten map values (if you still want)
        var asMap = wrapper.asMap();
        if (asMap.isPresent()) {
            for (Object v : asMap.get().values()) {
                collectLeavesInto(v, out, depth + 1);
            }
            return;
        }

        // Leaf
        out.add(value);
    }

    private static BooleanType buildNullMarkerBoolean(String cqlType) {
        BooleanType prim = new BooleanType();
        prim.setValue(null); // important: primitive value must be null

        prim.addExtension(new Extension(EXT_DATA_ABSENT_REASON, new org.hl7.fhir.r4.model.CodeType("unknown")));

        prim.addExtension(new Extension(EXT_CQF_CQL_TYPE, new StringType(cqlType != null ? cqlType : "System.Any")));

        return prim;
    }

    private static BooleanType buildEmptyListMarkerBoolean(String cqlType) {
        BooleanType prim = new BooleanType();
        prim.setValue(null); // important: primitive value must be null

        prim.addExtension(new Extension(EXT_CQF_IS_EMPTY_LIST, new BooleanType(true)));

        prim.addExtension(
                new Extension(EXT_CQF_CQL_TYPE, new StringType(cqlType != null ? cqlType : "List<System.Any>")));

        return prim;
    }

    /**
     * Encode a single leaf into a "value" slice.
     */
    private static void encodeLeafIntoValue(Extension valueExt, Object leaf) {

        // If a leaf is literally null (e.g. list with a null element),
        // encode as NULL RESULT marker (BooleanType with primitive extensions).
        if (leaf == null) {
            valueExt.setValue(buildNullMarkerBoolean("System.Any"));
            return;
        }

        // Tuple -> represented as nested extensions under this "value"
        if (leaf instanceof Tuple tuple) {
            encodeTupleIntoValue(valueExt, tuple);
            return;
        }

        org.hl7.fhir.r4.model.Type specialized = specializedLeafValue(leaf);
        valueExt.setValue(specialized != null ? specialized : genericLeafValue(leaf));
    }

    /** Tuple fields become repeated nested "value" slices under a per-field extension. */
    private static void encodeTupleIntoValue(Extension valueExt, Tuple tuple) {
        for (Map.Entry<String, Value> entry : tuple.getElements().entrySet()) {
            Extension fieldExt = new Extension(entry.getKey());
            addValues(fieldExt, entry.getValue());
            valueExt.addExtension(fieldExt);
        }
    }

    /**
     * Leaf types carrying a representation of their own, ahead of the generic tail. Returns null
     * when the leaf has no specialized form, which hands it to {@link #genericLeafValue(Object)}.
     */
    private static org.hl7.fhir.r4.model.Type specializedLeafValue(Object leaf) {

        // Interval<DateTime>/Interval<Date> -> Period. Kept ahead of the delegating tail, which
        // renders DateTime endpoints under a different offset; other intervals fall through.
        Interval interval = asInterval(leaf);
        if (interval != null) {
            Period period = tryBuildPeriod(interval);
            if (period != null) {
                return period;
            }
        }

        // CQL-5 changed Code.toString() to a quoted, multi-line form; render the stable single-line
        // representation the supporting-evidence string value has always carried. Handle this before
        // convertToFhirR4, which would coerce the Code into a bare CodeType losing system/display.
        if (leaf instanceof org.opencds.cqf.cql.engine.runtime.Code cqlCode) {
            return new StringType(formatCqlCode(cqlCode));
        }

        // R4 has no integer64; render the numeral as a string rather than a range-guarded integer
        if (leaf instanceof org.opencds.cqf.cql.engine.runtime.Long cqlLong) {
            return new StringType(String.valueOf(cqlLong.getValue()));
        }

        // ValueSet/CodeSystem -> canonical reference, versioned when the library pins one
        if (leaf instanceof Vocabulary vocabulary) {
            return new CanonicalType(canonicalReference(vocabulary));
        }

        if (leaf instanceof ClassInstance classInstance) {
            String reference = getId(classInstance);
            if (reference != null) {
                return new StringType(reference);
            }
        }

        return engineConvertedValue(leaf);
    }

    /**
     * Remaining System types (Quantity, Ratio, Concept, Time, non-temporal Interval) delegate to
     * the engine's converter; ClassInstance stays on the reference-string path.
     */
    private static org.hl7.fhir.r4.model.Type engineConvertedValue(Object leaf) {
        if (leaf instanceof Value cqlLeaf && !(leaf instanceof ClassInstance) && TYPE_CONVERTER.isCqlType(cqlLeaf)) {
            var converted = TYPE_CONVERTER.toFhirType(cqlLeaf);
            if (converted instanceof org.hl7.fhir.r4.model.Type fhirType) {
                return fhirType;
            }
        }
        return null;
    }

    /** Scalars / resources / numeric, after handing anything CQL-native to the converter. */
    private static org.hl7.fhir.r4.model.Type genericLeafValue(Object leaf) {
        var value = leaf instanceof Value cqlValue ? convertToFhirR4(cqlValue) : leaf;

        if (value instanceof Boolean b) {
            return new BooleanType(b);
        } else if (value instanceof Integer i) {
            return new IntegerType(i);
        } else if (value instanceof BigDecimal bd) {
            return new DecimalType(bd);
        } else if (value instanceof String s) {
            return new StringType(s);
        } else if (value instanceof IBaseResource r) {
            return new StringType(resourceIdString(r));
        } else if (value instanceof org.hl7.fhir.r4.model.Type t) {
            return t;
        } else {
            return new StringType(String.valueOf(leaf));
        }
    }

    private static String formatCqlCode(org.opencds.cqf.cql.engine.runtime.Code code) {
        return "Code { code: %s, system: %s, version: %s, display: %s }"
                .formatted(code.getCode(), code.getSystem(), code.getVersion(), code.getDisplay());
    }

    private static String canonicalReference(Vocabulary vocabulary) {
        String version = vocabulary.getVersion();
        if (version == null || version.isBlank()) {
            return vocabulary.getId();
        }
        return vocabulary.getId() + "|" + version;
    }

    private static Period tryBuildPeriod(Interval interval) {
        try {
            return DATE_HELPER.buildMeasurementPeriod(interval);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Interval asInterval(Object o) {
        return (o instanceof Interval i) ? i : null;
    }

    private static String resourceIdString(IBaseResource r) {
        var id = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4)
                .createResource(r)
                .getId();
        if (id == null || id.isEmpty()) {
            return "(no-id)";
        }
        return id;
    }

    @Nullable
    private static CodeableConcept conceptDefToConcept(ConceptDef c) {
        if (c == null) {
            return null;
        }
        var cc = new CodeableConcept().setText(c.text());
        for (var cd : c.codes()) {
            cc.addCoding(codeDefToCoding(cd));
        }
        return cc;
    }

    private static Coding codeDefToCoding(CodeDef c) {
        var cd = new Coding();
        cd.setSystem(c.system());
        cd.setCode(c.code());
        cd.setVersion(c.version());
        cd.setDisplay(c.display());
        return cd;
    }
}
