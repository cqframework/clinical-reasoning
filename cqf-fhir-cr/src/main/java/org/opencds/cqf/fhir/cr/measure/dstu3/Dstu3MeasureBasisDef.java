package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.opencds.cqf.fhir.cr.measure.common.MeasureBasisDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureConstants;

/*

*/
public class Dstu3MeasureBasisDef implements MeasureBasisDef<Measure> {

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
