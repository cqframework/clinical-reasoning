package org.opencds.cqf.fhir.utility.builder;

public class CodingSettings {

    private String system;
    private String code;
    private String display;

    public CodingSettings(String system, String code) {
        this(system, code, null);
    }

    public CodingSettings(String system, String code, String display) {
        this.system = system;
        this.code = code;
        this.display = display;
    }

    public String getSystem() {
        return this.system;
    }

    public String getCode() {
        return this.code;
    }

    public String getDisplay() {
        return this.display;
    }
}
