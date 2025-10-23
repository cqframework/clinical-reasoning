package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;

class QuantityHolderTest {

    @Test
    void sameIdDifferentQuantityR4() {
        var quantityQuantityHolder500 = new QuantityHolder<>("123", new org.hl7.fhir.r4.model.Quantity(500));
        var quantityQuantityHolder600 = new QuantityHolder<>("123", new org.hl7.fhir.r4.model.Quantity(600));

        assertEquals(quantityQuantityHolder500, quantityQuantityHolder600);

        var holders = new HashSetForFhirResources<>();
        holders.add(quantityQuantityHolder500);
        holders.add(quantityQuantityHolder600);

        assertEquals(1, holders.size());

        assertTrue(holders.contains(quantityQuantityHolder500));
    }

    @Test
    void differentIdSameQuantityR4() {
        var quantityQuantityHolder123 = new QuantityHolder<>("123", new org.hl7.fhir.r4.model.Quantity(500));
        var quantityQuantityHolder456 = new QuantityHolder<>("456", new org.hl7.fhir.r4.model.Quantity(500));

        assertNotEquals(quantityQuantityHolder123, quantityQuantityHolder456);

        var holders = new HashSetForFhirResources<>();
        holders.add(quantityQuantityHolder123);
        holders.add(quantityQuantityHolder456);

        assertEquals(2, holders.size());

        assertTrue(holders.contains(quantityQuantityHolder123));
        assertTrue(holders.contains(quantityQuantityHolder456));
    }

    @Test
    void sameIdSameQuantityR4() {
        var quantityQuantityHolder1 = new QuantityHolder<>("123", new Quantity(500));
        var quantityQuantityHolder2 = new QuantityHolder<>("123", new Quantity(500));

        assertEquals(quantityQuantityHolder1, quantityQuantityHolder2);

        var holders = new HashSetForFhirResources<>();
        holders.add(quantityQuantityHolder1);
        holders.add(quantityQuantityHolder2);

        assertEquals(1, holders.size());

        assertTrue(holders.contains(quantityQuantityHolder1));
    }

    @Test
    void sameIdDifferentQuantityDstu3() {
        var quantityQuantityHolder500 = new QuantityHolder<>("123", new org.hl7.fhir.dstu3.model.Quantity(500));
        var quantityQuantityHolder600 = new QuantityHolder<>("123", new org.hl7.fhir.dstu3.model.Quantity(600));

        assertEquals(quantityQuantityHolder500, quantityQuantityHolder600);

        var holders = new HashSetForFhirResources<>();
        holders.add(quantityQuantityHolder500);
        holders.add(quantityQuantityHolder600);

        assertEquals(1, holders.size());

        assertTrue(holders.contains(quantityQuantityHolder500));
    }

    @Test
    void differentIdSameQuantityDstu3() {
        var quantityQuantityHolder123 = new QuantityHolder<>("123", new org.hl7.fhir.dstu3.model.Quantity(500));
        var quantityQuantityHolder456 = new QuantityHolder<>("456", new org.hl7.fhir.dstu3.model.Quantity(500));

        assertNotEquals(quantityQuantityHolder123, quantityQuantityHolder456);

        var holders = new HashSetForFhirResources<>();
        holders.add(quantityQuantityHolder123);
        holders.add(quantityQuantityHolder456);

        assertEquals(2, holders.size());

        assertTrue(holders.contains(quantityQuantityHolder123));
        assertTrue(holders.contains(quantityQuantityHolder456));
    }

    @Test
    void sameIdSameQuantityDstu3() {
        var quantityQuantityHolder1 = new QuantityHolder<>("123", new org.hl7.fhir.dstu3.model.Quantity(500));
        var quantityQuantityHolder2 = new QuantityHolder<>("123", new org.hl7.fhir.dstu3.model.Quantity(500));

        assertEquals(quantityQuantityHolder1, quantityQuantityHolder2);

        var holders = new HashSetForFhirResources<>();
        holders.add(quantityQuantityHolder1);
        holders.add(quantityQuantityHolder2);

        assertEquals(1, holders.size());

        assertTrue(holders.contains(quantityQuantityHolder1));
    }
}
