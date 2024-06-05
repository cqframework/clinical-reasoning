package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.UriType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class QuestionnaireAdapter extends KnowledgeArtifactAdapter {

    private Questionnaire questionnaire;

    public QuestionnaireAdapter(Questionnaire questionnaire) {
        super(questionnaire);
        this.questionnaire = questionnaire;
    }

    protected Questionnaire getQuestionnaire() {
        return this.questionnaire;
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getQuestionnaire().hasVersion()
                ? this.getQuestionnaire().getUrl() + "|"
                        + this.getQuestionnaire().getVersion()
                : this.getQuestionnaire().getUrl();
        /*
           derivedFrom
           item[]..definition // NOTE: This is not a simple canonical, it will have a fragment to identify the specific element
           item[]..answerValueSet
           item[]..extension[itemMedia]
           item[]..extension[itemAnswerMedia]
           item[]..extension[unitValueSet]
           item[]..extension[referenceProfile]
           item[]..extension[candidateExpression].reference
           item[]..extension[lookupQuestionnaire]
           extension[cqf-library]
           extension[launchContext]
           extension[variable].reference
           item[]..extension[variable].reference
           item[]..extension[initialExpression].reference
           item[]..extension[calculatedExpression].reference
           item[]..extension[cqf-calculatedValue].reference
           item[]..extension[cqf-expression].reference
           item[]..extension[sdc-questionnaire-subQuestionnaire]
        */

        var libraryExtensions = questionnaire.getExtensionsByUrl(Constants.CQF_LIBRARY);
        for (var libraryExt : libraryExtensions) {
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource,
                    ((UriType) libraryExt.getValue()).asStringValue(),
                    libraryExt.getExtension(),
                    (reference) -> libraryExt.setValue(new UriType(reference)));
            references.add(dependency);
        }

        return references;
    }
}
