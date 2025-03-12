package org.opencds.cqf.fhir.cr.hapi.config.test;

import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.opencds.cqf.fhir.cql.CqlEngineOptions;
import org.opencds.cqf.fhir.cql.CqlOptions;

/**
 * Common CQL properties shared with downstream modules.
 */
public class TestCqlProperties {

    // cql settings
    private CqlEngineOptions cqlEngineOptions = CqlEngineOptions.defaultOptions();
    private Boolean cqlUseEmbeddedLibraries = true;
    private Boolean cqlRuntimeDebugLoggingEnabled = false;
    private Boolean cqlRuntimeEnableValidation = false;
    private Boolean cqlRuntimeEnableExpressionCaching = true;
    private Boolean cqlCompilerValidateUnits = true;
    private Boolean cqlCompilerVerifyOnly = false;
    private String cqlCompilerCompatibilityLevel = "1.5";
    private CqlCompilerException.ErrorSeverity cqlCompilerErrorLevel = CqlCompilerException.ErrorSeverity.Info;
    private LibraryBuilder.SignatureLevel cqlCompilerSignatureLevel = LibraryBuilder.SignatureLevel.All;
    private Boolean cqlCompilerAnalyzeDataRequirements = false;
    private Boolean cqlCompilerCollapseDataRequirements = false;
    private CqlTranslator.Format cqlCompilerTranslatorFormat = CqlTranslator.Format.JSON;
    private Boolean cqlCompilerEnableDateRangeOptimization = false;
    private Boolean cqlCompilerEnableAnnotations = false;
    private Boolean cqlCompilerEnableLocators = false;
    private Boolean cqlCompilerEnableResultsType = false;
    private Boolean cqlCompilerEnableDetailedErrors = false;
    private Boolean cqlCompilerDisableListTraversal = false;
    private Boolean cqlCompilerDisableListDemotion = false;
    private Boolean cqlCompilerDisableListPromotion = false;
    private Boolean cqlCompilerEnableIntervalDemotion = false;
    private Boolean cqlCompilerEnableIntervalPromotion = false;
    private Boolean cqlCompilerDisableMethodInvocation = false;
    private Boolean cqlCompilerRequireFromKeyword = false;
    private Boolean cqlCompilerDisableDefaultModelInfoLoad = false;

    // Care-gaps Settings
    private String caregapsReporter = "default";
    private String caregapsSectionAuthor = "default";

    public boolean isCqlUseEmbeddedLibraries() {
        return cqlUseEmbeddedLibraries;
    }

    public void setCqlUseEmbeddedLibraries(boolean cqlUseEmbeddedLibraries) {
        this.cqlUseEmbeddedLibraries = cqlUseEmbeddedLibraries;
    }

    public boolean isCqlRuntimeDebugLoggingEnabled() {
        return cqlRuntimeDebugLoggingEnabled;
    }

    public void setCqlRuntimeDebugLoggingEnabled(boolean cqlRuntimeDebugLoggingEnabled) {
        this.cqlRuntimeDebugLoggingEnabled = cqlRuntimeDebugLoggingEnabled;
    }

    public boolean isCqlCompilerValidateUnits() {
        return cqlCompilerValidateUnits;
    }

    public void setCqlCompilerValidateUnits(boolean cqlCompilerValidateUnits) {
        this.cqlCompilerValidateUnits = cqlCompilerValidateUnits;
    }

    public boolean isCqlCompilerVerifyOnly() {
        return cqlCompilerVerifyOnly;
    }

    public void setCqlCompilerVerifyOnly(boolean cqlCompilerVerifyOnly) {
        this.cqlCompilerVerifyOnly = cqlCompilerVerifyOnly;
    }

    public String getCqlCompilerCompatibilityLevel() {
        return cqlCompilerCompatibilityLevel;
    }

    public void setCqlCompilerCompatibilityLevel(String cqlCompilerCompatibilityLevel) {
        this.cqlCompilerCompatibilityLevel = cqlCompilerCompatibilityLevel;
    }

    public CqlCompilerException.ErrorSeverity getCqlCompilerErrorSeverityLevel() {
        return cqlCompilerErrorLevel;
    }

    public void setCqlCompilerErrorSeverityLevel(CqlCompilerException.ErrorSeverity cqlCompilerErrorSeverityLevel) {
        this.cqlCompilerErrorLevel = cqlCompilerErrorSeverityLevel;
    }

    public LibraryBuilder.SignatureLevel getCqlCompilerSignatureLevel() {
        return cqlCompilerSignatureLevel;
    }

    public void setCqlCompilerSignatureLevel(LibraryBuilder.SignatureLevel cqlCompilerSignatureLevel) {
        this.cqlCompilerSignatureLevel = cqlCompilerSignatureLevel;
    }

    public boolean isCqlCompilerAnalyzeDataRequirements() {
        return cqlCompilerAnalyzeDataRequirements;
    }

    public void setCqlCompilerAnalyzeDataRequirements(boolean cqlCompilerAnalyzeDataRequirements) {
        this.cqlCompilerAnalyzeDataRequirements = cqlCompilerAnalyzeDataRequirements;
    }

    public boolean isCqlCompilerCollapseDataRequirements() {
        return cqlCompilerCollapseDataRequirements;
    }

    public void setCqlCompilerCollapseDataRequirements(boolean cqlCompilerCollapseDataRequirements) {
        this.cqlCompilerCollapseDataRequirements = cqlCompilerCollapseDataRequirements;
    }

    public boolean isEnableDateRangeOptimization() {
        return cqlCompilerEnableDateRangeOptimization;
    }

