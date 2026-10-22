package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.model.api.annotation.Description
import ca.uhn.fhir.rest.annotation.Operation
import java.lang.reflect.Method
import java.util.concurrent.Callable
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Resources
import org.opencds.cqf.fhir.utility.Resources.clone

/*
 * This class is responsible for binding a Parameters resource to a Method annotated
 * with @Operation. Once the parameters are bound, the method can be invoked.
 *
 * Each individual parameter of the Method is bound using a ParameterBinder.
 */
class MethodBinder(private val method: Method) {
    private val operation: Operation =
        requireNotNull(method.getAnnotation(Operation::class.java)) {
            "method must be annotated with @Operation"
        }
    private val description: Description =
        method.getAnnotation(Description::class.java) ?: DEFAULT_DESCRIPTION
    private val name =
        normalizeName(requireNotNull(operation.name) { "@Operation name cannot be null" })
    private val typeName = typeNameFrom(operation)
    private val parameterBinders: MutableList<ParameterBinder> =
        ParameterBinder.Companion.validate(Companion.parameterBindersFrom(method))
    private val scope: Scope = determineScope(parameterBinders, typeName)

    // Hack to allow for a default instance for the Description annotation
    // If a given method doesn't have a Description annotation, we use this one
    @Description private class DefaultDescription

    private fun args(id: IIdType?, parameters: IBaseParameters?): MutableList<Any?> {
        require(!(this.scope != Scope.INSTANCE && id != null)) {
            "id not supported on non-instance operation"
        }
        require(!(this.scope == Scope.INSTANCE && id == null)) {
            "id required for instance operation"
        }

        val args = ArrayList<Any?>(parameterBinders.size)
        var paramIndex = 0

        // If we have an id parameter for the method
        // it's guaranteed to be the first parameter
        if (id != null) {
            args.add(id)
            paramIndex++
        }

        // The binding process consumes the parameters it binds,
        // so we need to clone the parameters to avoid modifying the original
        val cloned = if (parameters != null) clone<IBaseParameters>(parameters) else null

        while (paramIndex < parameterBinders.size) {
            args.add(parameterBinders.get(paramIndex).bind(cloned))
            paramIndex++
        }

        require(!(cloned != null && !cloned.isEmpty)) {
            "Parameters were not bound to @Operation invocation: " + Resources.stringify(cloned!!)
        }

        return args
    }

    fun operation(): Operation {
        return operation
    }

    fun name(): String {
        return name
    }

    fun typeName(): String? {
        return typeName
    }

    fun canonicalUrl(): String {
        return operation.canonicalUrl
    }

    fun method(): Method {
        return method
    }

    fun description(): String {
        return description.value
    }

    fun scope(): Scope {
        return scope
    }

    fun parameters(): MutableList<ParameterBinder> {
        return parameterBinders
    }

    /**
     * Bind the parameters to the method and return a Callable that can be used to invoke the
     * method.
     *
     * This means that the parameters of the Parameters resource and mapped to the Java method's
     * arguments
     *
     * @param provider the object that contains the method to be invoked
     * @param id the id of the resource that the operation is being invoked on
     * @param parameters the Parameters resource that contains the operation's parameters
     * @return a Callable that can be used to invoke the method
     */
    fun bind(provider: Any?, id: IIdType?, parameters: IBaseParameters?): Callable<IBaseResource?> {
        val args = args(id, parameters)
        return Callable { method.invoke(provider, *args.toTypedArray()) as IBaseResource? }
    }

    companion object {
        private val DEFAULT_DESCRIPTION: Description =
            DefaultDescription::class.java.getAnnotation(Description::class.java)

        private fun determineScope(
            parameterBinders: MutableList<ParameterBinder>,
            typeName: String?,
        ): Scope {
            val hasIdParam = parameterBinders.any { x -> x.type() == ParameterBinder.Type.ID }
            if (hasIdParam) {
                return Scope.INSTANCE
            } else if (typeName!!.isEmpty()) {
                return Scope.SERVER
            } else {
                return Scope.TYPE
            }
        }

        // Normalize the operation name to remove the $ prefix if present
        private fun normalizeName(name: String): String {
            return if (name.startsWith("$")) name.substring(1) else name
        }

        // Create a list of ParameterBinders from the method's parameters
        private fun parameterBindersFrom(method: Method): MutableList<ParameterBinder> {
            return method.parameters
                .map { parameter -> ParameterBinder.Companion.from(parameter) }
                .toMutableList()
        }

        private fun typeNameFrom(operation: Operation): String? {
            // IBaseResource is the default type for an operation, meaning the type is not specified
            if (operation.type != IBaseResource::class) {
                return operation.type.simpleName
            } else {
                return operation.typeName
            }
        }
    }
}
