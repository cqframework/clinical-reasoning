package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureEvaluation;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;

/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR DSTU3 structures.
 */
public class Dstu3MeasureEvaluation extends BaseMeasureEvaluation<Measure, MeasureReport, DomainResource> {

    public Dstu3MeasureEvaluation(
            CqlEngine context,
            Measure measure,
            LibraryEngine libraryEngine,
            VersionedIdentifier versionIdentifier,
            PopulationBasisValidator populationBasisValidator) {
        super(
                context,
                measure,
                new Dstu3MeasureDefBuilder(),
                new Dstu3MeasureReportBuilder(),
                libraryEngine,
                versionIdentifier,
                populationBasisValidator);
    }

    @Override
    protected String getMeasureUrl() {
        return measure.getUrl();
    }
}
