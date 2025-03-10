package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;

class R4SubmitDataServiceTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    private static final String OBSERVATION_VALUE = "ABC";
    private static final String MEASURE_FULL_ID = "Measure/A123";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final String PATIENT_ID = "Practitioner-2178";
    private static final String ENCOUNTER_ID = "Encounter-62912";
    private static final String PROCEDURE_ID = "Procedure-89972";

    private final Repository repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
    private final R4SubmitDataService testSubject = new R4SubmitDataService(repository);

    @Test
    void submitDataSimple() {

        // create resources
        var measureReport = newResource(MeasureReport.class).setMeasure(MEASURE_FULL_ID);
        var observation = newResource(Observation.class).setValue(new StringType(OBSERVATION_VALUE));

        // submit-data operation
        var result = testSubject.submitData(measureReport, List.of(observation));

        assertNotNull(result);

        var resultMeasureReport = getOnlyResourceFromSearch(MeasureReport.class);
        assertThat(resultMeasureReport.getMeasure(), equalTo(MEASURE_FULL_ID));

        var resultObservation = getOnlyResourceFromSearch(Observation.class);
        assertThat(resultObservation.getValue().primitiveValue(), equalTo(OBSERVATION_VALUE));
    }

    @Test
    void submitDataMedium() throws ParseException {

        // create resources
        var patient = newResource(Patient.class).setId(PATIENT_ID);

        var encounter = newResource(Encounter.class)
                .setPeriod(new Period()
                        .setStart(DATE_FORMAT.parse("2018-05-29T11:00:00-04:00"))
                        .setEnd(DATE_FORMAT.parse("2018-05-29T11:00:00-04:00")))
                .setSubject(new Reference(patient.getId()))
                .setId(ENCOUNTER_ID);

        var procedure = newResource(Procedure.class)
                .setSubject(new Reference(patient.getId()))
                .setPerformed(new Period()
                        .setStart(DATE_FORMAT.parse("2018-06-02T14:00:00-05:00"))
                        .setEnd(DATE_FORMAT.parse("2018-06-02T14:00:00-05:00")))
                .setId(PROCEDURE_ID);

        var measureReport = newResource(MeasureReport.class)
                .setMeasure(MEASURE_FULL_ID)
                .setPeriod(new Period()
                        .setStart(DATE_FORMAT.parse("2017-01-01T00:00:00+00:00"))
                        .setEnd(DATE_FORMAT.parse("2017-12-31T00:00:00+00:00")))
                .addEvaluatedResource(new Reference(patient.getId()))
                .addEvaluatedResource(new Reference(encounter.getId()))
                .addEvaluatedResource(new Reference(procedure.getId()));

        // submit-data operation
        var result = testSubject.submitData(measureReport, List.of(patient, encounter, procedure));

        assertNotNull(result);

        var resultMeasureReport = getOnlyResourceFromSearch(MeasureReport.class);
        assertThat(resultMeasureReport.getMeasure(), equalTo(MEASURE_FULL_ID));
        assertThat(
                resultMeasureReport.getEvaluatedResource().stream()
                        .map(Reference::getReference)
                        .toList(),
                containsInAnyOrder(PATIENT_ID, ENCOUNTER_ID, PROCEDURE_ID));

        var resultPatient = getOnlyResourceFromSearch(Patient.class);
        assertThat(resultPatient.getId(), equalTo(PATIENT_ID));

        var resultEncounter = getOnlyResourceFromSearch(Encounter.class);
        assertThat(resultEncounter.getSubject().getReference(), equalTo(PATIENT_ID));

        var resultProcedure = getOnlyResourceFromSearch(Procedure.class);
        assertThat(resultProcedure.getSubject().getReference(), equalTo(PATIENT_ID));
    }

    private <T extends IBaseResource> T getOnlyResourceFromSearch(Class<T> clazz) {
        final List<T> resourcesFromSearch = getResourcesFromSearch(clazz);

        assertThat(resourcesFromSearch.size(), equalTo(1));

        final T resource = resourcesFromSearch.get(0);

        assertThat(resource, notNullValue());

        return resource;
    }

    private <T extends IBaseResource> List<T> getResourcesFromSearch(Class<T> clazz) {
        var bundle = repository.search(Bundle.class, clazz, Searches.ALL);

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private <T extends IBaseResource> T newResource(Class<T> theResourceClass) {
        checkNotNull(theResourceClass);

        return (T) FHIR_CONTEXT.getResourceDefinition(theResourceClass).newInstance();
    }
}
