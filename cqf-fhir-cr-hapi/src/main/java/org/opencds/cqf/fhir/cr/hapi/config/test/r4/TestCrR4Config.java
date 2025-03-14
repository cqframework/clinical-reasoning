package org.opencds.cqf.fhir.cr.hapi.config.test.r4;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.cql2elm.model.Model;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;
import org.opencds.cqf.fhir.cr.hapi.common.CqlThreadFactory;
import org.opencds.cqf.fhir.cr.hapi.config.r4.CrR4Config;
import org.opencds.cqf.fhir.cr.hapi.config.test.TestCqlProperties;
import org.opencds.cqf.fhir.cr.hapi.config.test.TestCrConfig;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.utility.ValidationProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * Common hapi-fhir clinical reasoning config specifically for R4 shared with downstream modules.
 */
@Configuration
@Import({TestCrConfig.class, CrR4Config.class})
public class TestCrR4Config {
    @Primary
    @Bean
    public ExecutorService cqlExecutor() {
        CqlThreadFactory factory = new CqlThreadFactory();
        ExecutorService executor = Executors.newFixedThreadPool(2, factory);
        executor = new DelegatingSecurityContextExecutorService(executor);

        return executor;
    }

    @Bean
    CareGapsProperties careGapsProperties() {
        var careGapsProperties = new CareGapsProperties();
        careGapsProperties.setCareGapsReporter("alphora");
        careGapsProperties.setCareGapsCompositionSectionAuthor("alphora-author");
        return careGapsProperties;
    }

    @Bean
    MeasureEvaluationOptions measureEvaluationOptions(
            EvaluationSettings evaluationSettings, Map<String, ValidationProfile> validationProfiles) {
        MeasureEvaluationOptions measureEvalOptions = new MeasureEvaluationOptions();
        measureEvalOptions.setEvaluationSettings(evaluationSettings);
        if (measureEvalOptions.isValidationEnabled()) {
            measureEvalOptions.setValidationProfiles(validationProfiles);
        }
        return measureEvalOptions;
    }

    @Bean
    public TerminologySettings terminologySettings() {
        var termSettings = new TerminologySettings();
        termSettings.setCodeLookupMode(TerminologySettings.CODE_LOOKUP_MODE.USE_CODESYSTEM_URL);
        termSettings.setValuesetExpansionMode(TerminologySettings.VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        termSettings.setValuesetMembershipMode(TerminologySettings.VALUESET_MEMBERSHIP_MODE.USE_EXPANSION);
        termSettings.setValuesetPreExpansionMode(TerminologySettings.VALUESET_PRE_EXPANSION_MODE.USE_IF_PRESENT);

        return termSettings;
    }

    @Bean
    public RetrieveSettings retrieveSettings() {
        var retrieveSettings = new RetrieveSettings();
        retrieveSettings.setSearchParameterMode(RetrieveSettings.SEARCH_FILTER_MODE.USE_SEARCH_PARAMETERS);
        retrieveSettings.setTerminologyParameterMode(RetrieveSettings.TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
        retrieveSettings.setProfileMode(RetrieveSettings.PROFILE_MODE.OFF);

        return retrieveSettings;
    }

    @Bean
    public EvaluationSettings evaluationSettings(
            TestCqlProperties cqlProperties,
            Map<VersionedIdentifier, CompiledLibrary> globalLibraryCache,
            Map<ModelIdentifier, Model> globalModelCache,
            Map<String, List<Code>> globalValueSetCache,
            RetrieveSettings retrieveSettings,
            TerminologySettings terminologySettings) {
        var evaluationSettings = EvaluationSettings.getDefault();
        var cqlOptions = evaluationSettings.getCqlOptions();

        var cqlEngineOptions = cqlOptions.getCqlEngineOptions();
        Set<CqlEngine.Options> options = EnumSet.noneOf(CqlEngine.Options.class);
        if (cqlProperties.isCqlRuntimeEnableExpressionCaching()) {
            options.add(CqlEngine.Options.EnableExpressionCaching);
        }
        if (cqlProperties.isCqlRuntimeEnableValidation()) {
            options.add(CqlEngine.Options.EnableValidation);
        }
        cqlEngineOptions.setOptions(options);
        cqlOptions.setCqlEngineOptions(cqlEngineOptions);

        var cqlCompilerOptions = new CqlCompilerOptions();

        if (cqlProperties.isEnableDateRangeOptimization()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.EnableDateRangeOptimization);
        }
        if (cqlProperties.isEnableAnnotations()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.EnableAnnotations);
        }
        if (cqlProperties.isEnableLocators()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.EnableLocators);
        }
        if (cqlProperties.isEnableResultsType()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.EnableResultTypes);
        }
        cqlCompilerOptions.setVerifyOnly(cqlProperties.isCqlCompilerVerifyOnly());
        if (cqlProperties.isEnableDetailedErrors()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.EnableDetailedErrors);
        }
        cqlCompilerOptions.setErrorLevel(cqlProperties.getCqlCompilerErrorSeverityLevel());
        if (cqlProperties.isDisableListTraversal()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.DisableListTraversal);
        }
        if (cqlProperties.isDisableListDemotion()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.DisableListDemotion);
        }
        if (cqlProperties.isDisableListPromotion()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.DisableListPromotion);
        }
        if (cqlProperties.isEnableIntervalDemotion()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.EnableIntervalDemotion);
        }
        if (cqlProperties.isEnableIntervalPromotion()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.EnableIntervalPromotion);
        }
        if (cqlProperties.isDisableMethodInvocation()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.DisableMethodInvocation);
        }
        if (cqlProperties.isRequireFromKeyword()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.RequireFromKeyword);
        }
        cqlCompilerOptions.setValidateUnits(cqlProperties.isCqlCompilerValidateUnits());
        if (cqlProperties.isDisableDefaultModelInfoLoad()) {
            cqlCompilerOptions.setOptions(CqlCompilerOptions.Options.DisableDefaultModelInfoLoad);
        }
        cqlCompilerOptions.setSignatureLevel(cqlProperties.getCqlCompilerSignatureLevel());
        cqlCompilerOptions.setCompatibilityLevel(cqlProperties.getCqlCompilerCompatibilityLevel());
        cqlCompilerOptions.setAnalyzeDataRequirements(cqlProperties.isCqlCompilerAnalyzeDataRequirements());
        cqlCompilerOptions.setCollapseDataRequirements(cqlProperties.isCqlCompilerCollapseDataRequirements());

        cqlOptions.setCqlCompilerOptions(cqlCompilerOptions);
        evaluationSettings.setTerminologySettings(terminologySettings);
        evaluationSettings.setRetrieveSettings(retrieveSettings);
        evaluationSettings.setLibraryCache(globalLibraryCache);
        evaluationSettings.setModelCache(globalModelCache);
        evaluationSettings.setValueSetCache(globalValueSetCache);
        return evaluationSettings;
    }
}
