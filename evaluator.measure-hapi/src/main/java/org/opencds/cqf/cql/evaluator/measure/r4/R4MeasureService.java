package org.opencds.cqf.cql.evaluator.measure.r4;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.evaluator.fhir.util.Repositories;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class R4MeasureService {

  private final Repository repository;
  private final MeasureEvaluationOptions measureEvaluationOptions;

  public R4MeasureService(Repository repository,
      MeasureEvaluationOptions measureEvaluationOptions) {
    this.repository = repository;
    this.measureEvaluationOptions = measureEvaluationOptions;
  }

  public MeasureReport evaluate(Either3<CanonicalType, IdType, Measure> measure, String periodStart,
      String periodEnd, String reportType,
      String subjectId, String lastReceivedOn, Endpoint contentEndpoint,
      Endpoint terminologyEndpoint, Endpoint dataEndpoint, Bundle additionalData) {


    var repo =
        this.proxyAndFederate(contentEndpoint, terminologyEndpoint, dataEndpoint, additionalData);

    var evalType = MeasureEvalType.fromCode(reportType)
        .orElse(MeasureEvalType.POPULATION);

    var subjects = this.getSubjects(repo, evalType, subjectId);

    var processor = new R4MeasureProcessor(repo, this.measureEvaluationOptions);

    return processor.evaluateMeasure(
        measure,
        periodStart,
        periodEnd,
        reportType,
        subjects.collect(Collectors.toList()));
  }


  public Stream<String> getSubjects(Repository repo, MeasureEvalType evalType, String subjectId) {
    var subjectProvider = new R4RepositorySubjectProvider(repo);
    return subjectProvider.getSubjects(evalType, subjectId);
  }


  public Repository proxyAndFederate(Endpoint contentEndpoint,
      Endpoint terminologyEndpoint, Endpoint dataEndpoint, Bundle additionalData) {
    var proxy = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);

    // TODO: Federation for additional data.
    return proxy;
  }
}
