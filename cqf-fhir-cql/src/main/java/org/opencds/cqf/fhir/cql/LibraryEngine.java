package org.opencds.cqf.fhir.cql;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ParametersUtil;
import com.google.common.collect.Lists;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlParameterDefinition;
import org.opencds.cqf.fhir.cql.npm.R4NpmPackageLoader;
import org.opencds.cqf.fhir.cql.npm.R4NpmResourceInfoForCql;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryEngine {

    private static final Logger logger = LoggerFactory.getLogger(LibraryEngine.class);

    protected final Repository repository;
    protected final FhirContext fhirContext;
    protected final EvaluationSettings settings;

    public LibraryEngine(Repository repository, EvaluationSettings evaluationSettings) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.settings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        fhirContext = repository.fhirContext();
    }

    public Repository getRepository() {
        return repository;
    }

    public EvaluationSettings getSettings() {
        return settings;
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

    public IBaseParameters evaluate(
            String url,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle additionalData,
            ZonedDateTime zonedDateTime,
            Set<String> expressions) {
        return this.evaluate(
                VersionedIdentifiers.forUrl(url), patientId, parameters, additionalData, zonedDateTime, expressions);
    }

    public IBaseParameters evaluate(
            VersionedIdentifier id,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle additionalData,
            ZonedDateTime zonedDateTime,
            Set<String> expressions) {
        var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(repository.fhirContext());
        var result = getEvaluationResult(
                id,
                patientId,
                parameters,
                additionalData,
                expressions,
                cqlFhirParametersConverter,
                zonedDateTime,
                null);

        return cqlFhirParametersConverter.toFhirParameters(result);
    }

    public IBaseParameters evaluateExpression(
            String expression,
            IBaseParameters parameters,
            String patientId,
            List<Pair<String, String>> libraries,
            IBaseBundle bundle,
            IBase contextParameter,
            IBase resourceParameter) {
        var libraryConstructor = new LibraryConstructor(fhirContext);
        var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(fhirContext);
        var cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(parameters);
        if (contextParameter != null) {
            var contextType = contextParameter.getClass().getSimpleName();
            cqlParameters.add(new CqlParameterDefinition("%fhirpathcontext", contextType, false, contextParameter));
            var resourceType = resourceParameter == null
                    ? contextType
                    : resourceParameter.getClass().getSimpleName();
            cqlParameters.add(new CqlParameterDefinition(
                    "%resource",
                    resourceType, false, resourceParameter == null ? contextParameter : resourceParameter));
        }
        // There is currently a bug in the CQL compiler that causes the FHIRPath %context variable to fail.
        // This bit of hackery finds any uses of %context in the expression being evaluated and switches it to
        // fhirpathcontext to allow for successful evaluation.
        if (expression.contains("%context")) {
            expression = expression.replace("%context", "%fhirpathcontext");
        }
        var cql = libraryConstructor.constructCqlLibrary(expression, libraries, cqlParameters);

        Set<String> expressions = new HashSet<>();
        expressions.add("return");

        var requestSettings = new EvaluationSettings(settings);

        requestSettings.getLibrarySourceProviders().add(new StringLibrarySourceProvider(Lists.newArrayList(cql)));

        // LUKETODO:  can we ever get a non-empty value here?
        var engine = Engines.forRepository(
                repository, requestSettings, bundle, R4NpmPackageLoader.DEFAULT, R4NpmResourceInfoForCql.EMPTY);

        var evaluationParameters = cqlFhirParametersConverter.toCqlParameters(parameters);
        if (contextParameter != null) {
            evaluationParameters.put("%fhirpathcontext", contextParameter);
            evaluationParameters.put("%resource", resourceParameter == null ? contextParameter : resourceParameter);
        }
        var id = new VersionedIdentifier().withId("expression").withVersion("1.0.0");
        var result = engine.evaluate(id.getId(), expressions, buildContextParameter(patientId), evaluationParameters);

        return cqlFhirParametersConverter.toFhirParameters(result);
    }

    public List<IBase> getExpressionResult(
            String subjectId,
            String expression,
            String language,
            String libraryToBeEvaluated,
            IBaseParameters parameters,
            IBaseBundle bundle,
            IBase contextParameter,
            IBase resourceParameter) {
        validateExpression(language, expression);
        List<IBase> results = null;
        IBaseParameters parametersResult;
        switch (language) {
            case "text/cql":
            case "text/cql.expression":
            case "text/cql-expression":
            case "text/fhirpath":
                var libraries = new ArrayList<Pair<String, String>>();
                if (!StringUtils.isBlank(libraryToBeEvaluated)) {
                    libraries.add(
                            new ImmutablePair<>(libraryToBeEvaluated, Canonicals.getIdPart(libraryToBeEvaluated)));
                }
                parametersResult = this.evaluateExpression(
                        expression, parameters, subjectId, libraries, bundle, contextParameter, resourceParameter);
                // The expression is assumed to be the parameter component name
                // The expression evaluator creates a library with a single expression defined as "return"
                results = resolveParameterValues(
                        ParametersUtil.getNamedParameters(fhirContext, parametersResult, "return"));
                break;
            case "text/cql-identifier":
            case "text/cql.identifier":
            case "text/cql.name":
            case "text/cql-name":
                validateLibrary(libraryToBeEvaluated);
                parametersResult = this.evaluate(
                        libraryToBeEvaluated, subjectId, parameters, bundle, null, Collections.singleton(expression));
                results = resolveParameterValues(
                        ParametersUtil.getNamedParameters(fhirContext, parametersResult, expression));
                break;
            default:
                logger.warn("An action language other than CQL was found: {}", language);
        }

        return results;
    }

    public void validateExpression(String language, String expression) {
        if (language == null) {
            logger.error("Missing language type for the Expression");
            throw new IllegalArgumentException("Missing language type for the Expression");
        } else if (expression == null) {
            logger.error("Missing expression for the Expression");
            throw new IllegalArgumentException("Missing expression for the Expression");
        }
    }

    public void validateLibrary(String libraryUrl) {
        if (libraryUrl == null) {
            logger.error("Missing library for the Expression");
            throw new IllegalArgumentException("Missing library for the Expression");
        }
    }

    public List<IBase> resolveParameterValues(List<IBase> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<IBase> returnValues = new ArrayList<>();
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                values.forEach(v -> {
                    var param = (org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent) v;
                    if (param.hasValue()) {
                        returnValues.add(param.getValue());
                    } else if (param.hasResource()) {
                        returnValues.add(param.getResource());
                    }
                });
                break;
            case R4:
                values.forEach(v -> {
                    var param = (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent) v;
                    if (param.hasValue()) {
                        returnValues.add(param.getValue());
                    } else if (param.hasResource()) {
                        returnValues.add(param.getResource());
                    }
                });
                break;
            case R5:
                values.forEach(v -> {
                    var param = (org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent) v;
                    if (param.hasValue()) {
                        returnValues.add(param.getValue());
                    } else if (param.hasResource()) {
                        returnValues.add(param.getResource());
                    }
                });
                break;
            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s", fhirContext));
        }

        return returnValues;
    }

    public List<IBase> resolveExpression(
            String patientId,
            CqfExpression expression,
            IBaseParameters params,
            IBaseBundle bundle,
            IBase contextParameter,
            IBase resourceParameter) {
        var result = getExpressionResult(
                patientId,
                expression.getExpression(),
                expression.getLanguage(),
                expression.getLibraryUrl(),
                params,
                bundle,
                contextParameter,
                resourceParameter);
        if (result == null && expression.getAltExpression() != null) {
            result = getExpressionResult(
                    patientId,
                    expression.getAltExpression(),
                    expression.getAltLanguage(),
                    expression.getAltLibraryUrl(),
                    params,
                    bundle,
                    contextParameter,
                    resourceParameter);
        }

        return result;
    }

    public EvaluationResult getEvaluationResult(
            VersionedIdentifier id,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle additionalData,
            Set<String> expressions,
            CqlFhirParametersConverter cqlFhirParametersConverter,
            @Nullable ZonedDateTime zonedDateTime,
            CqlEngine engine) {

        if (cqlFhirParametersConverter == null) {
            cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(repository.fhirContext());
        }
        // engine context built externally of LibraryEngine?
        if (engine == null) {
            // LUKETODO:  can we ever get a non-empty value here?
            engine = Engines.forRepository(
                    repository, settings, additionalData, R4NpmPackageLoader.DEFAULT, R4NpmResourceInfoForCql.EMPTY);
        }

        var evaluationParameters = cqlFhirParametersConverter.toCqlParameters(parameters);

        return engine.evaluate(
                new VersionedIdentifier().withId(id.getId()),
                expressions,
                buildContextParameter(patientId),
                evaluationParameters,
                null,
                zonedDateTime);
    }
}
