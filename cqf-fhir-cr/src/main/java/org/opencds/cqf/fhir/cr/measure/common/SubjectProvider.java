package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.stream.Stream;
import org.opencds.cqf.fhir.api.Repository;

public interface SubjectProvider {
    Stream<String> getSubjects(Repository repository, MeasureEvalType measureEvalType, String subjectId);

    Stream<String> getSubjects(Repository repository, MeasureEvalType measureEvalType, List<String> subjectIds);

    // LUKETODO:  consider these contracts carefully
    Stream<String> getSubjectsWithPartOf(Repository repository, MeasureEvalType measureEvalType, String subjectId);

    Stream<String> getSubjectsWithPartOf(Repository repository, MeasureEvalType measureEvalType, List<String> subjectIds);
}
