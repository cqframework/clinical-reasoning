package org.opencds.cqf.fhir.utility.operation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

public class MethodBinderTest {

    // These are simply a bunch of example method signatures to test the MethodBinder class
    class ExampleMethods {

        public void noAnnotation() {}

        @Operation(name = "idFirst")
        public void idFirst(@IdParam IdType id) {}

        @Operation(name = "idLast")
        public void idLast(@OperationParam(name = "param") String param, @IdParam IdType id) {}

        @Operation(name = "noId")
        public void noId(@OperationParam(name = "param") String param) {}

        @Operation(name = "manyIds")
        public void manyIds(@IdParam IdType id, @IdParam IdType id2) {}

        @Operation(name = "idIsNotIdType")
        public void idIsNotIdType(@IdParam StringType id) {}

        @Operation(name = "unboundFirst")
        public void unboundFirst(@UnboundParam IBaseParameters id, @OperationParam(name = "param") String param) {}

        @Operation(name = "unboundLast")
        public void unboundLast(
                @OperationParam(name = "param") String param, @UnboundParam IBaseParameters parameters) {}

        @Operation(name = "manyUnbound")
        public void manyUnbound(@UnboundParam IBaseParameters parameters, @UnboundParam IBaseParameters parameters2) {}

        @Operation(name = "unboundIsNotParametersType")
        public void unboundIsNotParametersType(@UnboundParam IdType idType) {}
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
    public void unboundFirst_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("unboundFirst"));
    }

    @Test
    public void unboundLast() {
        var m = methodBinderByName("unboundLast");
        assertEquals(m.scope(), Scope.SERVER);
        assertEquals("unboundLast", m.name());
        assertEquals(2, m.parameters().size());
    }

    @Test
    public void manyUnbound_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("manyUnbound"));
    }

    @Test
    public void unboundIsNotParametersType_throws() {
        assertThrows(IllegalArgumentException.class, () -> methodBinderByName("unboundIsNotParametersType"));
    }
}
