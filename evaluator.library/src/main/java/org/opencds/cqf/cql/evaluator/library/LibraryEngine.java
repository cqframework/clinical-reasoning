package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.utility.FhirPathCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.ParametersUtil;

public class LibraryEngine {

  private static Logger logger = LoggerFactory.getLogger(LibraryEngine.class);

  protected final Repository repository;
  protected final FhirContext fhirContext;
  protected final EvaluationSettings settings;
  protected final IFhirPath fhirPath;

  public LibraryEngine(Repository repository, EvaluationSettings evaluationSettings) {
    this.repository = requireNonNull(repository, "repository can not be null");
    this.settings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
    fhirContext = repository.fhirContext();
    fhirPath = FhirPathCache.cachedForContext(fhirContext);
  }

  private Pair<String, Object> buildContextParameter(String patientId) {
    Pair<String, Object> contextParameter = null;
    if (patientId != null) {
      if (patientId.startsWith("Patient/")) {
        patientId = patientId.replace("Patient/", "");
      }
      contextParameter = Pair.of("Patient", patientId);
    }

    return contextParameter;
  }

  public IBaseParameters evaluate(String url, String patientId, IBaseParameters parameters,
      IBaseBundle additionalData, Set<String> expressions) {
    return this.evaluate(VersionedIdentifiers.forUrl(url), patientId, parameters, additionalData,
        expressions);
  }

  public IBaseParameters evaluate(VersionedIdentifier theId, String thePatientId,
      IBaseParameters theParameters, IBaseBundle theAdditionalData, Set<String> theExpressions) {
    var libraryEvaluator = Evaluators.forRepository(settings, repository, theAdditionalData);

    return libraryEvaluator.evaluate(theId, buildContextParameter(thePatientId), theParameters,
        theExpressions);
  }

  public IBaseParameters evaluateExpression(String expression, IBaseParameters parameters,
      String patientId, List<Pair<String, String>> libraries, IBaseBundle bundle) {
    var libraryConstructor = new LibraryConstructor(fhirContext);
    var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(fhirContext);
    var cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(parameters);
    var cql = libraryConstructor.constructCqlLibrary(expression, libraries, cqlParameters);

    Set<String> expressions = new HashSet<>();
    expressions.add("return");

    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    librarySourceProviders.add(new StringLibrarySourceProvider(Lists.newArrayList(cql)));
    var libraryEvaluator = Evaluators.forRepository(settings, repository, bundle,
        librarySourceProviders, cqlFhirParametersConverter);

    return libraryEvaluator.evaluate(
        new VersionedIdentifier().withId("expression").withVersion("1.0.0"),
        buildContextParameter(patientId), parameters, expressions);
  }

  public List<IBase> getExpressionResult(String subjectId, String expression, String language,
      String libraryToBeEvaluated, IBaseParameters parameters, IBaseBundle bundle) {
    validateExpression(language, expression);
    List<IBase> results = null;
    IBaseParameters parametersResult;
    switch (language) {
      case "text/cql":
      case "text/cql.expression":
      case "text/cql-expression":
      case "text/fhirpath":
        parametersResult =
            this.evaluateExpression(expression, parameters, subjectId, null, bundle);
        // The expression is assumed to be the parameter component name
        // The expression evaluator creates a library with a single expression defined as "return"
        results = resolveParameterValues(ParametersUtil
            .getNamedParameters(fhirContext, parametersResult, "return"));
        break;
      case "text/cql-identifier":
      case "text/cql.identifier":
      case "text/cql.name":
      case "text/cql-name":
        validateLibrary(libraryToBeEvaluated);
        parametersResult =
            this.evaluate(libraryToBeEvaluated, subjectId, parameters, bundle,
                Collections.singleton(expression));
        results = resolveParameterValues(ParametersUtil
            .getNamedParameters(fhirContext, parametersResult, expression));
        break;
      default:
        logger.warn("An action language other than CQL was found: {}", language);
    }

    return results;
  }

  public void validateExpression(String theLanguage, String theExpression) {
    if (theLanguage == null) {
      logger.error("Missing language type for the Expression");
      throw new IllegalArgumentException("Missing language type for the Expression");
    } else if (theExpression == null) {
      logger.error("Missing expression for the Expression");
      throw new IllegalArgumentException("Missing expression for the Expression");
    }
  }

  public void validateLibrary(String theLibraryUrl) {
    if (theLibraryUrl == null) {
      logger.error("Missing library for the Expression");
      throw new IllegalArgumentException("Missing library for the Expression");
    }
  }

  public List<IBase> resolveParameterValues(List<IBase> theValues) {
    if (theValues == null || theValues.isEmpty()) {
      return null;
    }

    List<IBase> returnValues = new ArrayList<>();
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        theValues.forEach(v -> {
          var param = (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent) v;
          if (param.hasValue()) {
            returnValues.add(param.getValue());
          } else if (param.hasResource()) {
            returnValues.add(param.getResource());
          }
        });
        break;
      case R4:
        theValues.forEach(v -> {
          var param = (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent) v;
          if (param.hasValue()) {
            returnValues.add(param.getValue());
          } else if (param.hasResource()) {
            returnValues.add(param.getResource());
          }
        });
        break;
      case R5:
        theValues.forEach(v -> {
          var param = (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) v;
          if (param.hasValue()) {
            returnValues.add(param.getValue());
          } else if (param.hasResource()) {
            returnValues.add(param.getResource());
          }
        });
        break;
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", fhirContext));
    }

    return returnValues;
  }

  public List<IBase> resolveExpression(String patientId,
      CqfExpression expression, IBaseParameters params, IBaseBundle bundle) {
    var result = getExpressionResult(patientId, expression.getExpression(),
        expression.getLanguage(), expression.getLibraryUrl(), params, bundle);
    if (result == null && expression.getAltExpression() != null) {
      result = getExpressionResult(patientId, expression.getAltExpression(),
          expression.getAltLanguage(), expression.getAltLibraryUrl(), params, bundle);
    }

    return result;
  }
}
