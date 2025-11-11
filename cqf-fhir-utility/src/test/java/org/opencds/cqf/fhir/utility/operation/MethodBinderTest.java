package org.opencds.cqf.fhir.utility.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

public class MethodBinderTest {

    // These are simply a bunch of example method signatures to test the MethodBinder class
    class ExampleMethods {

        public void noAnnotation() {}

        @Operation(name = "idFirst")
        public void idFirst(@IdParam IdType id) {}

        @Operation(name = "idLast")
        public void idLast(@OperationParam(name = "param") StringType param, @IdParam IdType id) {}

        @Operation(name = "noId")
        public void noId(@OperationParam(name = "param") StringType param) {}

        @Operation(name = "manyIds")
        public void manyIds(@IdParam IdType id, @IdParam IdType id2) {}

        @Operation(name = "idIsNotIdType")
        public void idIsNotIdType(@IdParam StringType id) {}

        @Operation(name = "extraFirst")
        public void extraFirst(@ExtraParams IBaseParameters extras, @OperationParam(name = "param") String param) {}

        @Operation(name = "extraLast")
        public void extraLast(
                @OperationParam(name = "param") StringType param, @ExtraParams IBaseParameters parameters) {}

        @Operation(name = "manyExtra")
        public void manyExtra(@ExtraParams IBaseParameters parameters, @ExtraParams IBaseParameters parameters2) {}

        @Operation(name = "extraIsNotParametersType")
        public void extraIsNotParametersType(@ExtraParams IdType idType) {}

        @Operation(name = "operationParamIsNotFhirType")
        public void operationParamIsNotFhirType(@OperationParam(name = "stringType") String stringType) {}

        @Description("has a description")
        @Operation(name = "hasDescription")
        public void hasDescription(@IdParam IdType id) {}

        @Operation(name = "hasCanonical", canonicalUrl = "http://example.com")
        public void hasCanonical(@IdParam IdType id) {}

        @Operation(name = "hasTypeName", typeName = "Measure")
        public void hasTypeName(@OperationParam(name = "stringType") StringType stringType) {}

        @Operation(name = "hasType", type = Measure.class)
        public void hasType(@OperationParam(name = "stringType") StringType stringType) {}

        @Operation(name = "noArgs")
        public void noArgs() {}

        @Operation(name = "$nameNormalizes")
        public void nameNormalizes() {}
    }

    private static List<Method> methods = Arrays.asList(ExampleMethods.class.getDeclaredMethods());

    private static Method methodByName(String name) {
        return methods.stream()
                .filter(x -> x.getName().equals(name))
                .findFirst()
                .get();
    }

    private static MethodBinder methodBinderByName(String name) {
        return new MethodBinder(methodByName(name));
    }

    @Test
    public void nullValue_throws() {
        assertThrows(NullPointerException.class, () -> new MethodBinder(null));
    }

    @Test
    public void noAnnotation_throws() {
        assertThrows(NullPointerException.class, () -> new MethodBinder(methodByName("noAnnotation")));
    }

    @Test
    public void idFirst() {
        var m = methodBinderByName("idFirst");
        assertEquals(Scope.INSTANCE, m.scope());
        assertEquals("idFirst", m.name());
        assertEquals(1, m.parameters().size());
    }

