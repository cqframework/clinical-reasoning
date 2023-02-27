package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.List;

public interface SubjectProvider {
    Iterable<String> getSubjects(MeasureEvalType measureEvalType, String subjectId);
}
