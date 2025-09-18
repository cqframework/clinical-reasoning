package org.opencds.cqf.fhir.utility.repository.ig;

import static ca.uhn.fhir.rest.param.ParamPrefixEnum.GREATERTHAN;
import static ca.uhn.fhir.rest.param.ParamPrefixEnum.LESSTHAN;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.search.Searches.ALL;
import static org.opencds.cqf.fhir.utility.search.Searches.SearchBuilder;
import static org.opencds.cqf.fhir.utility.search.Searches.byCodeAndSystem;
import static org.opencds.cqf.fhir.utility.search.Searches.byId;
import static org.opencds.cqf.fhir.utility.search.Searches.byUrl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.DateAndListParam;
import ca.uhn.fhir.rest.param.DateOrListParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.collect.Multimap;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Ids;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IgRepositoryPrefixTest {

    private static IRepository repository;

    private static final FhirContext fhirContext = FhirContext.forR4Cached();

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        // This copies the sample IG to a temporary directory so that
        // we can test against an actual filesystem
        Resources.copyFromJar("/sampleIgs/directoryPerType/prefixed", tempDir);
        repository = new IgRepository(FhirContext.forR4Cached(), tempDir);
    }

    @Test
    void readLibrary() {
        var id = Ids.newId(Library.class, "123");
        var lib = repository.read(Library.class, id);
        assertNotNull(lib);
        assertEquals(id.getIdPart(), lib.getIdElement().getIdPart());
    }

    @Test
    void readLibraryNotExists() {
        var id = Ids.newId(Library.class, "DoesNotExist");
        assertThrows(ResourceNotFoundException.class, () -> repository.read(Library.class, id));
    }

    @Test
    void searchLibrary() {
        var libs = repository.search(Bundle.class, Library.class, ALL);

        assertNotNull(libs);
        assertEquals(2, libs.getEntry().size());
    }

    @Test
    void searchLibraryWithFilter() {
        var libs = repository.search(Bundle.class, Library.class, byUrl("http://example.com/Library/Test"));

        assertNotNull(libs);
        assertEquals(1, libs.getEntry().size());
    }

    @Test
    void searchLibraryNotExists() {
        var libs = repository.search(Bundle.class, Library.class, byUrl("not-exists"));
        assertNotNull(libs);
        assertEquals(0, libs.getEntry().size());
    }

    @Test
    void readPatient() {
        var id = Ids.newId(Patient.class, "ABC");
        var cond = repository.read(Patient.class, id);

        assertNotNull(cond);
        assertEquals(id.getIdPart(), cond.getIdElement().getIdPart());
    }

    @Test
    void searchCondition() {
        var cons = repository.search(Bundle.class, Condition.class, byCodeAndSystem("12345", "example.com/codesystem"));
        assertNotNull(cons);
        assertEquals(2, cons.getEntry().size());
    }

    @Test
    void readValueSet() {
        var id = Ids.newId(ValueSet.class, "456");
        var vs = repository.read(ValueSet.class, id);

        assertNotNull(vs);
        assertEquals(vs.getIdPart(), vs.getIdElement().getIdPart());
    }

    @Test
    void searchValueSet() {
        var sets = repository.search(Bundle.class, ValueSet.class, byUrl("example.com/ValueSet/456"));
        assertNotNull(sets);
        assertEquals(1, sets.getEntry().size());
    }

    @Test
    void createAndDeleteLibrary() {
        var lib = new Library();
        lib.setId("new-library");
        var o = repository.create(lib);
        var created = repository.read(Library.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("input/resources/library/Library-new-library.json");
        assertTrue(Files.exists(loc));

        repository.delete(Library.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void createAndDeletePatient() {
        var p = new Patient();
        p.setId("new-patient");
        var o = repository.create(p);
        var created = repository.read(Patient.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("input/tests/patient/Patient-new-patient.json");
        assertTrue(Files.exists(loc));

        repository.delete(Patient.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void createAndDeleteValueSet() {
        var v = new ValueSet();
        v.setId("new-valueset");
        var o = repository.create(v);
        var created = repository.read(ValueSet.class, o.getId());
        assertNotNull(created);

        var loc = tempDir.resolve("input/vocabulary/valueset/ValueSet-new-valueset.json");
        assertTrue(Files.exists(loc));

        repository.delete(ValueSet.class, created.getIdElement());
        assertFalse(Files.exists(loc));
    }

    @Test
    void updatePatient() {
        var id = Ids.newId(Patient.class, "ABC");
        var p = repository.read(Patient.class, id);
        assertFalse(p.hasActive());

        p.setActive(true);
        repository.update(p);

        var updated = repository.read(Patient.class, id);
        assertTrue(updated.hasActive());
        assertTrue(updated.getActive());
    }

    @Test
    void deleteNonExistentPatient() {
        var id = Ids.newId(Patient.class, "DoesNotExist");
        assertThrows(ResourceNotFoundException.class, () -> repository.delete(Patient.class, id));
    }

    @Test
    void searchNonExistentType() {
        var results = repository.search(Bundle.class, Encounter.class, ALL);
        assertNotNull(results);
        assertEquals(0, results.getEntry().size());
    }

    @Test
    void searchById() {
        var bundle = repository.search(Bundle.class, Library.class, byId("123"));
        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());
    }

    @Test
    void searchByIdNotFound() {
        var bundle = repository.search(Bundle.class, Library.class, byId("DoesNotExist"));
        assertNotNull(bundle);
        assertEquals(0, bundle.getEntry().size());
    }

    @Test
    @Order(1) // Do this test first because it puts the filesystem (temporarily) in an invalid state
    void resourceMissingWhenCacheCleared() throws IOException {
        var id = new IdType("Library", "ToDelete");
        var lib = new Library().setIdElement(id);
        var path = tempDir.resolve("input/resources/library/Library-ToDelete.json");

        repository.create(lib);
        assertTrue(path.toFile().exists());

        // Read back, should exist
        lib = repository.read(Library.class, id);
        assertNotNull(lib);

        // Overwrite the file on disk.
        Files.writeString(path, "");

        // Read from cache, repo doesn't know the content is gone.
        lib = repository.read(Library.class, id);
        assertNotNull(lib);
        assertEquals("ToDelete", lib.getIdElement().getIdPart());

        ((IgRepository) repository).clearCache();

        // Try to read again, should be gone because it's not in the cache and the content is gone.
        assertThrows(ResourceNotFoundException.class, () -> repository.read(Library.class, id));

        // Clean up so that we don't affect other tests
        path.toFile().delete();
    }

    @Test
    void searchBySearchParameterIntersection() {
        IIdType org2Id = new IdType("Organization", "2");

        createOrganization(withId("1"), withEffectiveDate("2021-01-01"));
        createOrganization(withId(org2Id), withEffectiveDate("2023-01-01"));
        createOrganization(withId("3"), withEffectiveDate("2025-01-01"));

        SearchBuilder searchBuilder = new SearchBuilder();

        DateAndListParam dateAndListParam = new DateAndListParam();
        dateAndListParam.addAnd(dateParamFrom(GREATERTHAN, "2022-01-01"));
        dateAndListParam.addAnd(dateParamFrom(LESSTHAN, "2024-01-01"));

        searchBuilder.withAndListParam("date", dateAndListParam);
        Multimap<String, List<IQueryParameterType>> multimap = searchBuilder.build();

        // we are searching for Observation where effectiveDate is within a range
        var bundle = repository.search(Bundle.class, Observation.class, multimap);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());

        IIdType bundledResourceId =
                BundleHelper.getEntryResourceFirstRep(bundle).getIdElement().toUnqualifiedVersionless();

        assertEquals(org2Id.getIdPart(), bundledResourceId.getIdPart());
    }

    @Test
    void searchBySearchParameterUnion() {
        IIdType org1Id = new IdType("Organization", "1");
        IIdType org3Id = new IdType("Organization", "3");

        createOrganization(withId(org1Id), withEffectiveDate("2021-01-01"));
        createOrganization(withId("2"), withEffectiveDate("2023-01-01"));
        createOrganization(withId(org3Id), withEffectiveDate("2025-01-01"));

        SearchBuilder searchBuilder = new SearchBuilder();

        DateOrListParam dateOrListParam = new DateOrListParam();
        dateOrListParam.addOr((dateParamFrom(LESSTHAN, "2022-01-01")));
        dateOrListParam.addOr((dateParamFrom(GREATERTHAN, "2024-01-01")));

        searchBuilder.withOrListParam("date", dateOrListParam);
        Multimap<String, List<IQueryParameterType>> multimap = searchBuilder.build();

        // we are searching for Observation where effectiveDate is outside a range
        var bundle = repository.search(Bundle.class, Observation.class, multimap);

        assertNotNull(bundle);
        assertEquals(2, bundle.getEntry().size());

        List<IIdType> bundledResourceIds = BundleHelper.getBundleEntryResourceIds(getFhirContext(), bundle);

        var actualSorted = sortBundleEntryResourceIds(bundledResourceIds);

        assertIterableEquals(List.of(org1Id, org3Id), actualSorted);
    }

    @Test
    void searchByReference() {
        String patientReference = "Patient/123";
        IIdType org1Id = new IdType("Organization", "1");

        createOrganization(withId(org1Id), withSubjectReference(patientReference));
        createOrganization(withId("2"), withEffectiveDate("2023-01-01"));

        SearchBuilder searchBuilder = new SearchBuilder();
        Multimap<String, List<IQueryParameterType>> multimap =
                searchBuilder.withReferenceParam("subject", patientReference).build();

        // we are searching for Observation where effectiveDate is outside a range
        var bundle = repository.search(Bundle.class, Observation.class, multimap);

        assertNotNull(bundle);
        assertEquals(1, bundle.getEntry().size());

        List<IIdType> bundledResourceIds = BundleHelper.getBundleEntryResourceIds(getFhirContext(), bundle);

        assertIterableEquals(List.of(org1Id), bundledResourceIds);
    }

    IIdType createOrganization(ICreationArgument... modifiers) {
        return createResource("Observation", modifiers);
    }

    ICreationArgument withSubjectReference(String subjectReference) {
        return t -> ((Observation) t).setSubject(new Reference(subjectReference));
    }

    ICreationArgument withEffectiveDate(String dateTime) {
        Date date = toDate(dateTime);
        return t -> ((Observation) t).setEffective(new DateTimeType(date));
    }

    ICreationArgument withId(@Nonnull String id) {
        return t -> {
            assertTrue(id.matches("[a-zA-Z0-9-]+"));
            ((IBaseResource) t).setId(id);
        };
    }

    private ICreationArgument withId(IIdType iid) {
        return t -> ((IBaseResource) t).setId(iid);
    }

    interface ICreationArgument extends Consumer<IBase> {}

    IIdType createResource(String resourceType, ICreationArgument... modifiers) {
        IBaseResource resource = buildResource(resourceType, modifiers);

        if (isNotBlank(resource.getIdElement().getValue())) {
            return repository.update(resource).getId().toUnqualifiedVersionless();
        } else {
            return repository.create(resource).getId().toUnqualifiedVersionless();
        }
    }

    <T extends IBaseResource> T buildResource(String resourceType, ICreationArgument... modifiers) {
        IBaseResource resource =
                getFhirContext().getResourceDefinition(resourceType).newInstance();
        applyElementModifiers(resource, modifiers);
        return (T) resource;
    }

    <E extends IBase> void applyElementModifiers(E element, Consumer<E>[] modifiers) {
        for (Consumer<E> nextModifier : modifiers) {
            nextModifier.accept(element);
        }
    }

    DateParam dateParamFrom(ParamPrefixEnum paramPrefixEnum, String dateTime) {
        return new DateParam(paramPrefixEnum, toDate(dateTime));
    }

    Date toDate(String dateTime) {
        ZoneId zone = ZoneId.of("UTC");
        return Date.from(LocalDate.parse(dateTime).atStartOfDay(zone).toInstant());
    }

    FhirContext getFhirContext() {
        return fhirContext;
    }

    List<IIdType> sortBundleEntryResourceIds(List<IIdType> ids) {
        var actualSorted = new ArrayList<>(ids);
        actualSorted.sort((o1, o2) -> o1.getIdPart().compareTo(o2.getIdPart()));
        return actualSorted;
    }
}
