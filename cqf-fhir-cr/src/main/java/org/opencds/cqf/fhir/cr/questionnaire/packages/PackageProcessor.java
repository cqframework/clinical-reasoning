package org.opencds.cqf.fhir.cr.questionnaire.packages;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.utility.Constants;

import ca.uhn.fhir.context.FhirVersionEnum;

public class PackageProcessor implements IPackageProcessor {
    protected final Repository repository;
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;

    public PackageProcessor(Repository repository, ModelResolver modelResolver) {
        this.repository = repository;
        this.modelResolver = modelResolver;
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource) {
        return packageResource(resource, "POST");
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource, String method) {
        return packageQuestionnaire(resource, method.equals("PUT"));
    }

    protected IBaseBundle packageQuestionnaire(IBaseResource questionnaire, boolean isPut) {
        switch (fhirVersion) {
            case DSTU3:
                return packageDstu3(questionnaire, isPut);
            case R4:
                return packageR4(questionnaire, isPut);
            case R5:
                return packageR5(questionnaire, isPut);

            default:
                return null;
        }
    }

    protected IBaseBundle packageDstu3(IBaseResource questionnaire, Boolean isPut) {
        var bundle = new org.hl7.fhir.dstu3.model.Bundle();
        bundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.TRANSACTION);
        bundle.addEntry(org.opencds.cqf.fhir.utility.dstu3.PackageHelper.createEntry(
                (org.hl7.fhir.dstu3.model.Resource) questionnaire, isPut));
        var libraryExtension =
                ((org.hl7.fhir.dstu3.model.Questionnaire) questionnaire).getExtensionByUrl(Constants.CQF_LIBRARY);
        if (libraryExtension != null) {
            var libraryCanonical = (org.hl7.fhir.dstu3.model.UriType) libraryExtension.getValue();
            var library = (org.hl7.fhir.dstu3.model.Library)
                    org.opencds.cqf.fhir.utility.dstu3.SearchHelper.searchRepositoryByCanonical(
                            repository, libraryCanonical);
            if (library != null) {
                bundle.addEntry(org.opencds.cqf.fhir.utility.dstu3.PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    org.opencds.cqf.fhir.utility.dstu3.PackageHelper.addRelatedArtifacts(
                            bundle, library.getRelatedArtifact(), repository, isPut);
                }
            }
        }
        return bundle;
    }

    protected IBaseBundle packageR4(IBaseResource questionnaire, Boolean isPut) {
        var bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION);
        bundle.addEntry(org.opencds.cqf.fhir.utility.r4.PackageHelper.createEntry(
                (org.hl7.fhir.r4.model.Resource) questionnaire, isPut));
        var libraryExtension =
                ((org.hl7.fhir.r4.model.Questionnaire) questionnaire).getExtensionByUrl(Constants.CQF_LIBRARY);
        if (libraryExtension != null) {
            var libraryCanonical = (org.hl7.fhir.r4.model.CanonicalType) libraryExtension.getValue();
            var library = (org.hl7.fhir.r4.model.Library)
                    org.opencds.cqf.fhir.utility.r4.SearchHelper.searchRepositoryByCanonical(
                            repository, libraryCanonical);
            if (library != null) {
                bundle.addEntry(org.opencds.cqf.fhir.utility.r4.PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    org.opencds.cqf.fhir.utility.r4.PackageHelper.addRelatedArtifacts(
                            bundle, library.getRelatedArtifact(), repository, isPut);
                }
            }
        }
        return bundle;
    }

    protected IBaseBundle packageR5(IBaseResource questionnaire, Boolean isPut) {
        var bundle = new org.hl7.fhir.r5.model.Bundle();
        bundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.TRANSACTION);
        bundle.addEntry(org.opencds.cqf.fhir.utility.r5.PackageHelper.createEntry(
                (org.hl7.fhir.r5.model.Resource) questionnaire, isPut));
        var libraryExtension =
                ((org.hl7.fhir.r5.model.Questionnaire) questionnaire).getExtensionByUrl(Constants.CQF_LIBRARY);
        if (libraryExtension != null) {
            var libraryCanonical = (org.hl7.fhir.r5.model.CanonicalType) libraryExtension.getValue();
            var library = (org.hl7.fhir.r5.model.Library)
                    org.opencds.cqf.fhir.utility.r5.SearchHelper.searchRepositoryByCanonical(
                            repository, libraryCanonical);
            if (library != null) {
                bundle.addEntry(org.opencds.cqf.fhir.utility.r5.PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    org.opencds.cqf.fhir.utility.r5.PackageHelper.addRelatedArtifacts(
                            bundle, library.getRelatedArtifact(), repository, isPut);
                }
            }
        }
        return bundle;
    }
}
