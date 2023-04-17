package org.opencds.cqf.cql.evaluator.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.util.ParametersUtil;

public class LibraryEngine {

  private static Logger logger = LoggerFactory.getLogger(LibraryEngine.class);

  protected final Repository repository;
  protected final FhirContext fhirContext;
  protected final IFhirPath fhirPath;
  protected final ModelResolver modelResolver;
  protected EvaluationSettings settings;

  public LibraryEngine(Repository theRepository) {
    repository = theRepository;
    fhirContext = repository.fhirContext();
    fhirPath = FhirPathCache.cachedForContext(fhirContext);
    settings = EvaluationSettings.getDefault().withFhirContext(fhirContext);
    modelResolver = new FhirModelResolverFactory()
        .create(repository.fhirContext().getVersion().getVersion().getFhirVersionString());
  }

  public void setSettings(EvaluationSettings theSettings) {
    settings = theSettings;
  }

  public LibraryEngine withSettings(EvaluationSettings theSettings) {
    setSettings(theSettings);

    return this;
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

  public IBaseParameters evaluate(String url, String patientId, IBaseParameters parameters,
      IBaseBundle additionalData, Set<String> expressions) {
    return this.evaluate(VersionedIdentifiers.forUrl(url), patientId, parameters, additionalData,
        expressions);
  }

  public IBaseParameters evaluate(VersionedIdentifier theId, String thePatientId,
      IBaseParameters theParameters, IBaseBundle theAdditionalData, Set<String> theExpressions) {
    var libraryEvaluator = Contexts.forRepository(settings, repository, theAdditionalData);

    return libraryEvaluator.evaluate(theId, buildContextParameter(thePatientId), theParameters,
        theExpressions);
  }

  public IBaseParameters evaluateExpression(String theExpression, IBaseParameters theParameters,
      String thePatientId, List<Pair<String, String>> theLibraries, IBaseBundle theBundle) {
    var libraryConstructor = new LibraryConstructor(fhirContext);
    var cqlFhirParametersConverter = Contexts.getCqlFhirParametersConverter(fhirContext);
    var cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(theParameters);
    var cql = libraryConstructor.constructCqlLibrary(theExpression, theLibraries, cqlParameters);

    Set<String> expressions = new HashSet<>();
    expressions.add("return");

    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    librarySourceProviders.add(new InMemoryLibrarySourceProvider(Lists.newArrayList(cql)));
    var libraryEvaluator = Contexts.forRepository(settings, repository, theBundle,
        librarySourceProviders, cqlFhirParametersConverter);

    return libraryEvaluator.evaluate(
        new VersionedIdentifier().withId("expression").withVersion("1.0.0"),
        buildContextParameter(thePatientId), theParameters, expressions);
  }

  public List<IBase> getExpressionResult(String theSubjectId, String theSubjectType,
      String theExpression,
      String theLanguage, String theLibraryToBeEvaluated, IBaseParameters theParameters,
      IBaseBundle theBundle) {
    validateExpression(theLanguage, theExpression, theLibraryToBeEvaluated);
    List<IBase> results = null;
    IBaseParameters parametersResult;
    switch (theLanguage) {
      case "text/cql":
      case "text/cql.expression":
      case "text/cql-expression":
        parametersResult =
            this.evaluateExpression(theExpression, theParameters, theSubjectId, null, theBundle);
        // The expression is assumed to be the parameter component name
        // The expression evaluator creates a library with a single expression defined as "return"
        theExpression = "return";
        results = resolveParameterValues(ParametersUtil
            .getNamedParameters(fhirContext, parametersResult, theExpression));
        break;
      case "text/cql-identifier":
      case "text/cql.identifier":
      case "text/cql.name":
      case "text/cql-name":
        parametersResult =
            this.evaluate(theLibraryToBeEvaluated, theSubjectId, theParameters, theBundle,
                Collections.singleton(theExpression));
        results = resolveParameterValues(ParametersUtil
            .getNamedParameters(fhirContext, parametersResult, theExpression));
        break;
      case "text/fhirpath":
        List<IBase> outputs;
        try {
          outputs =
              fhirPath.evaluate(getSubject(theSubjectId, theSubjectType), theExpression,
                  IBase.class);
        } catch (FhirPathExecutionException e) {
          throw new IllegalArgumentException("Error evaluating FHIRPath expression", e);
        }
        if (outputs != null && outputs.size() == 1) {
          results = Collections.singletonList(outputs.get(0));
        } else {
          throw new IllegalArgumentException(
              "Expected only one value when evaluating FHIRPath expression: " + theExpression);
        }
        break;
      default:
        logger.warn("An action language other than CQL was found: {}", theLanguage);
    }

    return results;
  }

  public void validateExpression(String theLanguage, String theExpression, String theLibraryUrl) {
    if (theLanguage == null) {
      logger.error("Missing language type for the Expression");
      throw new IllegalArgumentException("Missing language type for the Expression");
    } else if (theExpression == null) {
      logger.error("Missing expression for the Expression");
      throw new IllegalArgumentException("Missing expression for the Expression");
    } else if (theLibraryUrl == null) {
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

  protected IBaseResource getSubject(String subjectId, String subjectType) {
    if (subjectType == null || subjectType.isEmpty()) {
      subjectType = "Patient";
    }
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return repository.read(org.hl7.fhir.dstu3.model.Patient.class,
            new org.hl7.fhir.dstu3.model.IdType(subjectType, subjectId));
      case R4:
        return repository.read(org.hl7.fhir.r4.model.Patient.class,
            new org.hl7.fhir.r4.model.IdType(subjectType, subjectId));
      case R5:
        return repository.read(org.hl7.fhir.r5.model.Patient.class,
            new org.hl7.fhir.r5.model.IdType(subjectType, subjectId));
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", fhirContext));
    }
  }
}
