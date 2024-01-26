package org.opencds.cqf.fhir.utility.builder;

public class NarrativeSettings {

    private String text;
    private String status = "generated";

    public NarrativeSettings(String text) {
        this.text = text;
    }

    public NarrativeSettings(String text, String status) {
        this.text = text;
        this.status = status;
    }

    public String getText() {
        return this.text;
    }

    public String getStatus() {
        return this.status;
    }
}
