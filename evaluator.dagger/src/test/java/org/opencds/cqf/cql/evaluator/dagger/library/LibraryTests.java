package org.opencds.cqf.cql.evaluator.dagger.library;

import static org.testng.Assert.assertNotNull;

import org.opencds.cqf.cql.evaluator.dagger.DaggerCqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class LibraryTests {

  @Test
  public void canInstantiateDstu3() {
    LibraryProcessor libraryProcessor = DaggerCqlEvaluatorComponent.builder()
        .fhirContext(FhirContext.forCached(FhirVersionEnum.DSTU3)).build().createLibraryProcessor();

    assertNotNull(libraryProcessor);
  }

  @Test
  public void canInstantiateR4() {
    LibraryProcessor libraryProcessor = DaggerCqlEvaluatorComponent.builder()
        .fhirContext(FhirContext.forCached(FhirVersionEnum.R4)).build().createLibraryProcessor();

    assertNotNull(libraryProcessor);
  }
}
