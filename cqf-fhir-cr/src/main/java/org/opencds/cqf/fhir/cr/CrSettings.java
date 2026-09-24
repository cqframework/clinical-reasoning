package org.opencds.cqf.fhir.cr;

import static java.util.Objects.requireNonNull;

import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.common.ExtensionPropagationPolicy;
import org.opencds.cqf.fhir.utility.IResourceValidator;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

public class CrSettings {
    private EvaluationSettings evaluationSettings;
    private TerminologyServerClientSettings terminologyServerClientSettings;
    private IResourceValidator resourceValidator;
    private ExtensionPropagationPolicy extensionPropagationPolicy;

    public static CrSettings getDefault() {
        return new CrSettings();
    }

    public CrSettings() {
        evaluationSettings = EvaluationSettings.getDefault();
        terminologyServerClientSettings = TerminologyServerClientSettings.getDefault();
        resourceValidator = null;
        extensionPropagationPolicy = ExtensionPropagationPolicy.legacy();
    }

    public ExtensionPropagationPolicy getExtensionPropagationPolicy() {
        return extensionPropagationPolicy;
    }

    /** Configures automatic extension copying. Set before the processors are first used. */
    public CrSettings withExtensionPropagationPolicy(ExtensionPropagationPolicy extensionPropagationPolicy) {
        setExtensionPropagationPolicy(extensionPropagationPolicy);
        return this;
    }

    public void setExtensionPropagationPolicy(ExtensionPropagationPolicy extensionPropagationPolicy) {
        this.extensionPropagationPolicy =
                requireNonNull(extensionPropagationPolicy, "extensionPropagationPolicy can not be null");
    }

    public EvaluationSettings getEvaluationSettings() {
        return evaluationSettings;
    }

    public CrSettings withEvaluationSettings(EvaluationSettings evaluationSettings) {
        this.evaluationSettings = evaluationSettings;
        return this;
    }

    public void setEvaluationSettings(EvaluationSettings evaluationSettings) {
        this.evaluationSettings = evaluationSettings;
    }

    public TerminologyServerClientSettings getTerminologyServerClientSettings() {
        return terminologyServerClientSettings;
    }

    public CrSettings withTerminologyServerClientSettings(
            TerminologyServerClientSettings terminologyServerClientSettings) {
        this.terminologyServerClientSettings = terminologyServerClientSettings;
        return this;
    }

    public void setTerminologyServerClientSettings(TerminologyServerClientSettings terminologyServerClientSettings) {
        this.terminologyServerClientSettings = terminologyServerClientSettings;
    }

    /**
     * Returns the configured resource validator, or null if none has been set.
     * When null, components should bootstrap their own validator as needed.
     *
     * @return the configured IResourceValidator, or null
     */
    public IResourceValidator getResourceValidator() {
        return resourceValidator;
    }

    /**
     * Sets a custom resource validator to be used instead of bootstrapping one internally.
     * This allows external configuration of the FHIR validation support chain.
     *
     * @param resourceValidator the validator to use
     * @return this CrSettings instance for fluent chaining
     */
    public CrSettings withResourceValidator(IResourceValidator resourceValidator) {
        this.resourceValidator = resourceValidator;
        return this;
    }

    public void setResourceValidator(IResourceValidator resourceValidator) {
        this.resourceValidator = resourceValidator;
    }
}
