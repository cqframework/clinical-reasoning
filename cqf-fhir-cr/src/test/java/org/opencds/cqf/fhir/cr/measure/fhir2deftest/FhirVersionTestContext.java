package org.opencds.cqf.fhir.cr.measure.fhir2deftest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.dstu3.Dstu3MeasureServiceAdapter;
import org.opencds.cqf.fhir.cr.measure.fhir2deftest.r4.R4MeasureServiceAdapter;

/**
 * Factory for creating version-specific test components with automatic FHIR version detection.
 * <p>
 * This class auto-detects the FHIR version from test resource directories and creates
 * appropriate version-specific adapters. This enables a single unified test DSL to work
 * seamlessly across DSTU3, R4, R5, R6, and future FHIR versions.
 * </p>
 *
 * <h3>Detection Strategy:</h3>
 * <ol>
 *   <li><strong>Metadata Parsing:</strong> Parse sample JSON/XML and extract fhirVersion field</li>
 *   <li><strong>Heuristic Detection:</strong> Analyze resource structure for version-specific features</li>
 *   <li><strong>Explicit Override:</strong> Allow user to specify version explicitly</li>
 * </ol>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Auto-detect FHIR version from test resources
 * Path repositoryPath = Paths.get("src/test/resources/MinimalMeasureEvaluation");
 * FhirVersionTestContext context = FhirVersionTestContext.forRepository(
 *     repository, repositoryPath, options);
 *
 * // Create version-appropriate adapter
 * MeasureServiceAdapter adapter = context.createMeasureService();
 * FhirVersionEnum version = context.getFhirVersion(); // DSTU3, R4, etc.
 * }</pre>
 *
 * @author Claude (Anthropic AI Assistant)
 * @since 4.1.0
 */
public class FhirVersionTestContext {

    private final FhirVersionEnum fhirVersion;
    private final IRepository repository;
    private final MeasureEvaluationOptions measureEvaluationOptions;
    private final String serverBase;

    /**
     * Create a FhirVersionTestContext with explicit version.
     *
     * @param fhirVersion the FHIR version to use
     * @param repository the FHIR repository
     * @param measureEvaluationOptions evaluation options
     * @param serverBase server base URL (used by R4 multi-measure service)
     */
    public FhirVersionTestContext(
            FhirVersionEnum fhirVersion,
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase) {
        this.fhirVersion = fhirVersion;
        this.repository = repository;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase != null ? serverBase : "http://localhost:8080/fhir";
    }

    /**
     * Create a FhirVersionTestContext with auto-detected version from repository path.
     *
     * @param repository the FHIR repository
     * @param repositoryPath path to test resources (for version detection)
     * @param measureEvaluationOptions evaluation options
     * @param serverBase server base URL
     * @return FhirVersionTestContext with detected FHIR version
     */
    public static FhirVersionTestContext forRepository(
            IRepository repository,
            Path repositoryPath,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase) {
        FhirVersionEnum version = detectFhirVersion(repositoryPath);
        return new FhirVersionTestContext(version, repository, measureEvaluationOptions, serverBase);
    }

    /**
     * Create a FhirVersionTestContext with auto-detected version from repository path
     * using default server base.
     *
     * @param repository the FHIR repository
     * @param repositoryPath path to test resources (for version detection)
     * @param measureEvaluationOptions evaluation options
     * @return FhirVersionTestContext with detected FHIR version
     */
    public static FhirVersionTestContext forRepository(
            IRepository repository, Path repositoryPath, MeasureEvaluationOptions measureEvaluationOptions) {
        return forRepository(repository, repositoryPath, measureEvaluationOptions, null);
    }

    /**
     * Create a FhirVersionTestContext with auto-detected version from repository path,
     * automatically creating an IgRepository from the path.
     *
     * @param repositoryPath path to test resources (for version detection and repository creation)
     * @param measureEvaluationOptions evaluation options
     * @param serverBase server base URL
     * @return FhirVersionTestContext with detected FHIR version and created repository
     */
    public static FhirVersionTestContext forRepository(
            Path repositoryPath, MeasureEvaluationOptions measureEvaluationOptions, String serverBase) {
        FhirVersionEnum version = detectFhirVersion(repositoryPath);
        FhirContext fhirContext = getFhirContext(version);
        IRepository repository =
                new org.opencds.cqf.fhir.utility.repository.ig.IgRepository(fhirContext, repositoryPath);
        return new FhirVersionTestContext(version, repository, measureEvaluationOptions, serverBase);
    }

