package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.model.api.annotation.Description
import ca.uhn.fhir.rest.annotation.Operation
import java.lang.reflect.Method
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Resources
import org.opencds.cqf.fhir.utility.operation.ParameterBinder.Companion.validate
import org.opencds.cqf.fhir.utility.operation.ParameterBinder.Type

class MethodBinder(val method: Method) {
    val operation: Operation =
        requireNotNull(method.getAnnotation(Operation::class.java)) {
            "Method must be annotated with @Operation"
        }
    val description: Description = method.getAnnotation(Description::class.java) ?: Description()
    val name: String = operation.name.normalized()
    val typeName: String = typeNameFrom(operation)
    val canonicalUrl: String
        get() = operation.canonicalUrl

    internal val parameters: List<ParameterBinder> =
        validate(method.parameters.map { ParameterBinder.from(it) })
    internal val scope: Scope = determineScope(parameters, typeName)

    private fun args(id: IIdType?, parameters: IBaseParameters?): List<Any?> {
        require(!(scope != Scope.INSTANCE && id != null)) {
            "id not supported on non-instance operation"
        }
        require(!(scope == Scope.INSTANCE && id == null)) { "id required for instance operation" }

        val args = mutableListOf<Any?>()
        var startIndex = 0

        id?.let {
            args.add(it)
            startIndex++
        }

        val cloned = parameters?.let { Resources.clone(it) }

        for (i in startIndex..<this.parameters.size) {
            args.add(this.parameters[i].bind(cloned))
        }

        if (cloned != null && !cloned.isEmpty) {
            throw IllegalArgumentException(
                "Parameters were not bound to @Operation invocation: ${Resources.stringify(cloned)}"
            )
        }

        return args
    }

    fun bind(provider: Any, id: IIdType?, parameters: IBaseParameters?): () -> IBaseResource? {
        val args = args(id, parameters)
        return { method.invoke(provider, *args.toTypedArray()) as IBaseResource? }
    }

    companion object {
        private fun String.normalized(): String =
            if (this.startsWith("$")) this.substring(1) else this

        private fun determineScope(
            parameterBinders: List<ParameterBinder>,
            typeName: String,
        ): Scope {
            return when {
                parameterBinders.any { it.type() == Type.ID } -> Scope.INSTANCE
                typeName.isNotEmpty() -> Scope.TYPE
                else -> Scope.SERVER
            }
        }

        private fun typeNameFrom(operation: Operation): String {
            return if (operation.type != IBaseResource::class) {
                operation.type.simpleName!!
            } else {
                operation.typeName
            }
        }
    }
}
