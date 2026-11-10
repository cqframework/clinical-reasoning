package org.opencds.cqf.fhir.utility.builder

class NarrativeSettings {
    val text: String?
    var status: String? = "generated"
        private set

    constructor(text: String?) {
        this.text = text
    }

    constructor(text: String?, status: String?) {
        this.text = text
        this.status = status
    }
}
