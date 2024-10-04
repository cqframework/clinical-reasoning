package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.model.api.annotation.Description
import ca.uhn.fhir.rest.annotation.IdParam
import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.annotation.OperationParam
import java.lang.reflect.Method
import org.hl7.fhir.dstu3.model.Measure
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@Suppress("unused", "UNUSED_PARAMETER")
class MethodBinderTest {
    // These are simply a bunch of example method signatures to test the MethodBinder class
    internal inner class ExampleMethods {
        fun noAnnotation() {}

        @Operation(name = "idFirst") fun idFirst(@IdParam id: IdType?) {}

        @Operation(name = "idLast")
        fun idLast(@OperationParam(name = "param") param: StringType?, @IdParam id: IdType?) {}

        @Operation(name = "noId") fun noId(@OperationParam(name = "param") param: StringType?) {}

        @Operation(name = "manyIds") fun manyIds(@IdParam id: IdType?, @IdParam id2: IdType?) {}

        @Operation(name = "idIsNotIdType") fun idIsNotIdType(@IdParam id: StringType?) {}

        @Operation(name = "extraFirst")
        fun extraFirst(
            @ExtraParams extras: IBaseParameters?,
            @OperationParam(name = "param") param: String?,
        ) {}

        @Operation(name = "extraLast")
        fun extraLast(
            @OperationParam(name = "param") param: StringType?,
            @ExtraParams parameters: IBaseParameters?,
        ) {}

        @Operation(name = "manyExtra")
        fun manyExtra(
            @ExtraParams parameters: IBaseParameters?,
            @ExtraParams parameters2: IBaseParameters?,
        ) {}

        @Operation(name = "extraIsNotParametersType")
        fun extraIsNotParametersType(@ExtraParams idType: IdType?) {}

        @Operation(name = "operationParamIsNotFhirType")
        fun operationParamIsNotFhirType(@OperationParam(name = "stringType") stringType: String?) {}

        @Description("has a description")
        @Operation(name = "hasDescription")
        fun hasDescription(@IdParam id: IdType?) {}

        @Operation(name = "hasCanonical", canonicalUrl = "http://example.com")
        fun hasCanonical(@IdParam id: IdType?) {}

        @Operation(name = "hasTypeName", typeName = "Measure")
        fun hasTypeName(@OperationParam(name = "stringType") stringType: StringType?) {}

        @Operation(name = "hasType", type = Measure::class)
        fun hasType(@OperationParam(name = "stringType") stringType: StringType?) {}

        @Operation(name = "noArgs") fun noArgs() {}

        @Operation(name = "\$nameNormalizes") fun nameNormalizes() {}
    }

