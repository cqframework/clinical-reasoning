package org.opencds.cqf.cql.evaluator.measure.dstu3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Group.GroupMemberComponent;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.SubjectProvider;
import org.opencds.cqf.fhir.api.Repository;

public class Dstu3RepositorySubjectProvider implements SubjectProvider {

  private Repository repo;

  public Dstu3RepositorySubjectProvider(Repository repo) {
    this.repo = repo;
  }

  @Override
  public List<String> getSubjects(MeasureEvalType measureEvalType, String subjectId) {
    if (subjectId == null) {
      // TODO: Grab the Bundle iterator
      // Iterable<IBaseResource> resources = repo.search("Patient");
      // List<String> ids = new ArrayList<>();
      // for (IBaseResource r : resources) {
      // ids.add(r.getIdElement().getResourceType() + "/" + r.getIdElement().getIdPart());
      // }

      // return ids;
      return null;
    } else if (subjectId.indexOf("/") == -1) {
      var r = repo.read(Patient.class, new IdType("Patient/" + subjectId));
      return Collections
          .singletonList(r.getIdElement().getResourceType() + "/" + r.getIdElement().getIdPart());
    } else if (subjectId.startsWith("Group")) {
      var r = repo.read(Group.class, new IdType(subjectId));
      List<String> subjectIds = new ArrayList<>();

      for (GroupMemberComponent gmc : r.getMember()) {
        IIdType ref = gmc.getEntity().getReferenceElement();
        subjectIds.add(ref.getResourceType() + "/" + ref.getIdPart());
      }

      return subjectIds;
    } else {
      throw new IllegalArgumentException(String.format("Unsupported subjectId: %s", subjectId));
    }
  }
}
