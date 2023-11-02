package org.opencds.cqf.fhir.cr.questionnaire.r5.processor;

import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Questionnaire;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.r5.PackageHelper;
import org.opencds.cqf.fhir.utility.r5.SearchHelper;

public class PackageProcessor {
    final Repository repository;

    public PackageProcessor(Repository  repository) {
        this.repository = repository;
    }

    public Bundle packageQuestionnaire(Questionnaire questionnaire, boolean isPut) {
        // TODO: this is incomplete and needs to be abstracted further
        final Bundle bundle = new Bundle();
        bundle.setType(BundleType.TRANSACTION);
        bundle.addEntry(PackageHelper.createEntry(questionnaire, isPut));
        var libraryExtension = questionnaire.getExtensionByUrl(Constants.CQF_LIBRARY);
        if (libraryExtension != null) {
            var libraryCanonical = (CanonicalType) libraryExtension.getValue();
            var library = (Library) SearchHelper.searchRepositoryByCanonical(repository, libraryCanonical);
            if (library != null) {
                bundle.addEntry(PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    PackageHelper.addRelatedArtifacts(bundle, library.getRelatedArtifact(),
                        repository, isPut);
                }
            }
        }
        return bundle;
    }
}