    @Test
    fun noAnnotation_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            MethodBinder(methodByName("noAnnotation"))
        }
    }

    @Test
    fun idFirst() {
        val m = methodBinderByName("idFirst")
        assertEquals(Scope.INSTANCE, m.scope)
        assertEquals("idFirst", m.name)
        assertEquals(1, m.parameters.size.toLong())
    }

    @Test
    fun idLast_throws() {
        assertThrows(IllegalArgumentException::class.java) { methodBinderByName("idLast") }
    }

    @Test
    fun manyIds_throws() {
        assertThrows(IllegalArgumentException::class.java) { methodBinderByName("manyIds") }
    }

    @Test
    fun idParamNotIdType_throws() {
        assertThrows(IllegalArgumentException::class.java) { methodBinderByName("idIsNotIdType") }
    }

    @Test
    fun noId() {
        val m = methodBinderByName("noId")
        assertEquals(Scope.SERVER, m.scope)
        assertEquals("noId", m.name)
        assertEquals(1, m.parameters.size.toLong())
    }

    @Test
    fun extraFirst_throws() {
        assertThrows(IllegalArgumentException::class.java) { methodBinderByName("extraFirst") }
    }

    @Test
    fun extraLast() {
        val m = methodBinderByName("extraLast")
        assertEquals(Scope.SERVER, m.scope)
        assertEquals("extraLast", m.name)
        assertEquals(2, m.parameters.size.toLong())
    }

    @Test
    fun manyExtra_throws() {
        assertThrows(IllegalArgumentException::class.java) { methodBinderByName("manyExtra") }
    }

    @Test
    fun extraIsNotParametersType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            methodBinderByName("extraIsNotParametersType")
        }
    }

    @Test
    fun operationParamIsNotFhirType_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            methodBinderByName("operationParamIsNotFhirType")
        }
    }

    @Test
    fun binderGetsDescription() {
        val m = methodBinderByName("hasDescription")
        assertEquals("has a description", m.description.value)
    }

    @Test
    fun binderGetsCanonical() {
        val m = methodBinderByName("hasCanonical")
        assertEquals("http://example.com", m.canonicalUrl)
    }

    @Test
    fun binderGetsMethod() {
        val m = methodBinderByName("idFirst")
        Assert.assertNotNull(m.method)
        assertEquals(m.method.name, "idFirst")
    }

    @Test
    fun binderGetsOperationAnnotation() {
        val m = methodBinderByName("idFirst")
        Assert.assertNotNull(m.operation)
        assertEquals(m.operation.name, "idFirst")
    }

    @Test
    fun hasType() {
        val m = methodBinderByName("hasType")
        assertEquals("Measure", m.typeName)
        assertEquals(Scope.TYPE, m.scope)
    }

    @Test
    fun hasTypeName() {
        val m = methodBinderByName("hasTypeName")
        assertEquals("Measure", m.typeName)
        assertEquals(Scope.TYPE, m.scope)
    }

    @Test
    fun noArgs() {
        val m = methodBinderByName("noArgs")
        assertEquals(Scope.SERVER, m.scope)
    }

    @Test
    fun nameNormalizes() {
        val m = methodBinderByName("nameNormalizes")
        assertEquals("nameNormalizes", m.name)
    }

    @Test
    fun unboundArgs_throws() {
        val m = methodBinderByName("noArgs")

        val provider = ExampleMethods()
        val parameters = Parameters().addParameter("anyParam", StringType("anyValue"))
        val e =
            assertThrows(IllegalArgumentException::class.java) {
                m.bind(provider, null, parameters)
            }
        Assert.assertTrue(e.message!!.contains("not bound"))
    }

    @Test
    fun idWithoutInstanceScope_throws() {
        val m = methodBinderByName("noArgs")

        val provider = ExampleMethods()
        val id = IdType("Library/123")
        val e = assertThrows(IllegalArgumentException::class.java) { m.bind(provider, id, null) }
        Assert.assertTrue(e.message!!.contains("non-instance"))
    }

    @Test
    fun instanceScopeWithoutId_throws() {
        val m = methodBinderByName("idFirst")

        val provider = ExampleMethods()
        val e = assertThrows(IllegalArgumentException::class.java) { m.bind(provider, null, null) }
        Assert.assertTrue(e.message!!.contains("id required"))
    }

    @Test
    fun missingOperationParamAnnotation_throws() {
        class MissingOperationAnnotation {
            @Operation(name = "missingOperationParam")
            fun missingOperationParam(param: StringType?): IBaseResource? {
                return null
            }
        }

        val method = MissingOperationAnnotation::class.java.declaredMethods[0]
        val e = assertThrows(IllegalArgumentException::class.java) { MethodBinder(method) }
        Assert.assertTrue(e.message!!.contains("must be annotated"))
    }

    @Test
    fun conflictingAnnotations_throws() {
        class ConflictingAnnotation {
            @Operation(name = "function")
            fun function(@IdParam @OperationParam(name = "id") id: IdType?): IBaseResource? {
                return null
            }
        }

        val method = ConflictingAnnotation::class.java.declaredMethods[0]
        val e = assertThrows(IllegalArgumentException::class.java) { MethodBinder(method) }
        Assert.assertTrue(e.message!!.contains("one of"))
    }

    @Test
    fun missingOperationAnnotation_throws() {
        class MissingOperationAnnotation {
            fun function(@IdParam id: IdType?): IBaseResource? {
                return null
            }
        }

        val method = MissingOperationAnnotation::class.java.declaredMethods[0]
        val e = assertThrows(IllegalArgumentException::class.java) { MethodBinder(method) }
        Assert.assertTrue(e.message!!.contains("must be annotated"))
    }

    companion object {
        private val methods: List<Method> =
            listOf(*ExampleMethods::class.java.declaredMethods)

        private fun methodByName(name: String): Method {
            return methods.stream().filter { x: Method -> x.name == name }.findFirst().get()
        }

        private fun methodBinderByName(name: String): MethodBinder {
            return MethodBinder(methodByName(name))
        }
    }
}
