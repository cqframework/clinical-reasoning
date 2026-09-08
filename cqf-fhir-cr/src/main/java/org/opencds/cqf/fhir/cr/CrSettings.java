package org.opencds.cqf.fhir.cr;

import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

public class CrSettings {
    private EvaluationSettings evaluationSettings;
    private TerminologyServerClientSettings terminologyServerClientSettings;
    private boolean pauseOnUnknownApplicability = true;

    /**
     * Whether ordered ANY action groups stop at an unresolved applicability condition.
     * Defaults to true. When enabled, the unresolved action's
     * inputs remain available, but its descendants and later alternatives are not applied.
     * Disabling this setting treats unknown as non-applicable and permits ordered fallthrough.
     * Independent ALL actions remain independently evaluated. This does not remove items
     * from a Questionnaire supplied by the caller.
     */
    public boolean isPauseOnUnknownApplicability() {
        return pauseOnUnknownApplicability;
    }

    public void setPauseOnUnknownApplicability(boolean pauseOnUnknownApplicability) {
        this.pauseOnUnknownApplicability = pauseOnUnknownApplicability;
    }

    public CrSettings withPauseOnUnknownApplicability(boolean pauseOnUnknownApplicability) {
        setPauseOnUnknownApplicability(pauseOnUnknownApplicability);
        return this;
    }

    public static CrSettings getDefault() {
        return new CrSettings();
    }

    public CrSettings() {
        evaluationSettings = EvaluationSettings.getDefault();
        terminologyServerClientSettings = TerminologyServerClientSettings.getDefault();
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
}
