package org.opencds.cqf.cql.evaluator.measure.common;

public interface MeasureDefBuilder<MeasureT> {
    MeasureDef build(MeasureT measure);
}
