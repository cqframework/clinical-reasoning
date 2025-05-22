package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.utility.Resources.newResource;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Care Gaps Processor houses construction of result body with input of different Result Bodies, such as Document Bundle vs non-document bundle
 */
public class R4CareGapsProcessor implements R4CareGapsProcessorInterface {

    private static final Logger ourLog = LoggerFactory.getLogger(R4CareGapsProcessor.class);
    private final IRepository repository;
    private final CareGapsProperties careGapsProperties;
    private final Map<String, Resource> configuredResources = new HashMap<>();
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final R4CareGapsBundleBuilder r4CareGapsBundleBuilder;
    private final R4RepositorySubjectProvider subjectProvider;

    public R4CareGapsProcessor(
            CareGapsProperties careGapsProperties,
            IRepository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;

        r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4CareGapsBundleBuilder = new R4CareGapsBundleBuilder(
                careGapsProperties,
                repository,
                measureEvaluationOptions,
                serverBase,
                configuredResources,
                measurePeriodValidator);
        subjectProvider = new R4RepositorySubjectProvider(measureEvaluationOptions.getSubjectProviderOptions());
    }

    @Override
    public Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<Either3<IdType, String, CanonicalType>> measure,
            boolean notDocument) {

        // set Parameters
        R4CareGapsParameters r4CareGapsParams =
                setCareGapParameters(periodStart, periodEnd, subject, status, measure, notDocument);

        // validate and set required configuration resources for care-gaps
        checkConfigurationReferences();

        // validate required parameter values
        checkValidStatusCode(measure, r4CareGapsParams.getStatus());
        List<Measure> measures = resolveMeasure(r4CareGapsParams.getMeasure());
        measureCompatibilityCheck(measures);

        // Subject Population for Report
        List<String> subjects = getSubjects(r4CareGapsParams.getSubject());

        // Build Results
        Parameters result = initializeResult();

        // Build Patient Bundles

        List<Parameters.ParametersParameterComponent> components = r4CareGapsBundleBuilder.makePatientBundles(
                subjects,
                r4CareGapsParams,
                measures.stream().map(Resource::getIdElement).collect(Collectors.toList()));

        // Return Results with Bundles
        return result.setParameter(components);
    }

    @Override
    public R4CareGapsParameters setCareGapParameters(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            String subject,
            List<String> status,
            List<Either3<IdType, String, CanonicalType>> measure,
            boolean notDocument) {
        R4CareGapsParameters r4CareGapsParams = new R4CareGapsParameters();
        r4CareGapsParams.setMeasure(measure);
        r4CareGapsParams.setPeriodStart(periodStart);
        r4CareGapsParams.setPeriodEnd(periodEnd);
        r4CareGapsParams.setStatus(status);
        r4CareGapsParams.setSubject(subject);
        r4CareGapsParams.setNotDocument(notDocument);
        return r4CareGapsParams;
    }

    @Override
    public List<Measure> resolveMeasure(List<Either3<IdType, String, CanonicalType>> measure) {
        return measure.stream()
                .map(x -> x.fold(
                        id -> repository.read(Measure.class, id),
                        r4MeasureServiceUtils::resolveByIdentifier,
                        canonical -> r4MeasureServiceUtils.resolveByUrl(canonical.asStringValue())))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSubjects(String subject) {
        var subjects = subjectProvider.getSubjects(repository, subject).collect(Collectors.toList());
        if (!subjects.isEmpty()) {
            ourLog.info(String.format("care-gaps report requested for: %s subjects.", subjects.size()));
        } else {
            ourLog.info("care-gaps report requested for: 0 subjects.");
        }
        return subjects;
    }

    @Override
    public void addConfiguredResource(String id, String key) {
        // read resource from repository
        Resource resource = repository.read(Organization.class, new IdType(RESOURCE_TYPE_ORGANIZATION, id));

        // validate resource
        checkNotNull(
                resource,
                String.format(
                        "The %s Resource is configured as the %s but the Resource could not be read.",
                        careGapsProperties.getCareGapsReporter(), key));

        // add resource to configured resources
        configuredResources.put(key, resource);
    }

    @Override
    public void checkMeasureImprovementNotation(Measure measure) {
        if (!measure.hasImprovementNotation()) {
            ourLog.warn(
                    "Measure '{}' does not specify an improvement notation, defaulting to: '{}'.",
                    measure.getId(),
                    "increase");
        }
    }

    @Override
    public Parameters initializeResult() {
        return newResource(Parameters.class, "care-gaps-report-" + UUID.randomUUID());
    }

    @Override
    public void checkValidStatusCode(List<Either3<IdType, String, CanonicalType>> measure, List<String> statuses) {
        r4MeasureServiceUtils.listThrowIllegalArgumentIfEmpty(statuses, "status");

        for (String status : statuses) {
            if (!CareGapsStatusCode.CLOSED_GAP.toString().equals(status)
                    && !CareGapsStatusCode.OPEN_GAP.toString().equals(status)
                    && !CareGapsStatusCode.NOT_APPLICABLE.toString().equals(status)
                    && !CareGapsStatusCode.PROSPECTIVE_GAP.toString().equals(status)) {
                throw new InvalidRequestException(String.format(
                        "CareGap status parameter: %s, is not an accepted value for Measure: %s",
                        status, printEithers(measure)));
            }
        }
    }

    @Override
    public void measureCompatibilityCheck(List<Measure> measures) {
        for (Measure measure : measures) {
            checkMeasureScoringType(measure);
            checkMeasureImprovementNotation(measure);
            checkMeasureBasis(measure);
            checkMeasureGroupComponents(measure);
        }
    }

    @Override
    public void checkMeasureBasis(Measure measure) {
        var msg = String.format("CareGaps can't process Measure: %s, it is not Boolean basis.", measure.getIdPart());
        R4MeasureDefBuilder measureDefBuilder = new R4MeasureDefBuilder();
        var measureDef = measureDefBuilder.build(measure);

        for (GroupDef groupDef : measureDef.groups()) {
            if (!groupDef.isBooleanBasis()) {
                throw new InvalidRequestException(msg);
            }
        }
    }

    /**
     * MultiRate Measures require a unique 'id' per GroupComponent to uniquely identify results in Measure Report.
     * This is helpful when creating DetectedIssues per GroupComponent so endUsers can attribute evidence of a Care-Gap to the specific MeasureReport result
     * @param measure Measure resource
     */
    @Override
    public void checkMeasureGroupComponents(Measure measure) {
        // if a Multi-rate Measure, enforce groupId to be populated
        if (measure.getGroup().size() > 1) {
            for (MeasureGroupComponent group : measure.getGroup()) {
                if (measure.getGroup().size() > 1
                        && (group.getId() == null || group.getId().isEmpty())) {
                    throw new InvalidRequestException(
                            "Multi-rate Measure resources require unique 'id' for GroupComponents to be populated for Measure: "
                                    + measure.getUrl());
                }
            }
        }
    }

    @Override
    public void checkMeasureScoringType(Measure measure) {
        List<MeasureScoring> scoringTypes = r4MeasureServiceUtils.getMeasureScoringDef(measure);
        for (MeasureScoring measureScoringType : scoringTypes) {
            if (!MeasureScoring.PROPORTION.equals(measureScoringType)
                    && !MeasureScoring.RATIO.equals(measureScoringType)) {
                throw new InvalidRequestException(String.format(
                        "MeasureScoring type: %s, is not an accepted Type for care-gaps service for Measure: %s",
                        measureScoringType.getDisplay(), measure.getUrl()));
            }
        }
    }

    @Override
    public void checkConfigurationReferences() {
        careGapsProperties.validateRequiredProperties();

        addConfiguredResource(careGapsProperties.getCareGapsReporter(), CareGapsConstants.CARE_GAPS_REPORTER_KEY);
        addConfiguredResource(
                careGapsProperties.getCareGapsCompositionSectionAuthor(),
                CareGapsConstants.CARE_GAPS_SECTION_AUTHOR_KEY);
    }

    private static List<String> printEithers(List<Either3<IdType, String, CanonicalType>> either) {
        return either.stream()
                .map(x -> x.fold(IdType::getIdPart, Function.identity(), PrimitiveType::asStringValue))
                .collect(Collectors.toList());
    }
}
