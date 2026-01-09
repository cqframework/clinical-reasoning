package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.EXT_SUPPORTING_EVIDENCE_URL;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
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

public class R4SupportingEvidenceExtension {

    private static final R4DateHelper DATE_HELPER = new R4DateHelper();
    private static final int MAX_DEPTH = 25;

    // If you have the official constant for this, use it instead of the literal:
    private static final String EXT_CQF_EXPRESSION_CODE = "http://hl7.org/fhir/StructureDefinition/cqf-expressionCode";

    /**
     * Produces ONE cqf-supportingEvidence extension PER SupportingEvidenceDef, and adds
     * each to reportPopulation.extension.
     *
     * Each supportingEvidence extension contains:
     * - name (required)
     * - description (optional)
     * - code (optional CodeableConcept)
     * - value (0..*) as repeated nested extensions holding the result(s)
     *
     * Matches StructureDefinition cqf-supportingEvidence (FHIR R5) shape, but authored on R4 MeasureReport.
     */
    public static void addSupportingEvidenceExtensions(
            MeasureReport.MeasureReportGroupPopulationComponent reportPopulation,
            List<SupportingEvidenceDef> supportingEvidenceDefs) {

        if (reportPopulation == null || supportingEvidenceDefs == null || supportingEvidenceDefs.isEmpty()) {
            return;
        }

        for (SupportingEvidenceDef def : supportingEvidenceDefs) {
            if (def == null) continue;

            // Name is required by the SD (min=1)
            String name = def.getName();
            if (name == null || name.isBlank()) {
                // Fallback to expression if name wasn't provided
                name = def.getExpression();
            }
            if (name == null || name.isBlank()) continue;

            Extension seExt = new Extension().setUrl(EXT_SUPPORTING_EVIDENCE_URL);

            // ---- name slice (required) ----
            seExt.addExtension(new Extension(
                    "name", new StringType(name))); // SD says valueCode; in R4 use StringType or CodeType as needed
            // If you truly need valueCode specifically, swap to: new CodeType(name)

            // ---- description slice (optional) ----
            String desc = def.getExpressionDescription();
            if (desc != null && !desc.isBlank()) {
                seExt.addExtension(new Extension("description", new StringType(desc)));
            }

            // ---- code slice (optional CodeableConcept) ----
            CodeableConcept codeConcept = conceptDefToConcept(def.getCode());
            if (codeConcept != null && codeConcept.hasCoding()) {
                seExt.addExtension(new Extension("code", codeConcept));
            }

            // ---- value slice(s) (0..*) ----
            Object exprValue = resolveExpressionValue(def);
            addValues(seExt, exprValue);

            reportPopulation.addExtension(seExt);
        }
    }

    /**
     * Pick the data you want to render for this SupportingEvidenceDef.
     *
     * Primary: subjectResources for the expression (if present).
     * Fallback: if a single key exists, use it.
     */
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
     * Each "value" entry may contain:
     * - value[x] for scalar/period/resourceId/etc
     * - nested extensions for tuple/list representations
     */
    private static void addValues(Extension supportingEvidenceExt, Object value) {
        if (value == null) {
            // SD says min 0 on value, so you can omit entirely. If you prefer explicit null, uncomment:
            // supportingEvidenceExt.addExtension(new Extension("value", new StringType("null")));
            return;
        }

        List<Object> leaves = new ArrayList<>();
        collectLeaves(value, leaves, 0);

        if (leaves.isEmpty()) {
            return;
        }

        // Create one "value" extension per leaf
        for (Object leaf : leaves) {
            Extension valueExt = new Extension("value");
            encodeLeafIntoValue(valueExt, leaf);
            supportingEvidenceExt.addExtension(valueExt);
        }
    }

    /**
     * Collect leaves without destroying Interval/Tuple structure.
     * Keep CqlType as leaf; encode later (don't stringify here).
     */
    private static void collectLeaves(Object value, List<Object> out, int depth) {
        if (value == null) return;
        if (depth > MAX_DEPTH) {
            out.add("[max-depth]");
            return;
        }

        // Preserve Interval and Tuple as structural leaves
        if (asInterval(value) != null || value instanceof Tuple) {
            out.add(value);
            return;
        }

        // Preserve CqlType as leaf (engine wrappers). We'll encode later.
        if (value instanceof CqlType) {
            out.add(value);
            return;
        }

        // Flatten iterables (Set/List/HashSetForFhirResourcesAndCqlTypes)
        if (value instanceof Iterable<?> it) {
            for (Object item : it) {
                collectLeaves(item, out, depth + 1);
            }
            return;
        }

        // Single leaf
        out.add(value);
    }

    /**
     * Encode a single leaf into a "value" extension.
     * This is where we map to FHIR value[x] or nested extensions for tuples/lists.
     */
    private static void encodeLeafIntoValue(Extension valueExt, Object leaf) {
        if (leaf == null) {
            valueExt.setValue(new StringType("null"));
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
            // non-date interval -> fall through to string
        }

        // Tuple -> represented as nested extensions under this "value"
        if (leaf instanceof Tuple tuple) {
            // Represent each tuple field as a nested extension where url == fieldName,
            // and its value is stored as value[x] or nested structure.
            for (Map.Entry<String, Object> entry : tuple.getElements().entrySet()) {
                Extension fieldExt = new Extension(entry.getKey());
                // A field can have multiple leaves; store them as repeated nested "value" inside fieldExt
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
            // store id string
            valueExt.setValue(new StringType(resourceIdString(r)));
        } else if (leaf instanceof org.hl7.fhir.r4.model.Type t) {
            // allow direct FHIR datatype
            valueExt.setValue(t);
        } else {
            // fallback
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
        if (o instanceof Interval i) return i;
        return null;
    }

    private static String resourceIdString(IBaseResource r) {
        var id = r.getIdElement();
        if (id == null || id.isEmpty()) {
            return "(no-id)";
        }
        return id.toUnqualifiedVersionless().getValue();
    }

    /**
     * Builds CodeableConcept for the "code" slice from SupportingEvidenceDef.
     * This example assumes you have a Coding available on the def, OR it is stored
     * similarly to your earlier valueExpression extension approach.
     *
     * Adjust to your real SupportingEvidenceDef model.
     */
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
