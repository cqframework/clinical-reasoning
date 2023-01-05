
package org.opencds.cqf.cql.evaluator.engine.retrieve;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Collections;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;


public class BundleRetrieveProviderTests {

    private static FhirContext fhirContext;

    @BeforeClass
    public void setup() {
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    }

    private IBaseBundle loadBundle(String path) {
        InputStream stream = BundleRetrieveProviderTests.class.getResourceAsStream(path);
        IParser parser = path.endsWith("json") ? fhirContext.newJsonParser() : fhirContext.newXmlParser();
        IBaseResource resource = parser.parseResource(stream);

        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Bundle", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        return (IBaseBundle) resource;
    }

    private RetrieveProvider getBundleRetrieveProvider() {
        return this.getBundleRetrieveProvider(null);
    }

    private RetrieveProvider getBundleRetrieveProvider(TerminologyProvider terminologyProvider) {
        IBaseBundle bundle = this.loadBundle("../util/r4/TestBundleTwoPatients.json");
        BundleRetrieveProvider brp = new BundleRetrieveProvider(fhirContext, bundle);
        brp.setTerminologyProvider(terminologyProvider);

        return brp;
    }

    @Test
    public void test_noResults_returnsEmptySet() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        Iterable<Object> results = retrieve.retrieve(null, null, null, "PlanDefinition", null, null, null, null, null, null, null, null);
        assertNotNull(results);
<<<<<<< HEAD
        assertThat(results, is(emptyIterable()));
=======
        assertThat(results, emptyIterable());
>>>>>>> bb6c78ee (Use more concise assertions in BundleRetrieveProvider.java)
    }


    @Test
    public void test_filterToDataType() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        Iterable<Object> results = retrieve.retrieve(null, null, null, "Patient", null, null, null, null, null, null, null, null);
        assertThat(results, allOf(iterableWithSize(2), hasItem(instanceOf(Patient.class))));
    }

    @Test
    public void test_filterToDataType_dataTypeNotPresent() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        Iterable<Object> results = retrieve.retrieve(null, null, null, "PlanDefinition", null, null, null, null, null, null, null, null);
<<<<<<< HEAD
        assertThat(results, is(emptyIterable()));
=======
        assertThat(results, emptyIterable());
>>>>>>> bb6c78ee (Use more concise assertions in BundleRetrieveProvider.java)
    }

    @Test
    public void test_filterToContext() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        Iterable<Object> results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, null, null, null, null, null, null, null);
        
        assertThat(results, is(iterableWithSize((2))));

        Object firstEntry = results.iterator().next();
        assertThat(firstEntry, is(instanceOf(Condition.class)));
        assertEquals("test-one-r4", ((Condition)firstEntry).getSubject().getReferenceElement().getIdPart());
    }

    @Test
    public void test_filterToContext_noContextRelation() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        Iterable<Object> results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null, null, null, null, null, null, null, null);
        assertThat(results, contains(instanceOf(Medication.class)));
    }

    // This test covers a special case that's outside of normal usage, which is supplying
    // Strings in the "Codes" element to allows filtering by Id.
    @Test
    @SuppressWarnings("unchecked")
    public void test_filterById() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        // Id does exist
        Iterable<Code> codes = (Iterable<Code>)(Iterable<?>)Collections.singletonList("test-med");
        Iterable<Object> results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null, "id", codes, null, null, null, null, null);
        assertThat(results, contains(instanceOf(Medication.class)));

        // Id does not exist
        codes = (Iterable<Code>)(Iterable<?>)Collections.singletonList("test-med-does-exist");
        results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null, "id", codes, null, null, null, null, null);
<<<<<<< HEAD
        assertThat(results, is(emptyIterable()));
=======
        assertThat(results, emptyIterable());
>>>>>>> bb6c78ee (Use more concise assertions in BundleRetrieveProvider.java)
    }

    @Test
    public void test_filterToCodes() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        // Code doesn't match
        Code code = new Code().withCode("not-a-code").withSystem("not-a-system");
        Iterable<Object> results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", Collections.singleton(code), null, null, null, null, null);
        assertNotNull(results); 
<<<<<<< HEAD
        assertThat(results, is(emptyIterable()));
=======
        assertThat(results, emptyIterable());
>>>>>>> bb6c78ee (Use more concise assertions in BundleRetrieveProvider.java)

        // Codes does match
        code = new Code().withCode("10327003").withSystem("http://snomed.info/sct");
        results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", Collections.singleton(code), null, null, null, null, null);
        assertNotNull(results);
        assertThat(results, contains(instanceOf(Condition.class)));
        assertEquals(((Condition)results.iterator().next()).getSubject().getReferenceElement().getIdPart(), "test-one-r4");
    }


    @Test(expectedExceptions = IllegalStateException.class)
    public void test_filterToValueSet_noTerminologyProvider() {
        RetrieveProvider retrieve = this.getBundleRetrieveProvider();

        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null, 
            "value-set-url", null, null, null, null);
    }

    @Test
    public void test_filterToValueSet() {
        IBaseBundle bundle = this.loadBundle("../util/r4/TestBundleValueSets.json");
        TerminologyProvider terminologyProvider = new BundleTerminologyProvider(fhirContext, bundle);

        RetrieveProvider retrieve = this.getBundleRetrieveProvider(terminologyProvider);

        // Not in the value set
        Iterable<Object> results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null, 
        "http://localhost/fhir/ValueSet/value-set-three", null, null, null, null);
        assertNotNull(results); 
        assertThat(results, is(emptyIterable()));


        // In the value set
        results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null, 
        "http://localhost/fhir/ValueSet/value-set-one", null, null, null, null);
        assertThat(results, is(iterableWithSize(1)));
    }

    @Test
    public void test_retrieveByUrn() {
        IBaseBundle bundle = this.loadBundle("TestBundleUrns.json");
        BundleRetrieveProvider brp = new BundleRetrieveProvider(fhirContext, bundle);

        Iterable<Object> results = brp.retrieve("Patient", "id", "e527283b-e4b1-4f4e-9aef-8a5162816e32" , "Patient", null, null, null, null, null, null, null, null);
        assertNotNull(results);
        assertThat(results, contains(instanceOf(Patient.class)));

        results = brp.retrieve("Patient", "subject", "e527283b-e4b1-4f4e-9aef-8a5162816e32" , "Condition", null, null, null, null, null, null, null, null);
        assertThat(results, contains(instanceOf(Condition.class)));
    }
}
