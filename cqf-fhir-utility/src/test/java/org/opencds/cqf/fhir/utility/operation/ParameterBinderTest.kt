package org.opencds.cqf.fhir.utility.operation

import ca.uhn.fhir.rest.annotation.IdParam
import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.annotation.OperationParam
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.lang.reflect.Method

@Suppress("unused", "UNUSED_PARAMETER") // The methods below are used via reflection
class ParameterBinderTest {
    // These are simply a bunch of example method signatures to test the MethodBinder class
    internal class ExampleParameters {
        @Operation(name = "id")
        fun id(@IdParam id: IdType?) {
        }

        @Operation(name = "extras")
        fun id(@ExtraParams parameters: IBaseParameters?) {
        }

        @Operation(name = "resource")
        fun resource(@OperationParam(name = "resource") resource: IBaseResource?) {
        }

        @Operation(name = "encounter")
        fun encounter(@OperationParam(name = "encounter") resource: Encounter?) {
        }

        @Operation(name = "resourceRequired")
        fun resourceRequired(@OperationParam(name = "resource", min = 1) resource: IBaseResource?) {
        }

        @Operation(name = "listOfResources")
        fun listOfResources(@OperationParam(name = "resources") resources: List<IBaseResource?>?) {
        }

        @Operation(name = "listOfResourcesWithMax")
        fun listOfResourcesWithMax(
            @OperationParam(name = "resources", max = 1) resources: List<IBaseResource?>?
        ) {
        }

        @Operation(name = "listOfResourcesWithMin")
        fun listOfResourcesWithMin(
            @OperationParam(name = "resources", min = 2) resources: List<IBaseResource?>?
        ) {
        }

        @Operation(name = "string")
        fun string(@OperationParam(name = "string") string: StringType?) {
        }

        @Operation(name = "stringRequired")
        fun stringRequired(@OperationParam(name = "string", min = 1) string: StringType?) {
        }

        @Operation(name = "listOfStrings")
        fun listOfStrings(@OperationParam(name = "strings") strings: List<StringType?>?) {
        }

        @Operation(name = "listOfStringsWithMax")
        fun listOfStringsWithMax(@OperationParam(name = "strings", max = 1) strings: List<StringType?>?) {
        }

        @Operation(name = "listOfStringsWithMin")
        fun listOfStringsWithMin(@OperationParam(name = "strings", min = 2) strings: List<StringType?>?) {
        }
    }

