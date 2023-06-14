package org.opencds.cqf.cql.evaluator.engine.retrieve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Patient;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.repository.InMemoryFhirRepository;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;

public class RepositoryRetrieveProviderTests {

  private RepositoryRetrieveProvider getBundleRetrieveProvider() {
    return this.getRepositoryRetrieveProvider(null);
  }

  private RepositoryRetrieveProvider getRepositoryRetrieveProvider(
      TerminologyProvider terminologyProvider) {
    RepositoryRetrieveProvider brp = new RepositoryRetrieveProvider(
        new InMemoryFhirRepository(FhirContext.forR4Cached(), this.getClass(),
            List.of("test1/", "../terminology/test1/"), false));
    brp.setTerminologyProvider(terminologyProvider);

    return brp;
  }

  @Test
  public void test_noResults_returnsEmptySet() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider();

    Iterable<Object> results = retrieve.retrieve(null, null, null, "PlanDefinition", null, null,
        null, null, null, null, null, null);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);
  }

  @Test
  public void test_filterToDataType() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider();

    Iterable<Object> results = retrieve.retrieve(null, null, null, "Patient", null, null, null,
        null, null, null, null, null);
    List<Object> resultList = Lists.newArrayList(results);

    assertEquals(resultList.size(), 2);
    assertThat(resultList.get(0), instanceOf(Patient.class));
  }

  @Test
  public void test_filterToDataType_dataTypeNotPresent() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider();

    Iterable<Object> results = retrieve.retrieve(null, null, null, "PlanDefinition", null, null,
        null, null, null, null, null, null);
    List<Object> resultList = Lists.newArrayList(results);

    assertEquals(resultList.size(), 0);
  }

  @Test
  public void test_filterToContext() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider();

    Iterable<Object> results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition",
        null, null, null, null, null, null, null, null);
    List<Object> resultList = Lists.newArrayList(results);

    assertEquals(resultList.size(), 2);
    assertThat(resultList.get(0), instanceOf(Condition.class));
    assertEquals("test-one-r4",
        ((Condition) resultList.get(0)).getSubject().getReferenceElement().getIdPart());
  }

  @Test
  public void test_filterToContext_noContextRelation() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider();

    Iterable<Object> results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null,
        null, null, null, null, null, null, null);
    List<Object> resultList = Lists.newArrayList(results);

    assertEquals(resultList.size(), 1);
    assertThat(resultList.get(0), instanceOf(Medication.class));
  }

  // This test covers a special case that's outside of normal usage, which is supplying
  // Strings in the "Codes" element to allows filtering by Id.
  @Test
  @SuppressWarnings("unchecked")
  public void test_filterById() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider().setFilterBySearchParam(false);

    // Id does exist
    Iterable<Code> codes = (Iterable<Code>) (Iterable<?>) Collections.singletonList("test-med");
    Iterable<Object> results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null,
        "id", codes, null, null, null, null, null);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 1);
    assertThat(resultList.get(0), instanceOf(Medication.class));

    // Id does not exist
    codes = (Iterable<Code>) (Iterable<?>) Collections.singletonList("test-med-does-exist");
    results = retrieve.retrieve("Patient", null, "test-one-r4", "Medication", null, "id", codes,
        null, null, null, null, null);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);
  }

  @Test
  public void test_filterToCodes() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider();

    // Code doesn't match
    Code code = new Code().withCode("not-a-code").withSystem("not-a-system");
    Iterable<Object> results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition",
        null, "code", Collections.singleton(code), null, null, null, null, null);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);

    // Codes does match
    code = new Code().withCode("10327003").withSystem("http://snomed.info/sct");
    results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code",
        Collections.singleton(code), null, null, null, null, null);
    assertNotNull(results);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 1);
    assertThat(resultList.get(0), instanceOf(Condition.class));
    assertEquals("test-one-r4",
        ((Condition) resultList.get(0)).getSubject().getReferenceElement().getIdPart());
  }


  @Test(expectedExceptions = IllegalStateException.class)
  public void test_filterToValueSet_noTerminologyProvider() {
    RetrieveProvider retrieve = this.getBundleRetrieveProvider().setFilterBySearchParam(false);

    retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null,
        "value-set-url", null, null, null, null);
  }

  @Test
  public void test_filterToValueSet() {
    TerminologyProvider terminologyProvider = new RepositoryTerminologyProvider(
        new InMemoryFhirRepository(FhirContext.forR4Cached(), this.getClass(),
            List.of("../terminology/test1/"), false));

    RetrieveProvider retrieve =
        this.getRepositoryRetrieveProvider(terminologyProvider).setFilterBySearchParam(false);

    // Not in the value set
    Iterable<Object> results =
        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null,
            "http://localhost/fhir/ValueSet/value-set-three", null, null, null, null);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);


    // In the value set
    results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code",
        null, "http://localhost/fhir/ValueSet/value-set-one", null, null, null, null);
    assertNotNull(results);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 1);
  }

  // todo : handle Urn id in repository
  // @Test
  // public void test_retrieveByUrn() {
  // RepositoryRetrieveProvider brp = new RepositoryRetrieveProvider(fhirContext,
  // new FhirRepository(this.getClass(), List.of("test2/"), false));
  //
  // Iterable<Object> results = brp.retrieve("Patient", "id",
  // "e527283b-e4b1-4f4e-9aef-8a5162816e32" , "Patient", null, null, null, null, null, null, null,
  // null);
  // assertNotNull(results);
  //
  // List<Object> resultList = Lists.newArrayList(results);
  // assertEquals(resultList.size(), 1);
  // assertThat(resultList.get(0), instanceOf(Patient.class));
  //
  // results = brp.retrieve("Patient", "subject", "e527283b-e4b1-4f4e-9aef-8a5162816e32" ,
  // "Condition", null, null, null, null, null, null, null, null);
  // resultList = Lists.newArrayList(results);
  // assertEquals(resultList.size(), 1);
  // assertThat(resultList.get(0), instanceOf(Condition.class));
  // }

  @Test
  public void test_filterToCode() {
    TerminologyProvider terminologyProvider = new RepositoryTerminologyProvider(
        new InMemoryFhirRepository(FhirContext.forR4Cached(), this.getClass(),
            List.of("../terminology/test1/"), false));

    RepositoryRetrieveProvider retrieve =
        this.getRepositoryRetrieveProvider(terminologyProvider);
    retrieve.setExpandValueSets(true);
    retrieve.setFilterBySearchParam(true);

    // Code doesn't match
    Code code = new Code().withCode("not-a-code").withSystem("not-a-system");
    Iterable<Object> results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition",
        null, "code", Collections.singleton(code), null, null, null, null, null);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);

    // Codes does match
    code = new Code().withCode("10327003").withSystem("http://snomed.info/sct");
    results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code",
        Collections.singleton(code), null, null, null, null, null);
    assertNotNull(results);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 1);
    assertThat(resultList.get(0), instanceOf(Condition.class));
    assertEquals("test-one-r4",
        ((Condition) resultList.get(0)).getSubject().getReferenceElement().getIdPart());
  }

  @Test
  public void test_filterValueSet() {
    TerminologyProvider terminologyProvider = new RepositoryTerminologyProvider(
        new InMemoryFhirRepository(FhirContext.forR4Cached(), this.getClass(),
            List.of("../terminology/test1/"), false));

    RepositoryRetrieveProvider retrieve =
        this.getRepositoryRetrieveProvider(terminologyProvider);
    retrieve.setExpandValueSets(true);
    retrieve.setFilterBySearchParam(true);

    // Not in the value set
    Iterable<Object> results =
        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code", null,
            "http://localhost/fhir/ValueSet/value-set-three", null, null, null, null);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);


    // In the value set
    results = retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, "code",
        null, "http://localhost/fhir/ValueSet/value-set-one", null, null, null, null);
    assertNotNull(results);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 1);
  }

  @Test
  public void test_filterDatePath() {
    TerminologyProvider terminologyProvider = new RepositoryTerminologyProvider(
        new InMemoryFhirRepository(FhirContext.forR4Cached(), this.getClass(),
            List.of("../terminology/test1/"), false));

    RepositoryRetrieveProvider retrieve =
        this.getRepositoryRetrieveProvider(terminologyProvider);
    retrieve.setExpandValueSets(true);
    retrieve.setFilterBySearchParam(true);

    // Positive
    Interval dateRange = new Interval(new Date(2022, 1, 1), true, new Date(2022, 12, 31), true);
    Iterable<Object> results =
        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, null, null,
            null, "recordedDate", null, null, dateRange);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 1);

    // Negative
    dateRange = new Interval(new Date(2021, 1, 1), true, new Date(2021, 12, 31), true);
    results =
        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition", null, null, null,
            null, "recordedDate", null, null, dateRange);
    assertNotNull(results);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);
  }

  @Test
  public void test_filterProfile() {
    TerminologyProvider terminologyProvider = new RepositoryTerminologyProvider(
        new InMemoryFhirRepository(FhirContext.forR4Cached(), this.getClass(),
            List.of("../terminology/test1/"), false));

    var retrieve =
        this.getRepositoryRetrieveProvider(terminologyProvider).setSearchByTemplate(true);
    retrieve.setExpandValueSets(true);
    retrieve.setFilterBySearchParam(true);

    // Positive
    Iterable<Object> results =
        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition",
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition", null,
            null, null, null, null, null, null);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 2);

    // Negative
    results =
        retrieve.retrieve("Patient", "subject", "test-one-r4", "Condition",
            "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-condition", null,
            null, null, null, null, null, null);
    assertNotNull(results);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);
  }

  @Test
  public void test_filterEncounterContext() {
    TerminologyProvider terminologyProvider = new RepositoryTerminologyProvider(
        new InMemoryFhirRepository(FhirContext.forR4Cached(), this.getClass(),
            List.of("../terminology/test1/"), false));

    RepositoryRetrieveProvider retrieve =
        this.getRepositoryRetrieveProvider(terminologyProvider);
    retrieve.setExpandValueSets(true);
    retrieve.setFilterBySearchParam(true);

    // Positive
    Iterable<Object> results =
        retrieve.retrieve("Encounter", "encounter", "test-one-r4-1",
            "Condition", null, null, null, null,
            null, null, null, null);
    assertNotNull(results);
    List<Object> resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 1);

    // Negative
    results =
        retrieve.retrieve("Encounter", "encounter", "imaginary-encounter",
            "Condition", null, null, null, null,
            null, null, null, null);
    assertNotNull(results);
    resultList = Lists.newArrayList(results);
    assertEquals(resultList.size(), 0);
  }
}
