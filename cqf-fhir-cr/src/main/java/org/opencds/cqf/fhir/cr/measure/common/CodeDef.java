package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CodeDef)) return false;
        CodeDef that = (CodeDef) o;
        return Objects.equals(system, that.system) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, code);
    }

    /** Returns {@code system|code} per FHIR coding format. */
    @Override
    public String toString() {
        return system + "|" + code;
    }
}
