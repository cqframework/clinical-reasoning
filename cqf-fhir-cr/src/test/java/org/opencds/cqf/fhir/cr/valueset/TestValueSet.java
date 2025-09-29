package org.opencds.cqf.fhir.cr.valueset;

import static org.opencds.cqf.fhir.test.Resources.getResourcePath;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.nio.file.Path;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.helpers.DataRequirementsLibrary;
import org.opencds.cqf.fhir.cr.helpers.GeneratedPackage;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

public class TestValueSet {
    public static final String CLASS_PATH = "org/opencds/cqf/fhir/cr/shared";

    public static Given given() {
        return new Given();
    }

    public static class Given {
        private IRepository repository;
        private EvaluationSettings evaluationSettings;
        private IPackageProcessor packageProcessor;
        private IDataRequirementsProcessor dataRequirementsProcessor;

        public Given repository(IRepository repository) {
            this.repository = repository;
            return this;
        }

        public Given repositoryFor(FhirContext fhirContext, String repositoryPath) {
            this.repository = new IgRepository(
                    fhirContext, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/" + repositoryPath));
            return this;
        }

        public Given evaluationSettings(EvaluationSettings evaluationSettings) {
            this.evaluationSettings = evaluationSettings;
            return this;
        }

        public Given packageProcessor(IPackageProcessor packageProcessor) {
            this.packageProcessor = packageProcessor;
            return this;
        }

        public Given dataRequirementsProcessor(IDataRequirementsProcessor dataRequirementsProcessor) {
            this.dataRequirementsProcessor = dataRequirementsProcessor;
            return this;
        }

        public ValueSetProcessor buildProcessor(IRepository repository) {
            if (evaluationSettings == null) {
                evaluationSettings = EvaluationSettings.getDefault();
                evaluationSettings
                        .getRetrieveSettings()
                        .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                        .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

                evaluationSettings
                        .getTerminologySettings()
                        .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
            }
            return new ValueSetProcessor(
                    repository,
                    evaluationSettings,
                    TerminologyServerClientSettings.getDefault(),
                    packageProcessor,
                    dataRequirementsProcessor);
        }

        public When when() {
            return new When(repository, buildProcessor(repository));
        }
    }

    public static class When {
        private final IRepository repository;
        private final ValueSetProcessor processor;
        private IPrimitiveType<String> valueSetUrl;
        private IIdType valueSetId;
        private IBaseResource valueSet;
        private IBaseParameters parameters;
        private Boolean isPut;

        When(IRepository repository, ValueSetProcessor processor) {
            this.repository = repository;
            this.processor = processor;
        }

        private FhirContext fhirContext() {
            return repository.fhirContext();
        }

        public When valueSetUrl(IPrimitiveType<String> url) {
            valueSetUrl = url;
            return this;
        }

        public When valueSetId(IIdType id) {
            valueSetId = id;
            return this;
        }

        public When valueSet(IBaseResource resource) {
            valueSet = resource;
            return this;
        }

        public When parameters(IBaseParameters params) {
            parameters = params;
            return this;
        }

        public When isPut(Boolean value) {
            isPut = value;
            return this;
        }

        public GeneratedPackage thenPackage() {
            var param = Eithers.for3(valueSetUrl, valueSetId, valueSet);
            return new GeneratedPackage(
                    isPut == null ? processor.packageValueSet(param) : processor.packageValueSet(param, isPut),
                    fhirContext());
        }

        public DataRequirementsLibrary thenDataRequirements() {
            var param = Eithers.for3(valueSetUrl, valueSetId, valueSet);
            return new DataRequirementsLibrary(processor.dataRequirements(param, parameters));
        }
    }
}
