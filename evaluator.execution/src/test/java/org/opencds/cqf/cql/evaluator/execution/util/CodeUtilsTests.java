package org.opencds.cqf.cql.evaluator.execution.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.opencds.cqf.cql.engine.runtime.Code;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class CodeUtilsTests {

    FhirContext dummyContext;
    @BeforeClass
    public void initialize() {
        this.dummyContext = FhirContext.forR4();
    }

    @Test
    public void TestGetCodeFromCode() {

        Code expected = new Code().withCode("code");

        List<Code> actual = CodeUtil.getElmCodesFromObject(expected, this.dummyContext);

        assertTrue(actual.get(0).equals(expected));
    }

    @Test
    public void TestGetCodesFromCodes() {

        List<Code> expected = Collections.singletonList(new Code().withCode("code"));

        List<Code> actual = CodeUtil.getElmCodesFromObject(expected, this.dummyContext);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected.get(0), actual.get(0));
    }

    @Test
    public void TestGetCodesFromNull() {

        List<Code> actual = CodeUtil.getElmCodesFromObject(null, this.dummyContext);

        assertEquals(0, actual.size());
    }  
}
