package org.opencds.cqf.fhir.cql.engine.parameters

import org.opencds.cqf.cql.engine.runtime.Value

class CqlParameterDefinition
@JvmOverloads
constructor(
    @JvmField val name: String,
    val type: String?,
    val isList: Boolean?,
    @JvmField val value: Value? = null,
)
