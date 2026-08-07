package org.opencds.cqf.fhir.cql.cql2elm.content

import org.cqframework.cql.cql2elm.MimeType

enum class ModelInfoContentType(private val mimeType: String) : MimeType {
    XML("application/xml"),
    JSON("application/json");

    override fun mimeType(): String {
        return this.mimeType
    }
}
