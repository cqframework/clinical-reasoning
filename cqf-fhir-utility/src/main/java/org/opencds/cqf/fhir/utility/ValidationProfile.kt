package org.opencds.cqf.fhir.utility

class ValidationProfile {
    var name: String? = null
    private var ignoreKeys: MutableList<String?>? = null

    constructor()

    constructor(name: String?, ignoreKeys: MutableList<String?>) {
        this.name = name
        this.ignoreKeys = ignoreKeys
    }

    fun getIgnoreKeys(): MutableList<String?> {
        return ignoreKeys!!
    }

    fun setIgnoreKeys(ignoreKeys: MutableList<String?>) {
        this.ignoreKeys = ignoreKeys
    }

    fun addIgnoreKey(key: String?) {
        this.ignoreKeys!!.add(key)
    }
}
