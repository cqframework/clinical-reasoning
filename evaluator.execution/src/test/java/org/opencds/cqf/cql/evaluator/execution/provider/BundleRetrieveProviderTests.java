
package org.opencds.cqf.cql.evaluator.execution.provider;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Collections;

import com.google.common.collect.Lists;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;


public class BundleRetrieveProviderTests {

    private IBaseBundle loadBundle(FhirContext fhirContext, String path) {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(path);
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
        var resolver = new R4FhirModelResolver();
        IBaseBundle bundle = this.loadBundle(resolver.getFhirContext(), "r4/TestBundleTwoPatients.json");
        return new BundleRetrieveProvider(resolver, bundle, terminologyProvider);
    }

    @Test
    public void test_noResults_returnsEmptySet() {
        var retrieve = this.getBundleRetrieveProvider();

        var results = retrieve.retrieve(null, null, null, "PlanDefinition", null, null, null, null, null, null, null, null);
        assertNotNull(results);
        var resultList = Lists.newArrayList(results);
        assertEquals(0, resultList.size());
    }


    @Test
    public void test_filterToDataType() {
        var retrieve = this.getBundleRetrieveProvider();

        var results = retrieve.retrieve(null, null, null, "Patient", null, null, null, null, null, null, null, null);
        var resultList = Lists.newArrayList(results);
        
        assertEquals(2, resultList.size());
        assertThat(resultList.get(0), instanceOf(Patient.class));
    }

    @Test
    public void test_filterToDataType_dataTypeNotPresent() {
        var retrieve = this.getBundleRetrieveProvider();

        var results = retrieve.retrieve(null, null, null, "PlanDefinition", null, null, null, null, null, null, null, null);
        var resultList = Lists.newArrayList(results);
        
        assertEquals(0, resultList.size());
    }

    @Test
    public void test_filterToContext() {
        var retrieve = this.getBundleRetrieveProvider();

        var results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, null, null, null, null, null, null, null);
        var resultList = Lists.newArrayList(results);
        
        assertEquals(2, resultList.size());
        assertThat(resultList.get(0), instanceOf(Condition.class));
        assertEquals("test-one-r4", ((Condition)resultList.get(0)).getSubject().getReferenceElement().getIdPart());
    }

    @Test
    public void test_filterToContext_noContextRelation() {
        var retrieve = this.getBundleRetrieveProvider();

        var results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null, null, null, null, null, null, null, null);
        var resultList = Lists.newArrayList(results);
        
        assertEquals(1, resultList.size());
        assertThat(resultList.get(0), instanceOf(Medication.class));
    }

    // This test covers a special case that's outside of normal usage, which is supplying
    // Strings in the "Codes" element to allows filtering by Id.
    @Test
    @SuppressWarnings("unchecked")
    public void test_filterById() {
        var retrieve = this.getBundleRetrieveProvider();

        // Id does exist
        var codes = (Iterable<Code>)(Iterable<?>)Collections.singletonList("test-med");
        var results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null, "id", codes, null, null, null, null, null);
        var resultList = Lists.newArrayList(results);
        assertEquals(1, resultList.size());
        assertThat(resultList.get(0), instanceOf(Medication.class));

        // Id does not exist
        codes = (Iterable<Code>)(Iterable<?>)Collections.singletonList("test-med-does-exist");
        results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null, "id", codes, null, null, null, null, null);
        resultList = Lists.newArrayList(results);
        assertEquals(0, resultList.size());
    }

    @Test
    public void test_filterToCodes() {
        var retrieve = this.getBundleRetrieveProvider();

        // Code doesn't match
        var code = new Code().withCode("not-a-code").withSystem("not-a-system");
        var results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", Collections.singleton(code), null, null, null, null, null);
        assertNotNull(results); 
        var resultList = Lists.newArrayList(results);
        assertEquals(0, resultList.size());

        // Codes does match
        code = new Code().withCode("10327003").withSystem("http://snomed.info/sct");
        results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", Collections.singleton(code), null, null, null, null, null);
        assertNotNull(results);
        resultList = Lists.newArrayList(results);
        assertEquals(1, resultList.size());
        assertThat(resultList.get(0), instanceOf(Condition.class));
        assertEquals("test-one-r4", ((Condition)resultList.get(0)).getSubject().getReferenceElement().getIdPart());
    }


    @Test(expectedExceptions = IllegalStateException.class)
    public void test_filterToValueSet_noTerminologyProvider() {
        var retrieve = this.getBundleRetrieveProvider();

        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null, 
            "value-set-url", null, null, null, null);
    }

    @Test
    public void test_filterToValueSet() {
        var fhirContext = FhirContext.forR4();
        var bundle = this.loadBundle(fhirContext, "r4/TestBundleValueSets.json");
        var terminologyProvider = new BundleTerminologyProvider(fhirContext, bundle);

        var retrieve = this.getBundleRetrieveProvider(terminologyProvider);

        // Not in the value set
        var results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null, 
        "http://localhost/fhir/ValueSet/value-set-three", null, null, null, null);
        assertNotNull(results); 
        var resultList = Lists.newArrayList(results);
        assertEquals(0, resultList.size());


        // In the value set
        results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null, 
        "http://localhost/fhir/ValueSet/value-set-one", null, null, null, null);
        assertNotNull(results); 
        resultList = Lists.newArrayList(results);
        assertEquals(1, resultList.size());
    }
}