package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class QuestionnaireAdapter extends KnowledgeArtifactAdapter {

    public QuestionnaireAdapter(IDomainResource questionnaire) {
        super(questionnaire);
        if (!(questionnaire instanceof Questionnaire)) {
            throw new IllegalArgumentException(
                    "resource passed as questionnaire argument is not a Questionnaire resource");
        }
    }

    public QuestionnaireAdapter(Questionnaire questionnaire) {
        super(questionnaire);
    }

    protected Questionnaire getQuestionnaire() {
        return (Questionnaire) resource;
    }

    @Override
    public Questionnaire get() {
        return getQuestionnaire();
    }

    @Override
    public Questionnaire copy() {
        return get().copy();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        /*
           derivedFrom
           extension[cqf-library]
           extension[launchContext]
           extension[variable].reference
           item[]..definition // NOTE: This is not a simple canonical, it will have a fragment to identify the specific element
           item[]..answerValueSet
           item[]..extension[itemMedia]
           item[]..extension[itemAnswerMedia]
           item[]..extension[unitValueSet]
           item[]..extension[referenceProfile]
           item[]..extension[candidateExpression].reference
           item[]..extension[lookupQuestionnaire]
           item[]..extension[variable].reference
           item[]..extension[initialExpression].reference
           item[]..extension[calculatedExpression].reference
           item[]..extension[cqf-calculatedValue].reference
           item[]..extension[cqf-expression].reference
           item[]..extension[sdc-questionnaire-subQuestionnaire]
        */

        // Not looking at launchContext as it references only base spec profiles and these are included implicitly as
        // dependencies per the CRMI IG

        getQuestionnaire()
                .getDerivedFrom()
                .forEach(derivedRef -> references.add(new DependencyInfo(
                        referenceSource,
                        derivedRef.asStringValue(),
                        derivedRef.getExtension(),
                        (reference) -> derivedRef.setValue(reference))));

        getQuestionnaire()
                .getExtensionsByUrl(Constants.CQF_LIBRARY)
                .forEach(libraryExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((CanonicalType) libraryExt.getValue()).asStringValue(),
                        libraryExt.getExtension(),
                        (reference) -> libraryExt.setValue(new CanonicalType(reference)))));

        getQuestionnaire().getExtensionsByUrl(Constants.VARIABLE_EXTENSION).stream()
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        (reference) -> expression.setReference(reference))));

        getQuestionnaire().getItem().forEach(item -> getDependenciesOfItem(item, references, referenceSource));

        return references;
    }

    private void getDependenciesOfItem(
            QuestionnaireItemComponent item, List<IDependencyInfo> references, String referenceSource) {
        if (item.hasDefinition()) {
            var definition = item.getDefinition().split("#")[0];
            // Not passing an updateReferenceConsumer here because the reference is not a simple canonical
            references.add(new DependencyInfo(referenceSource, definition, item.getExtension(), null));
        }
        if (item.hasAnswerValueSet()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    item.getAnswerValueSet(),
                    item.getExtension(),
                    (reference) -> item.setAnswerValueSet(reference)));
        }
        var referenceExtensions = Arrays.asList(
                Constants.QUESTIONNAIRE_UNIT_VALUE_SET,
                Constants.QUESTIONNAIRE_REFERENCE_PROFILE,
                Constants.SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE,
                Constants.SDC_QUESTIONNAIRE_SUB_QUESTIONNAIRE);
        item.getExtension().stream()
                .filter(e -> referenceExtensions.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((CanonicalType) referenceExt.getValue()).asStringValue(),
                        referenceExt.getExtension(),
                        (reference) -> referenceExt.setValue(new CanonicalType(reference)))));
        var expressionExtensions = Arrays.asList(
                Constants.VARIABLE_EXTENSION,
                Constants.SDC_QUESTIONNAIRE_CANDIDATE_EXPRESSION,
                Constants.SDC_QUESTIONNAIRE_INITIAL_EXPRESSION,
                Constants.SDC_QUESTIONNAIRE_CALCULATED_EXPRESSION,
                Constants.CQF_EXPRESSION);
        item.getExtension().stream()
                .filter(e -> expressionExtensions.contains(e.getUrl()))
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        (reference) -> expression.setReference(reference))));
        item.getItem().forEach(childItem -> getDependenciesOfItem(childItem, references, referenceSource));
    }
}
