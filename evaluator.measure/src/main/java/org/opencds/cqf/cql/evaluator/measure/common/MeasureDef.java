package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.List;

public class MeasureDef {

    private List<GroupDef> groups;

    private ArrayList<SdeDef> sdes;

    private MeasureScoring measureScoring;

    private String url;


    public List<SdeDef> getSdes() {
        if (this.sdes == null) {
            this.sdes = new ArrayList<>();
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
