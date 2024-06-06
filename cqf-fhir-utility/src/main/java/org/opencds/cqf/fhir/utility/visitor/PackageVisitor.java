package org.opencds.cqf.fhir.utility.visitor;

import static org.opencds.cqf.fhir.utility.visitor.VisitorHelper.findUnsupportedCapability;
import static org.opencds.cqf.fhir.utility.visitor.VisitorHelper.processCanonicals;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseIntegerDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;

public class PackageVisitor implements KnowledgeArtifactVisitor {

    @Override
    public IBase visit(KnowledgeArtifactAdapter adapter, Repository repository, IBaseParameters packageParameters) {
        var fhirVersion = adapter.get().getStructureFhirVersionEnum();

        Optional<String> artifactRoute = VisitorHelper.getParameter(
                        "artifactRoute", packageParameters, IPrimitiveType.class)
                .map(r -> (String) r.getValue());
        Optional<String> endpointUri = VisitorHelper.getParameter(
                        "endpointUri", packageParameters, IPrimitiveType.class)
                .map(r -> (String) r.getValue());
        Optional<IBaseResource> endpoint =
                VisitorHelper.getResourceParameter("endpoint", packageParameters, IBaseResource.class);
        Optional<IBaseResource> terminologyEndpoint =
                VisitorHelper.getResourceParameter("terminologyEndpoint", packageParameters, IBaseResource.class);
        Optional<Boolean> packageOnly = VisitorHelper.getParameter(
                        "packageOnly", packageParameters, IBaseBooleanDatatype.class)
                .map(r -> r.getValue());
        Optional<Integer> count = VisitorHelper.getParameter("count", packageParameters, IBaseIntegerDatatype.class)
                .map(r -> r.getValue());
        Optional<Integer> offset = VisitorHelper.getParameter("offset", packageParameters, IBaseIntegerDatatype.class)
                .map(r -> r.getValue());
        List<String> include = VisitorHelper.getListParameter("include", packageParameters, IPrimitiveType.class)
                .map(list -> list.stream().map(r -> (String) r.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> capability = VisitorHelper.getListParameter("capability", packageParameters, IPrimitiveType.class)
                .map(list -> list.stream().map(r -> (String) r.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> artifactVersion = VisitorHelper.getListParameter(
                        "artifactVersion", packageParameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> checkArtifactVersion = VisitorHelper.getListParameter(
                        "checkArtifactVersion", packageParameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        List<String> forceArtifactVersion = VisitorHelper.getListParameter(
                        "forceArtifactVersion", packageParameters, IPrimitiveType.class)
                .map(l -> l.stream().map(t -> (String) t.getValue()).collect(Collectors.toList()))
                .orElseGet(() -> new ArrayList<>());
        boolean isPut = VisitorHelper.getParameter("isPut", packageParameters, IBaseBooleanDatatype.class)
                .map(r -> r.getValue())
                .orElse(false);

        if ((artifactRoute.isPresent()
                        && !StringUtils.isBlank(artifactRoute.get())
                        && !artifactRoute.get().isEmpty())
                || (endpointUri.isPresent()
                        && !StringUtils.isBlank(endpointUri.get())
                        && !endpointUri.get().isEmpty())
                || endpoint.isPresent()) {
            throw new NotImplementedOperationException(
                    "This repository is not implementing custom Content and endpoints at this time");
        }
        if (packageOnly.isPresent()) {
            throw new NotImplementedOperationException("This repository is not implementing packageOnly at this time");
        }
        if (count.isPresent() && count.get() < 0) {
            throw new UnprocessableEntityException("'count' must be non-negative");
        }
        var resource = adapter.get();
        // TODO: In the case of a released (active) root Library we can depend on the relatedArtifacts as a
        // comprehensive manifest
        var packagedBundle = BundleHelper.newBundle(fhirVersion);
        if (include.size() == 1 && include.stream().anyMatch((includedType) -> includedType.equals("artifact"))) {
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, artifactVersion, checkArtifactVersion, forceArtifactVersion);
            var entry = PackageHelper.createEntry(resource, isPut);
            BundleHelper.addEntry(packagedBundle, entry);
        } else {
            recursivePackage(
                    resource,
                    packagedBundle,
                    repository,
                    capability,
                    include,
                    artifactVersion,
                    checkArtifactVersion,
                    forceArtifactVersion,
                    isPut);
            var included = findUnsupportedInclude(BundleHelper.getEntry(packagedBundle), include, fhirVersion);
            BundleHelper.setEntry(packagedBundle, included);
        }
        handleValueSets(resource, packagedBundle, repository, terminologyEndpoint);
        setCorrectBundleType(count, offset, packagedBundle, fhirVersion);
        pageBundleBasedOnCountAndOffset(count, offset, packagedBundle);
        return packagedBundle;

        // DependencyInfo --document here that there is a need for figuring out how to determine which package the
        // dependency is in.
        // what is dependency, where did it originate? potentially the package?
    }

    protected void handleValueSets(
            IDomainResource resource,
            IBaseBundle packagedBundle,
            Repository repository,
            Optional<IBaseResource> terminologyEndpoint) {
        switch (resource.getStructureFhirVersionEnum()) {
            case DSTU3:
                org.opencds.cqf.fhir.utility.visitor.dstu3.PackageVisitor packageVisitorDstu3 =
                        new org.opencds.cqf.fhir.utility.visitor.dstu3.PackageVisitor();
                packageVisitorDstu3.handleValueSets(
                        (org.hl7.fhir.dstu3.model.MetadataResource) resource,
                        ((org.hl7.fhir.dstu3.model.Bundle) packagedBundle).getEntry(),
                        repository,
                        terminologyEndpoint.map(te -> (org.hl7.fhir.dstu3.model.Endpoint) te));
                break;
            case R4:
                org.opencds.cqf.fhir.utility.visitor.r4.PackageVisitor packageVisitorR4 =
                        new org.opencds.cqf.fhir.utility.visitor.r4.PackageVisitor();
                packageVisitorR4.handleValueSets(
                        (org.hl7.fhir.r4.model.MetadataResource) resource,
                        ((org.hl7.fhir.r4.model.Bundle) packagedBundle).getEntry(),
                        repository,
                        terminologyEndpoint.map(te -> (org.hl7.fhir.r4.model.Endpoint) te));
                break;
            case R5:
                org.opencds.cqf.fhir.utility.visitor.r5.PackageVisitor packageVisitorR5 =
                        new org.opencds.cqf.fhir.utility.visitor.r5.PackageVisitor();
                packageVisitorR5.handleValueSets(
                        (org.hl7.fhir.r5.model.MetadataResource) resource,
                        ((org.hl7.fhir.r5.model.Bundle) packagedBundle).getEntry(),
                        repository,
                        terminologyEndpoint.map(te -> (org.hl7.fhir.r5.model.Endpoint) te));
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "Unsupported FHIR version: %s",
                        resource.getStructureFhirVersionEnum().getFhirVersionString()));
        }
    }

    protected void recursivePackage(
            IDomainResource resource,
            IBaseBundle bundle,
            Repository repository,
            List<String> capability,
            List<String> include,
            List<String> artifactVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion,
            boolean isPut)
            throws PreconditionFailedException {
        if (resource != null) {
            var fhirVersion = resource.getStructureFhirVersionEnum();
            var adapter = AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(resource);
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, artifactVersion, checkArtifactVersion, forceArtifactVersion);
            boolean entryExists = BundleHelper.getEntryResources(bundle).stream()
                    .map(e -> AdapterFactory.forFhirVersion(fhirVersion)
                            .createKnowledgeArtifactAdapter((IDomainResource) e))
                    .filter(mr -> mr.getUrl() != null)
                    .anyMatch(mr -> mr.getUrl().equals(adapter.getUrl())
                            && (!mr.hasVersion() || mr.getVersion().equals(adapter.getVersion())));
            if (!entryExists) {
                var entry = PackageHelper.createEntry(resource, isPut);
                BundleHelper.addEntry(bundle, entry);
            }

            var dependencies = adapter.combineComponentsAndDependencies();
            dependencies.stream()
                    // sometimes VS dependencies aren't FHIR resources, only include references that are
                    .filter(ra -> {
                        try {
                            return null
                                    != repository
                                            .fhirContext()
                                            .getResourceDefinition(Canonicals.getResourceType(ra.getReference()));
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(ra -> SearchHelper.searchRepositoryByCanonicalWithPaging(repository, ra.getReference()))
                    .map(searchBundle -> (IDomainResource) BundleHelper.getEntryResourceFirstRep(searchBundle))
                    .forEach(component -> recursivePackage(
                            component,
                            bundle,
                            repository,
                            capability,
                            include,
                            artifactVersion,
                            checkArtifactVersion,
                            forceArtifactVersion,
                            isPut));
        }
    }

    protected void setCorrectBundleType(
            Optional<Integer> count, Optional<Integer> offset, IBaseBundle bundle, FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                org.opencds.cqf.fhir.utility.visitor.dstu3.PackageVisitor.setCorrectBundleType(
                        count, offset, (org.hl7.fhir.dstu3.model.Bundle) bundle);
                break;
            case R4:
                org.opencds.cqf.fhir.utility.visitor.r4.PackageVisitor.setCorrectBundleType(
                        count, offset, (org.hl7.fhir.r4.model.Bundle) bundle);
                break;
            case R5:
                org.opencds.cqf.fhir.utility.visitor.r5.PackageVisitor.setCorrectBundleType(
                        count, offset, (org.hl7.fhir.r5.model.Bundle) bundle);
                break;
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }

    /**
     * $package allows for a bundle to be paged
     * @param count the maximum number of resources to be returned
     * @param offset the number of resources to skip beginning from the start of the bundle (starts from 1)
     * @param bundle the bundle to page
     */
    protected void pageBundleBasedOnCountAndOffset(
            Optional<Integer> count, Optional<Integer> offset, IBaseBundle bundle) {
        if (offset.isPresent()) {
            var entries = BundleHelper.getEntry(bundle);
            Integer bundleSize = entries.size();
            if (offset.get() < bundleSize) {
                BundleHelper.setEntry(bundle, entries.subList(offset.get(), bundleSize));
            } else {
                BundleHelper.setEntry(bundle, Arrays.asList());
            }
        }
        if (count.isPresent()) {
            // repeat these two from earlier because we might modify / replace the entries list at any time
            var entries = BundleHelper.getEntry(bundle);
            Integer bundleSize = entries.size();
            if (count.get() < bundleSize) {
                BundleHelper.setEntry(bundle, entries.subList(0, count.get()));
            } else {
                // there are not enough entries in the bundle to page, so we return all of them no change
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected List<? extends IBaseBackboneElement> findUnsupportedInclude(
            List<? extends IBaseBackboneElement> entries, List<String> include, FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return org.opencds.cqf.fhir.utility.visitor.dstu3.PackageVisitor.findUnsupportedInclude(
                        (List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent>) entries, include);
            case R4:
                return org.opencds.cqf.fhir.utility.visitor.r4.PackageVisitor.findUnsupportedInclude(
                        (List<org.hl7.fhir.r4.model.Bundle.BundleEntryComponent>) entries, include);
            case R5:
                return org.opencds.cqf.fhir.utility.visitor.r5.PackageVisitor.findUnsupportedInclude(
                        (List<org.hl7.fhir.r5.model.Bundle.BundleEntryComponent>) entries, include);
            case DSTU2:
            case DSTU2_1:
            case DSTU2_HL7ORG:
            default:
                throw new UnprocessableEntityException(
                        String.format("Unsupported version of FHIR: %s", fhirVersion.getFhirVersionString()));
        }
    }
}
