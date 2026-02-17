package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.cli.argument.CqlCommandArgument;

/**
 * Shared pipeline building blocks for CLI commands.
 */
public class EngineFactory {

    private EngineFactory() {}

    public record EngineBundle(
            FhirContext fhirContext,
            EvaluationSettings evaluationSettings,
            IRepository repository,
            CqlEngine engine,
            IParser parser) {}

    public static EngineBundle createEngineBundle(CqlCommandArgument arguments) {
        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.valueOf(arguments.fhir.fhirVersion));
        var evaluationSettings =
                Utilities.createEvaluationSettings(arguments.content.cqlPath, arguments.hedisCompatibilityMode);

        NpmProcessor npmProcessor = null;
        if (arguments.fhir.implementationGuidePath != null && arguments.fhir.rootDirectory != null) {
            try {
                var context = new IGContext();
                context.initializeFromIg(
                        arguments.fhir.rootDirectory,
                        arguments.fhir.implementationGuidePath,
                        fhirContext.getVersion().getVersion().getFhirVersionString());
                npmProcessor = new NpmProcessor(context);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to initialize IGContext from provided path", e);
            }
        }

        evaluationSettings.setNpmProcessor(npmProcessor);

        var repository = Utilities.createRepository(fhirContext, arguments.fhir.terminologyUrl, arguments.fhir.dataUrl);

        var engine = Engines.forRepository(repository, evaluationSettings);

        if (arguments.content.cqlPath != null) {
            var provider = new DefaultLibrarySourceProvider(
                    new kotlinx.io.files.Path(Path.of(arguments.content.cqlPath).toFile()));
            engine.getEnvironment().getLibraryManager().getLibrarySourceLoader().registerProvider(provider);
        }

        return new EngineBundle(fhirContext, evaluationSettings, repository, engine, fhirContext.newJsonParser());
    }
}
