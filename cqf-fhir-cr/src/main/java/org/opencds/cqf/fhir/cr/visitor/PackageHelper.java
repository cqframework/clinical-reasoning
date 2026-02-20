package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

/**
 * Helper class for fetching FHIR packages from the package registry (packages.fhir.org).
 */
public class PackageHelper {
    private static final String PACKAGE_REGISTRY_URL = "https://packages.fhir.org";
    private static final int CONNECT_TIMEOUT_MS = 10000; // 10 seconds
    private static final int READ_TIMEOUT_MS = 30000; // 30 seconds
    private static final PackageDownloader DEFAULT_DOWNLOADER = PackageDownloader.defaultDownloader();

    /**
     * Attempts to fetch an ImplementationGuide from the package registry.
     *
     * @param canonical the canonical URL of the ImplementationGuide (may include version)
     * @param fhirContext the FHIR context
     * @return the ImplementationGuide resource, or null if not found
     */
    public static IDomainResource fetchImplementationGuideFromRegistry(String canonical, FhirContext fhirContext) {
        return fetchImplementationGuideFromRegistry(canonical, fhirContext, DEFAULT_DOWNLOADER);
    }

    /**
     * Attempts to fetch an ImplementationGuide from the package registry.
     *
     * @param canonical the canonical URL of the ImplementationGuide (may include version)
     * @param fhirContext the FHIR context
     * @param downloader the package downloader to use for fetching from the registry
     * @return the ImplementationGuide resource, or null if not found
     */
    public static IDomainResource fetchImplementationGuideFromRegistry(
            String canonical, FhirContext fhirContext, PackageDownloader downloader) {
        if (canonical == null || canonical.isEmpty()) {
            return null;
        }

        try {
            // Parse canonical URL to extract package ID and version
            String[] parts = canonical.split("\\|");
            String url = parts[0];
            String version = parts.length > 1 ? parts[1] : null;

            // Extract package ID from URL
            String packageId = extractPackageId(url);
            if (packageId == null) {
                IAdapter.logger.debug("Could not extract package ID from canonical: {}", canonical);
                return null;
            }

            IAdapter.logger.info("Fetching package {} version {} from registry", packageId, version);

            // Determine version to fetch
            String versionToFetch = version != null ? version : "latest";

            // Download package from registry
            String packageUrl = String.format("%s/%s/%s", PACKAGE_REGISTRY_URL, packageId, versionToFetch);
            byte[] packageData = downloader.download(packageUrl);
            if (packageData == null) {
                IAdapter.logger.warn("Failed to download package from {}", packageUrl);
                return null;
            }

            // Extract ImplementationGuide from package
            IDomainResource ig = extractImplementationGuide(packageData, url, fhirContext);
            if (ig != null) {
                IAdapter.logger.info("Successfully fetched ImplementationGuide from package {}", packageId);
                return ig;
            }

            IAdapter.logger.warn("ImplementationGuide not found in package {}", packageId);
            return null;

        } catch (Exception e) {
            IAdapter.logger.debug(
                    "Error fetching ImplementationGuide from package registry for {}: {}", canonical, e.getMessage());
            return null;
        }
    }

