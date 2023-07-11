package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.List;
import java.util.stream.Stream;

public interface SubjectProvider {
  Stream<String> getSubjects(MeasureEvalType measureEvalType, String subjectId);

  Stream<String> getSubjects(MeasureEvalType measureEvalType, List<String> subjectIds);
}
