package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.cqframework.cql.cql2elm.CqlCompilerOptions.Options;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.CqlTranslatorOptionsMapper;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cql.CqlOptions;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.PROFILE_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.CODE_LOOKUP_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_MEMBERSHIP_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_PRE_EXPANSION_MODE;
import org.opencds.cqf.fhir.utility.repository.ProxyRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class Utilities {

    private Utilities() {}

    /**
     * Creates an instance of {@link EvaluationSettings} with based on the provided
     * CQL path and runtime arguments. If there's a `cql-options.json` file
     * present in the same directory as the CQL path, it will be used to set
     * additional CQL compiler options.
     *
     * @param cqlPath the directory path where the CQL files are located
     * @param enableHedisCompatibilityMode true to enable HEDIS compatibility mode
     * @return the configured {@link EvaluationSettings}
     */
    public static EvaluationSettings createEvaluationSettings(String cqlPath, boolean enableHedisCompatibilityMode) {
        CqlOptions cqlOptions = CqlOptions.defaultOptions();
        if (enableHedisCompatibilityMode) {
            cqlOptions.getCqlEngineOptions().getOptions().add(CqlEngine.Options.EnableHedisCompatibilityMode);
        }

        var optionPath = Path.of(cqlPath).resolve("cql-options.json");
        if (Files.exists(optionPath)) {
            CqlTranslatorOptions options = CqlTranslatorOptionsMapper.fromFile(optionPath.toString());
            cqlOptions.setCqlCompilerOptions(options.getCqlCompilerOptions());
        }

        // Always add results types, since correct behavior of the CQL engine
        // depends on it.
        cqlOptions.getCqlCompilerOptions().getOptions().add(Options.EnableResultTypes);

        var terminologySettings = new TerminologySettings();
        terminologySettings.setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        terminologySettings.setValuesetPreExpansionMode(VALUESET_PRE_EXPANSION_MODE.USE_IF_PRESENT);
        terminologySettings.setValuesetMembershipMode(VALUESET_MEMBERSHIP_MODE.USE_EXPANSION);
        terminologySettings.setCodeLookupMode(CODE_LOOKUP_MODE.USE_CODESYSTEM_URL);

        var retrieveSettings = new RetrieveSettings();
        retrieveSettings.setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
        retrieveSettings.setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY);
        retrieveSettings.setProfileMode(PROFILE_MODE.DECLARED);

        var evaluationSettings = EvaluationSettings.getDefault();
        evaluationSettings.setCqlOptions(cqlOptions);
        evaluationSettings.setTerminologySettings(terminologySettings);
        evaluationSettings.setRetrieveSettings(retrieveSettings);

        return evaluationSettings;
    }

    /**
     * Converts a value to a string representation. Full of hacks, this is
     * intended to be replaced by proper serialization in the future.
     * @param value the value to convert
     * @return a string representation of the value
     */
    public static String tempConvert(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Iterable<?> values) {
            return StreamSupport.stream(values.spliterator(), false)
                    .map(Utilities::tempConvert)
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        if (value instanceof IBaseResource resource) {
            return resource.fhirType()
                    + (resource.getIdElement() != null
                                    && resource.getIdElement().hasIdPart()
                            ? "(id=" + resource.getIdElement().getIdPart() + ")"
                            : "");
        }

        if (value instanceof IBaseDatatype datatype) {
            return datatype.fhirType();
        }

        if (value instanceof IBase base) {
            return base.fhirType();
        }

        return value.toString();
    }

    /**
     * Creates a repository instance based on the provided FHIR context,
     * terminology URL, and model URL.
     * @param fhirContext
     * @param terminologyUrl
     * @param modelUrl
     * @return
     */
    public static IRepository createRepository(FhirContext fhirContext, String terminologyUrl, String modelUrl) {
        IRepository data = null;
        IRepository terminology = null;

        if (modelUrl != null) {
            Path path = Path.of(modelUrl);
            data = new IgRepository(fhirContext, path);
        }

        if (terminologyUrl != null) {
            terminology = new IgRepository(fhirContext, Path.of(terminologyUrl));
        }

        return new ProxyRepository(data, null, terminology);
    }

    /**
     * Writes a CQL evaluation result to the provided output stream.
     * Each expression result is written as a line in the format "key=value",
     * where the key is the expression name and the value is the result of the
     * expression evaluation, converted to a string using the `tempConvert` method.
     *
     * @param result the evaluation result containing expression results
     * @param outputStream the output stream to write the results to
     * @throws RuntimeException if an I/O error occurs while writing to the output stream
     */
    public static void writeResult(EvaluationResult result, OutputStream outputStream) {
        // Inentionally not using try-with-resources here to avoid closing the
        // output stream, which may be managed by the caller
        var writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        try {
            // Write the result to the output stream
            for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
                String key = libraryEntry.getKey();
                Object value = Utilities.tempConvert(libraryEntry.getValue().value());
                writer.write(key + "=" + value);
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write evaluation result", e);
        }
    }
}
