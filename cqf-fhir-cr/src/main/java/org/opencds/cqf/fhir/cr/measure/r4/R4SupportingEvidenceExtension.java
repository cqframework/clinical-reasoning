package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.CqlType;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.SupportingEvidenceDef;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;

/**
 * R4SupportingEvidenceExtension appends Supporting Evidence Criteria Results to MeasureReport.
 * Appends to MeasureReport.Group.Population.Extension as defined on Measure Resource.
 */
public class R4SupportingEvidenceExtension {

    private static final R4DateHelper DATE_HELPER = new R4DateHelper();
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

        if (reportPopulation == null || supportingEvidenceDefs == null || supportingEvidenceDefs.isEmpty()) {
            return;
        }

        for (SupportingEvidenceDef def : supportingEvidenceDefs) {
            Extension seExt = buildSupportingEvidenceExtension(def);
            if (seExt != null) {
                reportPopulation.addExtension(seExt);
            }
        }
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
        if (value == null) {
            return ValueKind.NULL_RESULT;
        }

        if (value instanceof Iterable<?> it) {
            boolean sawAny = false;
            boolean sawNonNull = false;

            for (Object o : it) {
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

        if (value instanceof Map<?, ?> m) {
            return m.isEmpty() ? ValueKind.EMPTY_LIST : ValueKind.NORMAL;
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

        // Preserve CQL runtime wrappers (encode later)
        if (value instanceof CqlType) {
            out.add(value);
            return;
        }

        // Flatten lists & sets
        if (value instanceof Iterable<?> it) {
            for (Object item : it) {
                collectLeavesInto(item, out, depth + 1);
            }
            return;
        }

        // Optional: flatten map values (if you still want)
        if (value instanceof Map<?, ?> map) {
            for (Object v : map.values()) {
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

        // Interval<DateTime>/Interval<Date> -> Period
        Interval interval = asInterval(leaf);
        if (interval != null) {
            Period p = tryBuildPeriod(interval);
            if (p != null) {
                valueExt.setValue(p); // valuePeriod
                return;
            }
            // non-date interval -> fall through
        }

        // Tuple -> represented as nested extensions under this "value"
        if (leaf instanceof Tuple tuple) {
            for (Map.Entry<String, Object> entry : tuple.getElements().entrySet()) {
                Extension fieldExt = new Extension(entry.getKey());
                // field values become repeated nested "value" slices under the field extension
                addValues(fieldExt, entry.getValue());
                valueExt.addExtension(fieldExt);
            }
            return;
        }

        // Scalars / resources / numeric
        if (leaf instanceof Boolean b) {
            valueExt.setValue(new BooleanType(b));
        } else if (leaf instanceof Integer i) {
            valueExt.setValue(new IntegerType(i));
        } else if (leaf instanceof BigDecimal bd) {
            valueExt.setValue(new DecimalType(bd));
        } else if (leaf instanceof String s) {
            valueExt.setValue(new StringType(s));
        } else if (leaf instanceof IBaseResource r) {
            valueExt.setValue(new StringType(resourceIdString(r)));
        } else if (leaf instanceof org.hl7.fhir.r4.model.Type t) {
            valueExt.setValue(t);
        } else {
            valueExt.setValue(new StringType(String.valueOf(leaf)));
        }
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
        var id = r.getIdElement();
        if (id == null || id.isEmpty()) {
            return "(no-id)";
        }
        return id.toUnqualifiedVersionless().getValue();
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
