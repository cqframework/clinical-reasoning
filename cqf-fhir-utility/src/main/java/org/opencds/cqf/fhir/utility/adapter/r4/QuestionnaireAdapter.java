package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;

public class QuestionnaireAdapter extends KnowledgeArtifactAdapter implements IQuestionnaireAdapter {

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
                        referenceSource, derivedRef.asStringValue(), derivedRef.getExtension(), derivedRef::setValue)));

        getQuestionnaire()
                .getExtensionsByUrl(Constants.CQF_LIBRARY)
                .forEach(libraryExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((CanonicalType) libraryExt.getValue()).asStringValue(),
                        libraryExt.getExtension(),
                        reference -> libraryExt.setValue(new CanonicalType(reference)))));

        getQuestionnaire().getExtensionsByUrl(Constants.VARIABLE_EXTENSION).stream()
                .map(e -> (Expression) e.getValue())
                .filter(Expression::hasReference)
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));

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
                    referenceSource, item.getAnswerValueSet(), item.getExtension(), item::setAnswerValueSet));
        }
        item.getExtension().stream()
                .filter(e -> REFERENCE_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((CanonicalType) referenceExt.getValue()).asStringValue(),
                        referenceExt.getExtension(),
                        reference -> referenceExt.setValue(new CanonicalType(reference)))));
        item.getExtension().stream()
                .filter(e -> EXPRESSION_EXTENSIONS.contains(e.getUrl()))
                .map(e -> (Expression) e.getValue())
                .filter(Expression::hasReference)
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));
        item.getItem().forEach(childItem -> getDependenciesOfItem(childItem, references, referenceSource));
    }

    @Override
    public boolean hasItem() {
        return getQuestionnaire().hasItem();
    }

    @Override
    public List<IQuestionnaireItemComponentAdapter> getItem() {
        return getQuestionnaire().getItem().stream()
                .map(adapterFactory::createQuestionnaireItem)
                .toList();
    }

    @Override
    public void setItem(List<IQuestionnaireItemComponentAdapter> items) {
        getQuestionnaire()
                .setItem(items.stream()
                        .map(IAdapter::get)
                        .map(QuestionnaireItemComponent.class::cast)
                        .collect(Collectors.toList()));
    }

    @Override
    public void addItem(IBaseBackboneElement item) {
        if (item instanceof QuestionnaireItemComponent itemComponent) {
            getQuestionnaire().addItem(itemComponent);
        }
    }

    @Override
    public void addItem(IQuestionnaireItemComponentAdapter item) {
        getQuestionnaire().addItem((QuestionnaireItemComponent) item.get());
    }

    @Override
    public void addItems(List<IQuestionnaireItemComponentAdapter> items) {
        items.stream()
                .map(IAdapter::get)
                .map(QuestionnaireItemComponent.class::cast)
                .forEach(item -> getQuestionnaire().addItem(item));
    }
}
