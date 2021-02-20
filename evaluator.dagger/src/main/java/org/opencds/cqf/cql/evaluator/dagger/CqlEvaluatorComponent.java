package org.opencds.cqf.cql.evaluator.dagger;

import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.dagger.library.LibraryModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;

import ca.uhn.fhir.context.FhirContext;
import dagger.BindsInstance;
import dagger.Component;

@Component(modules = { LibraryModule.class })
public interface CqlEvaluatorComponent {
    LibraryProcessor createLibraryProcessor();
    CqlEvaluatorBuilder createBuilder();
    DataProviderFactory createDataProviderFactory();
    TerminologyProviderFactory createTerminologyProviderFactory();
    LibraryLoaderFactory createLibraryLoaderFactory();


  @Component.Builder
  public interface Builder {
    @BindsInstance Builder fhirContext(FhirContext fhirContext);
    CqlEvaluatorComponent build();
  }
}
