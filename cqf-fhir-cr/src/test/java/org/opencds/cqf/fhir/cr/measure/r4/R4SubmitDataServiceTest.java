package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import com.google.common.collect.Lists;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches;

class R4SubmitDataServiceTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    private final Repository repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
    private final R4SubmitDataService testSubject = new R4SubmitDataService(repository);

    @Test
    public void submitDataTest(){

        //create resources
        MeasureReport mr = newResource(MeasureReport.class).setMeasure("Measure/A123");
        Observation obs = newResource(Observation.class).setValue(new StringType("ABC"));

        //submit-data operation
        var res = testSubject
            .submitData(new IdType("Measure", "A123"), mr,
                Lists.newArrayList(obs));

        var resultMr = repository.search(Bundle.class, MeasureReport.class, Searches.ALL);
        var mrSize = resultMr.getEntry().size();
        MeasureReport report = null;
        for (int i = 0; i < mrSize; i++){
            var getEntry = resultMr.getEntry();
            var mrResource = (MeasureReport) getEntry.get(i).getResource();
            var measure = mrResource.getMeasure();
            if (measure.equals("Measure/A123")){
                report = mrResource;
                break;
            }
        }
        //found submitted MeasureReport!
        assertNotNull(report);

        var resultOb = repository.search(Bundle.class, Observation.class, Searches.ALL);
        var obSize = resultOb.getEntry().size();
        Observation observation = null;
        for (int i = 0; i < obSize; i++){
            var getEntry = resultOb.getEntry();
            var obResource = (Observation) getEntry.get(i).getResource();
            var val = obResource.getValue().primitiveValue();
            if (val.equals("ABC")){
                observation = obResource;
                break;
            }
        }
        //found submitted Observation!
        assertNotNull(observation);

    }

    @SuppressWarnings("unchecked")
    private <T extends IBaseResource> T newResource(Class<T> theResourceClass) {
        checkNotNull(theResourceClass);

        return (T) FHIR_CONTEXT.getResourceDefinition(theResourceClass).newInstance();
    }
}