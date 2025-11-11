package org.opencds.cqf.fhir.utility.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.operation.ParameterBinder.Type;

public class ParameterBinderTest {

    // These are simply a bunch of example method signatures to test the MethodBinder class
    class ExampleParameters {

        @Operation(name = "id")
        public void id(@IdParam IdType id) {}

        @Operation(name = "extras")
        public void id(@ExtraParams IBaseParameters parameters) {}

        @Operation(name = "resource")
        public void resource(@OperationParam(name = "resource") IBaseResource resource) {}

        @Operation(name = "encounter")
        public void encounter(@OperationParam(name = "encounter") Encounter resource) {}

        @Operation(name = "resourceRequired")
        public void resourceRequired(@OperationParam(name = "resource", min = 1) IBaseResource resource) {}

        @Operation(name = "listOfResources")
        public void listOfResources(@OperationParam(name = "resources") List<IBaseResource> resources) {}

        @Operation(name = "listOfResourcesWithMax")
        public void listOfResourcesWithMax(
                @OperationParam(name = "resources", max = 1) List<IBaseResource> resources) {}

        @Operation(name = "listOfResourcesWithMin")
        public void listOfResourcesWithMin(
                @OperationParam(name = "resources", min = 2) List<IBaseResource> resources) {}

        @Operation(name = "string")
        public void string(@OperationParam(name = "string") StringType string) {}

        @Operation(name = "stringRequired")
        public void stringRequired(@OperationParam(name = "string", min = 1) StringType string) {}

        @Operation(name = "listOfStrings")
        public void listOfStrings(@OperationParam(name = "strings") List<StringType> strings) {}

        @Operation(name = "listOfStringsWithMax")
        public void listOfStringsWithMax(@OperationParam(name = "strings", max = 1) List<StringType> strings) {}

        @Operation(name = "listOfStringsWithMin")
        public void listOfStringsWithMin(@OperationParam(name = "strings", min = 2) List<StringType> strings) {}
    }

    private static List<Method> methods = Arrays.asList(ExampleParameters.class.getDeclaredMethods());

    private static Method methodByName(String name) {
        return methods.stream()
                .filter(x -> x.getName().equals(name))
                .findFirst()
                .get();
    }

    private static MethodBinder methodBinderByName(String name) {
        return new MethodBinder(methodByName(name));
    }

    private static ParameterBinder firstParameterBinderOf(String name) {
        return methodBinderByName(name).parameters().get(0);
    }

