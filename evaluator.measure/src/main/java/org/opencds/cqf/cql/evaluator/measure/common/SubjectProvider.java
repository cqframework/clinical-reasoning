package org.opencds.cqf.cql.evaluator.measure.common;

public interface SubjectProvider {
  Iterable<String> getSubjects(MeasureEvalType measureEvalType, String subjectId);
}
