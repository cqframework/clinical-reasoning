package org.opencds.cqf.fhir.cr.measure.common;

public interface MeasureDefBuilder<MeasureT> {
  MeasureDef build(MeasureT measure);
}
