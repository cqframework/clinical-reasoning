package org.opencds.cqf.cql.evaluator.engine.terminology;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.cql.evaluator.fhir.repository.r4.FhirRepository;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class RepositoryTerminologyProviderTests {

  FhirRepository repository;
  TerminologyProvider terminology;

  public RepositoryTerminologyProviderTests() {
    repository = new FhirRepository(this.getClass(), List.of("test1/", "test2/"), false);
    terminology = getTerminologyProvider();
  }

  private TerminologyProvider getTerminologyProvider() {
    FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);
    return new RepositoryTerminologyProvider(context,
        new FhirRepository(this.getClass(), List.of("test1/", "test2/"), false));
  }

  @Test
  public void test_expandFromExpansion() {
    Iterable<Code> codes = terminology
        .expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-three"));
    assertNotNull(codes);
    List<Code> codesList = Lists.newArrayList(codes);
    assertEquals(3, codesList.size());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_notExpandedFromExpansionLogic() {
    terminology.in(new Code().withSystem("http://localhost/unit-test").withCode("000"),
        new ValueSetInfo().withId("http://localhost/fhir/ValueSet/expansion-logic-valueset"));
  }

  @Test
  public void test_expandFromCompose() {
    Iterable<Code> codes = terminology
        .expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-two"));
    assertNotNull(codes);
    List<Code> codesList = Lists.newArrayList(codes);
    assertEquals(3, codesList.size());
  }

  @Test
  public void test_expand_expansionOverridesCompose() {
    Iterable<Code> codes = terminology
        .expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
    assertNotNull(codes);
    List<Code> codesList = Lists.newArrayList(codes);
    assertEquals(3, codesList.size());
  }


  @Test
  public void test_expand_noCodes_returnsEmptySet() {
    Iterable<Code> codes = terminology
        .expand(new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-four"));
    assertNotNull(codes);
    List<Code> codesList = Lists.newArrayList(codes);
    assertEquals(0, codesList.size());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_expand_invalidValueSet() {
    terminology.expand(new ValueSetInfo().withId("http://not-value-set"));
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_expand_nullValueSet() {
    terminology.expand(null);
  }

  @Test
  public void test_inValueSet() {
    boolean inValueSet =
        terminology.in(new Code().withSystem("http://localhost/unit-test").withCode("000"),
            new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
    assertTrue(inValueSet);

    inValueSet =
        terminology.in(new Code().withSystem("http://localhost/not-a-system").withCode("XXX"),
            new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
    assertFalse(inValueSet);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_inValueSet_nullValueSet() {
    terminology.in(new Code().withSystem("http://localhost/not-a-system").withCode("XXX"), null);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_inValueSet_nullCode() {
    terminology.in(null, new ValueSetInfo().withId("http://localhost/fhir/ValueSet/value-set-one"));
  }

  // As of the the time this was written lookup does not work
  // for Bundles (the assumption is that the full code-system is not available)
  @Test
  public void test_lookupReturnsNull() {
    Code result = terminology.lookup(new Code().withCode("000"),
        new CodeSystemInfo().withId("http://localhost/unit-test"));
    assertNull(result);
  }


}
