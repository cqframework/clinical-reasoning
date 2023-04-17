package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.stream.Stream;

public interface SubjectProvider {
  Stream<String> getSubjects(MeasureEvalType measureEvalType, String subjectId);
}
