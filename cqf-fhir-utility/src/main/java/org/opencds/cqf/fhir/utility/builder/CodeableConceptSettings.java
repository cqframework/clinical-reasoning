package org.opencds.cqf.fhir.utility.builder;

import java.util.HashSet;
import java.util.Set;

public class CodeableConceptSettings {

    private Set<CodingSettings> codingSettings = new HashSet<>();

    public CodeableConceptSettings add(String system, String code) {
        add(system, code, null);

        return this;
    }

    public CodeableConceptSettings add(String system, String code, String display) {
        codingSettings.add(new CodingSettings(system, code, display));

        return this;
    }

    public Set<CodingSettings> getCodingSettings() {
        return this.codingSettings;
    }

    public CodingSettings[] getCodingSettingsArray() {
        return getCodingSettings().toArray(new CodingSettings[0]);
    }
}
