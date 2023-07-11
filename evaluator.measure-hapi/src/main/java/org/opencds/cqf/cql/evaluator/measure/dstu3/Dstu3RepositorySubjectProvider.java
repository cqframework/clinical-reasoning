package org.opencds.cqf.cql.evaluator.measure.dstu3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Group.GroupMemberComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Searches;
import org.opencds.cqf.fhir.utility.iterable.BundleIterator;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.bundle.BundleEntryParts;

public class Dstu3RepositorySubjectProvider implements SubjectProvider {

  private Repository repo;

  public Dstu3RepositorySubjectProvider(Repository repo) {
    this.repo = repo;
  }

  @Override
  public Stream<String> getSubjects(MeasureEvalType measureEvalType, String subjectId) {
    return getSubjects(measureEvalType, Collections.singletonList(subjectId));
  }

  @Override
  public Stream<String> getSubjects(MeasureEvalType measureEvalType, List<String> subjectIds) {
    if (subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null
        || subjectIds.get(0).isEmpty()) {
      var bundle = this.repo.search(Bundle.class, Patient.class, Searches.ALL);
      var iterator = new BundleIterator<>(repo, Bundle.class, bundle);
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
          iterator,
          Spliterator.ORDERED), false)
          .map(BundleEntryParts::getResource)
          .map(x -> x.getIdElement().toUnqualifiedVersionless().getValue());
    }

    List<String> subjects = new ArrayList<>();
    subjectIds.forEach(subjectId -> {
      if (subjectId.indexOf("/") == -1) {
        subjectId = "Patient/".concat(subjectId);
      }
      if (subjectId.startsWith("Patient")) {
        IdType id = new IdType(subjectId);
        Patient r = repo.read(Patient.class, id);

        if (r == null) {
          throw new ResourceNotFoundException(id);
        }

        subjects.add(r.getIdElement().toUnqualifiedVersionless().getValue());
      } else if (subjectId.startsWith("Group")) {
        IdType id = new IdType(subjectId);
        Group r = repo.read(Group.class, id);

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
