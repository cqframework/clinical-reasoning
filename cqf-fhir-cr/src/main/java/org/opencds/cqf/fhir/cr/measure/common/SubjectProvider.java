package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.stream.Stream;
import org.opencds.cqf.fhir.api.Repository;

public interface SubjectProvider {
    Stream<String> getSubjects(Repository repository, String subjectId);

    Stream<String> getSubjects(Repository repository, List<String> subjectIds);
}
