package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.util.FhirTerser;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.measure.SubjectProviderOptions;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.iterable.BundleIterator;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * Version-agnostic subject provider that resolves patient subjects from an IRepository.
 * Supports Patient, Practitioner, Group (person and practitioner types), and Organization subjects.
 */
public class RepositorySubjectProvider implements SubjectProvider {
    private final SubjectProviderOptions subjectProviderOptions;

    public RepositorySubjectProvider(SubjectProviderOptions subjectProviderOptions) {
        this.subjectProviderOptions = subjectProviderOptions;
    }

    @Override
    public Stream<SubjectRef> getSubjects(IRepository repository, @Nullable String subjectId) {
        return getSubjects(repository, Collections.singletonList(subjectId));
    }

    @Override
    public Stream<SubjectRef> getSubjects(IRepository repository, List<String> subjectIds) {
        // All patients in system
        if (subjectIds == null
                || subjectIds.isEmpty()
                || subjectIds.get(0) == null
                || subjectIds.get(0).isEmpty()) {
            var bundle = repository.search(bundleClass(repository), patientClass(repository), Searches.ALL);
            return new BundleMappingIterable<>(repository, bundle, x -> x.getResource()
                            .getIdElement()
                            .toUnqualifiedVersionless()
                            .getValue())
                    .toStream()
                    .map(SubjectRef::fromQualified);
        }

        List<SubjectRef> subjects = new ArrayList<>();
        subjectIds.forEach(subjectId -> {
            // add resource reference if missing
            if (!subjectId.contains("/")) {
                subjectId = "Patient/".concat(subjectId);
            }
            // Single Patient
            if (subjectId.startsWith("Patient")) {
                IIdType id = Ids.newId(repository.fhirContext(), subjectId);
                IBaseResource r = repository.read(patientClass(repository), id);

                if (r == null) {
                    throw new MeasureResolutionException("Resource not found: " + id.getValue());
                }

                subjects.add(SubjectRef.fromQualified(
                        r.getIdElement().toUnqualifiedVersionless().getValue()));
                // Single Practitioner
            } else if (subjectId.startsWith("Practitioner")) {

                addPractitionerSubjectIds(subjectId, repository, subjects);

                // Group Subject
            } else if (subjectId.startsWith("Group")) {
                IIdType id = Ids.newId(repository.fhirContext(), subjectId);
                IBaseResource group = repository.read(groupClass(repository), id);

                if (group == null) {
                    throw new MeasureResolutionException("Resource not found: " + id.getValue());
                }

                var terser = new FhirTerser(repository.fhirContext());
                String groupType = getGroupType(terser, group);

                // Group of Patients
                if ("person".equals(groupType)) {
                    List<IBaseReference> members = terser.getValues(group, "Group.member.entity", IBaseReference.class);
                    for (var memberRef : members) {
                        IIdType ref = memberRef.getReferenceElement();
                        subjects.add(new SubjectRef(ref.getResourceType(), ref.getIdPart()));
                    }
                }
                // Group of Practitioners
                else if ("practitioner".equals(groupType)) {
                    var practitionerGroupMembers = getMembersInGroup(terser, group);

                    // Loop through each practitioner in group
                    for (String practitioner : practitionerGroupMembers) {
                        // add patients associated with practitioner to subjects list
                        addPractitionerSubjectIds(practitioner, repository, subjects);
                    }
                }
            } else if (subjectId.startsWith("Organization")) {
                subjects.addAll(getOrganizationSubjectIds(subjectId, repository));
            } else {
                throw new IllegalArgumentException("Unsupported subjectId: %s".formatted(subjectIds));
            }
        });

        return subjects.stream();
    }

    @Nullable
    private static String getGroupType(FhirTerser terser, IBaseResource group) {
        var typePrimitive = terser.getSingleValueOrNull(group, "Group.type", IPrimitiveType.class);
        return typePrimitive != null ? typePrimitive.getValueAsString() : null;
    }

    private static List<String> getMembersInGroup(FhirTerser terser, IBaseResource group) {
        List<String> members = new ArrayList<>();
        List<IBaseReference> memberEntities = terser.getValues(group, "Group.member.entity", IBaseReference.class);

        for (var memberRef : memberEntities) {
            IIdType ref = memberRef.getReferenceElement();
            members.add(ref.getResourceType() + "/" + ref.getIdPart());
        }
        return members;
    }

    public void addPractitionerSubjectIds(String practitioner, IRepository repository, List<SubjectRef> patients) {
        Map<String, List<IQueryParameterType>> map = new HashMap<>();

        map.put(
                "general-practitioner",
                Collections.singletonList(new ReferenceParam(
                        practitioner.startsWith("Practitioner/") ? practitioner : "Practitioner/" + practitioner)));

        var bundle = repository.search(bundleClass(repository), patientClass(repository), map);
        var iterator = new BundleIterator<>(repository, bundle);

        while (iterator.hasNext()) {
            var patient = iterator.next().getResource();
            var idElement = patient.getIdElement();
            patients.add(new SubjectRef(idElement.getResourceType(), idElement.getIdPart()));
        }
    }

    private List<SubjectRef> getOrganizationSubjectIds(String organization, IRepository repository) {

        return Stream.concat(
                        getManagingOrganizationSubjectIds(organization, repository),
                        getPartOfSubjectIds(organization, repository))
                .collect(Collectors.toList());
    }

    private Stream<SubjectRef> getManagingOrganizationSubjectIds(String organization, IRepository repository) {
        final Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();

        searchParams.put("organization", Collections.singletonList(new ReferenceParam(organization)));

        return handlePatientBundle(repository, searchParams);
    }

    private Stream<SubjectRef> getPartOfSubjectIds(String organization, IRepository repository) {

        if (!subjectProviderOptions.isPartOfEnabled()) {
            return Stream.empty();
        }

        final Map<String, List<IQueryParameterType>> searchParam = new HashMap<>();

        searchParam.put(
                "organization",
                Collections.singletonList(new ReferenceParam("organization", organization).setChain("partof")));

        return handlePatientBundle(repository, searchParam);
    }

    private static Stream<SubjectRef> handlePatientBundle(
            IRepository repository, Map<String, List<IQueryParameterType>> searchParam) {
        var bundle = repository.search(bundleClass(repository), patientClass(repository), searchParam);

        var iterator = new BundleIterator<>(repository, bundle);
        if (!iterator.hasNext()) {
            return Stream.empty();
        }

        var patientIds = new ArrayList<SubjectRef>();
        iterator.forEachRemaining(item -> {
            var resource = item.getResource();
            var idElement = resource.getIdElement();
            patientIds.add(new SubjectRef("Patient", idElement.getIdPart()));
        });

        return patientIds.stream();
    }

    @SuppressWarnings("unchecked")
    private static <T extends IBaseResource> Class<T> patientClass(IRepository repository) {
        return (Class<T>)
                repository.fhirContext().getResourceDefinition("Patient").getImplementingClass();
    }

    @SuppressWarnings("unchecked")
    private static <T extends IBaseResource> Class<T> groupClass(IRepository repository) {
        return (Class<T>)
                repository.fhirContext().getResourceDefinition("Group").getImplementingClass();
    }

    @SuppressWarnings("unchecked")
    private static <T extends IBaseBundle> Class<T> bundleClass(IRepository repository) {
        return (Class<T>)
                repository.fhirContext().getResourceDefinition("Bundle").getImplementingClass();
    }
}
