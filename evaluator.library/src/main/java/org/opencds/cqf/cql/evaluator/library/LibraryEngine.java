package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.fhir.repository.InMemoryFhirRepository;
import org.opencds.cqf.cql.evaluator.fhir.util.FhirPathCache;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.FederatedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  protected final EvaluationSettings settings;
  protected final CqlFhirParametersConverter converter;

  public LibraryEngine(Repository repository, EvaluationSettings evaluationSettings,
      CqlFhirParametersConverter converter) {
    this.repository = requireNonNull(repository, "repository can not be null");
    this.settings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
    fhirContext = repository.fhirContext();
    fhirPath = FhirPathCache.cachedForContext(fhirContext);
    modelResolver = new FhirModelResolverFactory()
        .create(repository.fhirContext().getVersion().getVersion().getFhirVersionString());
    this.converter = converter;
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
    var engine =
        Contexts.forRepository(settings, repository, theAdditionalData, null, this.converter);

    var result = engine.evaluate(theId, theExpressions, buildContextParameter(thePatientId),
        this.converter.toCqlParameters(theParameters), null);

    return this.converter.toFhirParameters(result);
  }

  public IBaseParameters evaluateExpression(String theExpression, IBaseParameters theParameters,
      String thePatientId, List<Pair<String, String>> theLibraries, IBaseBundle theBundle) {
    // var libraryConstructor = new LibraryConstructor(fhirContext);
    // var cqlFhirParametersConverter = Contexts.getCqlFhirParametersConverter(fhirContext);
    // var cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(theParameters);
    // var cql = libraryConstructor.constructCqlLibrary(theExpression, theLibraries, cqlParameters);

    // Set<String> expressions = new HashSet<>();
    // expressions.add("return");

    // List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    // librarySourceProviders.add(new InMemoryLibrarySourceProvider(Lists.newArrayList(cql)));
    // var libraryEvaluator = Contexts.forRepository(settings, repository, theBundle,
    // librarySourceProviders, cqlFhirParametersConverter);

    // return libraryEvaluator.evaluate(
    // new VersionedIdentifier().withId("expression").withVersion("1.0.0"),
    // buildContextParameter(thePatientId), theParameters, expressions);

    var expEngine = new ExpressionEngine(repository, converter, settings);
    return expEngine.evaluate(theExpression, thePatientId, theLibraries, theParameters);
  }

  public List<IBase> getExpressionResult(String theSubjectId, String theSubjectType,
      String theExpression, String theLanguage, String theLibraryToBeEvaluated,
      IBaseParameters theParameters, IBaseBundle theBundle) {
    validateExpression(theLanguage, theExpression);
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
        validateLibrary(theLibraryToBeEvaluated);
        parametersResult =
            this.evaluate(theLibraryToBeEvaluated, theSubjectId, theParameters, theBundle,
                Collections.singleton(theExpression));
        results = resolveParameterValues(ParametersUtil
            .getNamedParameters(fhirContext, parametersResult, theExpression));
        break;
      case "text/fhirpath":
        List<IBase> outputs;
        try {
          var fedRepo = theBundle == null ? repository
              : new FederatedRepository(repository,
                  new InMemoryFhirRepository(repository.fhirContext(), theBundle));
          outputs =
              fhirPath.evaluate(getSubject(fedRepo, theSubjectId, theSubjectType),
                  theExpression,
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

  protected IBaseResource getSubject(Repository repository, String subjectId, String subjectType) {
    var id = subjectId;
    if (subjectId.contains("/")) {
      var split = subjectId.split("/");
      subjectType = split[0];
      id = split[1];
    }
    if (subjectType == null || subjectType.isEmpty()) {
      subjectType = "Patient";
    }
    var resourceType = (IBaseResource) modelResolver.createInstance(subjectType);
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return repository.read(resourceType.getClass(),
            new org.hl7.fhir.dstu3.model.IdType(subjectType, id));
      case R4:
        return repository.read(resourceType.getClass(),
            new org.hl7.fhir.r4.model.IdType(subjectType, id));
      case R5:
        return repository.read(resourceType.getClass(),
            new org.hl7.fhir.r5.model.IdType(subjectType, id));
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", fhirContext));
    }
  }
}
