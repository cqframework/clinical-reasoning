package org.opencds.cqf.cql.evaluator.library;

import static java.util.Objects.requireNonNull;

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

  private static Logger ourLogger = LoggerFactory.getLogger(LibraryEngine.class);

  protected final Repository myRepository;
  protected final FhirContext myFhirContext;
  protected final IFhirPath myFhirPath;
  protected final ModelResolver myModelResolver;
  protected final EvaluationSettings mySettings;

  public LibraryEngine(Repository repository, EvaluationSettings evaluationSettings) {
    this.myRepository = requireNonNull(repository, "repository can not be null");
    this.mySettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
    myFhirContext = myRepository.fhirContext();
    myFhirPath = FhirPathCache.cachedForContext(myFhirContext);
    myModelResolver = new FhirModelResolverFactory()
        .create(myRepository.fhirContext().getVersion().getVersion().getFhirVersionString());
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

  public IBaseParameters evaluate(String theUrl, String thePatientId, IBaseParameters theParameters,
      IBaseBundle theAdditionalData, Set<String> theExpressions) {
    return evaluate(getVersionedIdentifier(theUrl), thePatientId, theParameters, theAdditionalData,
        theExpressions);
  }

  public IBaseParameters evaluate(VersionedIdentifier theId, String thePatientId,
      IBaseParameters theParameters, IBaseBundle theAdditionalData, Set<String> theExpressions) {
    var libraryEvaluator = Contexts.forRepository(mySettings, myRepository, theAdditionalData);

    return libraryEvaluator.evaluate(theId, buildContextParameter(thePatientId), theParameters,
        theExpressions);
  }

  public IBaseParameters evaluateExpression(String theExpression, IBaseParameters theParameters,
      String thePatientId, List<Pair<String, String>> theLibraries, IBaseBundle theBundle) {
    var libraryConstructor = new LibraryConstructor(myFhirContext);
    var cqlFhirParametersConverter = Contexts.getCqlFhirParametersConverter(myFhirContext);
    var cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(theParameters);
    var cql = libraryConstructor.constructCqlLibrary(theExpression, theLibraries, cqlParameters);

    Set<String> expressions = new HashSet<>();
    expressions.add("return");

    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    librarySourceProviders.add(new InMemoryLibrarySourceProvider(Lists.newArrayList(cql)));
    var libraryEvaluator = Contexts.forRepository(mySettings, myRepository, theBundle,
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
            .getNamedParameters(myFhirContext, parametersResult, theExpression));
        break;
      case "text/cql-identifier":
      case "text/cql.identifier":
      case "text/cql.name":
      case "text/cql-name":
        parametersResult =
            this.evaluate(theLibraryToBeEvaluated, theSubjectId, theParameters, theBundle,
                Collections.singleton(theExpression));
        results = resolveParameterValues(ParametersUtil
            .getNamedParameters(myFhirContext, parametersResult, theExpression));
        break;
      case "text/fhirpath":
        List<IBase> outputs;
        try {
          outputs =
              myFhirPath.evaluate(getSubject(theSubjectId, theSubjectType), theExpression,
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
        ourLogger.warn("An action language other than CQL was found: {}", theLanguage);
    }

    return results;
  }

  public void validateExpression(String theLanguage, String theExpression, String theLibraryUrl) {
    if (theLanguage == null) {
      ourLogger.error("Missing language type for the Expression");
      throw new IllegalArgumentException("Missing language type for the Expression");
    } else if (theExpression == null) {
      ourLogger.error("Missing expression for the Expression");
      throw new IllegalArgumentException("Missing expression for the Expression");
    } else if (theLibraryUrl == null) {
      ourLogger.error("Missing library for the Expression");
      throw new IllegalArgumentException("Missing library for the Expression");
    }
  }

  public List<IBase> resolveParameterValues(List<IBase> theValues) {
    if (theValues == null || theValues.isEmpty()) {
      return null;
    }

    List<IBase> returnValues = new ArrayList<>();
    switch (myFhirContext.getVersion().getVersion()) {
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
            String.format("unsupported FHIR version: %s", myFhirContext));
    }

    return returnValues;
  }

  protected IBaseResource getSubject(String theSubjectId, String theSubjectType) {
    if (theSubjectType == null || theSubjectType.isEmpty()) {
      theSubjectType = "Patient";
    }
    var resourceType = (IBaseResource) myModelResolver.createInstance(theSubjectType);
    switch (myFhirContext.getVersion().getVersion()) {
      case DSTU3:
        return myRepository.read(resourceType.getClass(),
            new org.hl7.fhir.dstu3.model.IdType(theSubjectType, theSubjectId));
      case R4:
        return myRepository.read(resourceType.getClass(),
            new org.hl7.fhir.r4.model.IdType(theSubjectType, theSubjectId));
      case R5:
        return myRepository.read(resourceType.getClass(),
            new org.hl7.fhir.r5.model.IdType(theSubjectType, theSubjectId));
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", myFhirContext));
    }
  }

  protected VersionedIdentifier getVersionedIdentifier(String theUrl) {
    if (!theUrl.contains("/Library/")) {
      throw new IllegalArgumentException(
          "Invalid resource type for determining library version identifier: Library");
    }
    String[] urlSplit = theUrl.split("/Library/");
    if (urlSplit.length != 2) {
      throw new IllegalArgumentException(
          "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
    }

    String cqlName = urlSplit[1];
    VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
    if (cqlName.contains("|")) {
      String[] nameVersion = cqlName.split("\\|");
      String name = nameVersion[0];
      String version = nameVersion[1];
      versionedIdentifier.setId(name);
      versionedIdentifier.setVersion(version);
    } else {
      versionedIdentifier.setId(cqlName);
    }

    return versionedIdentifier;
  }
}
