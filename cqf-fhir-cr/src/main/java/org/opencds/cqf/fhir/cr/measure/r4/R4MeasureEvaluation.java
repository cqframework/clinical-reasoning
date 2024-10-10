package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureEvaluation;

/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR R4 structures.
 */
public class R4MeasureEvaluation extends BaseMeasureEvaluation<Measure, MeasureReport, DomainResource> {

    public R4MeasureEvaluation(
            CqlEngine context, Measure measure, LibraryEngine libraryEngine, VersionedIdentifier versionIdentifier) {
        super(
                context,
                measure,
                new R4MeasureDefBuilder(),
                new R4MeasureReportBuilder(),
                libraryEngine,
                versionIdentifier);
    }
}
