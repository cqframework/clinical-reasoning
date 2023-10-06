package org.opencds.cqf.fhir.cr.questionnaire.r4.processor.packager;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Questionnaire;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.r4.PackageHelper;
import org.opencds.cqf.fhir.utility.r4.SearchHelper;

public class PackageService {
    final Repository myRepository;

    public static PackageService of(Repository theRepository) {
        return new PackageService(theRepository);
    }

    private PackageService(Repository theRepository) {
        myRepository = theRepository;
    }

    public Bundle packageQuestionnaire(Questionnaire theQuestionnaire, boolean isPut) {
        // TODO: this is incomplete and needs to be abstracted further
        final Bundle bundle = new Bundle();
        bundle.setType(BundleType.TRANSACTION);
        bundle.addEntry(PackageHelper.createEntry(theQuestionnaire, isPut));
        var libraryExtension = theQuestionnaire.getExtensionByUrl(Constants.CQF_LIBRARY);
        if (libraryExtension != null) {
            var libraryCanonical = (CanonicalType) libraryExtension.getValue();
            var library = (Library) SearchHelper.searchRepositoryByCanonical(myRepository, libraryCanonical);
            if (library != null) {
                bundle.addEntry(PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    PackageHelper.addRelatedArtifacts(bundle, library.getRelatedArtifact(), myRepository, isPut);
                }
            }
        }
        return bundle;
    }
}
