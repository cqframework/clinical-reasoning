package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Map;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class MeasureDef {

    private final String id;
    private final String url;
    private final String version;
    private Interval defaultMeasurementPeriod;
    private final Map<GroupDef, MeasureScoring> scoring;
    private final List<GroupDef> groups;
    private final List<SdeDef> sdes;
    private final boolean isBooleanBasis;

    public MeasureDef(
            String id,
            String url,
            String version,
            Map<GroupDef, MeasureScoring> scoring,
            List<GroupDef> groups,
            List<SdeDef> sdes,
            boolean isBooleanBasis) {
        this.id = id;
        this.url = url;
        this.version = version;
        this.groups = groups;
        this.sdes = sdes;
        this.scoring = scoring;
        this.isBooleanBasis = isBooleanBasis;
    }

    public String id() {
        return this.id;
    }

    public String url() {
        return this.url;
    }

    public String version() {
        return this.version;
    }

    public Interval getDefaultMeasurementPeriod() {
        return defaultMeasurementPeriod;
    }

    public List<SdeDef> sdes() {
        return this.sdes;
    }

    public List<GroupDef> groups() {
        return this.groups;
    }

    public Map<GroupDef, MeasureScoring> scoring() {
        return this.scoring;
    }

    public boolean isBooleanBasis() {
        return this.isBooleanBasis;
    }
}
