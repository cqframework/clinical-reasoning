package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.COUNTRY_CODING_SYSTEM_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_LOCATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_ORGANIZATION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.RESOURCE_TYPE_PRACTITIONER_ROLE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.US_COUNTRY_DISPLAY;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.UriParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureEvalType;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.search.Searches;

public class R4MeasureServiceUtils {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(R4MeasureServiceUtils.class);
    private final IRepository repository;

    public R4MeasureServiceUtils(IRepository repository) {
        this.repository = repository;
    }

    public MeasureReport addProductLineExtension(MeasureReport measureReport, String productLine) {
        if (productLine != null) {
            Extension ext = new Extension();
            ext.setUrl(MEASUREREPORT_PRODUCT_LINE_EXT_URL);
            ext.setValue(new StringType(productLine));
            measureReport.addExtension(ext);
        }
        return measureReport;
    }

    public static final List<ContactDetail> CQI_CONTACTDETAIL = Collections.singletonList(new ContactDetail()
            .addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.URL)
                    .setValue("http://www.hl7.org/Special/committees/cqi/index.cfm")));

    public static final List<CodeableConcept> US_JURISDICTION_CODING = Collections.singletonList(new CodeableConcept()
            .addCoding(new Coding(COUNTRY_CODING_SYSTEM_CODE, US_COUNTRY_CODE, US_COUNTRY_DISPLAY)));

    public static final SearchParameter SUPPLEMENTAL_DATA_SEARCHPARAMETER = (SearchParameter) new SearchParameter()
            .setUrl(MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL)
            .setVersion(MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION)
            .setName("DEQMMeasureReportSupplementalData")
            .setStatus(Enumerations.PublicationStatus.ACTIVE)
            .setDate(MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE)
            .setPublisher("HL7 International - Clinical Quality Information Work Group")
            .setContact(CQI_CONTACTDETAIL)
            .setDescription(
                    "Returns resources (supplemental data) from references on extensions on the MeasureReport with urls matching %s."
                            .formatted(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setJurisdiction(US_JURISDICTION_CODING)
            .addBase("MeasureReport")
            .setCode("supplemental-data")
            .setType(Enumerations.SearchParamType.REFERENCE)
            .setExpression(
                    "MeasureReport.extension('%s').value".formatted(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setXpath("f:MeasureReport/f:extension[@url='%s'].value"
                    .formatted(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION))
            .setXpathUsage(SearchParameter.XPathUsageType.NORMAL)
            .setTitle("Supplemental Data")
            .setId("deqm-measurereport-supplemental-data");

    public static String getFullUrl(String serverAddress, IBaseResource resource) {
        checkArgument(
                resource.getIdElement().hasIdPart(),
                "Cannot generate a fullUrl because the resource does not have an id.");
        return getFullUrl(serverAddress, resource.fhirType(), Ids.simplePart(resource));
    }

    public static String getFullUrl(String serverAddress, String fhirType, String elementId) {
        return "%s%s/%s".formatted(serverAddress + (serverAddress.endsWith("/") ? "" : "/"), fhirType, elementId);
    }

    public void ensureSupplementalDataElementSearchParameter() {
        // create a transaction bundle
        ca.uhn.fhir.util.BundleBuilder builder = new ca.uhn.fhir.util.BundleBuilder(repository.fhirContext());

        // set the request to be condition on code == supplemental data
        builder.addTransactionCreateEntry(R4MeasureServiceUtils.SUPPLEMENTAL_DATA_SEARCHPARAMETER)
                .conditional("code=supplemental-data");
        try {
            repository.transaction(builder.getBundle());
        } catch (NotImplementedOperationException e) {
            log.warn(
                    "Error creating supplemental data search parameter. This may be due to the server not supporting transactions.",
                    e);
        }
    }

    public MeasureReport addSubjectReference(MeasureReport measureReport, String practitioner, String subjectId) {
        if ((StringUtils.isNotBlank(practitioner) || StringUtils.isNotBlank(subjectId))
                && (measureReport.getType().name().equals(MeasureReportType.SUMMARY.name())
                        || measureReport.getType().name().equals(MeasureReportType.SUBJECTLIST.name()))) {
            if (StringUtils.isNotBlank(practitioner)) {
                if (!practitioner.contains("/")) {
                    practitioner = "Practitioner/".concat(practitioner);
                }
                measureReport.setSubject(new Reference(practitioner));
            } else {
                if (!subjectId.contains("/")) {
                    subjectId = "Patient/".concat(subjectId);
                }
                measureReport.setSubject(new Reference(subjectId));
            }
        }
        return measureReport;
    }

    public Optional<Reference> getReporter(String reporter) {
        if (reporter != null && !reporter.isEmpty() && !reporter.contains("/")) {
            // This value may come from configuration, not a user request
            throw new IllegalArgumentException(
                    "R4MultiMeasureService requires '[ResourceType]/[ResourceId]' format to set MeasureReport.reporter reference.");
        }
        Reference reference = null;
        if (reporter != null && !reporter.isEmpty()) {
            if (reporter.startsWith(RESOURCE_TYPE_PRACTITIONER_ROLE)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_PRACTITIONER_ROLE));
            } else if (reporter.startsWith(RESOURCE_TYPE_PRACTITIONER)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_PRACTITIONER));
            } else if (reporter.startsWith(RESOURCE_TYPE_ORGANIZATION)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_ORGANIZATION));
            } else if (reporter.startsWith(RESOURCE_TYPE_LOCATION)) {
                reference = new Reference(Ids.ensureIdType(reporter, RESOURCE_TYPE_LOCATION));
            } else {
                // This value may come from configuration, not a user request
                throw new IllegalArgumentException("MeasureReport.reporter does not accept ResourceType: " + reporter);
            }
        }

        return Optional.ofNullable(reference);
    }

    public Measure resolveById(IdType id) {
        return this.repository.read(Measure.class, id);
    }

    public Measure resolveByUrl(String url) {
        Map<String, List<IQueryParameterType>> searchParameters = new HashMap<>();
        if (url.contains("|")) {
            // uri & version
            var splitId = url.split("\\|");
            var uri = splitId[0];
            var version = splitId[1];
            searchParameters.put("url", Collections.singletonList(new UriParam(uri)));
            searchParameters.put("version", Collections.singletonList(new TokenParam(version)));
        } else {
            // uri only
            searchParameters.put("url", Collections.singletonList(new UriParam(url)));
        }

        Bundle result = this.repository.search(Bundle.class, Measure.class, searchParameters);
        return (Measure) result.getEntryFirstRep().getResource();
    }

    public Measure resolveByIdentifier(String identifier) {
        List<IQueryParameterType> params = new ArrayList<>();
        Map<String, List<IQueryParameterType>> searchParams = new HashMap<>();
        Bundle bundle;
        if (identifier.contains("|")) {
            // system & value
            var splitId = identifier.split("\\|");
            var system = splitId[0];
            var code = splitId[1];
            params.add(new TokenParam(system, code));
        } else {
            // value only
            params.add(new TokenParam(identifier));
        }
        searchParams.put("identifier", params);
        bundle = this.repository.search(Bundle.class, Measure.class, searchParams);

        if (bundle != null && !bundle.getEntry().isEmpty()) {
            if (bundle.getEntry().size() > 1) {
                var msg = "Measure Identifier: %s, found more than one matching measure resource".formatted(identifier);
                throw new InvalidRequestException(msg);
            }
            return (Measure) bundle.getEntryFirstRep().getResource();
        } else {
            var msg = "Measure Identifier: %s, found no matching measure resources".formatted(identifier);
            throw new InvalidRequestException(msg);
        }
    }

    public List<Measure> getMeasures(
            List<IdType> measureIds, List<String> measureIdentifiers, List<String> measureCanonicals) {
        List<Measure> measures = new ArrayList<>();
        if (measureIds != null && !measureIds.isEmpty()) {
            for (IdType measureId : measureIds) {
                Measure measureById = resolveById(measureId);
                measures.add(measureById);
            }
        }

        if (measureCanonicals != null && !measureCanonicals.isEmpty()) {
            for (String measureCanonical : measureCanonicals) {
                Measure measureByUrl = resolveByUrl(measureCanonical);
                measures.add(measureByUrl);
            }
        }

        if (measureIdentifiers != null && !measureIdentifiers.isEmpty()) {
            for (String measureIdentifier : measureIdentifiers) {
                Measure measureByIdentifier = resolveByIdentifier(measureIdentifier);
                measures.add(measureByIdentifier);
            }
        }

        return distinctByKey(measures, Measure::getUrl);
    }

    public static <T> List<T> distinctByKey(List<T> list, Function<? super T, ?> keyExtractor) {
        Set<Object> seen = new HashSet<>();
        return list.stream()
                .filter(element -> seen.add(keyExtractor.apply(element)))
                .collect(Collectors.toList());
    }

    public List<MeasureScoring> getMeasureGroupScoringTypes(Measure measure) {
        var groupScoringCodes = measure.getGroup().stream()
                .map(t -> (CodeableConcept)
                        t.getExtensionByUrl(CQFM_SCORING_EXT_URL).getValue())
                .collect(Collectors.toList());
        // extract measureScoring Type from components
        return groupScoringCodes.stream()
                .map(t -> MeasureScoring.fromCode(t.getCodingFirstRep().getCode()))
                .collect(Collectors.toList());
    }
    /*]
    // TODO: add logic for handling multi-rate Measures for care-gaps
    // this method will check for varying scoring types defined on a measure
        public boolean hasMultipleGroupScoringTypes(Measure measure) {
            if (measure.getGroup().size() > 1) {
                var scoringType = getMeasureGroupScoringTypes(measure);
                // all scoringTypes in list match?
                return scoringType.stream().allMatch(scoringType.get(0)::equals);
            } else {
                // single rate Measures can't have multiple group scoring definitions
                return false;
            }
        }
    */
    public boolean hasGroupScoringDef(Measure measure) {

        return !measure.getGroup().stream()
                .filter(t -> t.getExtensionByUrl(CQFM_SCORING_EXT_URL) != null)
                .collect(Collectors.toList())
                .isEmpty();
    }

    public List<MeasureScoring> getMeasureScoringDef(Measure measure) {
        if (hasGroupScoringDef(measure)) {
            return getMeasureGroupScoringTypes(measure);
        } else {
            if (!measure.hasScoring()) {
                throw new InvalidRequestException(
                        "Measure: %s, does not have a defined Measure Scoring Type.".formatted(measure.getIdPart()));
            }
            return Collections.singletonList(MeasureScoring.fromCode(
                    measure.getScoring().getCodingFirstRep().getCode()));
        }
    }

    public void listThrowIllegalArgumentIfEmpty(List<String> value, String parameterName) {
        if (value == null || value.isEmpty()) {
            throw new InvalidRequestException(parameterName + " parameter requires a value.");
        }
    }

    public MeasureEvalType getMeasureEvalType(String reportType, @Nullable String subjectId) {
        return convertToNonVersionedMeasureEvalTypeOrDefault(
                getR4MeasureEvalType(reportType, Collections.singletonList(subjectId)));
    }

    public MeasureEvalType getMeasureEvalType(String reportType, List<String> subjectIds) {
        return convertToNonVersionedMeasureEvalTypeOrDefault(getR4MeasureEvalType(reportType, subjectIds));
    }

    @Nonnull
    public R4MeasureEvalType getR4MeasureEvalType(String reportType, List<String> subjectIds) {
        return
        // validate in R4 accepted values
        R4MeasureEvalType.fromCode(reportType)
                .orElse(
                        // map null reportType parameter to evalType if no subject parameter is provided
                        isSubjectListEffectivelyEmpty(subjectIds)
                                ? R4MeasureEvalType.POPULATION
                                : R4MeasureEvalType.SUBJECT);
    }

    @Nonnull
    public MeasureEvalType convertToNonVersionedMeasureEvalTypeOrDefault(R4MeasureEvalType r4MeasureEvalType) {
        return MeasureEvalType.fromCode(r4MeasureEvalType.toCode()).orElse(MeasureEvalType.SUBJECT);
    }

    public boolean isSubjectListEffectivelyEmpty(List<String> subjectIds) {
        return subjectIds == null || subjectIds.isEmpty() || subjectIds.get(0) == null;
    }

    public static Measure foldMeasure(Either3<CanonicalType, IdType, Measure> measure, IRepository repository) {
        return measure.fold(
                measureCanonicalType -> resolveByUrl(measureCanonicalType, repository),
                measureIdType -> resolveById(measureIdType, repository),
                Function.identity());
    }

    private static Measure resolveByUrl(CanonicalType url, IRepository repository) {
        var parts = Canonicals.getParts(url);
        var result = repository.search(
                Bundle.class, Measure.class, Searches.byNameAndVersion(parts.idPart(), parts.version()));
        return (Measure) result.getEntryFirstRep().getResource();
    }

    public static Measure resolveById(IdType id, IRepository repository) {
        return repository.read(Measure.class, id);
    }
}
