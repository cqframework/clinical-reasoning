package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeasureDef {

    private List<GroupDef> groups;

    private Set<SdeDef> sdes;

    private MeasureScoring measureScoring;

    private String url;


    public Set<SdeDef> getSdes() {
        if (this.sdes == null) {
            this.sdes = new HashSet<>();
        }

        return this.sdes;
    }

    public List<GroupDef> getGroups() {
        if (this.groups == null) {
            this.groups = new ArrayList<>();
        }

        return this.groups;
    }

    public MeasureScoring getMeasureScoring() {
        return this.measureScoring;
    }

    public void setMeasureScoring(MeasureScoring measureScoring) {
        this.measureScoring = measureScoring;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
