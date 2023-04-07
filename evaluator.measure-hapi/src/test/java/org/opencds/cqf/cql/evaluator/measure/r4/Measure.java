package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONException;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.GeneratedReport.PopulationSelector;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.GeneratedReport.PopulationValidator;
import org.opencds.cqf.fhir.api.Repository;
import org.skyscreamer.jsonassert.JSONAssert;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class Measure {
  private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
  private static final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

  private static InputStream open(String asset) {
    return Measure.class.getResourceAsStream(asset);
  }

  public static String load(InputStream asset) throws IOException {
    return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
  }

  public static String load(String asset) throws IOException {
    return load(open(asset));
  }

  public static IBaseResource parse(String asset) {
    return jsonParser.parseResource(open(asset));
  }

  public static R4MeasureProcessorB buildProcessor(Repository repository) {
    return new R4MeasureProcessorB(repository, null);
  }

  /** Fluent interface starts here **/

  static class Assert {
    public static Evaluate that(String measureId, String periodStart, String periodEnd) {
      return new Evaluate(measureId, periodStart, periodEnd);
    }
  }

  static class Evaluate {
    private String measureId;

    private String periodStart;
    private String periodEnd;

    private String subjectId;
    private String reportType;

    private Repository repository;
    private Repository dataRepository;
    private Repository contentRepository;
    private Repository terminologyRepository;
    private Bundle additionalData;
    private Parameters parameters;

    public Evaluate(String measureId, String periodStart, String periodEnd) {
      this.measureId = measureId;
      this.periodStart = periodStart;
      this.periodEnd = periodEnd;
    }

    public Evaluate subject(String subjectId) {
      this.subjectId = subjectId;
      return this;
    }

    public Evaluate reportType(String reportType) {
      this.reportType = reportType;
      return this;

    }

    public Evaluate additionalData(String dataAssetName) {
      var data = parse(dataAssetName);
      additionalData =
          data.getIdElement().getResourceType().equals(FHIRAllTypes.BUNDLE.toCode()) ? (Bundle) data
              : new Bundle().setType(BundleType.COLLECTION)
                  .addEntry(new BundleEntryComponent().setResource((Resource) data));

      return this;
    }

    public Evaluate parameters(Parameters params) {
      parameters = params;

      return this;
    }

    public Evaluate repository(Repository repository) {
      this.repository = repository;

      return this;
    }


    public GeneratedReport evaluate() {
      var processor = buildProcessor(repository);
      var result = processor.evaluateMeasure(new IdType("Measure", measureId), periodStart,
          periodEnd, reportType, Collections.singletonList(this.subjectId));
      return new GeneratedReport(result);
    }
  }

  static class GeneratedReport {

    @FunctionalInterface
    interface MeasureReportValidator {
      void validate(MeasureReport report);
    }

    @FunctionalInterface
    interface PopulationValidator {
      void validate(MeasureReport.MeasureReportGroupPopulationComponent population);
    }

    @FunctionalInterface
    interface GroupValidator {
      void validate(MeasureReport.MeasureReportGroupComponent group);
    }

    @FunctionalInterface
    interface StratifierValidator {
      void validate(MeasureReport.MeasureReportGroupStratifierComponent stratifier);
    }

    @FunctionalInterface
    interface GroupSelector {
      MeasureReport.MeasureReportGroupComponent selectGroup(MeasureReport report);
    }

    @FunctionalInterface
    interface PopulationSelector {
      MeasureReport.MeasureReportGroupPopulationComponent selectPopulation(
          MeasureReport.MeasureReportGroupComponent group);
    }

    @FunctionalInterface
    interface StratificationSelector {
      MeasureReport.MeasureReportGroupStratifierComponent selectStratification(
          MeasureReport.MeasureReportGroupComponent group);
    }

    private final MeasureReport report;

    public GeneratedReport(MeasureReport report) {
      this.report = report;
    }

    public GeneratedReport isEqualsTo(String expectedReportResourceName) {
      try {
        JSONAssert.assertEquals(load(expectedReportResourceName),
            jsonParser.encodeResourceToString(report), true);
      } catch (JSONException | IOException e) {
        e.printStackTrace();
        fail("Unable to compare Jsons: " + e.getMessage());
      }

      return this;
    }

    public GeneratedReport passes(MeasureReportValidator measureReportValidator) {
      measureReportValidator.validate(report);
      return this;
    }

    public MeasureReport report() {
      return this.report;
    }

    public SelectedGroup firstGroup() {
      return this.group(MeasureReport::getGroupFirstRep);
    }

    public SelectedGroup group(GroupSelector groupSelector) {
      var g = groupSelector.selectGroup(report);
      return new SelectedGroup(this, g);
    }
  }

  static class SelectedGroup {
    private final GeneratedReport generatedReport;
    private final MeasureReport.MeasureReportGroupComponent group;

    public SelectedGroup(GeneratedReport generatedReport,
        MeasureReport.MeasureReportGroupComponent group) {
      this.generatedReport = generatedReport;
      this.group = group;
    }

    public GeneratedReport up() {
      return this.generatedReport;
    }

    public SelectedGroup hasScore(String score) {
      MeasureValidationUtils.validateGroupScore(this.group, score);
      return this;
    }

    public SelectedPopulation firstPopulation() {
      return this.population(MeasureReport.MeasureReportGroupComponent::getPopulationFirstRep);
    }


    public SelectedPopulation population(String name) {
      return this.population(
          g -> g.getPopulation().stream().filter(x -> x.hasCode() && x.getCode().hasCoding()
              && x.getCode().getCoding().get(0).getCode().equals(name)).findFirst().get());
    }

    public SelectedPopulation population(PopulationSelector populationSelector) {
      var p = populationSelector.selectPopulation(group);
      return new SelectedPopulation(this, p);
    }
  }

  static class SelectedPopulation {
    private final SelectedGroup selectedGroup;
    private final MeasureReport.MeasureReportGroupPopulationComponent population;

    public SelectedPopulation(SelectedGroup selectedGroup,
        MeasureReport.MeasureReportGroupPopulationComponent population) {
      this.selectedGroup = selectedGroup;
      this.population = population;
    }

    public SelectedGroup up() {
      return this.selectedGroup;
    }

    public SelectedPopulation hasCount(int count) {
      MeasureValidationUtils.validatePopulation(population, count);
      return this;
    }

    public SelectedPopulation passes(PopulationValidator populationValidator) {
      populationValidator.validate(this.population);
      return this;
    }
  }
}
