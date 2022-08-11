
package org.opencds.cqf.cql.evaluator.engine.terminology;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class BundleTerminologyProviderTests {

    private String bundlePathOne = "../util/r4/TestBundleValueSets.json";

    private IBaseBundle loadBundle(FhirContext fhirContext, String path) {
        InputStream stream = BundleTerminologyProviderTests.class.getResourceAsStream(path);
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


    private TerminologyProvider getTerminologyProvider(String path) {
        FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);
        IBaseBundle bundle = this.loadBundle(context, path);
        return new BundleTerminologyProvider(context, bundle);
    }


    @Test
    public void test_expandFromExpansion() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        Iterable<Code> codes = terminology.expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-three"));
        assertNotNull(codes);
        List<Code> codesList = Lists.newArrayList(codes);
        assertEquals(3, codesList.size());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_notExpandedFromExpansionLogic() {
        TerminologyProvider terminology = this.getTerminologyProvider("../util/r4/TestExpansionLogicBundle.json");
        terminology.in(
            new Code().withSystem("http://localhost/unit-test").withCode("000"),
            new ValueSetInfo().withId("http://localhost/fhir/ValueSet/expansion-logic-valueset"));
    }

    @Test
    public void test_naiveExpansion() {
        TerminologyProvider terminology = this.getTerminologyProvider("../util/r4/TestNaiveExpansionBundle.json");
        terminology.in(
            new Code().withSystem("http://localhost/unit-test").withCode("000"),
            new ValueSetInfo().withId("http://localhost/fhir/ValueSet/naive-expansion-valueset"));

        // String expectedWarnMessage = "Codes expanded without a terminology server, some results may not be correct.";
        // boolean foundExpected = false;
        // for (String warning : logCaptor.getWarnLogs()) {
        //     if (warning.equals(expectedWarnMessage)) {
        //         foundExpected = true;
        //     }
        // }
        // assertTrue(foundExpected);
    }

    @Test
    public void test_expandFromCompose() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        Iterable<Code> codes = terminology.expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-two"));
        assertNotNull(codes);
        List<Code> codesList = Lists.newArrayList(codes);
        assertEquals(3, codesList.size());
    }

    @Test
    public void test_expand_expansionOverridesCompose() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        Iterable<Code> codes = terminology.expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
        assertNotNull(codes);
        List<Code> codesList = Lists.newArrayList(codes);
        assertEquals(3, codesList.size());
    }


    @Test
    public void test_expand_noCodes_returnsEmptySet() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        Iterable<Code> codes = terminology.expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-four"));
        assertNotNull(codes);
        List<Code> codesList = Lists.newArrayList(codes);
        assertEquals(0, codesList.size());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_expand_invalidValueSet() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        terminology.expand(new ValueSetInfo().withId("http://not-value-set"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_expand_nullValueSet() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        terminology.expand(null);
    }

    @Test
    public void test_inValueSet() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        boolean inValueSet = terminology.in(
            new Code().withSystem("http://localhost/unit-test").withCode("000"),
            new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
        assertTrue(inValueSet);

        inValueSet = terminology.in(
            new Code().withSystem("http://localhost/not-a-system").withCode("XXX"),
            new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
        assertFalse(inValueSet);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_inValueSet_nullValueSet() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        terminology.in(new Code().withSystem("http://localhost/not-a-system").withCode("XXX"), null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_inValueSet_nullCode() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        terminology.in(null, new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
    }

    // As of the the time this was written lookup does not work
    // for Bundles (the assumption is that the full code-system is not available)
    @Test
    public void test_lookupReturnsNull() {
        TerminologyProvider terminology = this.getTerminologyProvider(bundlePathOne);
        Code result = terminology.lookup(new Code().withCode("000"), new CodeSystemInfo().withId("http://localhost/unit-test"));
        assertNull(result);
    }
}