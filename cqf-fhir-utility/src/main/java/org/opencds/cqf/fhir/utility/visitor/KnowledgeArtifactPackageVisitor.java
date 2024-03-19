package org.opencds.cqf.fhir.utility.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.DataFormatException;
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
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseIntegerDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.r4.PackageHelper;

public class KnowledgeArtifactPackageVisitor implements KnowledgeArtifactVisitor {

    @Override
    public IBaseResource visit(LibraryAdapter library, Repository repository, IBaseParameters packageParameters) {
        var fhirVersion = library.get().getStructureFhirVersionEnum();
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

        if ((artifactRoute.isPresent()
                        && !StringUtils.isBlank(artifactRoute.get())
                        && !artifactRoute.get().isEmpty())
                || (endpointUri.isPresent()
                        && !StringUtils.isBlank(endpointUri.get())
                        && !endpointUri.get().isEmpty())
                || endpoint.isPresent()
                || terminologyEndpoint.isPresent()) {
            throw new NotImplementedOperationException(
                    "This repository is not implementing custom Content and Terminology endpoints at this time");
        }
        if (packageOnly.isPresent()) {
            throw new NotImplementedOperationException("This repository is not implementing packageOnly at this time");
        }
        if (count.isPresent() && count.get() < 0) {
            throw new UnprocessableEntityException("'count' must be non-negative");
        }
        var resource = library.get();
        // TODO: In the case of a released (active) root Library we can depend on the relatedArtifacts as a
        // comprehensive manifest
        var packagedBundle = BundleHelper.newBundle(fhirVersion);
        if (include.size() == 1 && include.stream().anyMatch((includedType) -> includedType.equals("artifact"))) {
            findUnsupportedCapability(library, capability);
            processCanonicals(library, artifactVersion, checkArtifactVersion, forceArtifactVersion);
            var entry = PackageHelper.createEntry(resource, false);
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
                    forceArtifactVersion);
            var included = findUnsupportedInclude(BundleHelper.getEntry(packagedBundle), include, fhirVersion);
            BundleHelper.setEntry(packagedBundle, included);
        }
        setCorrectBundleType(count, offset, packagedBundle, fhirVersion);
        pageBundleBasedOnCountAndOffset(count, offset, packagedBundle);
        return packagedBundle;

