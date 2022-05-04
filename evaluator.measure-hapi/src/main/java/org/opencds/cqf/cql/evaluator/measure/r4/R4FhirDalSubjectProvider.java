package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.hl7.fhir.r4.model.IdType;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.SubjectProvider;

public class R4FhirDalSubjectProvider implements SubjectProvider {

    private FhirDal fhirDal;

    public R4FhirDalSubjectProvider(FhirDal fhirDal) {
        this.fhirDal = fhirDal;
    }

    @Override
    public List<String> getSubjects(MeasureEvalType measureEvalType, String subjectId) {
        if (subjectId == null) {
            Iterable<IBaseResource> resources = fhirDal.search("Patient");
            List<String> ids = new ArrayList<>();
            for (IBaseResource r : resources) {
                ids.add(r.getIdElement().getResourceType() + "/" + r.getIdElement().getIdPart());
            }

            return ids;
        } else if (subjectId.indexOf("/") == -1) {
            IBaseResource r = fhirDal.read(new IdType("Patient/" + subjectId));
            return Collections
                    .singletonList(r.getIdElement().getResourceType() + "/" + r.getIdElement().getIdPart());
        }
        else if (subjectId.startsWith("Group")) {
            Group r = (Group)fhirDal.read(new IdType(subjectId));
            List<String> subjectIds = new ArrayList<>();

            for (GroupMemberComponent gmc : r.getMember()) {
                IIdType ref = gmc.getEntity().getReferenceElement();
                subjectIds.add(ref.getResourceType() + "/" + ref.getIdPart());
            }

            return subjectIds;
        } else {
            IBaseResource r = fhirDal.read(new IdType(subjectId));
            return Collections
                    .singletonList(r.getIdElement().getResourceType() + "/" + r.getIdElement().getIdPart());
        }
    }
}
