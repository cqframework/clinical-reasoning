package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetExpansionContainsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemTypeIsChoice {
    protected static final Logger logger = LoggerFactory.getLogger(ItemTypeIsChoice.class);
    protected final Repository repository;
    protected final IAdapterFactory adapterFactory;

    public ItemTypeIsChoice(Repository repository) {
        this.repository = repository;
        adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
    }

    private FhirVersionEnum fhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }

    public IBaseBackboneElement addProperties(IElementDefinitionAdapter element, IBaseBackboneElement item) {
        final IValueSetAdapter valueSet = getValueSet(element);
        if (valueSet != null) {
            if (valueSet.hasExpansion()) {
                addAnswerOptionsForValueSetWithExpansionComponent(valueSet, item);
            } else {
                addAnswerOptionsForValueSetWithComposeComponent(valueSet, item);
            }
        } else {
            // Is it possible to get the binding without the value set?
        }
        return item;
    }

    protected void addAnswerOptionsForValueSetWithExpansionComponent(
            IValueSetAdapter valueSet, IBaseBackboneElement item) {
        var expansionList = valueSet.getExpansionContains();
        expansionList.forEach(expansion -> {
            var coding = getCodingFromExpansion(expansion);
            if (coding != null) {
                addAnswerOption(item, coding);
            }
        });
    }

    protected void addAnswerOptionsForValueSetWithComposeComponent(
            IValueSetAdapter valueSet, IBaseBackboneElement item) {
        var conceptSets = valueSet.getComposeInclude();
        conceptSets.forEach(conceptSet -> {
            var systemUri = conceptSet.getSystem();
            conceptSet.getConcept().forEach(concept -> {
                var coding = getCodingFromConcept(concept, systemUri);
                if (coding != null) {
                    addAnswerOption(item, coding);
                }
            });
        });
    }

    protected void addAnswerOption(IBaseBackboneElement item, ICodingAdapter coding) {
        if (item instanceof org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent r4Item) {
            r4Item.addAnswerOption().setValue((org.hl7.fhir.r4.model.Coding) coding.get());
        } else if (item instanceof org.hl7.fhir.r5.model.Questionnaire.QuestionnaireItemComponent r5Item) {
            r5Item.addAnswerOption().setValue((org.hl7.fhir.r5.model.Coding) coding.get());
        }
    }

    protected ICodingAdapter getCodingFromConcept(IValueSetConceptReferenceAdapter code, String systemUri) {
        if (code instanceof org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent r4Code) {
            return adapterFactory.createCoding(new org.hl7.fhir.r4.model.Coding()
                    .setCode(r4Code.getCode())
                    .setSystem(systemUri)
                    .setDisplay(r4Code.getDisplay()));
        }
        if (code instanceof org.hl7.fhir.r5.model.ValueSet.ConceptReferenceComponent r5Code) {
            return adapterFactory.createCoding(new org.hl7.fhir.r5.model.Coding()
                    .setCode(r5Code.getCode())
                    .setSystem(systemUri)
                    .setDisplay(r5Code.getDisplay()));
        }
        return null;
    }

    protected ICodingAdapter getCodingFromExpansion(IValueSetExpansionContainsAdapter code) {
        if (code instanceof org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent r4Code) {
            return adapterFactory.createCoding(new org.hl7.fhir.r4.model.Coding()
                    .setCode(r4Code.getCode())
                    .setSystem(r4Code.getSystem())
                    .setDisplay(r4Code.getDisplay()));
        }
        if (code instanceof org.hl7.fhir.r5.model.ValueSet.ValueSetExpansionContainsComponent r5Code) {
            return adapterFactory.createCoding(new org.hl7.fhir.r5.model.Coding()
                    .setCode(r5Code.getCode())
                    .setSystem(r5Code.getSystem())
                    .setDisplay(r5Code.getDisplay()));
        }
        return null;
    }

    protected IValueSetAdapter getValueSet(IElementDefinitionAdapter element) {
        if (element.hasBinding()) {
            try {
                final var valueSetUrl = canonicalTypeForVersion(fhirVersion(), element.getBindingValueSet());
                return (IValueSetAdapter) adapterFactory.createKnowledgeArtifactAdapter(
                        (IDomainResource) searchRepositoryByCanonical(repository, valueSetUrl));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }
}
