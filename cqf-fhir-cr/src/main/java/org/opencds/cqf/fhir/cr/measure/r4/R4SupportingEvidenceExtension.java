package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SUPPORTING_EVIDENCE_URL;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.CqlType;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.fhir.cr.measure.common.ExtensionDef;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;

public class R4SupportingEvidenceExtension {

    private static final R4DateHelper DATE_HELPER = new R4DateHelper();
    private static final int MAX_DEPTH = 25;

    /**
     * Produces ONE parent extension (url = EXT_SUPPORTING_EVIDENCE_URL) and within it
     * a nested extension per CQL expression, where:
     * - nested extension url == expressionName
     * - nested extension contains ONLY result nodes
     */
    public static void addCqlResultExtension(
            MeasureReport.MeasureReportGroupPopulationComponent reportPopulation, List<ExtensionDef> extensionDefs) {

        if (reportPopulation == null || extensionDefs == null || extensionDefs.isEmpty()) {
            return;
        }

        Extension parent = new Extension().setUrl(EXT_SUPPORTING_EVIDENCE_URL);

        for (ExtensionDef def : extensionDefs) {
            if (def == null) continue;

            String expressionName = def.getExpression();
            if (expressionName == null || expressionName.isBlank()) continue;

            Extension exprExt = new Extension().setUrl(expressionName);

            Object exprValue = resolveExpressionValue(def, expressionName);

            addResultValue(exprExt, exprValue);
            parent.addExtension(exprExt);
        }

        reportPopulation.addExtension(parent);
    }

    /**
     * Prefer the set for this expressionName.
     * If not found, try to find a single Interval anywhere in the values (common for Measurement Period).
     * Avoid subjectResources.values() “mix-all-expressions” fallback.
     */
    private static Object resolveExpressionValue(ExtensionDef def, String expressionName) {
        Map<String, Set<Object>> subjectResources = def.getSubjectResources();
        if (subjectResources == null || subjectResources.isEmpty()) {
            return null;
        }

        // Direct match by expression name
        Set<Object> direct = subjectResources.get(expressionName);
        if (direct != null) {
            return direct;
        }

        // Safe fallback: if there is exactly one key, use its set
        if (subjectResources.size() == 1) {
            return subjectResources.values().iterator().next();
        }

        // Special fallback: if we can find exactly one Interval anywhere, use it
        Interval found = null;
        for (Set<Object> set : subjectResources.values()) {
            if (set == null) continue;
            for (Object o : set) {
                Interval interval = asInterval(o);
                if (interval != null) {
                    if (found != null) {
                        // more than one interval present -> ambiguous
                        return null;
                    }
                    found = interval;
                }
            }
        }
        return found; // may be null
    }

    private static void addResultValue(Extension target, Object value) {
        if (value == null) {
            target.addExtension(new Extension("resultString", new StringType("null")));
            return;
        }

        List<Object> leaves = new ArrayList<>();
        collectLeaves(value, leaves, 0);

        if (leaves.isEmpty()) {
            target.addExtension(new Extension("resultString", new StringType("null")));
            return;
        }

        if (leaves.size() == 1) {
            addSingleLeaf(target, leaves.get(0));
            return;
        }

        Extension listExt = new Extension("resultList");
        for (Object leaf : leaves) {
            addListItem(listExt, leaf);
        }
        target.addExtension(listExt);
    }

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

        // Preserve CqlType as leaf (don't stringify here)
        if (value instanceof CqlType) {
            out.add(value);
            return;
        }

        // Flatten containers
        if (value instanceof Iterable<?> it) {
            for (Object item : it) {
                collectLeaves(item, out, depth + 1);
            }
            return;
        }

        if (value instanceof java.util.Map<?, ?> map) {
            collectLeaves(map.values(), out, depth + 1);
            return;
        }