        // DependencyInfo --document here that there is a need for figuring out how to determine which package the
        // dependency is in.
        // what is dependency, where did it originate? potentially the package?
    }

    void recursivePackage(
            IDomainResource resource,
            IBaseBundle bundle,
            Repository repository,
            List<String> capability,
            List<String> include,
            List<String> artifactVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion)
            throws PreconditionFailedException {
        if (resource != null) {
            var fhirVersion = resource.getStructureFhirVersionEnum();
            var adapter = AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter(resource);
            findUnsupportedCapability(adapter, capability);
            processCanonicals(adapter, artifactVersion, checkArtifactVersion, forceArtifactVersion);
            boolean entryExists = BundleHelper.getEntryResources(bundle).stream()
                    .map(e -> AdapterFactory.forFhirVersion(fhirVersion)
                            .createKnowledgeArtifactAdapter((IDomainResource) e))
                    .filter(mr -> mr.getUrl() != null && mr.getVersion() != null)
                    .anyMatch(mr -> mr.getUrl().equals(adapter.getUrl())
                            && mr.getVersion().equals(adapter.getVersion()));
            if (!entryExists) {
                var entry = PackageHelper.createEntry(resource, false);
                BundleHelper.addEntry(bundle, entry);
            }

            adapter.combineComponentsAndDependencies().stream()
                    // sometimes VS dependencies aren't FHIR resources
                    .filter(ra -> !StringUtils.isBlank(Canonicals.getResourceType(ra.getReference())))
                    .filter(ra -> {
                        try {
                            var resourceDef = repository
                                    .fhirContext()
                                    .getResourceDefinition(Canonicals.getResourceType(ra.getReference()));
                            return resourceDef != null;
                        } catch (DataFormatException e) {
                            if (e.getMessage().contains("1684")) {
                                return false;
                            } else {
                                throw new DataFormatException(e.getMessage());
                            }
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
                            forceArtifactVersion));
        }
    }

    @Override
    public IBase visit(KnowledgeArtifactAdapter library, Repository repository, IBaseParameters draftParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(
            PlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    @Override
    public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
        throw new NotImplementedOperationException("Not implemented");
    }

    private void findUnsupportedCapability(KnowledgeArtifactAdapter resource, List<String> capability)
            throws PreconditionFailedException {
        if (capability != null && !capability.isEmpty()) {
            List<IBaseExtension<?, ?>> knowledgeCapabilityExtension = resource.get().getExtension().stream()
                    .filter(ext -> ext.getUrl().contains("cqf-knowledgeCapability"))
                    .collect(Collectors.toList());
            if (knowledgeCapabilityExtension.isEmpty()) {
                // consider resource unsupported if it's knowledgeCapability is undefined
                throw new PreconditionFailedException(
                        String.format("Resource with url: '%s' does not specify capability.", resource.getUrl()));
            }
            knowledgeCapabilityExtension.stream()
                    .filter(ext -> !capability.contains(((IPrimitiveType<?>) ext.getValue()).getValue()))
                    .findAny()
                    .ifPresent((ext) -> {
                        throw new PreconditionFailedException(String.format(
                                "Resource with url: '%s' is not one of '%s'.",
                                resource.getUrl(), String.join(", ", capability)));
                    });
        }
    }

    private void processCanonicals(
            KnowledgeArtifactAdapter resource,
            List<String> canonicalVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion)
            throws PreconditionFailedException {
        if (checkArtifactVersion != null && !checkArtifactVersion.isEmpty()) {
            // check throws an error
            findVersionInListMatchingResource(checkArtifactVersion, resource).ifPresent((version) -> {
                if (!resource.getVersion().equals(version)) {
                    throw new PreconditionFailedException(String.format(
                            "Resource with url '%s' has version '%s' but checkVersion specifies '%s'",
                            resource.getUrl(), resource.getVersion(), version));
                }
            });
        } else if (forceArtifactVersion != null && !forceArtifactVersion.isEmpty()) {
            // force just does a silent override
            findVersionInListMatchingResource(forceArtifactVersion, resource)
                    .ifPresent((version) -> resource.setVersion(version));
        } else if (canonicalVersion != null && !canonicalVersion.isEmpty() && !resource.hasVersion()) {
            // canonicalVersion adds a version if it's missing
            findVersionInListMatchingResource(canonicalVersion, resource)
                    .ifPresent((version) -> resource.setVersion(version));
        }
    }

    private Optional<String> findVersionInListMatchingResource(List<String> list, KnowledgeArtifactAdapter resource) {
        return list.stream()
                .filter((canonical) -> Canonicals.getUrl(canonical).equals(resource.getUrl()))
                .map((canonical) -> Canonicals.getVersion(canonical))
                .findAny();
    }

    private void setCorrectBundleType(
            Optional<Integer> count, Optional<Integer> offset, IBaseBundle bundle, FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactPackageVisitor.setCorrectBundleType(
                        count, offset, (org.hl7.fhir.dstu3.model.Bundle) bundle);
                break;
            case R4:
                org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactPackageVisitor.setCorrectBundleType(
                        count, offset, (org.hl7.fhir.r4.model.Bundle) bundle);
                break;
            case R5:
                org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactPackageVisitor.setCorrectBundleType(
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
    private void pageBundleBasedOnCountAndOffset(
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
    private List<? extends IBaseBackboneElement> findUnsupportedInclude(
            List<? extends IBaseBackboneElement> entries, List<String> include, FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return org.opencds.cqf.fhir.utility.visitor.dstu3.KnowledgeArtifactPackageVisitor
                        .findUnsupportedInclude(
                                (List<org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent>) entries, include);
            case R4:
                return org.opencds.cqf.fhir.utility.visitor.r4.KnowledgeArtifactPackageVisitor.findUnsupportedInclude(
                        (List<org.hl7.fhir.r4.model.Bundle.BundleEntryComponent>) entries, include);
            case R5:
                return org.opencds.cqf.fhir.utility.visitor.r5.KnowledgeArtifactPackageVisitor.findUnsupportedInclude(
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
