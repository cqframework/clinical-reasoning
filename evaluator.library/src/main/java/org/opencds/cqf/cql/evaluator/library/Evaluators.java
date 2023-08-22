package org.opencds.cqf.cql.evaluator.library;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;

public class Evaluators {

  private Evaluators() {
    // intentionally empty
  }

  public static LibraryEvaluator forRepository(EvaluationSettings settings, Repository repository,
      IBaseBundle additionalData) {
    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    return forRepository(settings, repository, additionalData, librarySourceProviders,
        null);
  }

  public static LibraryEvaluator forRepository(EvaluationSettings settings,
      Repository repository,
      IBaseBundle additionalData, List<LibrarySourceProvider> librarySourceProviders,
      CqlFhirParametersConverter cqlFhirParametersConverter) {
    checkNotNull(settings);
    checkNotNull(repository);
    checkNotNull(librarySourceProviders);

    if (cqlFhirParametersConverter == null) {
      cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(repository.fhirContext());
    }

    var engine = Engines.forRepositoryAndSettings(settings, repository, additionalData);

    var providers = engine.getEnvironment().getLibraryManager().getLibrarySourceLoader();

    for (var source : librarySourceProviders) {
      providers.registerProvider(source);
    }

    return new LibraryEvaluator(cqlFhirParametersConverter, engine);
  }
}
