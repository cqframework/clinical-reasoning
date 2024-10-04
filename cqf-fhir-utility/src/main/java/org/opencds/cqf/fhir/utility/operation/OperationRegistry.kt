package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.api.Repository
import java.lang.reflect.InvocationTargetException

/**
 * That class allows registering of methods annotated with @Operation, and then can be used to execute those
 * operations by name.
 */
class OperationRegistry {
    inner class OperationInvocationParams internal constructor(val repository: Repository, val name: String) {
        private var parameters: IBaseParameters? = null
        private var id: IIdType? = null
        private var resourceType: Class<out IBaseResource>? = null

        fun id(id: IIdType?): OperationInvocationParams {
            this.id = id
            return this
        }

        fun id(): IIdType? {
            return id
        }

        fun parameters(): IBaseParameters? {
            return parameters
        }

        fun parameters(parameters: IBaseParameters?): OperationInvocationParams {
            this.parameters = parameters
            return this
        }

        fun resourceType(resourceType: Class<out IBaseResource>?): OperationInvocationParams {
            this.resourceType = resourceType
            return this
        }

        fun scope(): Scope {
            return if (id != null) {
                Scope.INSTANCE
            } else if (resourceType != null) {
                Scope.TYPE
            } else {
                Scope.SERVER
            }
        }

        fun typeName(): String {
            return if (resourceType != null) {
                resourceType!!.simpleName
            } else if (id != null) {
                id!!.resourceType
            } else {
                ""
            }
        }

        fun execute(): IBaseResource? {
            try {
                return this@OperationRegistry.execute(this)
            } catch (e: InvocationTargetException) {
                // unwrap the exception thrown by the method
                throw e.cause!!
            }
        }
    }

    private val invocationContextByName: Multimap<String, InvocationContext<*>> =
        MultimapBuilder.hashKeys().arrayListValues().build()

    /**
     * Used to register a new Operation provider. The class must have methods annotated with @Operation.
     * @param <T> The type of the class to register
     * @param clazz The class to register
     * @param factory A factory function that will create an instance of the class
    </T> */
    fun <T : Any> register(clazz: Class<T>, factory: (Repository?) -> T) {
        val methodBinders = listOf(*clazz.methods)
            .filter { it.isAnnotationPresent(Operation::class.java) }
            .map { MethodBinder(it) }

        require(methodBinders.isNotEmpty()) { "No operations found on class " + clazz.name }

        for (methodBinder in methodBinders) {
            val context = InvocationContext(factory, methodBinder)
            invocationContextByName.put(methodBinder.name, context)
        }
    }

    /**
     * Used to build an OperationInvocationParams object that can be used to execute an operation.
     * @param repository the repository to use for data access and recursive invocations
     * @param operationName the name of the operation to execute
     * @return an OperationInvocationParams object that can be used to execute the operation
     */
    fun buildInvocationContext(repository: Repository, operationName: String): OperationInvocationParams {
        return OperationInvocationParams(repository, operationName)
    }

    fun execute(params: OperationInvocationParams): IBaseResource? {
        val context = findInvocationContext(params.scope(), params.name, params.typeName())
        val instance = context.factory.apply(params.repository)
        val callable = context.methodBinder.bind(instance, params.id(), params.parameters())

        return callable.invoke()
    }

    private fun findInvocationContext(scope: Scope, name: String, typeName: String): InvocationContext<*> {
        val normalizedName = normalizeName(name)

        val contexts = invocationContextByName[normalizedName]
        if (contexts.isEmpty()) {
            throw InvalidRequestException("No operation found with name $normalizedName")
        }

        val scopedContexts = contexts.filter { it.methodBinder.scope == scope }
        if (scopedContexts.isEmpty()) {
            throw InvalidRequestException("No operation found with name $normalizedName and scope $scope")
        }

        // Only filter by type if the typeName is not empty
        val typePredicate: (InvocationContext<*>) -> Boolean =
                if (typeName.isEmpty())  { _ -> true }
                else { c -> c.methodBinder.typeName == typeName }

        val typeContexts = scopedContexts.filter(typePredicate)
        if (typeContexts.isEmpty()) {
            throw InvalidRequestException("No operation found with type $typeName")
        }

        check(typeContexts.size <= 1) { "Multiple operations found with name $normalizedName and type $typeName" }

        return typeContexts[0]
    }

    companion object {
        private fun normalizeName(name: String): String {
            return if (name.startsWith("$")) name.substring(1) else name
        }
    }
}
