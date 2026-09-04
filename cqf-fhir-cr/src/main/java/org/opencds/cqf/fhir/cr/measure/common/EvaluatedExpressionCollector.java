package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.EXT_SUPPORTING_EVIDENCE_URL;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collects results of expressions the measure does not declare onto
 * {@link MeasureDef#evaluatedExpressions()}, so every evaluated expression can surface as
 * supporting evidence. Reads results the evaluation already produced; nothing is re-evaluated.
 */
public class EvaluatedExpressionCollector {

    private EvaluatedExpressionCollector() {}

    public static void collect(
            MeasureDef measureDef,
            Map<String, CqlEvaluationResult> evalResultsPerSubject,
            MeasureEvalType measureEvalType,
            SupportingEvidenceMode mode,
            FhirVersionEnum fhirVersion) {

        if (measureDef == null || evalResultsPerSubject == null || evalResultsPerSubject.isEmpty()) {
            return;
        }
        if (mode != SupportingEvidenceMode.ALL_EXPRESSIONS || !isSubjectScoped(measureEvalType)) {
            return;
        }

        Set<String> declared = declaredExpressions(measureDef);
        Set<String> collected = new HashSet<>();
        evalResultsPerSubject.forEach((subjectId, evalResult) -> {
            if (evalResult != null) {
                collectSubject(measureDef, subjectId, evalResult, declared, collected, fhirVersion);
            }
        });

        measureDef.evaluatedExpressions().sort(Comparator.comparing(SupportingEvidenceDef::getName));
    }

    private static void collectSubject(
            MeasureDef measureDef,
            String subjectId,
            CqlEvaluationResult evalResult,
            Set<String> declared,
            Set<String> collected,
            FhirVersionEnum fhirVersion) {

        for (CqlExpressionValue expressionValue : evalResult.getExpressionResults()) {
            String name = expressionValue.expressionName();
            if (name == null || name.isBlank() || declared.contains(name) || !collected.add(name)) {
                continue;
            }
            // dedupe precedes the filter so the recursive value walk runs once per expression,
            // not once per subject
            if (!SupportingEvidenceValueFilter.isSupported(expressionValue.raw(), fhirVersion)) {
                continue;
            }
            // description, language and code are author-supplied metadata with nothing to author from
            var def = new SupportingEvidenceDef(name, EXT_SUPPORTING_EVIDENCE_URL, null, name, null, null);
            def.addResource(subjectId, normalizeForEncoding(expressionValue.raw()));
            measureDef.addEvaluatedExpression(def);
        }
    }

    private static boolean isSubjectScoped(MeasureEvalType measureEvalType) {
        return measureEvalType == MeasureEvalType.SUBJECT || measureEvalType == MeasureEvalType.PATIENT;
    }

    private static Set<String> declaredExpressions(MeasureDef measureDef) {
        return measureDef.groups().stream()
                .flatMap(group -> group.populations().stream())
                .map(PopulationDef::getSupportingEvidenceDefs)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(SupportingEvidenceDef::getExpression)
                .collect(Collectors.toSet());
    }

    /** Null stays null (encodes as a data-absent marker); scalars are wrapped to arrive like lists. */
    private static Object normalizeForEncoding(Object raw) {
        if (raw == null || raw instanceof Iterable<?>) {
            return raw;
        }
        return List.of(raw);
    }
}
