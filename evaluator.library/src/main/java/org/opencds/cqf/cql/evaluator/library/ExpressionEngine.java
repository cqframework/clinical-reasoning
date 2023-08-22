package org.opencds.cqf.cql.evaluator.library;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;

import com.google.common.collect.Lists;

public class ExpressionEngine {

  private final LibraryConstructor libraryConstructor;
  private final CqlFhirParametersConverter parametersConverter;
  private final Repository repository;
  private final EvaluationSettings evaluationSettings;

  public ExpressionEngine(Repository repository,
      EvaluationSettings evaluationSettings) {
    this.libraryConstructor = new LibraryConstructor(repository.fhirContext());
    this.parametersConverter = Engines.getCqlFhirParametersConverter(repository.fhirContext());
    this.repository = repository;
    this.evaluationSettings = evaluationSettings;
  }

  public IBaseParameters evaluate(String expression) {
    return this.evaluate(expression, null);
  }

  public IBaseParameters evaluate(String expression, IBaseParameters parameters) {
    return this.evaluate(expression, parameters, null, null, null);
  }

  public IBaseParameters evaluate(String theExpression, IBaseParameters theParameters,
      String thePatientId, List<Pair<String, String>> theLibraries, IBaseBundle theBundle) {
    var cqlParameters = parametersConverter.toCqlParameterDefinitions(theParameters);
    var cql = libraryConstructor.constructCqlLibrary(theExpression, theLibraries, cqlParameters);

    Set<String> expressions = new HashSet<>();
    expressions.add("return");

    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    librarySourceProviders.add(new StringLibrarySourceProvider(Lists.newArrayList(cql)));
    var libraryEvaluator = Evaluators.forRepository(evaluationSettings, repository, theBundle,
        librarySourceProviders, parametersConverter);

    return libraryEvaluator.evaluate(
        new VersionedIdentifier().withId("expression").withVersion("1.0.0"),
        buildContextParameter(thePatientId), theParameters, expressions);
  }

  private Pair<String, Object> buildContextParameter(String thePatientId) {
    Pair<String, Object> contextParameter = null;
    if (thePatientId != null) {
      if (thePatientId.startsWith("Patient/")) {
        thePatientId = thePatientId.replace("Patient/", "");
      }
      contextParameter = Pair.of("Patient", thePatientId);
    }

    return contextParameter;
  }
}
