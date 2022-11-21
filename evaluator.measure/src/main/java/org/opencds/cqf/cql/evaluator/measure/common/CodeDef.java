package org.opencds.cqf.cql.evaluator.measure.common;

public class CodeDef {

    private final String system;
    private final String version;
    private final String code;
    private final String display;

    public CodeDef(String system, String code) {
        this(system, null, code, null);
    }

    public CodeDef(String system, String version, String code, String display) {
        this.system = system;
        this.version = version;
        this.code = code;
        this.display = display;
    }


    public String system() {
        return this.system;
    }

    public String version() {
        return this.version;
    }

    public String code() {
        return this.code;
    }

    public String display() {
        return this.display;
    }
}