    @Test
    public void idLast_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("idLast"));
    }

    @Test
    public void manyIds_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("manyIds"));
    }

    @Test
    public void idParamNotIdType_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("idIsNotIdType"));
    }

    @Test
    public void noId() {
        var m = methodBinderByName("noId");
        assertEquals(Scope.SERVER, m.scope());
        assertEquals("noId", m.name());
        assertEquals(1, m.parameters().size());
    }

    @Test
    public void extraFirst_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("extraFirst"));
    }

    @Test
    public void extraLast() {
        var m = methodBinderByName("extraLast");
        assertEquals(Scope.SERVER, m.scope());
        assertEquals("extraLast", m.name());
        assertEquals(2, m.parameters().size());
    }

    @Test
    public void manyExtra_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("manyExtra"));
    }

    @Test
    public void extraIsNotParametersType_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("extraIsNotParametersType"));
    }

    @Test
    public void operationParamIsNotFhirType_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("operationParamIsNotFhirType"));
    }

    @Test
    public void binderGetsDescription() {
        var m = methodBinderByName("hasDescription");
        assertEquals("has a description", m.description());
    }

    @Test
    public void binderGetsCanonical() {
        var m = methodBinderByName("hasCanonical");
        assertEquals("http://example.com", m.canonicalUrl());
    }

    @Test
    public void binderGetsMethod() {
        var m = methodBinderByName("idFirst");
        assertNotNull(m.method());
        assertEquals(m.method().getName(), "idFirst");
    }

    @Test
    public void binderGetsOperationAnnotation() {
        var m = methodBinderByName("idFirst");
        assertNotNull(m.operation());
        assertEquals(m.operation().name(), "idFirst");
    }

    @Test
    public void hasType() {
        var m = methodBinderByName("hasType");
        assertEquals("Measure", m.typeName());
        assertEquals(Scope.TYPE, m.scope());
    }

    @Test
    public void hasTypeName() {
        var m = methodBinderByName("hasTypeName");
        assertEquals("Measure", m.typeName());
        assertEquals(Scope.TYPE, m.scope());
    }

    @Test
    public void noArgs() {
        var m = methodBinderByName("noArgs");
        assertEquals(Scope.SERVER, m.scope());
    }

    @Test
    public void nameNormalizes() {
        var m = methodBinderByName("nameNormalizes");
        assertEquals("nameNormalizes", m.name());
    }

    @Test
    public void unboundArgs_throws() {
        var m = methodBinderByName("noArgs");

        var provider = new ExampleMethods();
        var parameters = new Parameters().addParameter("anyParam", new StringType("anyValue"));
        var e = assertThrows(IllegalArgumentException.class, () -> m.bind(provider, null, parameters));
        assertTrue(e.getMessage().contains("not bound"));
    }

    @Test
    public void idWithoutInstanceScope_throws() {
        var m = methodBinderByName("noArgs");

        var provider = new ExampleMethods();
        var id = new IdType("Library/123");
        var e = assertThrows(IllegalArgumentException.class, () -> m.bind(provider, id, null));
        assertTrue(e.getMessage().contains("non-instance"));
    }

    @Test
    public void instanceScopeWithoutId_throws() {
        var m = methodBinderByName("idFirst");

        var provider = new ExampleMethods();
        var e = assertThrows(IllegalArgumentException.class, () -> m.bind(provider, null, null));
        assertTrue(e.getMessage().contains("id required"));
    }

    @Test
    public void missingOperationParamAnnotation_throws() {
        final class MissingOperationAnnotation {
            @Operation(name = "missingOperationParam")
            public IBaseResource missingOperationParam(StringType param) {
                return null;
            }
        }

        var method = MissingOperationAnnotation.class.getDeclaredMethods()[0];
        var e = assertThrows(IllegalArgumentException.class, () -> new MethodBinder(method));
        assertTrue(e.getMessage().contains("must be annotated"));
    }

    @Test
    public void conflictingAnnotations_throws() {
        final class ConflictingAnnotation {
            @Operation(name = "function")
            public IBaseResource function(@IdParam @OperationParam(name = "id") IdType id) {
                return null;
            }
        }

        var method = ConflictingAnnotation.class.getDeclaredMethods()[0];
        var e = assertThrows(IllegalArgumentException.class, () -> new MethodBinder(method));
        assertTrue(e.getMessage().contains("one of"));
    }

    @Test
    public void missingOperationAnnotation_throws() {
        final class MissingOperationAnnotation {
            @SuppressWarnings("unused")
            public IBaseResource function(@IdParam IdType id) {
                return null;
            }
        }

        var method = MissingOperationAnnotation.class.getDeclaredMethods()[0];
        var e = assertThrows(NullPointerException.class, () -> new MethodBinder(method));
        assertTrue(e.getMessage().contains("must be annotated"));
    }
}
