package org.opencds.cqf.fhir.utility.builder

import org.hl7.fhir.instance.model.api.IBaseBackboneElement

class CompositionSectionComponentBuilder<T : IBaseBackboneElement> :
    BaseBackboneElementBuilder<CompositionSectionComponentBuilder<T>, T> {
    private var title: String? = null
    private var focus: String? = null
    private var entries: MutableList<String?>? = null
    private var text: NarrativeSettings? = null

    constructor(resourceClass: Class<T>) : super(resourceClass)

    constructor(resourceClass: Class<T>, id: String) : super(resourceClass, id)

    constructor(
        resourceClass: Class<T>,
        id: String,
        focus: String,
        entry: String,
    ) : this(resourceClass, id) {

        this.focus = focus
        addEntry(entry)
    }

    private fun addEntry(entry: String?) {
        if (this.entries == null) {
            this.entries = mutableListOf()
        }

        this.entries!!.add(entry)
    }

    private fun getEntries(): MutableList<String?> {
        if (entries == null) {
            return mutableListOf()
        }

        return entries!!
    }

    fun withTitle(title: String): CompositionSectionComponentBuilder<T> {

        this.title = title

        return this
    }

    fun withFocus(focus: String): CompositionSectionComponentBuilder<T> {

        this.focus = focus

        return this
    }

    fun withEntry(entry: String): CompositionSectionComponentBuilder<T> {

        addEntry(entry)

        return this
    }

    fun withText(text: NarrativeSettings): CompositionSectionComponentBuilder<T> {

        this.text = text

        return this
    }

    override fun build(): T {
        requireNotNull(focus)
        requireNotNull(entries)
        require(entries!!.isNotEmpty())

        return super.build()
    }

    override fun initializeDstu3(resource: T?) {
        super.initializeDstu3(resource)

        val section = resource as org.hl7.fhir.dstu3.model.Composition.SectionComponent

        section.setTitle(title).setId(this.id)
        getEntries().forEach { entry ->
            section.addEntry(org.hl7.fhir.dstu3.model.Reference(entry))
        }
        if (text != null) {
            val narrative = org.hl7.fhir.dstu3.model.Narrative()
            narrative.setStatusAsString(text!!.status)
            narrative.setDivAsString(text!!.text)
            section.setText(narrative)
        }
        // no focus
    }

    override fun initializeR4(resource: T?) {
        super.initializeR4(resource)
        val section = resource as org.hl7.fhir.r4.model.Composition.SectionComponent

        section.setFocus(org.hl7.fhir.r4.model.Reference(focus)).setTitle(title).setId(this.id)
        getEntries().forEach { entry -> section.addEntry(org.hl7.fhir.r4.model.Reference(entry)) }
        if (text != null) {
            val narrative = org.hl7.fhir.r4.model.Narrative()
            narrative.setStatusAsString(text!!.status)
            narrative.setDivAsString(text!!.text)
            section.setText(narrative)
        }
    }

    override fun initializeR5(resource: T?) {
        super.initializeR5(resource)
        val section = resource as org.hl7.fhir.r5.model.Composition.SectionComponent

        section.setFocus(org.hl7.fhir.r5.model.Reference(focus)).setTitle(title).setId(this.id)
        getEntries().forEach { entry -> section.addEntry(org.hl7.fhir.r5.model.Reference(entry)) }
        if (text != null) {
            val narrative = org.hl7.fhir.r5.model.Narrative()
            narrative.setStatusAsString(text!!.status)
            narrative.setDivAsString(text!!.text)
            section.setText(narrative)
        }
    }
}
