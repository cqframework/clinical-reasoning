package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.cql.evaluator.measure.TestRepositories;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureConstants;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.GeneratedReport.PopulationSelector;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.GeneratedReport.PopulationValidator;
import org.opencds.cqf.fhir.api.Repository;
import org.testng.TestException;

public class Measure {
  public static Given given() {
    return new Given();
  }

  static class Given {
    private Repository repository;

    Given repository(Repository repository) {
      this.repository = repository;
      return this;
    }

    Given repositoryFor(String repositoryPath) {
      this.repository = TestRepositories.createRepositoryForPath(repositoryPath);
      return this;
    }

    private static R4MeasureProcessorB buildProcessor(Repository repository) {
      return new R4MeasureProcessorB(repository, null);
    }

    When when() {
      return new When(buildProcessor(this.repository));
    }
  }

  static class When {
    private final R4MeasureProcessorB processor;

    When(R4MeasureProcessorB processor) {
      this.processor = processor;
    }

    private String measureId;

    private String periodStart;
    private String periodEnd;

    private String subjectId;
    private String reportType;

    private Bundle additionalData;

    private Supplier<GeneratedReport> operation;



    public When measureId(String measureId) {
      this.measureId = measureId;
      return this;
    }

    public When periodEnd(String periodEnd) {
      this.periodEnd = periodEnd;
      return this;
    }

    public When periodStart(String periodStart) {
      this.periodStart = periodStart;
      return this;
    }


    public When subject(String subjectId) {
      this.subjectId = subjectId;
      return this;
    }

    public When reportType(String reportType) {
      this.reportType = reportType;
      return this;
    }

    public When additionalData(Bundle additionalData) {
      this.additionalData = additionalData;
      throw new TestException("additional data is not yet supported in tests");
      // return this;
    }

    public When evaluate() {
      this.operation = () -> new GeneratedReport(
          processor.evaluateMeasure(new IdType("Measure", measureId), periodStart,
              periodEnd, reportType, Collections.singletonList(this.subjectId)));
      return this;
    }

    public GeneratedReport then() {
      if (this.operation == null) {
        throw new IllegalStateException(
            "No operation was selected as part of 'when'. Choose an operation to invoke by adding one, such as 'evaluate' to the method chain.");
      }

      try {
        return this.operation.get();
      } catch (Exception e) {
        throw new TestException("error when running 'then' and invoking the chosen operation", e);
      }
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

    @FunctionalInterface
    interface ReferenceSelector {
      Reference selectReference(
          MeasureReport measureReport);
    }

    private final MeasureReport report;

    public GeneratedReport(MeasureReport report) {
      this.report = report;
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

    public SelectedGroup group(String id) {
      return this
          .group(x -> x.getGroup().stream().filter(g -> g.getId().equals(id)).findFirst().get());
    }

    public SelectedGroup group(GroupSelector groupSelector) {
      var g = groupSelector.selectGroup(report);
      return new SelectedGroup(this, g);
    }

    public SelectedReference reference(ReferenceSelector referenceSelector) {
      var r = referenceSelector.selectReference(report);
      return new SelectedReference(this, r);
    }

    public SelectedReference evaluatedResource(String name) {
      return this.reference(x -> x.getEvaluatedResource().stream()
          .filter(y -> y.getReference().equals(name)).findFirst().get());
    }

    public GeneratedReport hasEvaluatedResourceCount(int count) {
      assertEquals(count, report().getEvaluatedResource().size());
      return this;
    }

    public GeneratedReport hasContainedResourceCount(int count) {
      assertEquals(count, report().getContained().size());
      return this;
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

  static class SelectedReference {
    private final GeneratedReport generatedReport;
    private final Reference reference;

    public SelectedReference(GeneratedReport generatedReport, Reference reference) {
      this.generatedReport = generatedReport;
      this.reference = reference;
    }

    public GeneratedReport up() {
      return generatedReport;
    }


    // Hmm.. may need to rethink this one a bit.
    public SelectedReference hasPopulations(String... population) {
      var ex = this.reference.getExtensionsByUrl(MeasureConstants.EXT_CRITERIA_REFERENCE_URL);
      if (ex.isEmpty()) {
        throw new TestException(String.format(
            "no evaluated resource extensions were found, and expected %s", population.length));
      }

      @SuppressWarnings("unchecked")
      var set = ex.stream().map(x -> ((IPrimitiveType<String>) x.getValue()).getValue())
          .collect(Collectors.toSet());

      for (var p : population) {
        assertTrue(set.contains(p),
            String.format(
                "population: %s was not found in the evaluated resources criteria reference extension list",
                p));
      }

      return this;
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
