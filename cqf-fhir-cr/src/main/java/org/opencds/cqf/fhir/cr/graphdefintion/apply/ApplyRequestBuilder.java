package org.opencds.cqf.fhir.cr.graphdefintion.apply;

import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.utility.EndpointHelper;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ApplyRequestBuilder {

    private IRepository repository;
    private final EvaluationSettings evaluationSettings;
    private final FhirVersionEnum fhirVersion;
    private GraphDefinition graphDefinition;
    private String subject;
    private CodeableConcept userType;
    private CodeableConcept userLanguage;
    private CodeableConcept userTaskContext;
    private CodeableConcept setting;
    private CodeableConcept settingContext;
    private boolean useServerData;
    private IBaseBundle data;
    private List<ParametersParameterComponent> prefetchData;
    private IRepository dataRepository;
    private IRepository contentRepository;
    private IRepository terminologyRepository;
    private Parameters parameters;
    private IIdType id;
    private ZonedDateTime periodStart;
    private ZonedDateTime periodEnd;
    private IPrimitiveType<String> canonicalType;

    public ApplyRequestBuilder(IRepository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
        this.evaluationSettings = evaluationSettings;
    }

    public ApplyRequestBuilder withGrapDefinitionId(IdType id) {
        this.id = id;
        return this;
    }

    public ApplyRequestBuilder withCanonicalType(IPrimitiveType<String> canonicalType) {
        this.canonicalType = canonicalType;
        return this;
    }

    public ApplyRequestBuilder withGraphDefinition(GraphDefinition graphDefinition) {
        this.graphDefinition = graphDefinition;
        return this;
    }

    public ApplyRequestBuilder withSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public ApplyRequestBuilder withUserType(CodeableConcept userType) {
        this.userType = userType;
        return this;
    }

    public ApplyRequestBuilder withUserLanguage(CodeableConcept userLanguage) {
        this.userLanguage = userLanguage;
        return this;
    }

    public ApplyRequestBuilder withUserTaskContext(CodeableConcept userTaskContext) {
        this.userTaskContext = userTaskContext;
        return this;
    }

    public ApplyRequestBuilder withSetting(CodeableConcept setting) {
        this.setting = setting;
        return this;
    }

    public ApplyRequestBuilder withSettingContext(CodeableConcept settingContext) {
        this.settingContext = settingContext;
        return this;
    }

    public ApplyRequestBuilder withParameters(Parameters parameters) {
        this.parameters = parameters;
        return this;
    }

    public ApplyRequestBuilder withUseLocalData(boolean useServerData) {
        this.useServerData = useServerData;
        return this;
    }

    public ApplyRequestBuilder withData(IBaseBundle data) {
        this.data = data;
        return this;
    }

    public ApplyRequestBuilder withPrefetchData(List<ParametersParameterComponent> prefetchData) {
        this.prefetchData = prefetchData;
        return this;
    }

    public ApplyRequestBuilder withDataEndpoint(ParametersParameterComponent dataEndpointParam) {
        IBaseResource endpoint = EndpointHelper.getEndpoint(fhirVersion, dataEndpointParam);
        this.dataRepository = createRestRepository(repository.fhirContext(), endpoint);
        return this;
    }

    public ApplyRequestBuilder withDataRepository(IRepository dataRepository) {
        this.dataRepository = dataRepository;
        return this;
    }

    public ApplyRequestBuilder withContentEndpoint(ParametersParameterComponent contentEndpointParam) {
        IBaseResource endpoint = EndpointHelper.getEndpoint(fhirVersion, contentEndpointParam);
        this.contentRepository = createRestRepository(repository.fhirContext(), endpoint);
        return this;
    }

    public ApplyRequestBuilder withContentRepository(IRepository contentRepository) {
        this.contentRepository = contentRepository;
        return this;
    }

    public ApplyRequestBuilder withTerminologyEndpoint(ParametersParameterComponent terminologyEndpointParam) {
        IBaseResource endpoint = EndpointHelper.getEndpoint(fhirVersion, terminologyEndpointParam);
        this.terminologyRepository = createRestRepository(repository.fhirContext(), endpoint);
        return this;
    }

    public ApplyRequestBuilder withTerminologyRepository(IRepository terminologyRepositoary) {
        this.terminologyRepository = terminologyRepositoary;
        return this;
    }

    public ApplyRequestBuilder withPeriodStart(ZonedDateTime periodStartString) {
        this.periodStart = periodStartString;
        return this;
    }

    public ApplyRequestBuilder withPeriodEnd(ZonedDateTime periodEndString) {
        this.periodEnd = periodEndString;
        return this;
    }

    public ApplyRequest buildApplyRequest() {
        if (StringUtils.isBlank(this.subject)) {
            throw new IllegalArgumentException("Missing required parameter: 'subject'");
        }

        this.repository = proxy(
                this.repository,
                this.useServerData,
                this.dataRepository,
                this.contentRepository,
                this.terminologyRepository);

        Either3<IPrimitiveType<String>, IIdType, IBaseResource> eitherGraphDefinition =
                Eithers.for3(canonicalType, this.id, this.graphDefinition);

        IBaseResource resolvedGraphDefinition =
                new ResourceResolver("GraphDefinition", this.repository).resolve(eitherGraphDefinition);

        LibraryEngine libraryEngine = new LibraryEngine(this.repository, this.evaluationSettings);

        ModelResolver modelResolver = FhirModelResolverCache.resolverForVersion(this.fhirVersion);

        return new ApplyRequest(
                resolvedGraphDefinition,
                Ids.newId(this.fhirVersion, Ids.ensureIdType(subject, "Patient")),
                this.userType,
                this.userLanguage,
                this.userTaskContext,
                this.setting,
                this.settingContext,
                this.parameters,
                this.data,
                this.prefetchData,
                libraryEngine,
                modelResolver,
                this.periodStart,
                this.periodEnd,
                null);
    }
}
