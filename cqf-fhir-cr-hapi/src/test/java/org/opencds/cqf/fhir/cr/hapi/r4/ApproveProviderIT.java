package org.opencds.cqf.fhir.cr.hapi.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.util.BundleUtil;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment;
import org.opencds.cqf.fhir.utility.r4.ArtifactAssessment.ArtifactAssessmentContentExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ApproveProviderIT extends BaseCrR4TestServer {

    private final FhirContext fhirContext = FhirContext.forR4Cached();

    public Bundle callApprove(
            String id,
            Date approvalDate,
            String artifactAssessmentType,
            String artifactAssessmentSummary,
            CanonicalType artifactAssessmentTarget,
            CanonicalType artifactAssessmentRelatedArtifact,
            Reference artifactAssessmentAuthor) {
        var parametersEval = new Parameters();
        parametersEval.addParameter(
                "approvalDate", approvalDate == null ? null : new DateType(approvalDate, TemporalPrecisionEnum.DAY));
        parametersEval.addParameter(
                "artifactAssessmentType",
                artifactAssessmentType == null ? null : new StringType(artifactAssessmentType));
        parametersEval.addParameter(
                "artifactAssessmentSummary",
                artifactAssessmentSummary == null ? null : new StringType(artifactAssessmentSummary));
        parametersEval.addParameter("artifactAssessmentTarget", artifactAssessmentTarget);
        parametersEval.addParameter("artifactAssessmentRelatedArtifact", artifactAssessmentRelatedArtifact);
        parametersEval.addParameter("artifactAssessmentAuthor", artifactAssessmentAuthor);

        return ourClient
                .operation()
                .onInstance(id)
                .named("$approve")
                .withParameters(parametersEval)
                .returnResourceType(Bundle.class)
                .execute();
    }

    @Test
    void test_approve() {
        loadResourceFromPath("ersd-active-library-example.json");
        loadResourceFromPath("practitioner-example-for-refs.json");

        var id = "Library/SpecificationLibrary";
        var approvalDate = new Date();
        var assessmentType = "comment";
        var artifactAssessmentSummary = "comment text";
        var artifactAssessmentTarget =
                new CanonicalType("http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|1.0.0");
        var artifactAssessmentRelatedArtifact = new CanonicalType("reference-valid-no-spaces");
        var artifactAssessmentAuthor = new Reference("Practitioner/sample-practitioner");
        var result = callApprove(
                id,
                approvalDate,
                assessmentType,
                artifactAssessmentSummary,
                artifactAssessmentTarget,
                artifactAssessmentRelatedArtifact,
                artifactAssessmentAuthor);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(BundleTypeEnum.TRANSACTION_RESPONSE, BundleUtil.getBundleTypeEnum(fhirContext, result));

        var artifactAssessment = result.getEntry().stream()
                .filter(e -> e.hasResponse()
                        && e.getResponse().hasLocation()
                        && e.getResponse().getLocation().contains("Basic"))
                .findFirst();
        Assertions.assertTrue(artifactAssessment.isPresent());
        var persisted = ourClient
                .read()
                .resource(Basic.class)
                .withUrl(artifactAssessment.get().getResponse().getLocation())
                .execute();
        Assertions.assertNotNull(persisted);

        Assertions.assertNotNull(persisted.getExtensionByUrl(ArtifactAssessment.ARTIFACT));
        var artifactExtValue =
                persisted.getExtensionByUrl(ArtifactAssessment.ARTIFACT).getValue();
        Assertions.assertInstanceOf(Reference.class, artifactExtValue);
        Assertions.assertEquals(id, ((Reference) artifactExtValue).getReference());

        Assertions.assertNotNull(persisted.getExtensionByUrl(ArtifactAssessment.CONTENT));
        var contentExt = persisted.getExtensionByUrl(ArtifactAssessment.CONTENT);
        Assertions.assertTrue(contentExt.hasExtension(ArtifactAssessmentContentExtension.INFOTYPE));
        var infoTypeExtValue = contentExt
                .getExtensionByUrl(ArtifactAssessmentContentExtension.INFOTYPE)
                .getValue();
        Assertions.assertInstanceOf(CodeType.class, infoTypeExtValue);
        Assertions.assertEquals(assessmentType, ((CodeType) infoTypeExtValue).getCode());
        Assertions.assertTrue(contentExt.hasExtension(ArtifactAssessmentContentExtension.SUMMARY));
        var summaryExtValue = contentExt
                .getExtensionByUrl(ArtifactAssessmentContentExtension.SUMMARY)
                .getValue();
        Assertions.assertInstanceOf(MarkdownType.class, summaryExtValue);
        Assertions.assertEquals(artifactAssessmentSummary, ((MarkdownType) summaryExtValue).getValueAsString());
        Assertions.assertTrue(contentExt.hasExtension(ArtifactAssessmentContentExtension.RELATEDARTIFACT));
        var relatedArtifactExtList = contentExt.getExtensionsByUrl(ArtifactAssessmentContentExtension.RELATEDARTIFACT);
        Assertions.assertEquals(2, relatedArtifactExtList.size());
        var citation = relatedArtifactExtList.stream()
                .filter(e -> e.getValue() instanceof RelatedArtifact
                        && ((RelatedArtifact) e.getValue()).hasType()
                        && ((RelatedArtifact) e.getValue()).getType().equals(RelatedArtifactType.CITATION))
                .findFirst();
        Assertions.assertTrue(citation.isPresent());
        var relatedArtifact = (RelatedArtifact) citation.get().getValue();
        Assertions.assertInstanceOf(CanonicalType.class, relatedArtifact.getResourceElement());
        Assertions.assertEquals(
                "reference-valid-no-spaces",
                relatedArtifact.getResourceElement().getValue());
        var derivedFrom = relatedArtifactExtList.stream()
                .filter(e -> e.getValue() instanceof RelatedArtifact
                        && ((RelatedArtifact) e.getValue()).hasType()
                        && ((RelatedArtifact) e.getValue()).getType().equals(RelatedArtifactType.DERIVEDFROM))
                .findFirst();
        Assertions.assertTrue(derivedFrom.isPresent());
        relatedArtifact = (RelatedArtifact) derivedFrom.get().getValue();
        Assertions.assertInstanceOf(CanonicalType.class, relatedArtifact.getResourceElement());
        Assertions.assertEquals(
                "http://hl7.org/fhir/us/ecr/Library/SpecificationLibrary|1.0.0",
                relatedArtifact.getResourceElement().getValue());

        Assertions.assertTrue(contentExt.hasExtension(ArtifactAssessmentContentExtension.AUTHOR));
        var authorExtValue = contentExt
                .getExtensionByUrl(ArtifactAssessmentContentExtension.AUTHOR)
                .getValue();
        Assertions.assertInstanceOf(Reference.class, authorExtValue);
        Assertions.assertEquals("Practitioner/sample-practitioner", ((Reference) authorExtValue).getReference());

        Assertions.assertNotNull(persisted.getExtensionByUrl(ArtifactAssessment.DATE));
        var dateExtValue = persisted.getExtensionByUrl(ArtifactAssessment.DATE).getValue();
        Assertions.assertInstanceOf(DateTimeType.class, dateExtValue);
        Assertions.assertEquals(
                approvalDate.toInstant().truncatedTo(ChronoUnit.DAYS),
                ((DateTimeType) dateExtValue).getValue().toInstant().truncatedTo(ChronoUnit.DAYS));
    }
}
