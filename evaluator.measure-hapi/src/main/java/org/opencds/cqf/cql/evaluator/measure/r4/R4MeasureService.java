package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.MeasureReport;

public interface R4MeasureService {

  MeasureReport evaluate(String url, String periodStart, String periodEnd, String reportType,
      String subjectId, String lastReceivedOn, Endpoint contentEndpoint,
      Endpoint terminologyEndpoint, Endpoint dataEndpoint, Bundle additionalData);

}
