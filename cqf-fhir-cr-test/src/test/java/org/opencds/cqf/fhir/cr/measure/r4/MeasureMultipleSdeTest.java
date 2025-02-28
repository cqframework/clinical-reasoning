package org.opencds.cqf.fhir.cr.measure.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class MeasureMultipleSdeTest {
    private static final Given GIVEN_MULTIPLE_SDE_MEASURE_REPO =
            Measure.given().repositoryFor("BreastCancerScreeningFHIR");

    @ParameterizedTest
    @CsvSource(
            value = {
                "null,null",
                "subject,null",
                "subject-list,null",
                "population,null",
                "null,Patient/numer-EXM125",
                "subject,Patient/numer-EXM125",
                "subject-list,Patient/numer-EXM125",
                "population,Patient/numer-EXM125",
                "null,Patient/denom-EXM125",
                "subject,Patient/denom-EXM125",
                "subject-list,Patient/denom-EXM125",
                "population,Patient/denom-EXM125",
                "null,Patient/numer-EXM125",
                "subject,Patient/numer-EXM125",
                "subject-list,Patient/numer-EXM125",
                "population,Patient/numer-EXM125",
            },
            nullValues = {"null"})
    void evaluateSucceedsMultipleSdesReportTypeSubjectAndSubjectNull(
            @Nullable String reportType, @Nullable String subject) {
        var when = GIVEN_MULTIPLE_SDE_MEASURE_REPO
                .when()
                .reportType(reportType)
                .subject(subject)
                .measureId("measure-EXM108-8.3.000")
                .evaluate();

        var report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());
        assertEquals(4, report.getGroupFirstRep().getPopulation().size());

        var extensions = report.getExtension();
        assertThat(extensions.size(), greaterThanOrEqualTo(4));

        var extensionValues = extensions.stream()
                .map(Extension::getValue)
                .filter(Reference.class::isInstance)
                .map(Reference.class::cast)
                .map(Reference::getExtension)
                .filter(innerExtensions -> innerExtensions.size() == 1)
                .map(innerExtensions -> innerExtensions.get(0))
                .map(Extension::getValue)
                .filter(StringType.class::isInstance)
                .map(StringType.class::cast)
                .map(StringType::getValue)
                .distinct()
                .toList();

        assertThat(extensionValues, hasSize(3));
        assertThat(extensionValues, containsInAnyOrder("sde-sex", "sde-race", "sde-ethnicity"));
    }
}
