package org.opencds.cqf.fhir.utility.builder

class CodeableConceptSettings {
    val codingSettings = mutableSetOf<CodingSettings?>()

    fun add(system: String?, code: String?): CodeableConceptSettings {
        add(system, code, null)

        return this
    }

    fun add(system: String?, code: String?, display: String?): CodeableConceptSettings {
        codingSettings.add(CodingSettings(system, code, display))

        return this
    }

    val codingSettingsArray: Array<CodingSettings?>
        get() = this.codingSettings.toTypedArray()
}