    /**
     * Downloads a package from the registry
     */
    private static byte[] downloadPackage(String packageUrl) {
        try {
            URL url = new URL(packageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/tar+gzip");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                IAdapter.logger.debug("Package download failed with HTTP {}: {}", responseCode, packageUrl);
                return null;
            }

            // Read package data
            try (var inputStream = connection.getInputStream()) {
                return inputStream.readAllBytes();
            }

        } catch (Exception e) {
            IAdapter.logger.debug("Error downloading package from {}: {}", packageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the ImplementationGuide from a downloaded package
     */
    private static IDomainResource extractImplementationGuide(
            byte[] packageData, String targetUrl, FhirContext fhirContext) {
        try {
            // The package is a tar.gz file - decompress and extract
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(packageData));
                    BufferedInputStream bis = new BufferedInputStream(gzis);
                    TarArchiveInputStream tais = new TarArchiveInputStream(bis)) {

                TarArchiveEntry entry;
                while ((entry = tais.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    String entryName = entry.getName();

                    // Look for ImplementationGuide resources in the package folder
                    // Packages typically have ImplementationGuide resources in package/ directory
                    if ((entryName.startsWith("package/ImplementationGuide-")
                                    || entryName.contains("/ImplementationGuide-"))
                            && entryName.endsWith(".json")) {
                        // Read the JSON content
                        byte[] entryData = readEntryData(tais, entry);
                        String jsonContent = new String(entryData, StandardCharsets.UTF_8);

                        // Parse as FHIR resource
                        try {
                            var resource = fhirContext.newJsonParser().parseResource(jsonContent);
                            if (resource instanceof IDomainResource) {
                                // Check if this is the right IG by comparing URLs
                                var urlMethod = resource.getClass().getMethod("getUrl");
                                String resourceUrl = (String) urlMethod.invoke(resource);
                                if (targetUrl.equals(resourceUrl)) {
                                    IAdapter.logger.debug("Found matching ImplementationGuide in {}", entryName);
                                    return (IDomainResource) resource;
                                }
                            }
                        } catch (Exception e) {
                            IAdapter.logger.debug(
                                    "Error parsing ImplementationGuide from {}: {}", entryName, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            IAdapter.logger.debug("Error extracting ImplementationGuide from package: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Reads all data from a tar entry
     */
    private static byte[] readEntryData(TarArchiveInputStream tais, TarArchiveEntry entry) throws java.io.IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        long totalRead = 0;
        long size = entry.getSize();

        while (totalRead < size && (bytesRead = tais.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
            totalRead += bytesRead;
        }

        return baos.toByteArray();
    }

    /**
     * Fetches all resources from a package, checking the repository first before falling back to the registry.
     * This includes ValueSets, CodeSystems, StructureDefinitions, and other resources.
     *
     * @param canonical the canonical URL of the ImplementationGuide (may include version)
     * @param fhirContext the FHIR context
     * @param adapterFactory the adapter factory for creating resource adapters
     * @param repository the repository to check first (may be null to skip repository check)
     * @return Map of canonical URL to IKnowledgeArtifactAdapter for all resources in the package
     */
    public static Map<String, IKnowledgeArtifactAdapter> fetchPackageResources(
            String canonical, FhirContext fhirContext, IAdapterFactory adapterFactory, IRepository repository) {
        return fetchPackageResources(canonical, fhirContext, adapterFactory, repository, DEFAULT_DOWNLOADER);
    }

    /**
     * Fetches all resources from a package, checking the repository first before falling back to the registry.
     * This includes ValueSets, CodeSystems, StructureDefinitions, and other resources.
     *
     * @param canonical the canonical URL of the ImplementationGuide (may include version)
     * @param fhirContext the FHIR context
     * @param adapterFactory the adapter factory for creating resource adapters
     * @param repository the repository to check first (may be null to skip repository check)
     * @param downloader the package downloader to use for fetching from the registry
     * @return Map of canonical URL to IKnowledgeArtifactAdapter for all resources in the package
     */
    public static Map<String, IKnowledgeArtifactAdapter> fetchPackageResources(
            String canonical,
            FhirContext fhirContext,
            IAdapterFactory adapterFactory,
            IRepository repository,
            PackageDownloader downloader) {
        Map<String, IKnowledgeArtifactAdapter> resources = new HashMap<>();

        if (canonical == null || canonical.isEmpty()) {
            return resources;
        }

        // First, try to get resources from the repository
        if (repository != null) {
            IAdapter.logger.info("Checking repository for package resources: {}", canonical);
            resources = queryPackageFromRepository(canonical, fhirContext, repository, adapterFactory);
            if (!resources.isEmpty()) {
                IAdapter.logger.info("Found {} resources in repository for package: {}", resources.size(), canonical);
                return resources;
            }
            IAdapter.logger.info("Package not found in repository, will fetch from registry: {}", canonical);
        }

        // If not found in repository, fetch from package registry
        try {
            // Parse canonical URL to extract package ID and version
            String[] parts = canonical.split("\\|");
            String url = parts[0];
            String version = parts.length > 1 ? parts[1] : null;

            // Extract package ID from URL
            String packageId = extractPackageId(url);
            if (packageId == null) {
                IAdapter.logger.debug("Could not extract package ID from canonical: {}", canonical);
                return resources;
            }

            IAdapter.logger.info("Fetching all resources from package {} version {} from registry", packageId, version);

            // Determine version to fetch
            String versionToFetch = version != null ? version : "latest";

            // Download package from registry
            String packageUrl = String.format("%s/%s/%s", PACKAGE_REGISTRY_URL, packageId, versionToFetch);
            byte[] packageData = downloader.download(packageUrl);
            if (packageData == null) {
                IAdapter.logger.warn("Failed to download package from {}", packageUrl);
                return resources;
            }

            // Extract all resources from package
            resources = extractAllResources(packageData, fhirContext, adapterFactory);
            IAdapter.logger.info("Successfully extracted {} resources from package {}", resources.size(), packageId);

        } catch (Exception e) {
            IAdapter.logger.debug(
                    "Error fetching resources from package registry for {}: {}", canonical, e.getMessage());
        }

        return resources;
    }

    /**
     * Extracts all FHIR resources from a downloaded package.
     * Focuses on ValueSet, CodeSystem, StructureDefinition, and ImplementationGuide resources.
     */
    private static Map<String, IKnowledgeArtifactAdapter> extractAllResources(
            byte[] packageData, FhirContext fhirContext, IAdapterFactory adapterFactory) {
        Map<String, IKnowledgeArtifactAdapter> resources = new HashMap<>();

        try {
            // The package is a tar.gz file - decompress and extract
            try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(packageData));
                    BufferedInputStream bis = new BufferedInputStream(gzis);
                    TarArchiveInputStream tais = new TarArchiveInputStream(bis)) {

                TarArchiveEntry entry;
                while ((entry = tais.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    String entryName = entry.getName();

                    // Look for FHIR resources in the package folder
                    // Focus on key resource types: ValueSet, CodeSystem, StructureDefinition, ImplementationGuide
                    if (entryName.startsWith("package/") && entryName.endsWith(".json")) {
                        String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);

                        // Check if this is a resource type we care about
                        if (fileName.startsWith("ValueSet-")
                                || fileName.startsWith("CodeSystem-")
                                || fileName.startsWith("StructureDefinition-")
                                || fileName.startsWith("ImplementationGuide-")) {

                            // Read the JSON content
                            byte[] entryData = readEntryData(tais, entry);
                            String jsonContent = new String(entryData, StandardCharsets.UTF_8);

                            // Parse as FHIR resource
                            try {
                                var resource = fhirContext.newJsonParser().parseResource(jsonContent);
                                if (resource instanceof IDomainResource) {
                                    // Get the canonical URL using reflection
                                    try {
                                        var urlMethod = resource.getClass().getMethod("getUrl");
                                        String resourceUrl = (String) urlMethod.invoke(resource);

                                        if (resourceUrl != null && !resourceUrl.isEmpty()) {
                                            // Create adapter and add to map
                                            var adapter = adapterFactory.createKnowledgeArtifactAdapter(
                                                    (IDomainResource) resource);
                                            resources.put(resourceUrl, adapter);
                                            IAdapter.logger.debug(
                                                    "Extracted {} from {}", resource.fhirType(), entryName);
                                        }
                                    } catch (NoSuchMethodException e) {
                                        // Resource doesn't have getUrl method, skip it
                                        IAdapter.logger.debug("Resource in {} has no URL method", entryName);
                                    }
                                }
                            } catch (Exception e) {
                                IAdapter.logger.debug("Error parsing resource from {}: {}", entryName, e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            IAdapter.logger.debug("Error extracting resources from package: {}", e.getMessage());
        }

        return resources;
    }

    /**
     * Queries the repository for all resources from a package (ImplementationGuide).
     * If the IG is found, retrieves all resources it references via the IG adapter.
     *
     * @param canonical the canonical URL of the ImplementationGuide (may include version)
     * @param fhirContext the FHIR context
     * @param repository the repository to query
     * @param adapterFactory the adapter factory for creating resource adapters
     * @return Map of canonical URL to IKnowledgeArtifactAdapter for all resources in the package
     */
    private static Map<String, IKnowledgeArtifactAdapter> queryPackageFromRepository(
            String canonical, FhirContext fhirContext, IRepository repository, IAdapterFactory adapterFactory) {
        Map<String, IKnowledgeArtifactAdapter> resources = new HashMap<>();

        try {
            // Parse canonical URL to extract URL and version
            String[] parts = canonical.split("\\|");
            String igUrl = parts[0];
            String version = parts.length > 1 ? parts[1] : null;

            IAdapter.logger.debug("Searching repository for ImplementationGuide: {} version: {}", igUrl, version);

            // Build search parameters
            Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
            List<IQueryParameterType> urlList = new ArrayList<>();
            urlList.add(new UriParam(igUrl));
            searchParams.put("url", urlList);

            if (version != null) {
                List<IQueryParameterType> versionList = new ArrayList<>();
                versionList.add(new TokenParam(version));
                searchParams.put("version", versionList);
            }

            // Determine which ImplementationGuide class and Bundle class to use based on FHIR version
            var fhirVersion = fhirContext.getVersion().getVersion();
            IBaseBundle bundle;

            switch (fhirVersion) {
                case DSTU3 -> {
                    try {
                        bundle = repository.search(
                                org.hl7.fhir.dstu3.model.Bundle.class,
                                org.hl7.fhir.dstu3.model.ImplementationGuide.class,
                                searchParams);
                    } catch (ResourceNotFoundException e) {
                        IAdapter.logger.debug("ImplementationGuide not found in repository: {}", canonical);
                        return resources;
                    }
                }
                case R4 -> {
                    try {
                        bundle = repository.search(
                                org.hl7.fhir.r4.model.Bundle.class,
                                org.hl7.fhir.r4.model.ImplementationGuide.class,
                                searchParams);
                    } catch (ResourceNotFoundException e) {
                        IAdapter.logger.debug("ImplementationGuide not found in repository: {}", canonical);
                        return resources;
                    }
                }
                case R5 -> {
                    try {
                        bundle = repository.search(
                                org.hl7.fhir.r5.model.Bundle.class,
                                org.hl7.fhir.r5.model.ImplementationGuide.class,
                                searchParams);
                    } catch (ResourceNotFoundException e) {
                        IAdapter.logger.debug("ImplementationGuide not found in repository: {}", canonical);
                        return resources;
                    }
                }
                default -> {
                    IAdapter.logger.warn("Unsupported FHIR version for IG search: {}", fhirVersion);
                    return resources;
                }
            }

            if (bundle == null || BundleHelper.getEntry(bundle).isEmpty()) {
                IAdapter.logger.debug("ImplementationGuide not found in repository: {}", canonical);
                return resources;
            }

            // Found the IG - get the first match
            var entries = BundleHelper.getEntry(bundle);
            if (entries.isEmpty()) {
                IAdapter.logger.debug("ImplementationGuide not found in repository: {}", canonical);
                return resources;
            }

            // Extract the resource based on FHIR version
            IDomainResource igResource =
                    switch (fhirVersion) {
                        case DSTU3 -> (IDomainResource)
                                ((org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent) entries.get(0)).getResource();
                        case R4 -> (IDomainResource)
                                ((org.hl7.fhir.r4.model.Bundle.BundleEntryComponent) entries.get(0)).getResource();
                        case R5 -> (IDomainResource)
                                ((org.hl7.fhir.r5.model.Bundle.BundleEntryComponent) entries.get(0)).getResource();
                        default -> null;
                    };

            if (igResource == null) {
                IAdapter.logger.debug("ImplementationGuide resource is null: {}", canonical);
                return resources;
            }

            var igAdapter = (IImplementationGuideAdapter) adapterFactory.createKnowledgeArtifactAdapter(igResource);

            IAdapter.logger.debug("Found ImplementationGuide in repository: {}", igAdapter.getUrl());

            // Use the IG adapter's method to retrieve referenced libraries (and other resources)
            // This leverages the existing repository query logic in the adapter
            var libraries = igAdapter.retrieveReferencedLibraries(repository);
            resources.putAll(libraries);

            IAdapter.logger.info("Retrieved {} resources from repository for package: {}", resources.size(), canonical);

        } catch (Exception e) {
            IAdapter.logger.debug("Error querying package from repository for {}: {}", canonical, e.getMessage());
        }

        return resources;
    }

    /**
     * Extracts the resource type from a canonical URL.
     * Examples:
     * - http://hl7.org/fhir/ValueSet/administrative-gender -> ValueSet
     * - http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient -> StructureDefinition
     */
    private static String extractResourceType(String canonical) {
        if (canonical == null || canonical.isEmpty()) {
            return null;
        }

        // Remove version if present
        String url = canonical.split("\\|")[0];

        // Common patterns: .../ResourceType/id or .../ResourceType-id
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            // Look for a part that looks like a resource type (starts with capital letter)
            for (int i = parts.length - 2; i >= 0; i--) {
                String part = parts[i];
                if (!part.isEmpty() && Character.isUpperCase(part.charAt(0))) {
                    // Check if it's a known resource type
                    if (isKnownResourceType(part)) {
                        return part;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if a string is a known FHIR resource type.
     */
    private static boolean isKnownResourceType(String type) {
        return switch (type) {
            case "ValueSet",
                    "CodeSystem",
                    "StructureDefinition",
                    "Library",
                    "Measure",
                    "PlanDefinition",
                    "ActivityDefinition",
                    "Questionnaire",
                    "ImplementationGuide",
                    "SearchParameter" -> true;
            default -> false;
        };
    }

    /**
     * Gets the resource class for a given resource type and FHIR version.
     */
    private static Class<? extends IDomainResource> getResourceClass(
            String resourceType, ca.uhn.fhir.context.FhirVersionEnum fhirVersion) {
        return switch (fhirVersion) {
            case DSTU3 -> switch (resourceType) {
                case "ValueSet" -> org.hl7.fhir.dstu3.model.ValueSet.class;
                case "CodeSystem" -> org.hl7.fhir.dstu3.model.CodeSystem.class;
                case "StructureDefinition" -> org.hl7.fhir.dstu3.model.StructureDefinition.class;
                case "Library" -> org.hl7.fhir.dstu3.model.Library.class;
                case "Measure" -> org.hl7.fhir.dstu3.model.Measure.class;
                case "PlanDefinition" -> org.hl7.fhir.dstu3.model.PlanDefinition.class;
                case "ActivityDefinition" -> org.hl7.fhir.dstu3.model.ActivityDefinition.class;
                case "Questionnaire" -> org.hl7.fhir.dstu3.model.Questionnaire.class;
                case "SearchParameter" -> org.hl7.fhir.dstu3.model.SearchParameter.class;
                default -> null;
            };
            case R4 -> switch (resourceType) {
                case "ValueSet" -> org.hl7.fhir.r4.model.ValueSet.class;
                case "CodeSystem" -> org.hl7.fhir.r4.model.CodeSystem.class;
                case "StructureDefinition" -> org.hl7.fhir.r4.model.StructureDefinition.class;
                case "Library" -> org.hl7.fhir.r4.model.Library.class;
                case "Measure" -> org.hl7.fhir.r4.model.Measure.class;
                case "PlanDefinition" -> org.hl7.fhir.r4.model.PlanDefinition.class;
                case "ActivityDefinition" -> org.hl7.fhir.r4.model.ActivityDefinition.class;
                case "Questionnaire" -> org.hl7.fhir.r4.model.Questionnaire.class;
                case "SearchParameter" -> org.hl7.fhir.r4.model.SearchParameter.class;
                default -> null;
            };
            case R5 -> switch (resourceType) {
                case "ValueSet" -> org.hl7.fhir.r5.model.ValueSet.class;
                case "CodeSystem" -> org.hl7.fhir.r5.model.CodeSystem.class;
                case "StructureDefinition" -> org.hl7.fhir.r5.model.StructureDefinition.class;
                case "Library" -> org.hl7.fhir.r5.model.Library.class;
                case "Measure" -> org.hl7.fhir.r5.model.Measure.class;
                case "PlanDefinition" -> org.hl7.fhir.r5.model.PlanDefinition.class;
                case "ActivityDefinition" -> org.hl7.fhir.r5.model.ActivityDefinition.class;
                case "Questionnaire" -> org.hl7.fhir.r5.model.Questionnaire.class;
                case "SearchParameter" -> org.hl7.fhir.r5.model.SearchParameter.class;
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * Extracts the package ID from a canonical URL.
     *
     * Examples:
     * - http://hl7.org/fhir/us/core/ImplementationGuide/hl7.fhir.us.core -> hl7.fhir.us.core
     * - http://terminology.hl7.org/ImplementationGuide/hl7.terminology -> hl7.terminology
     */
    private static String extractPackageId(String url) {
        if (url == null) {
            return null;
        }

        // The package ID is typically the last segment after the last slash
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }

        return null;
    }
}
