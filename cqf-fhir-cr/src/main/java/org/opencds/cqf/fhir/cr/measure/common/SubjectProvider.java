package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import java.util.stream.Stream;

public interface SubjectProvider {
    Stream<String> getSubjects(IRepository repository, String subjectId);

    Stream<String> getSubjects(IRepository repository, List<String> subjectIds);
}
