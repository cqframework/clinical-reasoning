package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.opencds.cqf.fhir.utility.adapter.IAdapter.Companion.newStringType
import org.opencds.cqf.fhir.utility.adapter.IAdapter.Companion.newUrlType

interface IEndpointAdapter : IResourceAdapter {
    var address: String?
        get() = resolvePathString(get(), "address")
        set(address) {
            setValue(get(), "address", newUrlType(fhirContext()!!.version.version, address))
        }

    fun hasHeaders(): Boolean {
        return this.headers.isNotEmpty()
    }

    var headers: MutableList<String?>
        get() =
            resolvePathList(get(), "header")
                .map { header -> (header as IPrimitiveType<*>).valueAsString }
                .toMutableList()
        set(headers) {
            val mappedHeaders =
                if (headers.isEmpty()) null
                else
                    headers
                        .map { header ->
                            newStringType<IPrimitiveType<String?>>(
                                fhirContext()!!.version.version,
                                header,
                            )
                        }
                        .toList()
            setValue(get(), "header", mappedHeaders)
        }

    fun addHeader(header: String?) {
        val headers = this.headers
        headers.add(header)
        this.headers = headers
    }
}
