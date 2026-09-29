package org.opencds.cqf.fhir.utility.adapter.r4

import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Questionnaire
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter

class QuestionnaireAdapter : KnowledgeArtifactAdapter, IQuestionnaireAdapter {
    constructor(questionnaire: IDomainResource) : super(questionnaire) {
        require(questionnaire is Questionnaire) {
            "resource passed as questionnaire argument is not a Questionnaire resource"
        }
    }

    constructor(questionnaire: Questionnaire) : super(questionnaire)

    protected val questionnaire: Questionnaire
        get() = resource as Questionnaire

    override fun get(): Questionnaire {
        return this.questionnaire
    }

    override val dependencies: MutableList<IDependencyInfo?>
        get() {
            val references = mutableListOf<IDependencyInfo?>()
            val referenceSource = this.referenceSource
            addProfileReferences(references, referenceSource)

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

            // Not looking at launchContext as it references only base spec profiles and these are
            // included implicitly as
            // dependencies per the CRMI IG
            this.questionnaire.derivedFrom.forEach { derivedRef ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        derivedRef!!.asStringValue(),
                        derivedRef.extension,
                        { theValue -> derivedRef.setValue(theValue) },
                    )
                )
            }

            this.questionnaire.getExtensionsByUrl(Constants.CQF_LIBRARY).forEach { libraryExt ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        (libraryExt!!.value as CanonicalType).asStringValue(),
                        libraryExt.extension,
                        { reference -> libraryExt.setValue(CanonicalType(reference)) },
                    )
                )
            }

            this.questionnaire
                .getExtensionsByUrl(Constants.VARIABLE_EXTENSION)
                .map { e -> e!!.value as Expression? }
                .filter { obj -> obj!!.hasReference() }
                .forEach { expression ->
                    references.add(
                        DependencyInfo(
                            referenceSource,
                            expression!!.reference,
                            expression.extension,
                            { value -> expression.setReference(value) },
                        )
                    )
                }

            this.questionnaire.item.forEach { item ->
                getDependenciesOfItem(item!!, references, referenceSource)
            }

            return references
        }

    private fun getDependenciesOfItem(
        item: Questionnaire.QuestionnaireItemComponent,
        references: MutableList<IDependencyInfo?>,
        referenceSource: String?,
    ) {
        if (item.hasDefinition()) {
            val definition = item.definition.split("#")[0]
            // Not passing an updateReferenceConsumer here because the reference is not a simple
            // canonical
            references.add(DependencyInfo(referenceSource, definition, item.extension, null))
        }
        if (item.hasAnswerValueSet()) {
            references.add(
                DependencyInfo(
                    referenceSource,
                    item.answerValueSet,
                    item.extension,
                    { value -> item.setAnswerValueSet(value) },
                )
            )
        }
        item.extension
            .filter { e -> REFERENCE_EXTENSIONS.contains(e!!.url) }
            .forEach { referenceExt ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        (referenceExt!!.value as CanonicalType).asStringValue(),
                        referenceExt.extension,
                        { reference -> referenceExt.setValue(CanonicalType(reference)) },
                    )
                )
            }
        item.extension
            .filter { e -> EXPRESSION_EXTENSIONS.contains(e!!.url) }
            .map { e -> e!!.value as Expression? }
            .filter { obj -> obj!!.hasReference() }
            .forEach { expression ->
                references.add(
                    DependencyInfo(
                        referenceSource,
                        expression!!.reference,
                        expression.extension,
                        { value -> expression.setReference(value) },
                    )
                )
            }
        item.item.forEach { childItem ->
            getDependenciesOfItem(childItem!!, references, referenceSource)
        }
    }

    override fun hasItem(): Boolean {
        return this.questionnaire.hasItem()
    }

    override var item: MutableList<IQuestionnaireItemComponentAdapter?>?
        get() {
            return this.questionnaire.item
                .map { questionnaireItem ->
                    adapterFactory.createQuestionnaireItem(questionnaireItem)
                }
                .toMutableList()
        }
        set(items) {
            this.questionnaire.setItem(
                items!!
                    .map { obj -> obj!!.get() }
                    .map { obj -> Questionnaire.QuestionnaireItemComponent::class.java.cast(obj) }
                    .toMutableList()
            )
        }

    override fun addItem(item: IBaseBackboneElement?) {
        if (item is Questionnaire.QuestionnaireItemComponent) {
            this.questionnaire.addItem(item)
        }
    }

    override fun addItem(item: IQuestionnaireItemComponentAdapter?) {
        this.questionnaire.addItem(item!!.get() as Questionnaire.QuestionnaireItemComponent?)
    }

    override fun addItems(items: MutableList<IQuestionnaireItemComponentAdapter?>?) {
        items!!
            .map { obj -> obj!!.get() }
            .map { obj -> Questionnaire.QuestionnaireItemComponent::class.java.cast(obj) }
            .forEach { item -> this.questionnaire.addItem(item) }
    }
}
