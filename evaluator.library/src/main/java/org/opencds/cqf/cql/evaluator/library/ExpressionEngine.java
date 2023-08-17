package org.opencds.cqf.cql.evaluator.library;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.evaluator.cql2elm.content.InMemoryLibrarySourceProvider;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class ExpressionEngine {

  private static final Logger log = LoggerFactory.getLogger(ExpressionEngine.class);
  private final CqlFhirParametersConverter cqlFhirParametersConverter;
  private final Repository repository;
  private final EvaluationSettings evaluationSettings;

  public ExpressionEngine(Repository repository,
      CqlFhirParametersConverter cqlFhirParametersConverter,
      EvaluationSettings evaluationSettings) {
    this.repository = repository;
    this.cqlFhirParametersConverter = cqlFhirParametersConverter;
    this.evaluationSettings = evaluationSettings;
  }


  /**
   * Evaluates a CQL expression and returns the results as a Parameters resource.
   *
   * @param expression Expression to be evaluated. Note that this is an expression of CQL, not the
   *        text of a library with definition statements.
   * @param subject Subject for which the expression will be evaluated. This corresponds to the
   *        context in which the expression will be evaluated and is represented as a relative FHIR
   *        id (e.g. Patient/123), which establishes both the context and context value for the
   *        evaluation
   * @param libraries The list of libraries to be included in the evaluation context.
   * @param parameters Any input parameters for the expression. Parameters defined in this input
   *        will be made available by name to the CQL expression. Parameter types are mapped to CQL
   *        as specified in the Using CQL section of the cpg implementation guide. If a parameter
   *        appears more than once in the input Parameters resource, it is represented with a List
   *        in the input CQL. If a parameter has parts, it is represented as a Tuple in the input
   *        CQL.
   * @return IBaseParameters The result of evaluating the given expression, returned as a FHIR type,
   *         either a resource, or a FHIR-defined type corresponding to the CQL return type, as
   *         defined in the Using CQL section of the cpg implementation guide. If the result is a
   *         List of resources, the result will be a Bundle. If the result is a CQL system-defined
   *         or FHIR-defined type, the result is returned as a Parameters resource
   */
  public IBaseParameters evaluate(String expression, String subject,
      List<Pair<String, String>> libraries,
      IBaseParameters parameters) {
    var cql = this.constructCqlLibrary(expression, libraries, parameters);

    LibrarySourceProvider contentProvider =
        new InMemoryLibrarySourceProvider(Lists.newArrayList(cql));

    var engine = Contexts.forRepository(evaluationSettings, repository, null,
        Collections.singletonList(contentProvider), cqlFhirParametersConverter);

    var params = this.cqlFhirParametersConverter.toCqlParameters(parameters);

    Set<String> expressions = new HashSet<>();
    expressions.add("return");

    var result = engine.evaluate(new VersionedIdentifier().withId("expression"), expressions,
        Pair.of("Patient", subject), params, null);

    return this.cqlFhirParametersConverter.toFhirParameters(result);
  }


  private String constructCqlLibrary(String expression, List<Pair<String, String>> libraries,
      IBaseParameters parameters) {
    log.debug("Constructing expression for local evaluation");

    StringBuilder sb = new StringBuilder();

    constructHeader(sb);
    constructUsings(sb, parameters);
    constructIncludes(sb, parameters, libraries);
    constructParameters(sb, parameters);
    constructExpression(sb, expression);

    return sb.toString();
  }

  private void constructExpression(StringBuilder sb, String expression) {
    sb.append(String.format("%ndefine \"return\":%n       %s", expression));
  }

  private void constructIncludes(StringBuilder sb, IBaseParameters parameters,
      List<Pair<String, String>> libraries) {
    String fhirVersion = getFhirVersion(parameters);
    if (fhirVersion != null) {
      sb.append(
          String.format("include FHIRHelpers version '%s' called FHIRHelpers%n", fhirVersion));
    }

    if (libraries != null) {
      for (Pair<String, String> library : libraries) {
        VersionedIdentifier vi = getVersionedIdentifier(library.getLeft());
        sb.append(String.format("include \"%s\"", vi.getId()));
        if (vi.getVersion() != null) {
          sb.append(String.format(" version '%s'", vi.getVersion()));
        }
        if (library.getRight() != null) {
          sb.append(String.format(" called \"%s\"", library.getRight()));
        }
        sb.append("\n");
      }
    }
  }

  private void constructParameters(StringBuilder sb, IBaseParameters parameters) {
    if (parameters == null) {
      return;
    }

    // TODO: Can we consolidate this logic in the Library evaluator somehow? Then we
    // don't have to do this conversion twice
    List<CqlParameterDefinition> cqlParameters =
        this.cqlFhirParametersConverter.toCqlParameterDefinitions(parameters);
    if (cqlParameters.isEmpty()) {
      return;
    }

    for (CqlParameterDefinition cpd : cqlParameters) {
      sb.append("parameter \"").append(cpd.getName()).append("\" ")
          .append(this.getTypeDeclaration(cpd.getType(), cpd.getIsList()))
          .append(String.format("%n"));
    }
  }

  private String getTypeDeclaration(String type, Boolean isList) {
    // TODO: Handle "FHIR" and "System" prefixes
    // Should probably mark system types in the CqlParameterDefinition?
    if (Boolean.TRUE.equals(isList)) {
      return "List<" + type + ">";
    } else {
      return type;
    }
  }

  private void constructUsings(StringBuilder sb, IBaseParameters parameters) {
    String fhirVersion = getFhirVersion(parameters);
    if (fhirVersion != null) {
      sb.append(String.format("using FHIR version '%s'%n", fhirVersion));
    }
  }

  private void constructHeader(StringBuilder sb) {
    sb.append(String.format("library expression version '1.0.0'%n%n"));
  }

  private String getFhirVersion(IBaseParameters parameters) {
    if (parameters == null) {
      return null;
    }

    switch (parameters.getStructureFhirVersionEnum()) {
      case DSTU3:
        return "3.0.1";
      case R4:
        return "4.0.1";
      case DSTU2:
      case DSTU2_1:
      case DSTU2_HL7ORG:
      case R5:
      default:
        throw new IllegalArgumentException(String.format("Unsupported version of FHIR: %s",
            parameters.getStructureFhirVersionEnum().getFhirVersionString()));
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
    String cqlNamespaceUrl = urlSplit[0];

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
