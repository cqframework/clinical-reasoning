package org.opencds.cqf.fhir.utility.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r5.model.Encounter;
import org.hl7.fhir.r5.model.Observation;
import org.hl7.fhir.r5.model.Patient;
import org.junit.jupiter.api.Test;

class ContainedHelperTest {

    @Test
    void liftContainedResourcesToParent() {
        var parent = new Patient();
        parent.setId("parent");
        var contained = new Observation();
        contained.setId("obs1");
        var nestedContained = new Encounter();
        nestedContained.setId("enc1");
        contained.addContained(nestedContained);
        parent.addContained(contained);

        ContainedHelper.liftContainedResourcesToParent(parent);
        assertTrue(parent.getContained().size() >= 2);
    }

    @Test
    void getAllContainedResourcesNested() {
        var parent = new Patient();
        var child = new Observation();
        child.setId("obs1");
        var grandchild = new Encounter();
        grandchild.setId("enc1");
        child.addContained(grandchild);
        parent.addContained(child);

        var all = ContainedHelper.getAllContainedResources(parent);
        assertEquals(2, all.size());
    }

    @Test
    void getAllContainedResourcesNonDomainResource() {
        var bundle = new org.hl7.fhir.r5.model.Bundle();
        var result = ContainedHelper.getAllContainedResources(bundle);
        assertTrue(result.isEmpty());
    }
}
