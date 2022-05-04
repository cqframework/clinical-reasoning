package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.List;

public interface SubjectProvider {
    List<String> getSubjects(MeasureEvalType measureEvalType, String subjectId);
}
