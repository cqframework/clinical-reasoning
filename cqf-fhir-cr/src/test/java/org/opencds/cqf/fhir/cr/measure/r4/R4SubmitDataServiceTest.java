package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import com.google.common.collect.Lists;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;

// LUKETODO:  https://www.hl7.org/fhir/R4/measure-operation-submit-data.html

/*
The official URL for this operation definition is

 http://hl7.org/fhir/OperationDefinition/Measure-submit-data
Formal Definition (as a OperationDefinition).

URL: [base]/Measure/$submit-data

URL: [base]/Measure/[id]/$submit-data

This is an idempotent operation

In Parameters:

| Name          | Cardinality | Type          | Binding | Profile | Documentation                                                              |
| --------------| ----------- | ------------- | ------- | ------- | -------------------------------------------------------------------------- |
| measureReport | 1..1        | MeasureReport |         |         | The measure report being submitted                                         |
| resource      | 0..*        | Resource      |         |         | The individual resources that make up the data-of-interest being submitted |

The effect of invoking this operation is that the submitted data is posted to the receiving system and can be used for
subsequent calculation of the relevant quality measure. The data-of-interest for a measure can be determined by
examining the measure definition, or by invoking the $data-requirements operation

 */
class R4SubmitDataServiceTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    private static final String OBSERVATION_VALUE = "ABC";
    private static final String MEASURE_ID_COMPONENT = "A123";
    private static final String MEASURE_FULL_ID = "Measure/A123";

    private final Repository repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
    private final R4SubmitDataService testSubject = new R4SubmitDataService(repository);

    @Test
    public void submitDataTest() {

        // create resources
        var measureReport = newResource(MeasureReport.class).setMeasure(MEASURE_FULL_ID);
        var observation = newResource(Observation.class).setValue(new StringType(OBSERVATION_VALUE));

        // submit-data operation
        var result = testSubject.submitData(
                new IdType(ResourceType.Measure.toString(), MEASURE_ID_COMPONENT),
                measureReport,
                Lists.newArrayList(observation));

        assertNotNull(result);

        var resultMr = repository.search(Bundle.class, MeasureReport.class, Searches.ALL);
        var mrSize = resultMr.getEntry().size();
        MeasureReport report = null;
        for (int i = 0; i < mrSize; i++) {
            var getEntry = resultMr.getEntry();
            var mrResource = (MeasureReport) getEntry.get(i).getResource();
            var measure = mrResource.getMeasure();
            if (MEASURE_FULL_ID.equals(measure)) {
                report = mrResource;
                break;
            }
        }

        // found submitted MeasureReport!
        assertNotNull(report);

        var resultOb = repository.search(Bundle.class, Observation.class, Searches.ALL);
        var obSize = resultOb.getEntry().size();
        Observation observationFromRepo = null;
        for (int i = 0; i < obSize; i++) {
            var getEntry = resultOb.getEntry();
            var obResource = (Observation) getEntry.get(i).getResource();
            var val = obResource.getValue().primitiveValue();
            if (OBSERVATION_VALUE.equals(val)) {
                observationFromRepo = obResource;
                break;
            }
        }
        // found submitted Observation!
        assertNotNull(observationFromRepo);
    }

    @SuppressWarnings("unchecked")
    private <T extends IBaseResource> T newResource(Class<T> theResourceClass) {
        checkNotNull(theResourceClass);

        return (T) FHIR_CONTEXT.getResourceDefinition(theResourceClass).newInstance();
    }
}