    @Test
    fun resourceParameter() {
        val binder = firstParameterBinderOf("resource")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("resource", binder.name())

        // Happy path, null gives null
        var result = binder.bind(null)
        Assert.assertNull(result)

        // Happy, resource not required, not given
        var p = Parameters()
        result = binder.bind(p)
        Assert.assertNull(result)

        // Happy, resource not required, not set
        p = Parameters()
        p.addParameter().setName("resource")
        result = binder.bind(p)
        Assert.assertNull(result)

        // Happy, resource expected, resource given
        p = Parameters()
        val l = Library().setId("123")
        p.addParameter().setName("resource").setResource(l)
        result = binder.bind(p)
        Assert.assertSame(l, result)

        // Error, passed a string instead of a resource
        val p2 = Parameters()
        p2.addParameter().setName("resource").setValue(StringType("123"))
        var e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p2) }
        Assert.assertTrue(e.message!!.contains("type"))

        // Error, passed a list of resources instead of a single resource
        val p3 = Parameters()
        val part = p3.addParameter().setName("resource").setValue(StringType("123"))
        part.addPart().setResource(Library().setId("456"))
        part.addPart().setResource(Library().setId("789"))
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(p3) }
        Assert.assertTrue(e.message!!.contains("type"))

        // Error, passed both a resource and a string
        val p4 = Parameters()
        val part2 = p4.addParameter().setName("resource").setValue(StringType("123"))
        part2.setResource(Library().setId("456"))
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(p4) }
        Assert.assertTrue(e.message!!.contains("both"))
    }

    @Test
    fun encounter() {
        val binder = firstParameterBinderOf("encounter")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("encounter", binder.name())

        // Happy, resource expected, resource given
        val p = Parameters()
        val enc = Encounter().setId("123")
        p.addParameter().setName("encounter").setResource(enc)
        val result = binder.bind(p)
        Assert.assertSame(enc, result)

        // Error, passed a Library instead of an Encounter
        val p2 = Parameters()
        p2.addParameter().setName("encounter").setResource(Library().setId("123"))
        val e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p2) }
        Assert.assertTrue(e.message!!.contains("type"))
    }

    @Test
    fun resourceRequired() {
        val binder = firstParameterBinderOf("resourceRequired")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("resource", binder.name())

        // Happy path, resource given
        val p = Parameters()
        val l = Library().setId("123")
        p.addParameter().setName("resource").setResource(l)
        val result = binder.bind(p)
        Assert.assertSame(l, result)

        // Error, no parameter given
        val p2 = Parameters()
        var e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p2) }
        Assert.assertTrue(e.message!!.contains("required"))

        // Error, no resource given
        val p3 = Parameters()
        p3.addParameter().setName("resource")
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(p3) }
        Assert.assertTrue(e.message!!.contains("required"))

        // Error, nothing given
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(null) }
        Assert.assertTrue(e.message!!.contains("required"))
    }

    @Test
    fun stringParameter() {
        val binder = firstParameterBinderOf("string")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("string", binder.name())

        // Happy path, null gives null
        var result = binder.bind(null)
        Assert.assertNull(result)

        // Happy, value not required, not given
        var p = Parameters()
        result = binder.bind(p)
        Assert.assertNull(result)

        // Happy, value not required, not set
        p = Parameters()
        p.addParameter().setName("string")
        result = binder.bind(p)
        Assert.assertNull(result)

        // Happy, value type expected, value given
        val s = StringType("123")
        p = Parameters()
        p.addParameter().setName("string").setValue(s)
        result = binder.bind(p)
        Assert.assertSame(s, result)

        // Error, passed a resource instead of a string
        val p2 = Parameters()
        p2.addParameter().setName("string").setResource(Library().setId("123"))
        var e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p2) }
        Assert.assertTrue(e.message!!.contains("type"))

        // Error, passed a list of strings instead of a single string
        val p3 = Parameters()
        val part = p3.addParameter().setName("string")
        part.addPart().setValue(StringType("123"))
        part.addPart().setValue(StringType("456"))
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(p3) }
        Assert.assertTrue(e.message!!.contains("type"))

        // Error, passed both a resource and a string
        val p4 = Parameters()
        val part2 = p4.addParameter().setName("string").setValue(StringType("123"))
        part2.setResource(Library().setId("456"))
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(p4) }
        Assert.assertTrue(e.message!!.contains("both"))

        // Error, passed a date instead of a string
        val p5 = Parameters()
        p5.addParameter().setName("string").setValue(DateType("2020-01-01"))
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(p5) }
        Assert.assertTrue(e.message!!.contains("type"))
    }

    @Test
    fun listOfResources() {
        val binder = firstParameterBinderOf("listOfResources")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("resources", binder.name())

        // Happy, list of Resources expected, list of Resources given
        val p = Parameters()
        val part = p.addParameter().setName("resources")
        part.addPart().setResource(Library().setId("456"))
        part.addPart().setResource(Library().setId("789"))
        val result = binder.bind(p)

        Assertions.assertInstanceOf(List::class.java, result)
        Assert.assertEquals(2, (result as List<*>?)!!.size.toLong())

        // Error, empty part
        val p2 = Parameters()
        val part2 = p2.addParameter().setName("resources")
        part2.addPart()
        val e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p2) }
        Assert.assertTrue(e.message!!.contains("empty"))
    }

    @Test
    fun listOfResourcesWithMax() {
        val binder = firstParameterBinderOf("listOfResourcesWithMax")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("resources", binder.name())

        // Happy, list of Resources expected, list of Resources given
        val p = Parameters()
        val part = p.addParameter().setName("resources")
        part.addPart().setResource(Library().setId("456"))
        part.addPart().setResource(Library().setId("789"))
        val e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p) }
        Assert.assertTrue(e.message!!.contains("max"))
    }

    @Test
    fun listOfResourcesWithMin() {
        val binder = firstParameterBinderOf("listOfResourcesWithMin")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("resources", binder.name())

        // Happy, list of Resources expected, list of Resources given
        val p = Parameters()
        val part = p.addParameter().setName("resources")
        part.addPart().setResource(Library().setId("456"))
        val e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p) }
        Assert.assertTrue(e.message!!.contains("min"))
    }

    @Test
    fun stringRequired() {
        val binder = firstParameterBinderOf("stringRequired")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("string", binder.name())

        // Happy path, string given
        val p = Parameters()
        val s = StringType("123")
        p.addParameter().setName("string").setValue(s)
        val result = binder.bind(p)
        Assert.assertSame(s, result)

        // Error, no parameter given
        val p2 = Parameters()
        var e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p2) }
        Assert.assertTrue(e.message!!.contains("required"))

        // Error, no string given
        val p3 = Parameters()
        p3.addParameter().setName("string")
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(p3) }
        Assert.assertTrue(e.message!!.contains("required"))

        // Error, nothing given
        e = Assert.assertThrows(IllegalArgumentException::class.java) { binder.bind(null) }
        Assert.assertTrue(e.message!!.contains("required"))
    }

    @Test
    fun listOfStrings() {
        val binder = firstParameterBinderOf("listOfStrings")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("strings", binder.name())

        // Happy, list of Resources expected, list of Resources given
        val p = Parameters()
        val part = p.addParameter().setName("strings")
        part.addPart().setValue(StringType("456"))
        part.addPart().setValue(StringType("456"))
        val result = binder.bind(p)

        Assertions.assertInstanceOf(List::class.java, result)
        Assert.assertEquals(2, (result as List<*>?)!!.size.toLong())

        // Error, empty part
        val p2 = Parameters()
        val part2 = p2.addParameter().setName("strings")
        part2.addPart()
        val e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p2) }
        Assert.assertTrue(e.message!!.contains("empty"))
    }

    @Test
    fun listOfStringsWithMax() {
        val binder = firstParameterBinderOf("listOfStringsWithMax")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("strings", binder.name())

        // Happy, list of Resources expected, list of Resources given
        val p = Parameters()
        val part = p.addParameter().setName("strings")
        part.addPart().setValue(StringType("123"))
        part.addPart().setValue(StringType("456"))
        val e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p) }
        Assert.assertTrue(e.message!!.contains("max"))
    }

    @Test
    fun listOfStringsWithMin() {
        val binder = firstParameterBinderOf("listOfStringsWithMin")
        Assert.assertEquals(ParameterBinder.Type.OPERATION, binder.type())
        Assert.assertEquals("strings", binder.name())

        // Happy, list of Resources expected, list of Resources given
        val p = Parameters()
        val part = p.addParameter().setName("strings")
        part.addPart().setValue(StringType("123"))
        val e = Assert.assertThrows(
            IllegalArgumentException::class.java
        ) { binder.bind(p) }
        Assert.assertTrue(e.message!!.contains("min"))
    }

    companion object {
        private val methods: List<Method> = listOf(*ExampleParameters::class.java.declaredMethods)

        private fun methodByName(name: String): Method {
            return methods.stream()
                .filter { x: Method -> x.name == name }
                .findFirst()
                .orElseThrow()
        }

        private fun methodBinderByName(name: String): MethodBinder {
            return MethodBinder(methodByName(name))
        }

        private fun firstParameterBinderOf(name: String): ParameterBinder {
            return methodBinderByName(name).parameters[0]
        }
    }
}
