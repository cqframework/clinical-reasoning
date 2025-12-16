package org.opencds.cqf.fhir.cr.measure.common.def;

/**
 * Immutable representation of a FHIR code with optional system, version, and display.
 *
 * Converted to record by Claude Sonnet 4.5 on 2025-12-15.
 */
public record CodeDef(String system, String version, String code, String display) {

    /**
     * Convenience constructor for creating a CodeDef with only system and code.
     * Version and display are set to null.
     *
     * @param system the code system
     * @param code the code value
     */
    public CodeDef(String system, String code) {
        this(system, null, code, null);
    }
}