        if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                collectLeaves(java.lang.reflect.Array.get(value, i), out, depth + 1);
            }
            return;
        }

        out.add(value);
    }

    private static void addSingleLeaf(Extension target, Object leaf) {
        if (leaf == null) {
            target.addExtension(new Extension("resultString", new StringType("null")));
            return;
        }

        // Interval<DateTime>/Interval<Date> -> Period via existing helper, with CQF accessor fallback
        Interval interval = asInterval(leaf);
        if (interval != null) {
            Period p = tryBuildPeriod(interval);
            if (p != null) {
                target.addExtension(new Extension("resultPeriod", p));
                return;
            }
            // if it isn't Date/DateTime interval, fall through to string fallback
        }

        // Tuple (field name in url)
        if (leaf instanceof Tuple tuple) {
            Extension tupleExt = new Extension("resultTuple");
            for (Map.Entry<String, Object> entry : tuple.getElements().entrySet()) {
                Extension fieldExt = new Extension(entry.getKey());
                addResultValue(fieldExt, entry.getValue());
                tupleExt.addExtension(fieldExt);
            }
            target.addExtension(tupleExt);
            return;
        }

        // Scalars / resources / FHIR datatypes
        if (leaf instanceof Boolean b) {
            target.addExtension(new Extension("resultBoolean", new BooleanType(b)));
        } else if (leaf instanceof Integer i) {
            target.addExtension(new Extension("resultInteger", new IntegerType(i)));
        } else if (leaf instanceof String s) {
            target.addExtension(new Extension("resultString", new StringType(s)));
        } else if (leaf instanceof IBaseResource r) {
            target.addExtension(new Extension("resultResourceId", new StringType(resourceIdString(r))));
        } else if (leaf instanceof org.hl7.fhir.r4.model.Type t) {
            target.addExtension(new Extension("result", t));
        } else {
            target.addExtension(new Extension("resultString", new StringType(String.valueOf(leaf))));
        }
    }

    private static void addListItem(Extension listExt, Object leaf) {
        if (leaf == null) {
            listExt.addExtension(new Extension("itemNull", new StringType("null")));
            return;
        }

        // Interval item -> Period when possible
        Interval interval = asInterval(leaf);
        if (interval != null) {
            Period p = tryBuildPeriod(interval);
            if (p != null) {
                listExt.addExtension(new Extension("itemPeriod", p));
                return;
            }
        }

        // Tuple item
        if (leaf instanceof Tuple tuple) {
            Extension tupleItem = new Extension("itemTuple");
            for (Map.Entry<String, Object> entry : tuple.getElements().entrySet()) {
                Extension fieldExt = new Extension(entry.getKey());
                addResultValue(fieldExt, entry.getValue());
                tupleItem.addExtension(fieldExt);
            }
            listExt.addExtension(tupleItem);
            return;
        }

        // Scalar/resource items
        if (leaf instanceof IBaseResource r) {
            listExt.addExtension(new Extension("itemResourceId", new StringType(resourceIdString(r))));
        } else if (leaf instanceof Boolean b) {
            listExt.addExtension(new Extension("itemBoolean", new BooleanType(b)));
        } else if (leaf instanceof Integer i) {
            listExt.addExtension(new Extension("itemInteger", new IntegerType(i)));
        } else if (leaf instanceof String s) {
            listExt.addExtension(new Extension("itemString", new StringType(s)));
        } else if (leaf instanceof org.hl7.fhir.r4.model.Type t) {
            listExt.addExtension(new Extension("item", t));
        } else {
            listExt.addExtension(new Extension("itemString", new StringType(String.valueOf(leaf))));
        }
    }

    /**
     * Uses existing R4DateHelper, but if interval.getStart()/getEnd() are not populated,
     * rebuilds a new Interval using getLow()/getHigh() (common CQF variation) and retries.
     */
    private static Period tryBuildPeriod(Interval interval) {
        // First try directly (works when getStart/getEnd are DateTime/Date)
        try {
            return DATE_HELPER.buildMeasurementPeriod(interval);
        } catch (IllegalArgumentException ex) {
            // fall through
        }

        // Retry using low/high if present (some Interval instances populate these instead)
        Object low = invokeNoArgIfPresent(interval, "getLow");
        Object high = invokeNoArgIfPresent(interval, "getHigh");
        if (low == null && high == null) {
            return null;
        }

        try {
            Interval rebuilt = new Interval(low, true, high, true);
            return DATE_HELPER.buildMeasurementPeriod(rebuilt);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Returns Interval if object is an Interval or if it's wrapped as a CqlType that is actually Interval.
     */
    private static Interval asInterval(Object o) {
        if (o instanceof Interval i) return i;

        return null;
    }

    private static Object invokeNoArgIfPresent(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private static String resourceIdString(IBaseResource r) {
        var id = r.getIdElement();
        if (id == null || id.isEmpty()) {
            return "(no-id)";
        }
        return id.toUnqualifiedVersionless().getValue();
    }
}
