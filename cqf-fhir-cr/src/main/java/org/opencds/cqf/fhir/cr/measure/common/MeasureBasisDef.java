package org.opencds.cqf.fhir.cr.measure.common;

public interface MeasureBasisDef<MeasureT> {
    boolean isBooleanBasis(MeasureT measure);
}
