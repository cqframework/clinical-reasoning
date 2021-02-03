package org.opencds.cqf.cql.evaluator.converter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.opencds.cqf.cql.evaluator.converter.VersionedIdentifierConverter.toElmIdentifier;
import static org.opencds.cqf.cql.evaluator.converter.VersionedIdentifierConverter.toEngineIdentifier;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.testng.annotations.Test;

public class VersionedIdentifierConverterTests {

    @Test
    public void toElmIdentifierTest() {
        VersionedIdentifier test = new VersionedIdentifier().withId("1").withSystem("2").withVersion("3");
        org.hl7.elm.r1.VersionedIdentifier expected = new org.hl7.elm.r1.VersionedIdentifier().withId("1").withSystem("2").withVersion("3");

        org.hl7.elm.r1.VersionedIdentifier actual = toElmIdentifier(test);

        assertEquals(expected, actual);


        actual = toElmIdentifier(null);
        assertNull(actual);
    }

    @Test
    public void toEngineIdentifierTest() {
        org.hl7.elm.r1.VersionedIdentifier test = new org.hl7.elm.r1.VersionedIdentifier().withId("1").withSystem("2").withVersion("3");
        VersionedIdentifier expected = new VersionedIdentifier().withId("1").withSystem("2").withVersion("3");

        VersionedIdentifier actual = toEngineIdentifier(test);

        assertEquals(expected, actual);


        actual = toEngineIdentifier(null);
        assertNull(actual);
    }
    
}
