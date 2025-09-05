package org.opencds.cqf.fhir.cr;

import ca.uhn.fhir.repository.IRepository;
import org.opencds.cqf.fhir.cql.Engines.EngineInitializationContext;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.ActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessor;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessorFactory;

public class ActivityDefinitionProcessorFactory implements IActivityDefinitionProcessorFactory {

    private final NpmPackageLoader npmPackageLoader;
    private final EvaluationSettings evaluationSettings;

    public ActivityDefinitionProcessorFactory(
            NpmPackageLoader npmPackageLoader, EvaluationSettings evaluationSettings) {
        this.npmPackageLoader = npmPackageLoader;
        this.evaluationSettings = evaluationSettings;
    }

    @Override
    public IActivityDefinitionProcessor create(IRepository repository) {
        var engineInitializationContext =
                new EngineInitializationContext(repository, npmPackageLoader, evaluationSettings);
        return new ActivityDefinitionProcessor(repository, engineInitializationContext);
    }
}
