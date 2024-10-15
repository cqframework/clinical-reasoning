package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.fhir.cr.measure.common.MeasureBasisDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

/*

*/
public class R4MeasureBasisDef implements MeasureBasisDef<Measure> {

    @Override
    public boolean isBooleanBasis(Measure measure) {
        // check for population-basis Extension, assume boolean if no Extension is found
        if (measure.hasExtension()) {
            return measure.getExtension().stream().anyMatch(this::isBooleanBasisExtension);
        }
        return true;
    }

    private boolean isBooleanBasisExtension(IBaseExtension<?, ?> item) {
        return (item.getUrl().equalsIgnoreCase(MeasureConstants.POPULATION_BASIS_URL)
                && item.getValue().toString().equalsIgnoreCase("boolean"));
    }
}
