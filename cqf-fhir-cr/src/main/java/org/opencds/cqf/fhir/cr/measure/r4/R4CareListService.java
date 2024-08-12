package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_COMPATIBLE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4CareListService {
    private static final Logger ourLog = LoggerFactory.getLogger(R4CareListService.class);
    private final Repository repository;

    private final MeasureEvaluationOptions measureEvaluationOptions;

    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

    private CareGapsProperties careGapsProperties;

    private String serverBase;

    protected final Map<String, Resource> configuredResources = new HashMap<>();

    private final R4MeasureServiceUtils r4MeasureServiceUtils;

    public R4CareListService(
            CareGapsProperties careGapsProperties,
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase;

        r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
    }

    /**
     * Calculate measures describing gaps in care
     *
     * @param periodStart
     * @param periodEnd
     * @param topic
     * @param subject
     * @param practitioner
     * @param organization
     * @param statuses
     * @param measureIds
     * @param measureIdentifiers
     * @param measureUrls
     * @param programs
     * @return Parameters that includes zero to many document bundles that include Care Gap Measure
     *         Reports will be returned.
     */
    public void getDetectedIssue(
            IPrimitiveType<Date> periodStart,
            IPrimitiveType<Date> periodEnd,
            String subject,
            String reporter,
            List<String> statuses,
            List<String> measureId,
            List<String> measureUrl) {

        // validate Measure has care-gap extension
        // check for Date of non-compliance
        // check measureScoringType compliance in measureProcessor

        // $evaluate-measure

        // ratio/proportion algorithm with prospective-gap logic

        // create collection bundle for results

        // add DetectedIssue per Subject/per Measure
        // set date of non-compliance extension
        // set status of care-gap
        // set evidence by Measure resource
        // set dates to measurement period used
    }

    public boolean isMeasureCareGapAble(Measure measure) {
        return measure.getExtensionByUrl(CQFM_CARE_GAP_COMPATIBLE_EXT_URL) != null;
    }

    public void validateMeasureCompatibility(Measure measure) {
        if (measure.getScoring() != null
                && (!measure.getScoring().getCodingFirstRep().getCode().equals(MeasureScoring.PROPORTION.toCode())
                        && !measure.getScoring().getCodingFirstRep().getCode().equals(MeasureScoring.RATIO.toCode()))) {
            var msg = String.format(
                    "Measure: %s, has invalid measure scoringType: %s",
                    measure.getIdPart(),
                    measure.getScoring().getCodingFirstRep().getCode());
            throw new IllegalArgumentException(msg);
        } else {
            var groups = measure.getGroup();
            for (int i = 0; i < groups.size(); i++) {
                if (!groups.get(i)
                                .getExtensionByUrl(CQFM_SCORING_EXT_URL)
                                .getValue()
                                .toString()
                                .equals(MeasureScoring.PROPORTION.toCode())
                        && !groups.get(i)
                                .getExtensionByUrl(CQFM_SCORING_EXT_URL)
                                .getValue()
                                .toString()
                                .equals(MeasureScoring.RATIO.toCode())) {
                    var msg = String.format(
                            "Measure: %s, has invalid group level scoringType: %s",
                            measure.getIdPart(),
                            groups.get(i)
                                    .getExtensionByUrl(CQFM_SCORING_EXT_URL)
                                    .getValue()
                                    .toString());
                    throw new IllegalArgumentException(msg);
                }
            }
        }
    }
}
