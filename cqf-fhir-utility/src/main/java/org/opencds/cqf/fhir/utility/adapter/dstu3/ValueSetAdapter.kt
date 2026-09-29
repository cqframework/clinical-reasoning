package org.opencds.cqf.fhir.utility.adapter.dstu3

import java.time.Instant
import java.util.*
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.opencds.cqf.fhir.utility.ValueSets.getCodesInCompose
import org.opencds.cqf.fhir.utility.adapter.*

class ValueSetAdapter : KnowledgeArtifactAdapter, IValueSetAdapter {
    constructor(valueSet: IDomainResource) : super(valueSet) {
        require(valueSet is ValueSet) {
            "resource passed as valueSet argument is not a ValueSet resource"
        }
    }

    constructor(valueSet: ValueSet) : super(valueSet)

    protected val valueSet: ValueSet
        get() = resource as ValueSet

    override fun get(): ValueSet {
        return resource as ValueSet
    }

    override fun copy(): ValueSet? {
        return get().copy()
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

            /*
              compose.include[].valueSet
              compose.include[].system
              compose.exclude[].valueSet
              compose.exclude[].system
            */
            (this.valueSet.compose.include + this.valueSet.compose.exclude).forEach { component ->
                if (component!!.hasValueSet()) {
                    component.valueSet.forEach { ct ->
                        references.add(
                            DependencyInfo(
                                referenceSource,
                                ct!!.value,
                                ct.extension,
                                { theValue -> ct.value = theValue },
                            )
                        )
                    }
                }
                if (component.hasSystem()) {
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            component.system,
                            component.systemElement.extension,
                            { value -> component.system = value },
                        )
                    )
                }
            }
            return references
        }

    override fun addUseContext(usageContext: IUsageContextAdapter?): IValueSetAdapter {
        if (usageContext == null) return this

        // underlying ValueSet from this adapter
        val vs = get()

        val underlying: Any? = usageContext.get()
        if (underlying !is UsageContext) return this
        val incoming = underlying

        val existingUseContexts = vs.useContext
        if (existingUseContexts == null || existingUseContexts.isEmpty()) {
            vs.addUseContext(incoming)
            return this
        }

        val alreadyExists =
            existingUseContexts.any { existing ->
                existing != null && existing.equalsDeep(incoming)
            }

        if (!alreadyExists) {
            vs.addUseContext(incoming)
        }

        return this
    }

    override fun <T : IBaseBackboneElement> setExpansion(expansion: T?) {
        this.valueSet.expansion = expansion as ValueSet.ValueSetExpansionComponent?
    }

    override fun <T : IBaseBackboneElement> getExpansion(): T? {
        @Suppress("UNCHECKED_CAST")
        return this.valueSet.expansion as T?
    }

    override fun hasExpansion(): Boolean {
        return this.valueSet.hasExpansion()
    }

    override fun hasExpansionContains(): Boolean {
        return this.getExpansion<ValueSet.ValueSetExpansionComponent>()!!.hasContains()
    }

    override val expansionTotal: Int
        get() {
            return this.getExpansion<ValueSet.ValueSetExpansionComponent>()!!.total
        }

    override val expansionContains: MutableList<IValueSetExpansionContainsAdapter?>
        get() {
            return this.getExpansion<ValueSet.ValueSetExpansionComponent>()!!
                .contains
                .map { contains -> ValueSetExpansionContainsAdapter(contains) }
                .toMutableList()
        }

    override fun appendExpansionContains(
        expansionContains: MutableList<IValueSetExpansionContainsAdapter?>?
    ) {
        this.getExpansion<ValueSet.ValueSetExpansionComponent>()!!
            .contains
            .addAll(
                (expansionContains!!
                    .map { e -> e!!.get() as ValueSet.ValueSetExpansionContainsComponent? }
                    .toMutableList())
            )

        val countParam =
            this.getExpansion<ValueSet.ValueSetExpansionComponent>()!!
                .parameter
                .filter { param -> param!!.name == "count" }
                .firstOrNull()
        if (countParam != null) {
            var count = (countParam.value as IntegerType).value
            count += expansionContains.size
            countParam.value = IntegerType(count)
        }
    }

    override fun <T : IBaseBackboneElement> newExpansion(): T? {
        val expansion = ValueSet.ValueSetExpansionComponent().setTimestamp(Date.from(Instant.now()))
        expansion.contains
        @Suppress("UNCHECKED_CAST")
        return expansion as T?
    }

    override fun addExpansionStringParameter(name: String?, value: String?) {
        this.getExpansion<ValueSet.ValueSetExpansionComponent>()!!
            .addParameter()
            .setName(name)
            .setValue(StringType(value))
    }

    override fun hasExpansionStringParameter(name: String?, value: String?): Boolean {
        if (!hasExpansion() || value == null) {
            return false
        }

        return this.getExpansion<ValueSet.ValueSetExpansionComponent>()!!
            .parameter
            .filter { p -> name == p!!.name }
            .map { it.value }
            .filterIsInstance<IPrimitiveType<*>>()
            .any { primitive -> value == primitive.valueAsString }
    }

    override fun hasCompose(): Boolean {
        return this.get().hasCompose()
    }

    override fun hasComposeInclude(): Boolean {
        return this.get().compose.hasInclude()
    }

    override val composeInclude: MutableList<IValueSetConceptSetAdapter?>
        get() {
            return this.valueSet.compose.include
                .map { conceptSet -> ValueSetConceptSetAdapter(conceptSet) }
                .toMutableList()
        }

    override fun hasComposeExclude(): Boolean {
        return this.valueSet.hasCompose() &&
            this.valueSet.compose.hasExclude() &&
            !this.valueSet.compose.exclude.isEmpty()
    }

    override fun hasComposeFilters(): Boolean {
        return this.valueSet.hasCompose() &&
            (this.valueSet.compose.include.any { i -> i!!.hasFilter() && !i.filter.isEmpty() } ||
                this.valueSet.compose.exclude.any { e -> e!!.hasFilter() && !e.filter.isEmpty() })
    }

    override val valueSetIncludes: MutableList<String?>
        get() {
            return this.valueSet.compose.include
                .flatMap { obj -> obj!!.valueSet }
                .map { obj -> obj!!.asStringValue() }
                .distinct()
                .toMutableList()
        }

    override fun hasSimpleCompose(): Boolean {
        return this.valueSet.hasCompose() &&
            !this.valueSet.compose.hasExclude() &&
            this.valueSet.compose.include.none { csc ->
                csc!!.hasFilter() || csc.hasValueSet() || !csc.hasSystem() || !csc.hasConcept()
            }
    }

    override fun hasGroupingCompose(): Boolean {
        return this.valueSet.hasCompose() &&
            !this.valueSet.compose.hasExclude() &&
            this.valueSet.compose.include.none { csc -> !csc!!.hasValueSet() || csc.hasFilter() }
    }

    override fun hasExplicitConcepts(): Boolean {
        return this.valueSet.hasCompose() &&
            this.valueSet.compose.include.any { i -> i!!.hasConcept() && !i.concept.isEmpty() }
    }

    override fun hasValueSetReferences(): Boolean {
        return this.valueSet.hasCompose() &&
            this.valueSet.compose.include.any { i -> i!!.hasValueSet() && !i.valueSet.isEmpty() }
    }

    override fun hasNaiveParameter(): Boolean {
        return this.valueSet.expansion.parameter.any { p -> p!!.name == "naive" }
    }

    override fun <T : IBaseBackboneElement> createNaiveParameter(): T? {
        @Suppress("UNCHECKED_CAST")
        return ValueSet.ValueSetExpansionParameterComponent()
            .setName("naive")
            .setValue(BooleanType(true)) as T?
    }

    override fun naiveExpand() {
        val expansion =
            newExpansion<ValueSet.ValueSetExpansionComponent>()!!.addParameter(
                createNaiveParameter()
            )

        val codesInCompose = getCodesInCompose(fhirContext, this.valueSet)
        if (codesInCompose == null) {
            return
        }
        for (code in codesInCompose) {
            expansion
                .addContains()
                .setCode(code.code)
                .setSystem(code.system)
                .setVersion(code.version)
                .setDisplay(code.display)
        }
        this.valueSet.expansion = expansion
    }
}
