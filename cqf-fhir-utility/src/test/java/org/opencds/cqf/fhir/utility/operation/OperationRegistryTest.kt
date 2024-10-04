package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.annotation.OperationParam
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException
import org.hl7.fhir.dstu2.model.IdType
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.opencds.cqf.fhir.api.Repository
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository
import java.util.*
import java.util.stream.Collectors

@Suppress("unused", "UNUSED_PARAMETERS")
internal class OperationRegistryTest {
    @Test
    fun noAnnotatedMethod_throws() {
        class MissingAnnotationExample {
            fun noAnnotation() {
            }
        }

        val operationRegistry = OperationRegistry()
        Assert.assertThrows(
            IllegalArgumentException::class.java
        ) {
            operationRegistry.register(
                MissingAnnotationExample::class.java) { _ -> MissingAnnotationExample() }
        }
    }

    @Test
    fun noMatchingOperationByName_throws() {
        val registry = OperationRegistry()
        // TODO: Need to think about this. This is a suboptimal API experience because
        // we don't actually check for the operation name when we start
        // the build process. We only check for it when we try to execute it.
        // Whether an operation invocation is scoped by instance, type, or server
        // depends on the parameters passed to the operation invocation. We don't
        // have all those parameters until we try to execute the operation.
        val ctx = registry.buildInvocationContext(repo, "nonExistentOperation")
        val e = Assert.assertThrows(InvalidRequestException::class.java) { ctx.execute() }
        Assertions.assertTrue(e.message!!.contains("name"))
    }

    @Test
    fun mismatchedScope_throws() {
        // Server scoped operation, down below we'll try to execute it as an instance scoped operation

        class ServerScopedExample {
            @Operation(name = "serverScoped")
            fun serverScoped() {
            }
        }

        val ctx = constructAndRegister(
            ServerScopedExample::class.java
        ).buildInvocationContext(repo, "serverScoped")
        ctx.id(IdType("Library/123"))
        val e = Assert.assertThrows(InvalidRequestException::class.java) { ctx.execute() }
        Assertions.assertTrue(e.message!!.lowercase(Locale.getDefault()).contains("scope"))
    }

    @Test
    fun mismatchedType_throws() {
        // Type scoped operation
        class TypedScopeExample {
            @Operation(name = "typeScoped", typeName = "Measure")
            fun typeScoped() {
            }
        }

        val ctx = constructAndRegister(
            TypedScopeExample::class.java
        ).buildInvocationContext(repo, "typeScoped")
        ctx.resourceType(Library::class.java)
        val e = Assert.assertThrows(InvalidRequestException::class.java) { ctx.execute() }
        Assertions.assertTrue(e.message!!.lowercase(Locale.getDefault()).contains("type"))
    }

    @Test
    fun ambiguousOperations_throws() {
        class AmbiguousExample {
            @Operation(name = "ambiguous", typeName = "Library")
            fun ambiguous1() {
            }

            @Operation(name = "ambiguous", typeName = "Library")
            fun ambiguous2() {
            }
        }

        val ctx = constructAndRegister(
            AmbiguousExample::class.java
        ).buildInvocationContext(repo, "ambiguous")
        ctx.resourceType(Library::class.java)
        val e = Assert.assertThrows(IllegalStateException::class.java) { ctx.execute() }
        Assertions.assertTrue(e.message!!.lowercase(Locale.getDefault()).contains("multiple"))
    }