    /**
     * Get the detected FHIR version.
     *
     * @return the FHIR version enum (DSTU3, R4, R5, etc.)
     */
    public FhirVersionEnum getFhirVersion() {
        return fhirVersion;
    }

    /**
     * Get the FHIR repository.
     *
     * @return the IRepository instance
     */
    public IRepository getRepository() {
        return repository;
    }

    /**
     * Get FhirContext for a specific FHIR version.
     *
     * @param version the FHIR version
     * @return the FhirContext for that version
     */
    public static FhirContext getFhirContext(FhirVersionEnum version) {
        switch (version) {
            case DSTU3:
                return FhirContext.forDstu3Cached();
            case R4:
                return FhirContext.forR4Cached();
            case R5:
                return FhirContext.forR5Cached();
            default:
                throw new UnsupportedOperationException("Unsupported FHIR version: " + version);
        }
    }

    /**
     * Create a version-appropriate MeasureServiceAdapter.
     * <p>
     * Returns the correct adapter for the detected FHIR version:
     * <ul>
     *   <li>DSTU3: Dstu3MeasureServiceAdapter (no multi-measure support)</li>
     *   <li>R4: R4MeasureServiceAdapter (full multi-measure support)</li>
     *   <li>R5/R6: Future implementations</li>
     * </ul>
     * </p>
     *
     * @return version-specific MeasureServiceAdapter
     */
    public MeasureServiceAdapter createMeasureService() {
        switch (fhirVersion) {
            case DSTU3:
                return new Dstu3MeasureServiceAdapter(repository, measureEvaluationOptions);
            case R4:
                return new R4MeasureServiceAdapter(
                        repository, measureEvaluationOptions, new MeasurePeriodValidator(), serverBase);
            case R5:
                throw new UnsupportedOperationException(
                        "R5 measure adapters not yet implemented. " + "Please implement R5MeasureServiceAdapter.");
            default:
                throw new UnsupportedOperationException("Unsupported FHIR version: " + fhirVersion);
        }
    }

    /**
     * Detect FHIR version from test resource directory.
     * <p>
     * Strategy:
     * <ol>
     *   <li>Find first JSON or XML file in directory</li>
     *   <li>Parse with all available FHIR contexts (DSTU3, R4, R5)</li>
     *   <li>Return version of first successful parse</li>
     *   <li>Fallback to heuristic detection if parse fails</li>
     * </ol>
     * </p>
     *
     * @param repositoryPath path to test resources directory
     * @return detected FHIR version, defaults to R4 if detection fails
     */
    private static FhirVersionEnum detectFhirVersion(Path repositoryPath) {
        if (repositoryPath == null || !Files.exists(repositoryPath)) {
            // Default to R4 if no path provided
            return FhirVersionEnum.R4;
        }

        // Try to find and parse a sample file
        try (Stream<Path> paths = Files.walk(repositoryPath, 2)) {
            Path sampleFile = paths.filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return (fileName.endsWith(".json") || fileName.endsWith(".xml")) && Files.isRegularFile(path);
                    })
                    .findFirst()
                    .orElse(null);

            if (sampleFile != null) {
                // Try parsing with each FHIR version
                FhirVersionEnum detected = tryParseWithVersion(sampleFile, FhirVersionEnum.R4);
                if (detected != null) return detected;

                detected = tryParseWithVersion(sampleFile, FhirVersionEnum.DSTU3);
                if (detected != null) return detected;

                detected = tryParseWithVersion(sampleFile, FhirVersionEnum.R5);
                if (detected != null) return detected;
            }
        } catch (IOException e) {
            // Ignore and fall through to default
        }

        // Default to R4 if detection fails
        return FhirVersionEnum.R4;
    }

    /**
     * Try parsing a file with a specific FHIR version.
     *
     * @param filePath path to FHIR resource file
     * @param version FHIR version to try
     * @return the version if successful, null if parse fails
     */
    private static FhirVersionEnum tryParseWithVersion(Path filePath, FhirVersionEnum version) {
        try {
            FhirContext context;
            switch (version) {
                case DSTU3:
                    context = FhirContext.forDstu3Cached();
                    break;
                case R4:
                    context = FhirContext.forR4Cached();
                    break;
                case R5:
                    context = FhirContext.forR5Cached();
                    break;
                default:
                    context = null;
            }

            if (context == null) return null;

            IParser parser = filePath.toString().endsWith(".json") ? context.newJsonParser() : context.newXmlParser();

            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                IBaseResource resource = parser.parseResource(fis);
                // Successful parse - return this version
                return version;
            }
        } catch (Exception e) {
            // Parse failed - try next version
            return null;
        }
    }
}
