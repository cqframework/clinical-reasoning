package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureConstants;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class MeasureProcessorSdeAddCriteriaExtensionTest extends BaseMeasureProcessorTest {
  public MeasureProcessorSdeAddCriteriaExtensionTest() {
    super("ClientNonPatientBasedMeasureBundle.json");
  }

  @Test
  public void exm124_subject_list() {
    MeasureReport report = this.measureProcessor.evaluateMeasure(
        "http://nhsnlink.org/fhir/Measure/InitialInpatientPopulation", "2019-01-01", "2020-01-01",
        "subject", "Patient/97f27374-8a5c-4aa1-a26f-5a1ab03caa47", null, null, endpoint, endpoint,
        endpoint, null);

    for (Extension extension : report.getExtension()) {
      if (StringUtils.equalsIgnoreCase(extension.getUrl(), MeasureConstants.EXT_SDE_URL)) {
        assertNotNull(extension.getValue());
        break;
      }
    }

    String populationName = "initial-population";
    int expectedCount = 2;

    Optional<MeasureReport.MeasureReportGroupPopulationComponent> population = report.getGroup()
        .get(0).getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding()
            && x.getCode().getCoding().get(0).getCode().equals(populationName))
        .findFirst();

    assertTrue(population.isPresent(),
        String.format("Unable to locate a population with id \"%s\"", populationName));
    assertEquals(population.get().getCount(), expectedCount,
        String.format("expected count for population \"%s\" did not match", populationName));
  }
}
