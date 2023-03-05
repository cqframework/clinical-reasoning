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

  protected Repository repository;
  protected FhirContext fhirContext;
  protected IFhirPath fhirPath;

  public LibraryEngine(FhirContext fhirContext, Repository repository) {
    this.fhirContext = fhirContext;
    this.repository = repository;
    this.fhirPath = FhirPathCache.cachedForContext(fhirContext);
    initContext();
  }

  private void initContext() {}

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
    return this.evaluate(this.getVersionedIdentifier(url), patientId, parameters, additionalData,
        expressions);
  }

  public IBaseParameters evaluate(VersionedIdentifier id, String patientId,
      IBaseParameters parameters, IBaseBundle additionalData, Set<String> expressions) {
    var libraryEvaluator = Contexts.forRepository(fhirContext, null, repository, additionalData);

    return libraryEvaluator.evaluate(id, buildContextParameter(patientId), parameters, expressions);
  }

  public IBaseParameters evaluateExpression(String expression, IBaseParameters parameters,
      String patientId, List<Pair<String, String>> libraries, IBaseBundle bundle) {
    var libraryConstructor = new LibraryConstructor(fhirContext);
    var cqlFhirParametersConverter = Contexts.getCqlFhirParametersConverter(fhirContext);
    var cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(parameters);
    var cql = libraryConstructor.constructCqlLibrary(expression, libraries, cqlParameters);

    Set<String> expressions = new HashSet<>();
    expressions.add("return");

    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    librarySourceProviders.add(new InMemoryLibrarySourceProvider(Lists.newArrayList(cql)));
    var libraryEvaluator = Contexts.forRepository(fhirContext, null, repository, bundle,
        librarySourceProviders, cqlFhirParametersConverter);

    return libraryEvaluator.evaluate(
        new VersionedIdentifier().withId("expression").withVersion("1.0.0"),
        buildContextParameter(patientId), parameters, expressions);
  }

  public IBase getExpressionResult(String patientId, String subjectType, String expression,
      String language, String libraryToBeEvaluated, IBaseParameters parameters,
      IBaseBundle bundle) {
    validateExpression(language, expression, libraryToBeEvaluated);
    IBase result = null;
    IBaseParameters parametersResult;
    switch (language) {
      case "text/cql":
      case "text/cql.expression":
      case "text/cql-expression":
        parametersResult = this.evaluateExpression(expression, parameters, patientId, null, bundle);
        // The expression is assumed to be the parameter component name
        // The expression evaluator creates a library with a single expression defined as
        // "return"
        expression = "return";
        result = (IBase) resolveParameterValue(ParametersUtil
            .getNamedParameter(fhirContext, parametersResult, expression).orElse(null));
        break;
      case "text/cql-identifier":
      case "text/cql.identifier":
      case "text/cql.name":
      case "text/cql-name":
        parametersResult = this.evaluate(libraryToBeEvaluated, patientId, parameters, bundle,
            Collections.singleton(expression));
        result = (IBase) resolveParameterValue(ParametersUtil
            .getNamedParameter(fhirContext, parametersResult, expression).orElse(null));
        break;
      case "text/fhirpath":
        List<IBase> outputs;
        try {
          outputs = fhirPath.evaluate(getSubject(patientId, subjectType), expression, IBase.class);
        } catch (FhirPathExecutionException e) {
          throw new IllegalArgumentException("Error evaluating FHIRPath expression", e);
        }
        if (outputs != null && outputs.size() == 1) {
          result = outputs.get(0);
        } else {
          throw new IllegalArgumentException(
              "Expected only one value when evaluating FHIRPath expression: " + expression);
        }
        break;
      default:
        logger.warn("An action language other than CQL was found: {}", language);
    }

    return result;
  }

  public void validateExpression(String language, String expression, String libraryUrl) {
    if (language == null) {
      logger.error("Missing language type for the Expression");
      throw new IllegalArgumentException("Missing language type for the Expression");
    } else if (expression == null) {
      logger.error("Missing expression for the Expression");
      throw new IllegalArgumentException("Missing expression for the Expression");
    } else if (libraryUrl == null) {
      logger.error("Missing library for the Expression");
      throw new IllegalArgumentException("Missing library for the Expression");
    }
  }

  public Object resolveParameterValue(IBase value) {
    if (value == null) {
      return null;
    }
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return ((org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent) value)
            .getValue();
      case R4:
        return ((org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent) value).getValue();
      case R5:
        return ((org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) value).getValue();
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", fhirContext));
    }
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

  protected VersionedIdentifier getVersionedIdentifier(String url) {
    if (!url.contains("/Library/")) {
      throw new IllegalArgumentException(
          "Invalid resource type for determining library version identifier: Library");
    }
    String[] urlSplit = url.split("/Library/");
    if (urlSplit.length != 2) {
      throw new IllegalArgumentException(
          "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
    }

    // String cqlNamespaceUrl = urlSplit[0];

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
