package org.opencds.cqf.fhir.cr.measure.dstu3;

import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Group.GroupMemberComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureResolutionException;
import org.opencds.cqf.fhir.cr.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.cr.measure.common.SubjectRef;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.search.Searches;

public class Dstu3RepositorySubjectProvider implements SubjectProvider {

    @Override
    public Stream<SubjectRef> getSubjects(IRepository repository, String subjectId) {
        return getSubjects(repository, Collections.singletonList(subjectId));
    }

    @Override
    public Stream<SubjectRef> getSubjects(IRepository repository, List<String> subjectIds) {
        if (subjectIds == null
                || subjectIds.isEmpty()
                || subjectIds.get(0) == null
                || subjectIds.get(0).isEmpty()) {
            var bundle = repository.search(Bundle.class, Patient.class, Searches.ALL);
            return new BundleMappingIterable<>(repository, bundle, x -> x.getResource()
                            .getIdElement()
                            .toUnqualifiedVersionless()
                            .getValue())
                    .toStream()
                    .map(SubjectRef::fromQualified);
        }

        List<SubjectRef> subjects = new ArrayList<>();
        subjectIds.forEach(subjectId -> {
            if (subjectId.indexOf("/") == -1) {
                subjectId = "Patient/".concat(subjectId);
            }
            if (subjectId.startsWith("Patient")) {
                IdType id = new IdType(subjectId);
                Patient r = repository.read(Patient.class, id);

                if (r == null) {
                    throw new MeasureResolutionException("Resource " + id.getValue() + " is not known");
                }

                subjects.add(SubjectRef.fromQualified(
                        r.getIdElement().toUnqualifiedVersionless().getValue()));
            } else if (subjectId.startsWith("Group")) {
                IdType id = new IdType(subjectId);
                Group r = repository.read(Group.class, id);

                if (r == null) {
                    throw new MeasureResolutionException("Resource " + id.getValue() + " is not known");
                }

                for (GroupMemberComponent gmc : r.getMember()) {
                    IIdType ref = gmc.getEntity().getReferenceElement();
                    subjects.add(new SubjectRef(ref.getResourceType(), ref.getIdPart()));
                }

            } else {
                throw new IllegalArgumentException("Unsupported subjectId: %s".formatted(subjectIds));
            }
        });

        return subjects.stream();
    }
}
