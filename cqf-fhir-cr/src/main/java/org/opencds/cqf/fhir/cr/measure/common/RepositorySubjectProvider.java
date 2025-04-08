package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.SubjectProviderOptions;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IBundleAdapter;
import org.opencds.cqf.fhir.utility.iterable.BundleIterator;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositorySubjectProvider implements SubjectProvider {

    // LUKETODO:  implement
    @Override
    public Stream<String> getSubjects(Repository repository, String subjectId) {
        return Stream.empty();
    }

    @Override
    public Stream<String> getSubjects(Repository repository, List<String> subjectIds) {
        return Stream.empty();
    }

//    private static final Logger logger = LoggerFactory.getLogger(RepositorySubjectProvider.class);
//
//    private final SubjectProviderOptions subjectProviderOptions;
//
//    public RepositorySubjectProvider(SubjectProviderOptions subjectProviderOptions) {
//        this.subjectProviderOptions = subjectProviderOptions;
//    }
//
//    @Override
//    public Stream<String> getSubjects(Repository repository, @Nullable String subjectId) {
//        return getSubjects(repository, Collections.singletonList(subjectId));
//    }
//
//    @Override
//    public Stream<String> getSubjects(Repository repository, List<String> subjectIds) {
//        // All patients in system
//        if (subjectIds == null
//                || subjectIds.isEmpty()
//                || subjectIds.get(0) == null
//                || subjectIds.get(0).isEmpty()) {
//            // LUKETODO:  how do we abstract this away?
//            var bundle = search(repository, Searches.ALL);
//            return new BundleMappingIterable<>(repository, bundle.get(), entry -> entry.getResource()
//                            .getIdElement()
//                            .toUnqualifiedVersionless()
//                            .getValue())
//                    .toStream();
//        }
//
//        List<String> subjects = new ArrayList<>();
//        subjectIds.forEach(subjectId -> {
//            // add resource reference if missing
//            if (!subjectId.contains("/")) {
//                subjectId = "Patient/".concat(subjectId);
//            }
//            // Single Patient
//            if (subjectId.startsWith("Patient")) {
//                // LUKETODO:  adapter based on fhir version
//                IdType id = new IdType(subjectId);
//                // LUKETODO:  how do we abstract this away?
//                Patient r = repository.read(Patient.class, id);
//
//                if (r == null) {
//                    throw new ResourceNotFoundException(id);
//                }
//
//                subjects.add(r.getIdElement().toUnqualifiedVersionless().getValue());
//                // Single Practitioner
//            } else if (subjectId.startsWith("Practitioner")) {
//
//                addPractitionerSubjectIds(subjectId, repository, subjects);
//
//                // Group Subject
//            } else if (subjectId.startsWith("Group")) {
//                IdType id = new IdType(subjectId);
//                // LUKETODO:  how do we abstract this away?
//                Group r = repository.read(Group.class, id);
//
//                if (r == null) {
//                    throw new ResourceNotFoundException(id);
//                }
//                // Group of Patients
//                if (r.getType().equals(GroupType.PERSON)) {
//                    // LUKETODO:  how do we abstract this away?
//                    for (GroupMemberComponent gmc : r.getMember()) {
//                        IIdType ref = gmc.getEntity().getReferenceElement();
//
//                        subjects.add(ref.getResourceType() + "/" + ref.getIdPart());
//                    }
//                }
//                // Group of Practitioners
//                else if (r.getType().equals(GroupType.PRACTITIONER)) {
//                    var practitionerGroupMembers = getMembersInGroup(r);
//
//                    // Loop through each practitioner in group
//                    for (String practitioner : practitionerGroupMembers) {
//                        // add patients associated with practitioner to subjects list
//                        addPractitionerSubjectIds(practitioner, repository, subjects);
//                    }
//                }
//            } else if (subjectId.startsWith("Organization")) {
//                subjects.addAll(getOrganizationSubjectIds(subjectId, repository));
//            } else {
//                throw new IllegalArgumentException(String.format("Unsupported subjectId: %s", subjectIds));
//            }
//        });
//
//        return subjects.stream();
//    }
//
//    private List<String> getMembersInGroup(Group group) {
//        List<String> members = new ArrayList<>();
//
//        for (GroupMemberComponent gmc : group.getMember()) {
//            IIdType ref = gmc.getEntity().getReferenceElement();
//            members.add(ref.getResourceType() + "/" + ref.getIdPart());
//        }
//        return members;
//    }
//
//    public void addPractitionerSubjectIds(String practitioner, Repository repository, List<String> patients) {
//        Map<String, List<IQueryParameterType>> map = new HashMap<>();
//
//        map.put(
//                "general-practitioner",
//                Collections.singletonList(new ReferenceParam(
//                        practitioner.startsWith("Practitioner/") ? practitioner : "Practitioner/" + practitioner)));
//
//        var bundle = search(repository, map);
//        var iterator = new BundleIterator<>(repository, bundle);
//
//        while (iterator.hasNext()) {
//            var patient = iterator.next().getResource();
//            var refString = patient.getIdElement().getResourceType() + "/"
//                    + patient.getIdElement().getIdPart();
//            patients.add(refString);
//        }
//    }
//
//    private List<String> getOrganizationSubjectIds(String organization, Repository repository) {
//
//        return Stream.concat(
//                        getManagingOrganizationSubjectIds(organization, repository),
//                        getPartOfSubjectIds(organization, repository))
//                .collect(Collectors.toList());
//    }
//
//    private Stream<String> getManagingOrganizationSubjectIds(String organization, Repository repository) {
//        final Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
//
//        searchParams.put("organization", Collections.singletonList(new ReferenceParam(organization)));
//
//        return handlePatientBundle(repository, searchParams);
//    }
//
//    private Stream<String> getPartOfSubjectIds(String organization, Repository repository) {
//
//        if (!subjectProviderOptions.isPartOfEnabled()) {
//            return Stream.empty();
//        }
//
//        final Map<String, List<IQueryParameterType>> searchParam = new HashMap<>();
//
//        searchParam.put(
//                "organization",
//                Collections.singletonList(new ReferenceParam("organization", organization).setChain("partof")));
//
//        return handlePatientBundle(repository, searchParam);
//    }
//
//    private static Stream<String> handlePatientBundle(
//            Repository repository, Map<String, List<IQueryParameterType>> searchParam) {
//
//        var bundle = search(repository, searchParam);
//
//        var bundleEntries = bundle.getEntry();
//
//        if (bundleEntries == null || bundleEntries.isEmpty()) {
//            return Stream.empty();
//        }
//
//        var iterator = new BundleIterator<>(repository, bundle.get());
//        var patientIds = new ArrayList<String>();
//
//        iterator.forEachRemaining(item -> {
//            var resource = item.getResource();
//            var idElement = resource.getIdElement();
//            patientIds.add(ResourceType.Patient + "/" + idElement.getIdPart());
//        });
//
//        return patientIds.stream();
//    }
//
//    private static IBundleAdapter search(
//            Repository repository, Map<String, List<IQueryParameterType>> searchParameters) {
//
//        return IAdapterFactory.forFhirVersion(repository.fhirVersion())
//                .createBundle(searchForBundle(repository, searchParameters));
//    }
//
//    private static IBaseBundle searchForBundle(
//            Repository repository, Map<String, List<IQueryParameterType>> searchParameters) {
//        return switch (repository.fhirVersion()) {
//            case DSTU3 -> repository.search(
//                    org.hl7.fhir.dstu3.model.Bundle.class, org.hl7.fhir.dstu3.model.Patient.class, searchParameters);
//            case R4 -> repository.search(
//                    org.hl7.fhir.r4.model.Bundle.class, org.hl7.fhir.r4.model.Patient.class, searchParameters);
//            case R5 -> repository.search(
//                    org.hl7.fhir.r5.model.Bundle.class, org.hl7.fhir.r5.model.Patient.class, searchParameters);
//            default -> throw new InvalidRequestException("Unexpected version: "
//                    + repository.fhirContext().getVersion().getVersion());
//        };
//    }
}