    @Test
    fun parametersWithResource() {
        class GetIdFromResource {
            @Operation(name = "get-id")
            fun getId(@OperationParam(name = "resource") resource: IBaseResource): IBaseParameters {
                return Parameters()
                    .addParameter(
                        "result", StringType(resource.idElement.idPart)
                    )
            }
        }

        val registry = constructAndRegister(
            GetIdFromResource::class.java
        ) { _: Repository? -> GetIdFromResource() }

        var arguments = Parameters()
        arguments.addParameter().setName("resource").setResource(Library().setId("123"))

        val op = registry.buildInvocationContext(repo, "get-id").parameters(arguments)
        val result = Assertions.assertDoesNotThrow<IBaseResource> { op.execute() }

        val p = Assertions.assertInstanceOf(Parameters::class.java, result)
        val id = Assertions.assertInstanceOf(StringType::class.java, p.getParameter("result").value)
            .value
        Assertions.assertEquals("123", id)

        // Ensure that multiple parameters of the same name are not allowed
        arguments = Parameters()
        arguments.addParameter().setName("resource").setResource(Library().setId("123"))
        arguments.addParameter().setName("resource").setResource(Library().setId("456"))

        val op2 = registry.buildInvocationContext(repo, "get-id").parameters(arguments)

        var e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { op2.execute() }
        Assertions.assertTrue(e.message!!.contains("parts"))

        // Parameters with wrong type should fail
        arguments = Parameters()
        arguments.addParameter().setName("resource").setValue(StringType("123"))

        val op3 = registry.buildInvocationContext(repo, "get-id").parameters(arguments)
        e = Assert.assertThrows(IllegalArgumentException::class.java) { op3.execute() }
        Assertions.assertTrue(e.message!!.contains("type"))
    }

    @Test
    fun nameNormalization() {
        class NameNormalizationExample {
            @Operation(name = "\$withAmpersand")
            fun withAmpersand(): IBaseResource? {
                return null
            }

            @Operation(name = "noAmpersand")
            fun noAmpersand(): IBaseResource? {
                return null
            }
        }

        val registry = constructAndRegister(
            NameNormalizationExample::class.java
        ) { _: Repository? -> NameNormalizationExample() }
        val op = registry.buildInvocationContext(repo, "\$withAmpersand")
        Assertions.assertDoesNotThrow<IBaseResource> { op.execute() }

        val op2 = registry.buildInvocationContext(repo, "withAmpersand")
        Assertions.assertDoesNotThrow<IBaseResource> { op2.execute() }

        val op3 = registry.buildInvocationContext(repo, "\$noAmpersand")
        Assertions.assertDoesNotThrow<IBaseResource> { op3.execute() }

        val op4 = registry.buildInvocationContext(repo, "noAmpersand")
        Assertions.assertDoesNotThrow<IBaseResource> { op4.execute() }
    }

    @Test
    fun parametersWithList() {
        class GetIdFromResources {
            @Operation(name = "get-id")
            fun getId(@OperationParam(name = "resources") resources: List<IBaseResource>): IBaseParameters {
                val ids = resources.stream()
                    .map { r: IBaseResource -> r.idElement.idPart }
                    .collect(Collectors.toList())

                val params = Parameters()
                for (id in ids) {
                    params.addParameter("result", StringType(id))
                }

                return params
            }
        }

        val arguments = Parameters()
        val resourceParam = arguments.addParameter().setName("resources")
        resourceParam.addPart().setResource(Library().setId("123"))
        resourceParam.addPart().setResource(Measure().setId("456"))

        val op = constructAndRegister(
            GetIdFromResources::class.java
        ) { _: Repository? -> GetIdFromResources() }
            .buildInvocationContext(repo, "get-id")
            .parameters(arguments)
        val result = Assertions.assertDoesNotThrow<IBaseResource> { op.execute() }

        val p = Assertions.assertInstanceOf(Parameters::class.java, result)
        val results = p.getParameters("result")
        Assertions.assertEquals(2, results.size)
        Assertions.assertEquals(
            "123",
            Assertions.assertInstanceOf(StringType::class.java, results[0].value).value
        )
        Assertions.assertEquals(
            "456",
            Assertions.assertInstanceOf(StringType::class.java, results[1].value).value
        )
    }

    companion object {
        // Dummy repository for testing... Doesn't actually do anything in these tests
        private val repo: Repository = InMemoryFhirRepository(FhirContext.forR4Cached())

        private fun <T : Any> constructAndRegister(clazz: Class<T>, factory: (Repository?) -> T): OperationRegistry {
            val registry = OperationRegistry()
            registry.register(clazz, factory)
            return registry
        }

        // Convenience method for when the operation provider is a no-arg constructor
        // See test below for example usage
        private fun <T : Any> constructAndRegister(clazz: Class<T>): OperationRegistry {
            return constructAndRegister(clazz) {
                _ ->
                try {
                    clazz.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}
