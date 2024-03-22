package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.apache.commons.lang3.StringUtils;

import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Measure;
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

    private boolean isBooleanBasisExtension(Extension item) {
        return (item.getUrl() != null
            && StringUtils.equalsIgnoreCase(item.getUrl(), MeasureConstants.POPULATION_BASIS_URL)
            && StringUtils.equalsIgnoreCase(item.getValue().toString(), "boolean"));
    }



}