    public void setEnableDateRangeOptimization(boolean enableDateRangeOptimization) {
        this.cqlCompilerEnableDateRangeOptimization = enableDateRangeOptimization;
    }

    public boolean isEnableAnnotations() {
        return cqlCompilerEnableAnnotations;
    }

    public void setEnableAnnotations(boolean enableAnnotations) {
        this.cqlCompilerEnableAnnotations = enableAnnotations;
    }

    public boolean isEnableLocators() {
        return cqlCompilerEnableLocators;
    }

    public void setEnableLocators(boolean enableLocators) {
        this.cqlCompilerEnableLocators = enableLocators;
    }

    public boolean isEnableResultsType() {
        return cqlCompilerEnableResultsType;
    }

    public void setEnableResultsType(boolean enableResultsType) {
        this.cqlCompilerEnableResultsType = enableResultsType;
    }

    public boolean isEnableDetailedErrors() {
        return cqlCompilerEnableDetailedErrors;
    }

    public void setEnableDetailedErrors(boolean enableDetailedErrors) {
        this.cqlCompilerEnableDetailedErrors = enableDetailedErrors;
    }

    public boolean isDisableListTraversal() {
        return cqlCompilerDisableListTraversal;
    }

    public void setDisableListTraversal(boolean disableListTraversal) {
        this.cqlCompilerDisableListTraversal = disableListTraversal;
    }

    public boolean isDisableListDemotion() {
        return cqlCompilerDisableListDemotion;
    }

    public void setDisableListDemotion(boolean disableListDemotion) {
        this.cqlCompilerDisableListDemotion = disableListDemotion;
    }

    public boolean isDisableListPromotion() {
        return cqlCompilerDisableListPromotion;
    }

    public void setDisableListPromotion(boolean disableListPromotion) {
        this.cqlCompilerDisableListPromotion = disableListPromotion;
    }

    public boolean isEnableIntervalPromotion() {
        return cqlCompilerEnableIntervalPromotion;
    }

    public void setEnableIntervalPromotion(boolean enableIntervalPromotion) {
        this.cqlCompilerEnableIntervalPromotion = enableIntervalPromotion;
    }

    public boolean isEnableIntervalDemotion() {
        return cqlCompilerEnableIntervalDemotion;
    }

    public void setEnableIntervalDemotion(boolean enableIntervalDemotion) {
        this.cqlCompilerEnableIntervalDemotion = enableIntervalDemotion;
    }

    public boolean isDisableMethodInvocation() {
        return cqlCompilerDisableMethodInvocation;
    }

    public void setDisableMethodInvocation(boolean disableMethodInvocation) {
        this.cqlCompilerDisableMethodInvocation = disableMethodInvocation;
    }

    public boolean isRequireFromKeyword() {
        return cqlCompilerRequireFromKeyword;
    }

    public void setRequireFromKeyword(boolean requireFromKeyword) {
        this.cqlCompilerRequireFromKeyword = requireFromKeyword;
    }

    public boolean isDisableDefaultModelInfoLoad() {
        return cqlCompilerDisableDefaultModelInfoLoad;
    }

    public void setDisableDefaultModelInfoLoad(boolean disableDefaultModelInfoLoad) {
        this.cqlCompilerDisableDefaultModelInfoLoad = disableDefaultModelInfoLoad;
    }

    public boolean isCqlRuntimeEnableExpressionCaching() {
        return cqlRuntimeEnableExpressionCaching;
    }

    public void setCqlRuntimeEnableExpressionCaching(boolean cqlRuntimeEnableExpressionCaching) {
        this.cqlRuntimeEnableExpressionCaching = cqlRuntimeEnableExpressionCaching;
    }

    public boolean isCqlRuntimeEnableValidation() {
        return cqlRuntimeEnableValidation;
    }

    public void setCqlRuntimeEnableValidation(boolean cqlRuntimeEnableValidation) {
        this.cqlRuntimeEnableValidation = cqlRuntimeEnableValidation;
    }

    public CqlTranslator.Format getCqlTranslatorFormat() {
        return cqlCompilerTranslatorFormat;
    }

    public void setCqlTranslatorFormat(CqlTranslator.Format cqlTranslatorFormat) {
        this.cqlCompilerTranslatorFormat = cqlTranslatorFormat;
    }

    private CqlCompilerOptions cqlCompilerOptions = new CqlCompilerOptions();

    public CqlCompilerOptions getCqlCompilerOptions() {
        return this.cqlCompilerOptions;
    }

    public void setCqlCompilerOptions(CqlCompilerOptions compilerOptions) {
        this.cqlCompilerOptions = compilerOptions;
    }

    public CqlEngineOptions getCqlEngineOptions() {
        return this.cqlEngineOptions;
    }

    public void setCqlEngineOptions(CqlEngineOptions engine) {
        this.cqlEngineOptions = engine;
    }

    public CqlOptions getCqlOptions() {
        CqlOptions cqlOptions = new CqlOptions();
        cqlOptions.setUseEmbeddedLibraries(this.cqlUseEmbeddedLibraries);
        cqlOptions.setCqlEngineOptions(this.getCqlEngineOptions());
        cqlOptions.setCqlCompilerOptions(this.getCqlCompilerOptions());
        return cqlOptions;
    }

    public String getCareGapsReporter() {
        return caregapsReporter;
    }

    public String getCareGapsSectionAuthor() {
        return caregapsSectionAuthor;
    }

    public void setCareGapsSectionAuthor(String careGapsSectionAuthor) {
        this.caregapsSectionAuthor = careGapsSectionAuthor;
    }

    public void setCareGapsReporter(String careGapsReporter) {
        this.caregapsReporter = careGapsReporter;
    }
}
