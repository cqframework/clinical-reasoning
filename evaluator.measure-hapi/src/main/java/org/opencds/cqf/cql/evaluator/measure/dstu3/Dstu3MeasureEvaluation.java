package org.opencds.cqf.cql.evaluator.measure.dstu3;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvaluation;
/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR R4 structures.
 */
public class Dstu3MeasureEvaluation extends
        MeasureEvaluation<IBase, Measure, MeasureReport, DomainResource> {

    public Dstu3MeasureEvaluation(Context context, Measure measure) {
        super(context, measure, x -> x.getIdElement().getResourceType() + "/" + x.getIdElement().getIdPart(), new Dstu3MeasureReportBuilder(), new Dstu3MeasureDefBuilder());
    }
}
