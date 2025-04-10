package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.repository.Repository;
import java.util.List;
import java.util.stream.Stream;

public interface SubjectProvider {
    Stream<String> getSubjects(Repository repository, String subjectId);

    Stream<String> getSubjects(Repository repository, List<String> subjectIds);
}
