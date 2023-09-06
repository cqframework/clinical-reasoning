package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.utility.iterable.BundleIterable;
import org.opencds.cqf.fhir.utility.search.Searches;

public class R4RepositorySubjectProvider implements SubjectProvider {

    @Override
    public Stream<String> getSubjects(Repository repository, MeasureEvalType measureEvalType, String subjectId) {
        return getSubjects(repository, measureEvalType, Collections.singletonList(subjectId));
    }

    @Override
    public Stream<String> getSubjects(Repository repository, MeasureEvalType measureEvalType, List<String> subjectIds) {
        if (subjectIds == null
                || subjectIds.isEmpty()
                || subjectIds.get(0) == null
                || subjectIds.get(0).isEmpty()) {
            var bundle = repository.search(Bundle.class, Patient.class, Searches.ALL);
            var iterator = new BundleIterable<>(repository, bundle);
            return iterator.toStream().map(x -> x.getResource()
                    .getIdElement()
                    .toUnqualifiedVersionless()
                    .getValue());
        }

        List<String> subjects = new ArrayList<>();
        subjectIds.forEach(subjectId -> {
            if (subjectId.indexOf("/") == -1) {
                subjectId = "Patient/".concat(subjectId);
            }
            if (subjectId.startsWith("Patient")) {
                IdType id = new IdType(subjectId);
                Patient r = repository.read(Patient.class, id);

                if (r == null) {
                    throw new ResourceNotFoundException(id);
                }

                subjects.add(r.getIdElement().toUnqualifiedVersionless().getValue());
            } else if (subjectId.startsWith("Group")) {
                IdType id = new IdType(subjectId);
                Group r = repository.read(Group.class, id);

                if (r == null) {
                    throw new ResourceNotFoundException(id);
                }

                for (GroupMemberComponent gmc : r.getMember()) {
                    IIdType ref = gmc.getEntity().getReferenceElement();
                    subjects.add(ref.getResourceType() + "/" + ref.getIdPart());
                }

            } else {
                throw new IllegalArgumentException(String.format("Unsupported subjectId: %s", subjectIds));
            }
        });

        return subjects.stream();
    }
}
