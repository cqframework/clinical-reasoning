package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import java.util.*
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Tuple
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ITupleAdapter

class TupleAdapter(tuple: IBase) : BaseElementAdapter(FhirVersionEnum.R4, tuple), ITupleAdapter {
    init {
        require(tuple is Tuple) { "object passed as tuple argument is not a Tuple data type" }
    }

    override fun get(): Tuple? {
        return element as Tuple?
    }

    override fun getProperty(name: String?): Any? {
        return get()!!
            .children()
            .filter { c -> c!!.name == name }
            .map { p -> if (p!!.hasValues()) p.values.get(0) else null }
            .filter { obj -> Objects.nonNull(obj) }
            .firstOrNull()
    }

    override val properties: LinkedHashMap<String, Any?>
        get() {
            val properties = LinkedHashMap<String, Any?>()
            get()!!.children().forEach { c -> properties[c!!.name] = c.values }
            return properties
        }

    override fun resolvePath(target: Any?, path: String): Any? {
        // A Tuple resolves its named members directly rather than through the FHIR runtime
        // definitions used for structured elements.
        val values =
            get()!!
                .children()
                .filter { c -> c!!.name == path }
                .filter { it.hasValues() }
                .map { it.values }
                .firstOrNull()
        if (values.isNullOrEmpty()) {
            return null
        }
        return if (values.size == 1) values[0] else values
    }
}