    @Test
    void resourceParameter() {
        var binder = firstParameterBinderOf("resource");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("resource", binder.name());

        // Happy path, null gives null
        var result = binder.bind(null);
        assertNull(null);

        // Happy, resource not required, not given
        var p = new Parameters();
        result = binder.bind(p);
        assertNull(null);

        // Happy, resource not required, not set
        p = new Parameters();
        p.addParameter().setName("resource");
        result = binder.bind(p);
        assertNull(null);

        // Happy, resource expected, resource given
        p = new Parameters();
        var l = new Library().setId("123");
        p.addParameter().setName("resource").setResource(l);
        result = binder.bind(p);
        assertSame(l, result);

        // Error, passed a string instead of a resource
        var p2 = new Parameters();
        p2.addParameter().setName("resource").setValue(new StringType("123"));
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p2));
        assertTrue(e.getMessage().contains("type"));

        // Error, passed a list of resources instead of a single resource
        var p3 = new Parameters();
        var part = p3.addParameter().setName("resource").setValue(new StringType("123"));
        part.addPart().setResource(new Library().setId("456"));
        part.addPart().setResource(new Library().setId("789"));
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p3));
        assertTrue(e.getMessage().contains("type"));

        // Error, passed both a resource and a string
        var p4 = new Parameters();
        var part2 = p4.addParameter().setName("resource").setValue(new StringType("123"));
        part2.setResource(new Library().setId("456"));
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p4));
        assertTrue(e.getMessage().contains("both"));
    }

    @Test
    void encounter() {
        var binder = firstParameterBinderOf("encounter");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("encounter", binder.name());

        // Happy, resource expected, resource given
        var p = new Parameters();
        var enc = new Encounter().setId("123");
        p.addParameter().setName("encounter").setResource(enc);
        var result = binder.bind(p);
        assertSame(enc, result);

        // Error, passed a Library instead of an Encounter
        var p2 = new Parameters();
        p2.addParameter().setName("encounter").setResource(new Library().setId("123"));
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p2));
        assertTrue(e.getMessage().contains("type"));
    }

    @Test
    void resourceRequired() {
        var binder = firstParameterBinderOf("resourceRequired");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("resource", binder.name());

        // Happy path, resource given
        var p = new Parameters();
        var l = new Library().setId("123");
        p.addParameter().setName("resource").setResource(l);
        var result = binder.bind(p);
        assertSame(l, result);

        // Error, no parameter given
        var p2 = new Parameters();
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p2));
        assertTrue(e.getMessage().contains("required"));

        // Error, no resource given
        var p3 = new Parameters();
        p3.addParameter().setName("resource");
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p3));
        assertTrue(e.getMessage().contains("required"));

        // Error, nothing given
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(null));
        assertTrue(e.getMessage().contains("required"));
    }

    @Test
    void stringParameter() {
        var binder = firstParameterBinderOf("string");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("string", binder.name());

        // Happy path, null gives null
        var result = binder.bind(null);
        assertNull(null);

        // Happy, value not required, not given
        var p = new Parameters();
        result = binder.bind(p);
        assertNull(null);

        // Happy, value not required, not set
        p = new Parameters();
        p.addParameter().setName("string");
        result = binder.bind(p);
        assertNull(null);

        // Happy, value type expected, value given
        var s = new StringType("123");
        p = new Parameters();
        p.addParameter().setName("string").setValue(s);
        result = binder.bind(p);
        assertSame(s, result);

        // Error, passed a resource instead of a string
        var p2 = new Parameters();
        p2.addParameter().setName("string").setResource(new Library().setId("123"));
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p2));
        assertTrue(e.getMessage().contains("type"));

        // Error, passed a list of strings instead of a single string
        var p3 = new Parameters();
        var part = p3.addParameter().setName("string");
        part.addPart().setValue(new StringType("123"));
        part.addPart().setValue(new StringType("456"));
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p3));
        assertTrue(e.getMessage().contains("type"));

        // Error, passed both a resource and a string
        var p4 = new Parameters();
        var part2 = p4.addParameter().setName("string").setValue(new StringType("123"));
        part2.setResource(new Library().setId("456"));
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p4));
        assertTrue(e.getMessage().contains("both"));

        // Error, passed a date instead of a string
        var p5 = new Parameters();
        p5.addParameter().setName("string").setValue(new DateType("2020-01-01"));
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p5));
        assertTrue(e.getMessage().contains("type"));
    }

    @Test
    void listOfResources() {
        var binder = firstParameterBinderOf("listOfResources");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("resources", binder.name());

        // Happy, list of Resources expected, list of Resources given
        var p = new Parameters();
        var part = p.addParameter().setName("resources");
        part.addPart().setResource(new Library().setId("456"));
        part.addPart().setResource(new Library().setId("789"));
        var result = binder.bind(p);

        assertInstanceOf(List.class, result);
        assertEquals(2, ((List<?>) result).size());

        // Error, empty part
        var p2 = new Parameters();
        var part2 = p2.addParameter().setName("resources");
        part2.addPart();
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p2));
        assertTrue(e.getMessage().contains("empty"));
    }

    @Test
    void listOfResourcesWithMax() {
        var binder = firstParameterBinderOf("listOfResourcesWithMax");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("resources", binder.name());

        // Happy, list of Resources expected, list of Resources given
        var p = new Parameters();
        var part = p.addParameter().setName("resources");
        part.addPart().setResource(new Library().setId("456"));
        part.addPart().setResource(new Library().setId("789"));
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p));
        assertTrue(e.getMessage().contains("max"));
    }

    @Test
    void listOfResourcesWithMin() {
        var binder = firstParameterBinderOf("listOfResourcesWithMin");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("resources", binder.name());

        // Happy, list of Resources expected, list of Resources given
        var p = new Parameters();
        var part = p.addParameter().setName("resources");
        part.addPart().setResource(new Library().setId("456"));
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p));
        assertTrue(e.getMessage().contains("min"));
    }

    @Test
    void stringRequired() {
        var binder = firstParameterBinderOf("stringRequired");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("string", binder.name());

        // Happy path, string given
        var p = new Parameters();
        var s = new StringType("123");
        p.addParameter().setName("string").setValue(s);
        var result = binder.bind(p);
        assertSame(s, result);

        // Error, no parameter given
        var p2 = new Parameters();
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p2));
        assertTrue(e.getMessage().contains("required"));

        // Error, no string given
        var p3 = new Parameters();
        p3.addParameter().setName("string");
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p3));
        assertTrue(e.getMessage().contains("required"));

        // Error, nothing given
        e = assertThrows(IllegalArgumentException.class, () -> binder.bind(null));
        assertTrue(e.getMessage().contains("required"));
    }

    @Test
    void listOfStrings() {
        var binder = firstParameterBinderOf("listOfStrings");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("strings", binder.name());

        // Happy, list of Resources expected, list of Resources given
        var p = new Parameters();
        var part = p.addParameter().setName("strings");
        part.addPart().setValue(new StringType("456"));
        part.addPart().setValue(new StringType("456"));
        var result = binder.bind(p);

        assertInstanceOf(List.class, result);
        assertEquals(2, ((List<?>) result).size());

        // Error, empty part
        var p2 = new Parameters();
        var part2 = p2.addParameter().setName("strings");
        part2.addPart();
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p2));
        assertTrue(e.getMessage().contains("empty"));
    }

    @Test
    void listOfStringsWithMax() {
        var binder = firstParameterBinderOf("listOfStringsWithMax");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("strings", binder.name());

        // Happy, list of Resources expected, list of Resources given
        var p = new Parameters();
        var part = p.addParameter().setName("strings");
        part.addPart().setValue(new StringType("123"));
        part.addPart().setValue(new StringType("456"));
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p));
        assertTrue(e.getMessage().contains("max"));
    }

    @Test
    void listOfStringsWithMin() {
        var binder = firstParameterBinderOf("listOfStringsWithMin");
        assertEquals(Type.OPERATION, binder.type());
        assertEquals("strings", binder.name());

        // Happy, list of Resources expected, list of Resources given
        var p = new Parameters();
        var part = p.addParameter().setName("strings");
        part.addPart().setValue(new StringType("123"));
        var e = assertThrows(IllegalArgumentException.class, () -> binder.bind(p));
        assertTrue(e.getMessage().contains("min"));
    }
}
