package org.opencds.cqf.cql.evaluator.engine.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.Patient;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.MedicationRequest.MedicationRequestStatus;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class CodeUtilTests {

    FhirContext fhirContext;
    CodeUtil codeUtil;

    @BeforeClass
    public void initialize() {
        this.fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        this.codeUtil = new CodeUtil(fhirContext);
    }

    @Test
    public void TestGetCodeFromCode() {

        Code expected = new Code().withCode("code");

        List<Code> actual = this.codeUtil.getElmCodesFromObject(expected);

        assertTrue(actual.get(0).equals(expected));
    }

    @Test
    public void TestGetCodesFromCodes() {

        List<Code> expected = Collections.singletonList(new Code().withCode("code"));

        List<Code> actual = this.codeUtil.getElmCodesFromObject(expected);

        assertEquals(expected.size(), actual.size());
        assertEquals(expected.get(0), actual.get(0));
    }

    @Test
    public void TestGetCodesFromNull() {

        List<Code> actual = this.codeUtil.getElmCodesFromObject(null);

        assertEquals(0, actual.size());
    }

    @Test
    public void TestGetCodesFromCodingNoValues() {

        Coding coding = new Coding();

        Code expected = new Code();

        List<Code> actual = this.codeUtil.getElmCodesFromObject(coding);

        assertTrue(expected.equivalent(actual.get(0)));
    }

    @Test
    public void TestGetCodesFromCodingValues() {

        Coding coding = new Coding();

        coding.setSystem("test-system");
        coding.setCode("test-code");

        Code expected = new Code().withCode("test-code").withSystem("test-system");

        List<Code> actualList = this.codeUtil.getElmCodesFromObject(coding);

        Code actual = actualList.get(0);

        assertTrue(expected.equivalent(actual));
    }

    @Test
    public void TestGetCodesFromCodeableConceptOneCode() {

        Coding coding = new Coding();

        coding.setSystem("test-system");
        coding.setCode("test-code");

        CodeableConcept concept = new CodeableConcept(coding);

        Code expected = new Code().withCode("test-code").withSystem("test-system");

        List<Code> actualList = this.codeUtil.getElmCodesFromObject(concept);

        Code actual = actualList.get(0);

        assertTrue(expected.equivalent(actual));
    }

    @Test
    public void TestGetCodesFromCodeableNoCodes() {

        CodeableConcept concept = new CodeableConcept();

        List<Code> actualList = this.codeUtil.getElmCodesFromObject(concept);
        assertTrue(actualList.isEmpty());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void TestGetCodesFromInvalidResource() {
        this.codeUtil.getElmCodesFromObject(new Patient());
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void TestGetCodesFromInvalidBase() {
        this.codeUtil.getElmCodesFromObject(new StringType("test"));
    }

    @Test
    public void TestGetCodesFromEnumeration() {
        Code expected = new Code().withCode("active").withSystem("http://hl7.org/fhir/CodeSystem/medicationrequest-status");

        MedicationRequest mr = new MedicationRequest();
        mr.setStatus(MedicationRequestStatus.ACTIVE);

        List<Code> actualList = this.codeUtil.getElmCodesFromObject(mr.getStatusElement());
        assertEquals(actualList.size(), 1);

        Code actual = actualList.get(0);

        assertTrue(expected.equivalent(actual));
    }
}