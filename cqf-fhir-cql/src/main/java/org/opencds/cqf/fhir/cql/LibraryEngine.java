package org.opencds.cqf.fhir.cql;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.util.ParametersUtil;
import com.google.common.collect.Lists;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kotlin.Unit;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationParams;
import org.opencds.cqf.cql.engine.execution.EvaluationParams.LibraryParams;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.EvaluationResults;
import org.opencds.cqf.cql.engine.runtime.Tuple;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlParameterDefinition;
import org.opencds.cqf.fhir.utility.CqfExpression;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class LibraryEngine {

    private static final Logger logger = LoggerFactory.getLogger(LibraryEngine.class);

    protected final IRepository repository;
    protected final FhirContext fhirContext;
    protected final EvaluationSettings settings;
    protected final IAdapterFactory adapterFactory;

    public LibraryEngine(IRepository repository, EvaluationSettings evaluationSettings) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.settings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        fhirContext = repository.fhirContext();
        adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
    }

    public IRepository getRepository() {
        return repository;
    }

    public EvaluationSettings getSettings() {
        return settings;
    }

    private kotlin.Pair<String, Object> buildContextParameter(String patientId) {
        kotlin.Pair<String, Object> contextParameter = null;
        if (patientId != null) {
            if (patientId.startsWith("Patient/")) {
                patientId = patientId.replace("Patient/", "");
            }
            contextParameter = new kotlin.Pair<>("Patient", patientId);
        }

        return contextParameter;
    }

    public IBaseParameters evaluate(
            String url,
            String patientId,
            IBaseParameters parameters,
            Map<String, Object> rawParameters,
            IBaseBundle additionalData,
            ZonedDateTime zonedDateTime,
            Set<String> expressions) {
        return this.evaluate(
                VersionedIdentifiers.forUrl(url),
                patientId,
                parameters,
                rawParameters,
                additionalData,
                zonedDateTime,
                expressions);
    }

    public IBaseParameters evaluate(
            VersionedIdentifier id,
            String patientId,
            IBaseParameters parameters,
            Map<String, Object> rawParameters,
            IBaseBundle additionalData,
            ZonedDateTime zonedDateTime,
            Set<String> expressions) {
        var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(repository.fhirContext());
        var result = getEvaluationResult(
                id,
                patientId,
                parameters,
                rawParameters,
                additionalData,
                expressions,
                cqlFhirParametersConverter,
                zonedDateTime,
                null);

        return cqlFhirParametersConverter.toFhirParameters(result);
    }

    protected String getModelName(Object base) {
        if (base instanceof List<?> list) {
            return getModelName(list.get(0));
        }
        if (base instanceof Tuple tuple) {
            var properties = new ArrayList<String>();
            tuple.getElements().forEach((propertyName, value) -> {
                properties.add("%s %s".formatted(propertyName, getModelName(value)));
            });
            return "Tuple { %s }".formatted(String.join(", ", properties));
        }
        var fhirType = ((IBase) base).fhirType();
        if (fhirType.contains(".")) {
            var split = fhirType.split("\\.");
            fhirType = Arrays.stream(split).map(StringUtils::capitalize).collect(Collectors.joining("."));
        }
        return "FHIR.%s".formatted(fhirType);
    }

    public IBaseParameters evaluateExpression(
            String expression,
            IBaseParameters parameters,
            Map<String, Object> rawParameters,
            String patientId,
            Map<String, String> referencedLibraries,
            IBaseBundle bundle,
            IBase contextParameter,
            IBase resourceParameter) {
        var libraryConstructor = new LibraryConstructor(fhirContext);
        var cqlFhirParametersConverter = Engines.getCqlFhirParametersConverter(fhirContext);
        var cqlParameters = cqlFhirParametersConverter.toCqlParameterDefinitions(parameters);
        var evaluationParameters = cqlFhirParametersConverter.toCqlParameters(parameters);
        if (contextParameter != null) {
            var contextType = getModelName(contextParameter);
            cqlParameters.add(new CqlParameterDefinition("%context", contextType, false));
            evaluationParameters.put("%context", contextParameter);

            var resourceType = resourceParameter == null ? contextType : getModelName(resourceParameter);
            cqlParameters.add(new CqlParameterDefinition("%resource", resourceType, false));
            evaluationParameters.put("%resource", resourceParameter == null ? contextParameter : resourceParameter);
        }
        if (rawParameters != null) {
            rawParameters.forEach((k, v) -> {
                cqlParameters.add(new CqlParameterDefinition(k, getModelName(v), v instanceof List<?>));
                evaluationParameters.put(k, v);
            });
        }
        var libraryName = "expression";
        var libraryVersion = "1.0.0";
        var cql = libraryConstructor.constructCqlLibrary(
                libraryName, libraryVersion, expression, referencedLibraries, cqlParameters);

        var requestSettings = new EvaluationSettings(settings);
        requestSettings.getLibrarySourceProviders().add(new StringLibrarySourceProvider(Lists.newArrayList(cql)));
        var engine = Engines.forRepository(repository, requestSettings, bundle);

        var id = new VersionedIdentifier().withId(libraryName).withVersion(libraryVersion);

        var paramsBuilder = new EvaluationParams.Builder();
        paramsBuilder.setParameters(evaluationParameters);
        paramsBuilder.setContextParameter(buildContextParameter(patientId));
        paramsBuilder.library(id, builder -> {
            builder.expressions(("return"));
            return Unit.INSTANCE;
        });

        var result = engine.evaluate(paramsBuilder.build()).getOnlyResultOrThrow();

        return cqlFhirParametersConverter.toFhirParameters(result);
    }

    public List<IBase> getExpressionResult(
            String subjectId,
            String expression,
            String language,
            String libraryToBeEvaluated,
            Map<String, String> referencedLibraries,
            IBaseParameters parameters,
            Map<String, Object> rawParameters,
            IBaseBundle bundle,
            IBase contextParameter,
            IBase resourceParameter) {
        validateExpression(language, expression);
        List<IBase> results = null;
        IBaseParameters parametersResult;
        if (libraryToBeEvaluated == null) {
            parametersResult = this.evaluateExpression(
                    expression,
                    parameters,
                    rawParameters,
                    subjectId,
                    referencedLibraries,
                    bundle,
                    contextParameter,
                    resourceParameter);
            // The expression is assumed to be the parameter component name
            // The expression evaluator creates a library with a single expression defined as "return"
            results =
                    resolveParameterValues(ParametersUtil.getNamedParameters(fhirContext, parametersResult, "return"));
        } else {
            validateLibrary(libraryToBeEvaluated);
            parametersResult = this.evaluate(
                    libraryToBeEvaluated,
                    subjectId,
                    parameters,
                    rawParameters,
                    bundle,
                    null,
                    Collections.singleton(expression));
            results = resolveParameterValues(
                    ParametersUtil.getNamedParameters(fhirContext, parametersResult, expression));
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

        return values.stream()
                .map(adapterFactory::createParametersParameter)
                .map(param -> {
                    if (param.hasValue()) {
                        return param.getValue();
                    } else if (param.hasResource()) {
                        return param.getResource();
                    } else if (param.hasPart()) {
                        return param.newTupleWithParts();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public List<IBase> resolveExpression(
            String patientId,
            CqfExpression expression,
            IBaseParameters params,
            Map<String, Object> rawParameters,
            IBaseBundle bundle,
            IBase contextParameter,
            IBase resourceParameter) {
        var result = getExpressionResult(
                patientId,
                expression.getExpression(),
                expression.getLanguage(),
                expression.getLibraryUrl(),
                expression.getReferencedLibraries(),
                params,
                rawParameters,
                bundle,
                contextParameter,
                resourceParameter);
        if (result == null && expression.getAltExpression() != null) {
            result = getExpressionResult(
                    patientId,
                    expression.getAltExpression(),
                    expression.getAltLanguage(),
                    expression.getAltLibraryUrl(),
                    expression.getReferencedLibraries(),
                    params,
                    rawParameters,
                    bundle,
                    contextParameter,
                    resourceParameter);
        }

        return result;
    }

    public EvaluationResults getEvaluationResult(
            List<VersionedIdentifier> ids,
            String patientId,
            IBaseParameters parameters,
            Map<String, Object> rawParameters,
            IBaseBundle additionalData,
            Set<String> expressions,
            CqlFhirParametersConverter cqlFhirParametersConverter,
            @Nullable ZonedDateTime zonedDateTime,
            CqlEngine engine) {

        var cqlFhirParametersConverterToUse = Objects.requireNonNullElseGet(
                cqlFhirParametersConverter, () -> Engines.getCqlFhirParametersConverter(repository.fhirContext()));

        // engine context built externally of LibraryEngine?
        var engineToUse = Objects.requireNonNullElseGet(
                engine, () -> Engines.forRepository(repository, settings, additionalData));

        var evaluationParameters = cqlFhirParametersConverterToUse.toCqlParameters(parameters);
        if (rawParameters != null && !rawParameters.isEmpty()) {
            evaluationParameters.putAll(rawParameters);
        }

        var paramsBuilder = new EvaluationParams.Builder();
        paramsBuilder.setParameters(evaluationParameters);
        paramsBuilder.setContextParameter(buildContextParameter(patientId));
        paramsBuilder.setEvaluationDateTime(zonedDateTime);
        ids.forEach(i -> {
            if (expressions != null && !expressions.isEmpty()) {
                paramsBuilder.library(i, builder -> {
                    builder.expressions(expressions);
                    return Unit.INSTANCE;
                });
            } else {
                paramsBuilder.library(i, new LibraryParams.Builder().build());
            }
        });

        return engineToUse.evaluate(paramsBuilder.build());
    }

    public EvaluationResult getEvaluationResult(
            VersionedIdentifier id,
            String patientId,
            IBaseParameters parameters,
            Map<String, Object> rawParameters,
            IBaseBundle additionalData,
            Set<String> expressions,
            CqlFhirParametersConverter cqlFhirParametersConverter,
            @Nullable ZonedDateTime zonedDateTime,
            CqlEngine engine) {

        var evaluationResultsForMultiLib = getEvaluationResult(
                List.of(id),
                patientId,
                parameters,
                rawParameters,
                additionalData,
                expressions,
                cqlFhirParametersConverter,
                zonedDateTime,
                engine);

        return evaluationResultsForMultiLib.getOnlyResultOrThrow();
    }
}
