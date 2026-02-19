package org.opencds.cqf.fhir.cr.cql;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
import org.opencds.cqf.fhir.cr.cql.evaluate.CqlEvaluationProcessor;
import org.opencds.cqf.fhir.cr.cql.evaluate.CqlEvaluationRequest;
import org.opencds.cqf.fhir.cr.cql.evaluate.ICqlEvaluationProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

@SuppressWarnings("UnstableApiUsage")
public class CqlProcessor {

    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected ICqlEvaluationProcessor cqlEvaluationProcessor;
    protected IRepository repository;
    protected CrSettings crSettings;

    public CqlProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public CqlProcessor(IRepository repository, CrSettings crSettings) {
        this(repository, crSettings, null);
    }

    public CqlProcessor(
            IRepository repository, CrSettings crSettings, List<? extends IOperationProcessor> operationProcessors) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.crSettings = requireNonNull(crSettings, "crSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        if (operationProcessors != null && !operationProcessors.isEmpty()) {
            operationProcessors.forEach(p -> {
                if (p instanceof ICqlEvaluationProcessor pack) {
                    cqlEvaluationProcessor = pack;
                }
            });
        }
    }

    public CrSettings settings() {
        return crSettings;
    }

    public IBaseParameters evaluate(
            String subject,
            String expression,
            IBaseParameters parameters,
            List<? extends IBaseBackboneElement> library,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            String content,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return evaluate(
                subject,
                expression,
                parameters,
                library,
                useServerData,
                data,
                prefetchData,
                content,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public IBaseParameters evaluate(
            String subject,
            String expression,
            IBaseParameters parameters,
            List<? extends IBaseBackboneElement> library,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            String content,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return evaluate(
                subject,
                expression,
                parameters,
                library,
                data,
                prefetchData,
                content,
                new LibraryEngine(repository, crSettings.getEvaluationSettings()));
    }

    public IBaseParameters evaluate(
            String subject,
            String expression,
            IBaseParameters parameters,
            List<? extends IBaseBackboneElement> library,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            String content,
            LibraryEngine libraryEngine) {
        var processor = cqlEvaluationProcessor != null
                ? cqlEvaluationProcessor
                : new CqlEvaluationProcessor(repository, crSettings.getEvaluationSettings());
        return processor.evaluate(buildEvaluateRequest(
                subject, expression, parameters, library, data, prefetchData, content, libraryEngine));
    }

    protected CqlEvaluationRequest buildEvaluateRequest(
            String subject,
            String expression,
            IBaseParameters parameters,
            List<? extends IBaseBackboneElement> library,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            String content,
            LibraryEngine libraryEngine) {

        return new CqlEvaluationRequest(
                StringUtils.isBlank(subject) ? null : Ids.newId(fhirVersion, subject),
                expression,
                parameters,
                library,
                data,
                prefetchData,
                content,
                libraryEngine,
                modelResolver);
    }
}
