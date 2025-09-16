package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static org.opencds.cqf.fhir.utility.Resources.newBaseForVersion;
import static org.opencds.cqf.fhir.utility.SearchHelper.searchRepositoryByCanonical;
import static org.opencds.cqf.fhir.utility.VersionUtilities.canonicalTypeForVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetConceptReferenceAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetExpansionContainsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ItemTypeIsChoice {
    protected static final Logger logger = LoggerFactory.getLogger(ItemTypeIsChoice.class);
    protected final IRepository repository;
    protected final IAdapterFactory adapterFactory;

    public ItemTypeIsChoice(IRepository repository) {
        this.repository = repository;
        adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
    }

    private FhirVersionEnum fhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }

    public void addProperties(IElementDefinitionAdapter element, IQuestionnaireItemComponentAdapter item) {
        final IValueSetAdapter valueSet = getValueSet(element);
        if (valueSet != null) {
            if (valueSet.hasExpansion()) {
                addAnswerOptionsForValueSetWithExpansionComponent(valueSet, item);
            } else {
                addAnswerOptionsForValueSetWithComposeComponent(valueSet, item);
            }
        }
    }

    protected void addAnswerOptionsForValueSetWithExpansionComponent(
            IValueSetAdapter valueSet, IQuestionnaireItemComponentAdapter item) {
        var expansionList = valueSet.getExpansionContains();
        expansionList.forEach(expansion -> {
            var coding = getCodingFromExpansion(expansion);
            if (coding != null) {
                item.addAnswerOption(coding);
            }
        });
    }

    protected void addAnswerOptionsForValueSetWithComposeComponent(
            IValueSetAdapter valueSet, IQuestionnaireItemComponentAdapter item) {
        var conceptSets = valueSet.getComposeInclude();
        conceptSets.forEach(conceptSet -> {
            var systemUri = conceptSet.getSystem();
            conceptSet.getConcept().forEach(concept -> {
                var coding = getCodingFromConcept(concept, systemUri);
                if (coding != null) {
                    item.addAnswerOption(coding);
                }
            });
        });
    }

    protected ICodingAdapter getCodingFromConcept(IValueSetConceptReferenceAdapter code, String systemUri) {
        return adapterFactory
                .createCoding(newBaseForVersion("Coding", fhirVersion()))
                .setCode(code.getCode())
                .setSystem(systemUri)
                .setDisplay(code.getDisplay());
    }

    protected ICodingAdapter getCodingFromExpansion(IValueSetExpansionContainsAdapter code) {
        return adapterFactory
                .createCoding(newBaseForVersion("Coding", fhirVersion()))
                .setCode(code.getCode())
                .setSystem(code.getSystem())
                .setDisplay(code.getDisplay());
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
