package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.fhir.cr.measure.common.MeasureBasisDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureConstants;

/*

*/
public class R4MeasureBasisDef implements MeasureBasisDef<Measure> {

    @Override
    public boolean isBooleanBasis(Measure measure) {
        if (measure.hasExtension()) {
            return measure.getExtension().stream().anyMatch(this::isBooleanBasisExtension);
        }
        return false;
    }

    private boolean isBooleanBasisExtension(IBaseExtension item) {
        return (item.getUrl().equalsIgnoreCase(MeasureConstants.POPULATION_BASIS_URL)
                && item.getValue().toString().equalsIgnoreCase("boolean"));
    }
}
