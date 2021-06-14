package org.opencds.cqf.cql.evaluator.dagger;

import javax.inject.Singleton;

import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.dagger.library.LibraryModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;

import ca.uhn.fhir.context.FhirContext;
import dagger.BindsInstance;
import dagger.Component;

@Component(modules = { LibraryModule.class })
@Singleton
public interface CqlEvaluatorComponent {
    LibraryProcessor createLibraryProcessor();
    CqlEvaluatorBuilder createBuilder();
    DataProviderFactory createDataProviderFactory();
    TerminologyProviderFactory createTerminologyProviderFactory();
    LibraryContentProviderFactory createLibraryContentProviderFactory();


  @Component.Builder
  public interface Builder {
    @BindsInstance Builder fhirContext(FhirContext fhirContext);
    CqlEvaluatorComponent build();
  }
}
