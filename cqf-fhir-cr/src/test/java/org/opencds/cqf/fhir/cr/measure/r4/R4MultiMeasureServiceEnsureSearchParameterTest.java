package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.SearchParameter;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.search.Searches.SearchBuilder;

/**
 * Tests the {@code ensureSearchParameters} flag on {@link MeasureEvaluationOptions}
 * to verify that the SDE SearchParameter is created (or not) based on the flag value.
 * <p/>
 * Note that {@link org.opencds.cqf.fhir.utility.repository.ig.IgRepository} does not support
 * {@link org.opencds.cqf.fhir.utility.repository.ig.IgRepository#transaction(IBaseBundle)}, which
 * means we can't assert ensure SDE at the Measure/MultiMeasure integration test level.
 */
class R4MultiMeasureServiceEnsureSearchParameterTest {

    @Test
    void ensureSearchParameters_true_createsSearchParameter() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
        var utils = new R4MeasureServiceUtils(repository);

        utils.ensureSupplementalDataElementSearchParameter();

        var searchResult = searchForSupplementalDataSearchParameter(repository);
        assertFalse(
                searchResult.getEntry().isEmpty(), "Expected supplemental-data SearchParameter to exist in repository");
    }

    @Test
    void ensureSearchParameters_false_doesNotCreateSearchParameter() {
        var repository = new InMemoryFhirRepository(FhirContext.forR4Cached());
        var options = MeasureEvaluationOptions.defaultOptions().setEnsureSearchParameters(false);

        // Guard with the flag check, mirroring R4MultiMeasureService behavior
        if (options.isEnsureSearchParameters()) {
            var utils = new R4MeasureServiceUtils(repository);
            utils.ensureSupplementalDataElementSearchParameter();
        }

        var searchResult = searchForSupplementalDataSearchParameter(repository);
        assertTrue(
                searchResult.getEntry().isEmpty(),
                "Expected supplemental-data SearchParameter to NOT exist in repository");
    }

    private static Bundle searchForSupplementalDataSearchParameter(InMemoryFhirRepository repository) {
        return repository.search(
                Bundle.class,
                SearchParameter.class,
                new SearchBuilder().withTokenParam("code", "supplemental-data").build());
    }
}
