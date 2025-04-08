package org.opencds.cqf.fhir.utility.adapter.r4;

import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IMeasureReportAdapter;

public class MeasureReportAdapter extends ResourceAdapter implements IMeasureReportAdapter {

    public MeasureReportAdapter(IBaseResource measureReport) {
        super(measureReport);
        if (!(measureReport instanceof MeasureReport)) {
            throw new IllegalArgumentException("resource passed as endpoint argument is not an MeasureReport resource");
        }
    }
}
