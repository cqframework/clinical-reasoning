package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import org.opencds.cqf.cql.engine.runtime.Interval;

public class MeasureDef {

    private final String id;
    private final String url;
    private final String version;
    private Interval defaultMeasurementPeriod;
    private final List<GroupDef> groups;
    private final List<SdeDef> sdes;

    public MeasureDef(String id, String url, String version, List<GroupDef> groups, List<SdeDef> sdes) {
        this.id = id;
        this.url = url;
        this.version = version;
        this.groups = groups;
        this.sdes = sdes;
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
}
