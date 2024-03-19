package org.opencds.cqf.fhir.cr.plandefinition.packages;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.utility.BundleHelper;

public class PackageProcessor implements IPackageProcessor {
    protected final Repository repository;
    protected final FhirVersionEnum fhirVersion;

    public PackageProcessor(Repository repository) {
        this.repository = repository;
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource) {
        return packageResource(resource, "POST");
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource, String method) {
        return packagePlanDefinition(resource, method.equals("PUT"));
    }

    protected IBaseBundle packagePlanDefinition(IBaseResource planDefinition, boolean isPut) {
        switch (fhirVersion) {
            case DSTU3:
                return packageDstu3((org.hl7.fhir.dstu3.model.PlanDefinition) planDefinition, isPut);
            case R4:
                return packageR4((org.hl7.fhir.r4.model.PlanDefinition) planDefinition, isPut);
            case R5:
                return packageR5((org.hl7.fhir.r5.model.PlanDefinition) planDefinition, isPut);

            default:
                return null;
        }
    }

    protected IBaseBundle packageDstu3(org.hl7.fhir.dstu3.model.PlanDefinition planDefinition, boolean isPut) {
        var packageBundle = new org.hl7.fhir.dstu3.model.Bundle();
        packageBundle.setType(org.hl7.fhir.dstu3.model.Bundle.BundleType.TRANSACTION);
        BundleHelper.addEntry(
                packageBundle, org.opencds.cqf.fhir.utility.dstu3.PackageHelper.createEntry(planDefinition, isPut));
        // The CPG IG specifies a main cql library for a PlanDefinition
        var libraryCanonical = planDefinition.hasLibrary()
                ? new org.hl7.fhir.dstu3.model.StringType(
                        planDefinition.getLibrary().get(0).getReference())
                : null;
        if (libraryCanonical != null) {
            var library = (org.hl7.fhir.dstu3.model.Library)
                    org.opencds.cqf.fhir.utility.dstu3.SearchHelper.searchRepositoryByCanonical(
                            repository, libraryCanonical);
            if (library != null) {
                BundleHelper.addEntry(
                        packageBundle, org.opencds.cqf.fhir.utility.dstu3.PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    org.opencds.cqf.fhir.utility.dstu3.PackageHelper.addRelatedArtifacts(
                            packageBundle, library.getRelatedArtifact(), repository, isPut);
                }
            }
        }
        if (planDefinition.hasRelatedArtifact()) {
            org.opencds.cqf.fhir.utility.dstu3.PackageHelper.addRelatedArtifacts(
                    packageBundle, planDefinition.getRelatedArtifact(), repository, isPut);
        }

        return packageBundle;
    }

    protected IBaseBundle packageR4(org.hl7.fhir.r4.model.PlanDefinition planDefinition, boolean isPut) {
        var packageBundle = new org.hl7.fhir.r4.model.Bundle();
        packageBundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION);
        packageBundle.addEntry((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent)
                org.opencds.cqf.fhir.utility.PackageHelper.createEntry(planDefinition, isPut));
        // The CPG IG specifies a main cql library for a PlanDefinition
        var libraryCanonical =
                planDefinition.hasLibrary() ? planDefinition.getLibrary().get(0) : null;
        if (libraryCanonical != null) {
            var library = (org.hl7.fhir.r4.model.Library)
                    org.opencds.cqf.fhir.utility.r4.SearchHelper.searchRepositoryByCanonical(
                            repository, libraryCanonical);
            if (library != null) {
                packageBundle.addEntry((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent)
                        org.opencds.cqf.fhir.utility.PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    org.opencds.cqf.fhir.utility.r4.PackageHelper.addRelatedArtifacts(
                            packageBundle, library.getRelatedArtifact(), repository, isPut);
                }
            }
        }
        if (planDefinition.hasRelatedArtifact()) {
            org.opencds.cqf.fhir.utility.r4.PackageHelper.addRelatedArtifacts(
                    packageBundle, planDefinition.getRelatedArtifact(), repository, isPut);
        }

        return packageBundle;
    }

    protected IBaseBundle packageR5(org.hl7.fhir.r5.model.PlanDefinition planDefinition, boolean isPut) {
        var packageBundle = new org.hl7.fhir.r5.model.Bundle();
        packageBundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.TRANSACTION);
        packageBundle.addEntry((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent)
                org.opencds.cqf.fhir.utility.PackageHelper.createEntry(planDefinition, isPut));
        // The CPG IG specifies a main cql library for a PlanDefinition
        var libraryCanonical =
                planDefinition.hasLibrary() ? planDefinition.getLibrary().get(0) : null;
        if (libraryCanonical != null) {
            var library = (org.hl7.fhir.r5.model.Library)
                    org.opencds.cqf.fhir.utility.r5.SearchHelper.searchRepositoryByCanonical(
                            repository, libraryCanonical);
            if (library != null) {
                packageBundle.addEntry((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent)
                        org.opencds.cqf.fhir.utility.PackageHelper.createEntry(library, isPut));
                if (library.hasRelatedArtifact()) {
                    org.opencds.cqf.fhir.utility.r5.PackageHelper.addRelatedArtifacts(
                            packageBundle, library.getRelatedArtifact(), repository, isPut);
                }
            }
        }
        if (planDefinition.hasRelatedArtifact()) {
            org.opencds.cqf.fhir.utility.r5.PackageHelper.addRelatedArtifacts(
                    packageBundle, planDefinition.getRelatedArtifact(), repository, isPut);
        }

        return packageBundle;
    }
}
