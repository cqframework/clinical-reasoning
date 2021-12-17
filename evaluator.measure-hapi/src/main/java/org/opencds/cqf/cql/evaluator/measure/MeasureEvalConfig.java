package org.opencds.cqf.cql.evaluator.measure;

import java.util.EnumSet;

import org.opencds.cqf.cql.engine.execution.CqlEngine;

public class MeasureEvalConfig {

    private EnumSet<CqlEngine.Options> cqlEngineOptions;

    private EnumSet<MeasureEvalOptions> measureEvalOptions;

    public EnumSet<CqlEngine.Options> getCqlEngineOptions() {
        if (this.cqlEngineOptions == null) {
            this.cqlEngineOptions = EnumSet.of(CqlEngine.Options.EnableExpressionCaching);
        }

        return this.cqlEngineOptions; 
    }

    public void setCqlEngineOptions(EnumSet<CqlEngine.Options> cqlEngineOptions) {
        this.cqlEngineOptions = cqlEngineOptions;
    }

    public EnumSet<MeasureEvalOptions> getMeasureEvalOptions() {
        if (this.measureEvalOptions == null) {
            this.measureEvalOptions = EnumSet.noneOf(MeasureEvalOptions.class);
        }

        return this.measureEvalOptions; 
    }

    public void setMeasureEvalOptions(EnumSet<MeasureEvalOptions> measureEvalOptions) {
        this.measureEvalOptions = measureEvalOptions;
    }

    public static MeasureEvalConfig defaultConfig() {
        return new MeasureEvalConfig();
    }
}
