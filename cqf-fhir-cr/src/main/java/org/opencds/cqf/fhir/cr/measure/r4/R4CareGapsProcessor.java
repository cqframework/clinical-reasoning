package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.utility.Resources.newResource;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.SubjectRef;
import org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Care Gaps Processor: validates parameters, resolves measures and subjects,
 * and delegates bundle construction to {@link R4CareGapsBundleBuilder}.
 */
public class R4CareGapsProcessor {

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

    /**
     * Calculate measures describing gaps in care.
     *
     * @param periodStart measurement period starting interval
     * @param periodEnd measurement period ending interval
     * @param subject subject reference (Patient/{id}, Group/{id}, Practitioner/{id}, or null for all)
     * @param status care-gap statuses to include in results
     * @param measureId measures to resolve by FHIR resource id
     * @param measureIdentifier measures to resolve by identifier value or system|value
     * @param measureUrl measures to resolve by canonical URL
     * @param notDocument if true, return summarized bundle with only DetectedIssue instead of document bundle
     * @return Parameters including zero to many document bundles with Care Gap Measure Reports
     */
    @SuppressWarnings("squid:S107")
    public Parameters getCareGapsReport(
            @Nullable ZonedDateTime periodStart,
            @Nullable ZonedDateTime periodEnd,
            @Nullable String subject,
            List<String> status,
            List<IdType> measureId,
            List<String> measureIdentifier,
            List<String> measureUrl,
            boolean notDocument) {

        // Normalize nulls to empty lists
        List<IdType> safeIds = sanitizeMeasureIds(measureId);
        List<String> safeIdentifiers = nullToEmpty(measureIdentifier);
        List<String> safeUrls = nullToEmpty(measureUrl);
        validateMeasureParameters(safeIds, safeIdentifiers, safeUrls);

        var params = new R4CareGapsParameters(
                periodStart, periodEnd, subject, status, safeIds, safeIdentifiers, safeUrls, notDocument);

        // Validate and set required configuration resources
        checkConfigurationReferences();

        // Validate required parameter values
        checkValidStatusCode(params.status());
        List<Measure> measures = r4MeasureServiceUtils.getMeasures(safeIds, safeIdentifiers, safeUrls);
        measureCompatibilityCheck(measures);

        // Subject population
        List<String> subjects = getSubjects(params.subject());

        // Build results
        Parameters result = initializeResult();
        List<Parameters.ParametersParameterComponent> components = r4CareGapsBundleBuilder.makePatientBundles(
                subjects, params, measures.stream().map(Resource::getIdElement).collect(Collectors.toList()));

        return result.setParameter(components);
    }

    /** Filters null entries and entries with null id parts. */
    private static List<IdType> sanitizeMeasureIds(@Nullable List<IdType> measureId) {
        return Optional.ofNullable(measureId).orElse(Collections.emptyList()).stream()
                .filter(id -> id != null && id.getIdPart() != null)
                .toList();
    }

    private static List<String> nullToEmpty(@Nullable List<String> list) {
        return Optional.ofNullable(list).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .toList();
    }

    /** Throws if no measure resolving parameter was provided across all three lists. */
    private static void validateMeasureParameters(
            List<IdType> measureId, List<String> measureIdentifier, List<String> measureUrl) {
        if (measureId.isEmpty() && measureIdentifier.isEmpty() && measureUrl.isEmpty()) {
            List<String> measureIdsAsStrings =
                    measureId.stream().map(IdType::getIdPart).collect(Collectors.toList());
            throw new InvalidRequestException(
                    "no measure resolving parameter was specified for Measure: " + measureIdsAsStrings);
        }
    }

    List<String> getSubjects(String subject) {
        var subjects = subjectProvider
                .getSubjects(repository, subject)
                .map(SubjectRef::qualified)
                .collect(Collectors.toList());
        if (!subjects.isEmpty()) {
            ourLog.info("care-gaps report requested for: %s subjects.".formatted(subjects.size()));
        } else {
            ourLog.info("care-gaps report requested for: 0 subjects.");
        }
        return subjects;
    }

    void addConfiguredResource(String id, String key) {
        Resource resource = repository.read(Organization.class, new IdType(RESOURCE_TYPE_ORGANIZATION, id));
        checkNotNull(
                resource,
                "The %s Resource is configured as the %s but the Resource could not be read."
                        .formatted(careGapsProperties.getCareGapsReporter(), key));
        configuredResources.put(key, resource);
    }

    void checkMeasureImprovementNotation(Measure measure) {
        if (!measure.hasImprovementNotation()) {
            ourLog.warn(
                    "Measure '{}' does not specify an improvement notation, defaulting to: '{}'.",
                    measure.getId(),
                    "increase");
        }
    }

    Parameters initializeResult() {
        return newResource(Parameters.class, "care-gaps-report-" + UUID.randomUUID());
    }

    void checkValidStatusCode(List<String> statuses) {
        r4MeasureServiceUtils.listThrowIllegalArgumentIfEmpty(statuses, "status");

        for (String status : statuses) {
            if (!CareGapsStatusCode.CLOSED_GAP.toString().equals(status)
                    && !CareGapsStatusCode.OPEN_GAP.toString().equals(status)
                    && !CareGapsStatusCode.NOT_APPLICABLE.toString().equals(status)
                    && !CareGapsStatusCode.PROSPECTIVE_GAP.toString().equals(status)) {
                throw new InvalidRequestException(
                        "CareGap status parameter: %s, is not an accepted value".formatted(status));
            }
        }
    }

    void measureCompatibilityCheck(List<Measure> measures) {
        for (Measure measure : measures) {
            checkMeasureScoringType(measure);
            checkMeasureImprovementNotation(measure);
            checkMeasureBasis(measure);
            checkMeasureGroupComponents(measure);
        }
    }

    void checkMeasureBasis(Measure measure) {
        var msg = "CareGaps can't process Measure: %s, it is not Boolean basis.".formatted(measure.getIdPart());
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
     */
    void checkMeasureGroupComponents(Measure measure) {
        if (measure.getGroup().size() > 1) {
            for (MeasureGroupComponent group : measure.getGroup()) {
                if (group.getId() == null || group.getId().isEmpty()) {
                    throw new InvalidRequestException(
                            "Multi-rate Measure resources require unique 'id' for GroupComponents to be populated for Measure: "
                                    + measure.getUrl());
                }
            }
        }
    }

    void checkMeasureScoringType(Measure measure) {
        List<MeasureScoring> scoringTypes = r4MeasureServiceUtils.getMeasureScoringDef(measure);
        for (MeasureScoring measureScoringType : scoringTypes) {
            if (!MeasureScoring.PROPORTION.equals(measureScoringType)
                    && !MeasureScoring.RATIO.equals(measureScoringType)) {
                throw new InvalidRequestException(
                        "MeasureScoring type: %s, is not an accepted Type for care-gaps service for Measure: %s"
                                .formatted(measureScoringType.getDisplay(), measure.getUrl()));
            }
        }
    }

    void checkConfigurationReferences() {
        careGapsProperties.validateRequiredProperties();

        addConfiguredResource(careGapsProperties.getCareGapsReporter(), CareGapsConstants.CARE_GAPS_REPORTER_KEY);
        addConfiguredResource(
                careGapsProperties.getCareGapsCompositionSectionAuthor(),
                CareGapsConstants.CARE_GAPS_SECTION_AUTHOR_KEY);
    }
}
