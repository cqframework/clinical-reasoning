package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.utility.Resources.newResource;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePeriodValidator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Care Gaps Processor houses construction of result body with input of different Result Bodies, such as Document Bundle vs non-document bundle
 */
public class R4CareGapsProcessor {

    private static final Logger ourLog = LoggerFactory.getLogger(R4CareGapsProcessor.class);
    private final Repository repository;
    private final CareGapsProperties careGapsProperties;
    private final Map<String, Resource> configuredResources = new HashMap<>();
    private final R4MeasureServiceUtils r4MeasureServiceUtils;
    private final R4CareGapsBundleBuilder r4CareGapsBundleBuilder;

    public R4CareGapsProcessor(
            CareGapsProperties careGapsProperties,
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase,
            MeasurePeriodValidator measurePeriodValidator) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;

        r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
        r4CareGapsBundleBuilder = new R4CareGapsBundleBuilder(
                careGapsProperties, repository, measureEvaluationOptions, serverBase, configuredResources, measurePeriodValidator);
    }

    public Parameters getCareGapsReport(
            ZonedDateTime periodStart,
            ZonedDateTime periodEnd,
            String subject,
            List<String> statuses,
            List<IdType> measureIds,
            List<String> measureIdentifiers,
            List<CanonicalType> measureUrls) {

        // validate and set required configuration resources for care-gaps
        checkConfigurationReferences();

        // Collect Measures to Evaluate
        List<Measure> measures =
                r4MeasureServiceUtils.getMeasures(measureIds, measureIdentifiers, canonicalToString(measureUrls));
        List<IdType> collectedMeasureIds =
                measures.stream().map(Resource::getIdElement).collect(Collectors.toList());

        // validate required parameter values
        checkValidStatusCode(statuses);
        measureCompatibilityCheck(measures);

        // Subject Population for Report
        List<String> subjects = getSubjects(subject);

        // Build Results
        Parameters result = initializeResult();

        // Build Patient Bundles

        List<Parameters.ParametersParameterComponent> components = r4CareGapsBundleBuilder.makePatientBundles(
                periodStart, periodEnd, subjects, statuses, collectedMeasureIds);

        // Return Results with Bundles
        return result.setParameter(components);
    }

    protected List<String> getSubjects(String subject) {
        R4RepositorySubjectProvider subjectProvider = new R4RepositorySubjectProvider();
        var subjects = subjectProvider.getSubjects(repository, null, subject).collect(Collectors.toList());
        if (!subjects.isEmpty()) {
            ourLog.info(String.format("care-gaps report requested for: %s subjects.", subjects.size()));
        } else {
            ourLog.info("care-gaps report requested for: 0 subjects.");
        }
        return subjects;
    }

    private void addConfiguredResource(String id, String key) {
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

    private void checkMeasureImprovementNotation(Measure measure) {
        if (!measure.hasImprovementNotation()) {
            ourLog.warn(
                    "Measure '{}' does not specify an improvement notation, defaulting to: '{}'.",
                    measure.getId(),
                    "increase");
        }
    }

    private Parameters initializeResult() {
        return newResource(Parameters.class, "care-gaps-report-" + UUID.randomUUID());
    }

    private List<String> canonicalToString(List<CanonicalType> measureUrls) {
        return measureUrls.stream()
                .map(PrimitiveType::toString)
                .map(x -> x.replace("CanonicalType[", ""))
                .map(x -> x.replace("]", ""))
                .collect(Collectors.toList());
    }

    private void checkValidStatusCode(List<String> statuses) {
        r4MeasureServiceUtils.listThrowIllegalArgumentIfEmpty(statuses, "status");

        for (String status : statuses) {
            if (!CareGapsStatusCode.CLOSED_GAP.toString().equals(status)
                    && !CareGapsStatusCode.OPEN_GAP.toString().equals(status)
                    && !CareGapsStatusCode.NOT_APPLICABLE.toString().equals(status)
                    && !CareGapsStatusCode.PROSPECTIVE_GAP.toString().equals(status)) {
                throw new IllegalArgumentException(
                        String.format("CareGap status parameter: %s, is not an accepted value", status));
            }
        }
    }

    private void measureCompatibilityCheck(List<Measure> measures) {
        for (Measure measure : measures) {
            checkMeasureScoringType(measure);
            checkMeasureImprovementNotation(measure);
            checkMeasureBasis(measure);
            checkMeasureGroupComponents(measure);
        }
    }

    private void checkMeasureBasis(Measure measure) {
        R4MeasureBasisDef measureDef = new R4MeasureBasisDef();
        if (!measureDef.isBooleanBasis(measure)) {
            throw new IllegalArgumentException(
                    String.format("CareGaps can't process Measure: %s, it is not Boolean basis.", measure.getIdPart()));
        }
    }

    /**
     * MultiRate Measures require a unique 'id' per GroupComponent to uniquely identify results in Measure Report.
     * This is helpful when creating DetectedIssues per GroupComponent so endUsers can attribute evidence of a Care-Gap to the specific MeasureReport result
     * @param measure Measure resource
     */
    private void checkMeasureGroupComponents(Measure measure) {
        // if a Multi-rate Measure, enforce groupId to be populated
        if (measure.getGroup().size() > 1) {
            for (MeasureGroupComponent group : measure.getGroup()) {
                if (measure.getGroup().size() > 1
                        && (group.getId() == null || group.getId().isEmpty())) {
                    throw new IllegalArgumentException(
                            "Multi-rate Measure resources require unique 'id' for GroupComponents to be populated.");
                }
            }
        }
    }

    private void checkMeasureScoringType(Measure measure) {
        List<MeasureScoring> scoringTypes = r4MeasureServiceUtils.getMeasureScoringDef(measure);
        for (MeasureScoring measureScoringType : scoringTypes) {
            if (!MeasureScoring.PROPORTION.equals(measureScoringType)
                    && !MeasureScoring.RATIO.equals(measureScoringType)) {
                throw new IllegalArgumentException(String.format(
                        "MeasureScoring type: %s, is not an accepted Type for care-gaps service",
                        measureScoringType.getDisplay()));
            }
        }
    }

    private void checkConfigurationReferences() {
        careGapsProperties.validateRequiredProperties();

        addConfiguredResource(careGapsProperties.getCareGapsReporter(), CareGapsConstants.CARE_GAPS_REPORTER_KEY);
        addConfiguredResource(
                careGapsProperties.getCareGapsCompositionSectionAuthor(),
                CareGapsConstants.CARE_GAPS_SECTION_AUTHOR_KEY);
    }
}
