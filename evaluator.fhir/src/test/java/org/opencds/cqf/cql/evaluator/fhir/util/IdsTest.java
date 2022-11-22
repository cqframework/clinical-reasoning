package org.opencds.cqf.cql.evaluator.fhir.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.hl7.fhir.instance.model.api.IIdType;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class IdsTest {
	@Test
	public void testAllVersionsSupported() {
		for (FhirVersionEnum fhirVersionEnum : FhirVersionEnum.values()) {
			Ids.newId(fhirVersionEnum, "Patient/123");
		}
	}

	@Test
	public void testContextSupported() {
		IIdType id = Ids.newId(FhirContext.forDstu3Cached(), "Patient/123");
		assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
	}

	@Test
	public void testPartsSupported() {
		IIdType id = Ids.newId(FhirVersionEnum.DSTU3, "Patient", "123");
		assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);

		assertEquals("Patient", id.getResourceType());
		assertEquals("123", id.getIdPart());
	}

	@Test
	public void testClassSupported() {
		IIdType id = Ids.newId(org.hl7.fhir.dstu3.model.Library.class, "123");
		assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
		assertEquals("Library", id.getResourceType());
		assertEquals("123", id.getIdPart());
	}
}
