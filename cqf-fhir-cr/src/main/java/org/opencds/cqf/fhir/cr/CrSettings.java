package org.opencds.cqf.fhir.cr;

import java.util.ArrayList;
import java.util.List;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

public class CrSettings {
    private EvaluationSettings evaluationSettings;
    private TerminologyServerClientSettings terminologyServerClientSettings;
    private List<String[]> validatorPackages;

    public static CrSettings getDefault() {
        return new CrSettings();
    }

    public CrSettings() {
        evaluationSettings = EvaluationSettings.getDefault();
        terminologyServerClientSettings = TerminologyServerClientSettings.getDefault();
        validatorPackages = new ArrayList<>();
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

    public List<String[]> getValidatorPackages() {
        return validatorPackages;
    }

    public CrSettings withValidatorPackage(String[] validatorPackage) {
        this.validatorPackages.add(validatorPackage);
        return this;
    }

    public void setValidatorPackages(List<String[]> validatorPackages) {
        this.validatorPackages = validatorPackages;
    }
}
