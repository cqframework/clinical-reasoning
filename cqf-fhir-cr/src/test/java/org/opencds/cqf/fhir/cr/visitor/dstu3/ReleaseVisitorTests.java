package org.opencds.cqf.fhir.cr.visitor.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opencds.cqf.fhir.utility.dstu3.Parameters.booleanPart;
import static org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.dstu3.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.SearchParameter;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.opencds.cqf.fhir.cr.visitor.ReleaseVisitor;
import org.opencds.cqf.fhir.cr.visitor.VisitorHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyProviderRouter;
import org.opencds.cqf.fhir.utility.client.terminology.ITerminologyServerClient;
import org.opencds.cqf.fhir.utility.dstu3.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.slf4j.event.Level;

class ReleaseVisitorTests {
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();
    private IRepository repo;
    private final IParser jsonParser = fhirContext.newJsonParser();
    private final List<String> badVersionList = Arrays.asList(
            "11asd1",
            "1.1.3.1.1",
            "1.|1.1.1",
            "1/.1.1.1",
            "-1.-1.2.1",
            "1.-1.2.1",
            "1.1.-2.1",
            "7.1..21",
            "1.2.1.3-draft",
            "1.2.3-draft",
            "3.2",
            "1.",
            "3.ad.2.",
            "1.0.0.1",
            "",
            null);

    @BeforeEach
    void setup() {
        SearchParameter sp = (SearchParameter) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("SearchParameter-artifactAssessment.json"));
        repo = new InMemoryFhirRepository(fhirContext);
        repo.update(sp);
    }

    @Test
    void visitMeasureCollectionTest() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-ecqm-qicore-2024-simplified.json"));
        repo.transaction(bundle);
        Library library = repo.read(Library.class, new IdType("Library/ecqm-update-2024-05-02"))
                .copy();

        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();
        var versionParam = params.addParameter();
        versionParam.setName("version").setValue(new StringType("1.0.0"));
        var versionBehaviourParam = params.addParameter();
        versionBehaviourParam.setName("versionBehavior").setValue(new StringType("default"));

        var releaseVisitor = new ReleaseVisitor(repo);
        // Approval date is required to release an artifact
        library.setApprovalDateElement(new DateType("2024-04-23"));
        // Set the ID to Manifest-Release
        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        assertNotNull(returnResource);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        var dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
                .toList();
        var componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
                .toList();
        // resolvable resources get descriptors
        for (final var dependency : dependenciesOnReleasedArtifact) {
            if (dependency
                    .getResource()
                    .getReference()
                    .equals("https://madie.cms.gov/Library/BreastCancerScreeningFHIR|0.0.001")) {
                assertEquals("Library BreastCancerScreeningFHIR, 0.0.001", dependency.getDisplay());
            }
            if (dependency
                    .getResource()
                    .getReference()
                    .equals("https://madie.cms.gov/Measure/BreastCancerScreeningFHIR|0.0.001")) {
                assertEquals("Measure Breast Cancer ScreeningFHIR, 0.0.001", dependency.getDisplay());
            }
            if (dependency
                    .getResource()
                    .getReference()
                    .equals("https://madie.cms.gov/Library/CervicalCancerScreeningFHIR|0.0.001")) {
                assertEquals("Library CervicalCancerScreeningFHIR, 0.0.001", dependency.getDisplay());
            }
            if (dependency
                    .getResource()
                    .getReference()
                    .equals("https://madie.cms.gov/Measure/CervicalCancerScreeningFHIR|0.0.001")) {
                assertEquals("Measure Cervical Cancer ScreeningFHIR, 0.0.001", dependency.getDisplay());
            }
            // expansion params versions should be used
            if (Canonicals.getUrl(dependency.getResource().getReference()) != null
                    && Canonicals.getUrl(dependency.getResource().getReference())
                            .equals("http://loinc.org")) {
                assertNotNull(Canonicals.getVersion(dependency.getResource().getReference()));
                assertEquals(
                        "2.76", Canonicals.getVersion(dependency.getResource().getReference()));
            }
            if (Canonicals.getUrl(dependency.getResource().getReference()) != null
                    && Canonicals.getUrl(dependency.getResource().getReference())
                            .equals("http://snomed.info/sct")) {
                assertNotNull(Canonicals.getVersion(dependency.getResource().getReference()));
                assertEquals(
                        "http://snomed.info/sct/731000124108/version/20230901",
                        Canonicals.getVersion(dependency.getResource().getReference()));
            }
        }
        assertEquals(56, dependenciesOnReleasedArtifact.size());
        assertEquals(2, componentsOnReleasedArtifact.size());
    }

    @Test
    void visitMeasureEffectiveDataRequirementsTest() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-ecqm-qicore-2024-simplified.json"));
        repo.transaction(bundle);
        Library library = repo.read(Library.class, new IdType("Library/ecqm-update-2024-05-02"))
                .copy();
        Measure cervicalCancerScreeningFHIR =
                repo.read(Measure.class, new IdType("Measure/CervicalCancerScreeningFHIR"));
        Measure breastCancerScreeningFHIR = repo.read(Measure.class, new IdType("Measure/BreastCancerScreeningFHIR"));
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();
        var versionParam = params.addParameter();
        versionParam.setName("version").setValue(new StringType("1.0.0"));
        var versionBehaviourParam = params.addParameter();
        versionBehaviourParam.setName("versionBehavior").setValue(new StringType("default"));

        var releaseVisitor = new ReleaseVisitor(repo);
        // Approval date is required to release an artifact
        library.setApprovalDateElement(new DateType("2024-04-23"));
        // removing the effectiveDataRequirements changes the dependency count
        cervicalCancerScreeningFHIR.setContained(null);
        repo.update(cervicalCancerScreeningFHIR);
        breastCancerScreeningFHIR.setContained(null);
        repo.update(breastCancerScreeningFHIR);

        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        assertNotNull(returnResource);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        var dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
                .toList();
        var componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
                .toList();

        assertEquals(67, dependenciesOnReleasedArtifact.size());
        assertEquals(2, componentsOnReleasedArtifact.size());
    }

    @Test
    void bothCRMIandCQFMEffectiveDataRequirementsTest() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-ecqm-qicore-2024-simplified.json"));
        repo.transaction(bundle);
        Library library = repo.read(Library.class, new IdType("Library/ecqm-update-2024-05-02"))
                .copy();
        Measure cervicalCancerScreeningFHIR =
                repo.read(Measure.class, new IdType("Measure/CervicalCancerScreeningFHIR"));
        Measure breastCancerScreeningFHIR = repo.read(Measure.class, new IdType("Measure/BreastCancerScreeningFHIR"));
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = new Parameters();
        params.addParameter().setName("version").setValue(new StringType("1.0.0"));
        params.addParameter().setName("versionBehavior").setValue(new CodeType("default"));
        var crmiEDRId = "exp-params-crmi-test";
        var crmiEDRExtension = new Extension();
        crmiEDRExtension.setUrl(Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS);
        crmiEDRExtension.setValue(new UriType("#" + crmiEDRId));
        var releaseVisitor = new ReleaseVisitor(repo);
        // Approval date is required to release an artifact
        library.setApprovalDateElement(new DateType("2024-04-23"));
        // if both cqfm and crmi effective data requirements are present then they will each be traced
        var crmiEDRCervical = cervicalCancerScreeningFHIR.getContained().get(0).copy();
        crmiEDRCervical.setId(crmiEDRId);
        cervicalCancerScreeningFHIR.addContained(crmiEDRCervical);
        cervicalCancerScreeningFHIR.addExtension(crmiEDRExtension);
        var crmiEDRBreastCancer =
                breastCancerScreeningFHIR.getContained().get(0).copy();
        crmiEDRBreastCancer.setId(crmiEDRId);
        breastCancerScreeningFHIR.addContained(crmiEDRBreastCancer);
        breastCancerScreeningFHIR.addExtension(crmiEDRExtension);
        repo.update(cervicalCancerScreeningFHIR);
        repo.update(breastCancerScreeningFHIR);

        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        assertNotNull(returnResource);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        var dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
                .toList();
        var componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
                .toList();

        // this should be 69, but we're not handling contained reference correctly
        assertEquals(68, dependenciesOnReleasedArtifact.size());
        assertEquals(2, componentsOnReleasedArtifact.size());
    }

    @Test
    void visitLibraryTest() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        library.addRelatedArtifact()
                .setResource(new Reference("should-be-deleted-1"))
                .setType(RelatedArtifactType.DEPENDSON);
        library.addRelatedArtifact()
                .setResource(new Reference("should-be-deleted-2"))
                .setType(RelatedArtifactType.DEPENDSON);
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        String version = "1.0.1";
        String existingVersion = "1.2.3";
        Parameters params = new Parameters();
        var versionParam = params.addParameter();
        versionParam.setName("version").setValue(new StringType(version));
        var versionBehaviorParam = params.addParameter();
        versionBehaviorParam.setName("versionBehavior").setValue(new CodeType("default"));

        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        assertNotNull(returnResource);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        // versionBehaviour == 'default' so version should be
        // existingVersion and not the new version provided in
        // the parameters
        assertEquals(releasedLibrary.getVersion(), existingVersion);
        var expectedErsdTestArtifactDependencies = Arrays.asList(
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/Library/rctc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/dxtc|" + existingVersion,
                "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-triggering-valueset-library",
                "http://notOwnedTest.com/Library/notOwnedRoot|0.1.1",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|20210526",
                "http://snomed.info/sct");
        var expectedErsdTestArtifactComponents = Arrays.asList(
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/us-ecr-specification|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/Library/rctc|" + existingVersion,
                "http://notOwnedTest.com/Library/notOwnedRoot|0.1.1");
        var dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
                .map(ra -> ra.getResource().getReference())
                .toList();
        var componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
                .map(ra -> ra.getResource().getReference())
                .toList();
        // check that the released artifact has all the required dependencies
        for (var dependency : expectedErsdTestArtifactDependencies) {
            assertTrue(dependenciesOnReleasedArtifact.contains(dependency));
        }
        // and components
        for (var component : expectedErsdTestArtifactComponents) {
            assertTrue(componentsOnReleasedArtifact.contains(component));
        }
        // has extra groupers and rctc dependencies
        assertEquals(expectedErsdTestArtifactDependencies.size(), dependenciesOnReleasedArtifact.size());
        assertEquals(expectedErsdTestArtifactComponents.size(), componentsOnReleasedArtifact.size());

        var expansionParameters =
                new AdapterFactory().createLibrary(releasedLibrary).getExpansionParameters();
        var canonicalVersionParams = expansionParameters
                .map(p -> VisitorHelper.getStringListParameter(Constants.CANONICAL_VERSION, p)
                        .orElse(null))
                .orElse(new ArrayList<String>());
        assertEquals(0, canonicalVersionParams.size());
    }

    @Test
    void releaseResource_force_version() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        // Existing version should be "1.2.3"
        String newVersionToForce = "1.2.7";
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(
                part("version", new StringType(newVersionToForce)), part("versionBehavior", new CodeType("force")));

        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);

        assertNotNull(returnResource);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        assertEquals(releasedLibrary.getVersion(), newVersionToForce);
    }

    @Test
    void releaseResource_require_non_experimental_error() {
        // SpecificationLibrary - root is experimental but HAS experimental children
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft-experimental.json"));
        repo.transaction(bundle);
        // SpecificationLibrary2 - root is NOT experimental but HAS experimental children
        Bundle bundle2 = (Bundle) jsonParser.parseResource(ReleaseVisitorTests.class.getResourceAsStream(
                "Bundle-small-approved-draft-experimental-children.json"));
        repo.transaction(bundle2);
        Parameters params = parameters(
                part("version", new StringType("1.2.3")),
                part("versionBehavior", new CodeType("default")),
                part("requireNonExperimental", new CodeType("error")));
        Exception notExpectingAnyException = null;
        // no Exception if root is experimental
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        try {
            libraryAdapter.accept(releaseVisitor, params);
        } catch (Exception e) {
            notExpectingAnyException = e;
        }
        assertNull(notExpectingAnyException);

        UnprocessableEntityException nonExperimentalChildException = null;
        Library library2 = repo.read(Library.class, new IdType("Library/SpecificationLibrary2"))
                .copy();
        ILibraryAdapter libraryAdapter2 = new AdapterFactory().createLibrary(library2);
        try {
            libraryAdapter2.accept(releaseVisitor, params);
        } catch (UnprocessableEntityException e) {
            nonExperimentalChildException = e;
        }
        assertNotNull(nonExperimentalChildException);
        assertTrue(nonExperimentalChildException.getMessage().contains("not Experimental"));
    }

    @Test
    void releaseResource_require_non_experimental_warn() {
        // SpecificationLibrary - root is experimental but HAS experimental children
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft-experimental.json"));
        repo.transaction(bundle);
        // SpecificationLibrary2 - root is NOT experimental but HAS experimental children
        Bundle bundle2 = (Bundle) jsonParser.parseResource(ReleaseVisitorTests.class.getResourceAsStream(
                "Bundle-small-approved-draft-experimental-children.json"));
        repo.transaction(bundle2);

        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        Library library2 = repo.read(Library.class, new IdType("Library/SpecificationLibrary2"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        ILibraryAdapter libraryAdapter2 = new AdapterFactory().createLibrary(library2);

        Parameters params = parameters(
                part("version", new StringType("1.2.3")),
                part("versionBehavior", new CodeType("default")),
                part("requireNonExperimental", new CodeType("warn")));

        var logger = TestLoggerFactory.getTestLogger(ReleaseVisitor.class);
        logger.clear();

        libraryAdapter.accept(releaseVisitor, params);
        // no warning if the root is Experimental
        assertEquals(0, logger.getLoggingEvents().size());

        libraryAdapter2.accept(releaseVisitor, params);

        var warningMessages = logger.getLoggingEvents().stream()
                .filter(event -> event.getLevel().equals(Level.WARN))
                .map(LoggingEvent::getMessage)
                .toList();

        // SHOULD warn if the root is not experimental
        assertTrue(warningMessages.stream()
                .anyMatch(message ->
                        message.contains("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.7")));
        assertTrue(warningMessages.stream()
                .anyMatch(message -> message.contains("http://ersd.aimsplatform.org/fhir/Library/rctc2")));
    }

    @Test
    void releaseResource_propagate_effective_period() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-no-child-effective-period.json"));
        repo.transaction(bundle);
        String effectivePeriodToPropagate = "2020-12-11";

        Parameters params =
                parameters(part("version", new StringType("1.2.7")), part("versionBehavior", new CodeType("default")));
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        assertNotNull(returnResource);
        MetadataResourceHelper.forEachMetadataResource(
                returnResource.getEntry(),
                resource -> {
                    assertNotNull(resource);
                    if (!(resource instanceof ValueSet)) {
                        IKnowledgeArtifactAdapter adapter = new AdapterFactory().createLibrary(library);
                        assertTrue(((Period) adapter.getEffectivePeriod()).hasStart());
                        Date start = ((Period) adapter.getEffectivePeriod()).getStart();
                        Calendar calendar = new GregorianCalendar();
                        calendar.setTime(start);
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH) + 1;
                        int day = calendar.get(Calendar.DAY_OF_MONTH);
                        String startString = year + "-" + month + "-" + day;
                        assertEquals(effectivePeriodToPropagate, startString);
                    }
                },
                repo);
    }

    @Test
    void release_latest_from_tx_server_sets_versions() {
        // SpecificationLibrary - root is experimental but HAS experimental children
        final var leafOid = "2.16.840.1.113762.1.4.1146.6";
        final var authoritativeSource = "http://cts.nlm.nih.gov/fhir/";
        var bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        removeVersionsFromLibraryAndGrouperAndUpdate(repo, leafOid);
        var latestVSet = repo.read(ValueSet.class, new IdType("ValueSet/2.16.840.1.113762.1.4.1146.6"));
        var library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();

        var endpoint = createEndpoint(authoritativeSource);

        var clientMock = mock(ITerminologyServerClient.class, new ReturnsDeepStubs());
        when(clientMock.getLatestValueSetResource(any(IEndpointAdapter.class), any())).thenReturn(Optional.of(latestVSet));
        var releaseVisitor = new ReleaseVisitor(repo, clientMock);
        var libraryAdapter = new AdapterFactory().createLibrary(library);
        var params = parameters(
                part("version", new StringType("1.2.7")),
                part("versionBehavior", new CodeType("default")),
                booleanPart("latestFromTxServer", true),
                part("terminologyEndpoint", (org.hl7.fhir.dstu3.model.Endpoint) endpoint.get()));
        libraryAdapter.accept(releaseVisitor, params);
        var grouper = repo.read(ValueSet.class, new IdType("ValueSet/dxtc"));
        var include = grouper.getCompose().getIncludeFirstRep();
        assertNotNull(Canonicals.getVersion(include.getValueSet().get(0).getValue()));
        assertEquals(
                latestVSet.getVersion(),
                Canonicals.getVersion(include.getValueSet().get(0).getValue()));
        var updatedLibrary = repo.read(Library.class, new IdType("Library/SpecificationLibrary"));
        var leafRelatedArtifact = updatedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getResource().getReference().contains(leafOid))
                .findAny();
        assertTrue(leafRelatedArtifact.isPresent());
        assertNotNull(
                Canonicals.getVersion(leafRelatedArtifact.get().getResource().getReference()));
        assertEquals(
                latestVSet.getVersion(),
                Canonicals.getVersion(leafRelatedArtifact.get().getResource().getReference()));
    }

    private IEndpointAdapter createEndpoint(String authoritativeSource) {
        var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.DSTU3);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.dstu3.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.dstu3.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.dstu3.model.StringType("username")));
        endpoint.addExtension(new org.hl7.fhir.dstu3.model.Extension(
                Constants.APIKEY, new org.hl7.fhir.dstu3.model.StringType("password")));
        return endpoint;
    }

    private void removeVersionsFromLibraryAndGrouperAndUpdate(IRepository repo, String leafOid) {
        // remove versions from references
        var library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"));
        library.getRelatedArtifact().forEach(ra -> {
            if (ra.getResource().getReference().contains(leafOid)) {
                ra.getResource().setReference(Canonicals.getUrl(ra.getResource().getReference()));
            }
        });
        var grouper = repo.read(ValueSet.class, new IdType("ValueSet/dxtc"));
        grouper.getCompose().getInclude().forEach(include -> {
            var valueSetCanonical = include.getValueSet().get(0);
            if (valueSetCanonical.getValue().contains(leafOid)) {
                valueSetCanonical.setValue(Canonicals.getUrl(valueSetCanonical.getValue()));
            }
        });
        repo.update(library);
        repo.update(grouper);
    }

    @Test
    void release_missing_approvalDate_validation_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-release-missing-approvalDate.json"));
        repo.transaction(bundle);

        String versionData = "1.2.3";
        String actualErrorMessage = "";

        Parameters params1 = parameters(part("version", versionData), part("versionBehavior", new CodeType("default")));
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/ReleaseSpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        try {
            libraryAdapter.accept(releaseVisitor, params1);
        } catch (Exception e) {
            actualErrorMessage = e.getMessage();
        }
        assertTrue(actualErrorMessage.contains("last modified date (indicated by date)"));

        libraryAdapter.setDate(new Date());
        try {
            libraryAdapter.accept(releaseVisitor, params1);
        } catch (Exception e) {
            actualErrorMessage = e.getMessage();
        }
        assertTrue(actualErrorMessage.contains("approvalDate"));
    }

    @Test
    void release_version_format_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        for (String version : badVersionList) {
            UnprocessableEntityException maybeException = null;
            Parameters params = parameters(
                    part("version", new StringType(version)), part("versionBehavior", new CodeType("force")));
            try {
                libraryAdapter.accept(releaseVisitor, params);

            } catch (UnprocessableEntityException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
    }

    @Test
    void release_releaseLabel_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        String releaseLabel = "release label test";
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(
                part("releaseLabel", new StringType(releaseLabel)),
                part("version", "1.2.3"),
                part("versionBehavior", new CodeType("default")));
        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);

        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        Optional<Extension> maybeReleaseLabel = releasedLibrary.getExtension().stream()
                .filter(ext -> ext.getUrl().equals(IKnowledgeArtifactAdapter.RELEASE_LABEL_URL))
                .findFirst();
        assertTrue(maybeReleaseLabel.isPresent());
        assertEquals(((StringType) maybeReleaseLabel.get().getValue()).getValue(), releaseLabel);
    }

    @Test
    void release_version_active_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        PreconditionFailedException maybeException = null;
        Parameters params =
                parameters(part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType("force")));
        try {
            libraryAdapter.accept(releaseVisitor, params);
        } catch (PreconditionFailedException e) {
            maybeException = e;
        }
        assertNotNull(maybeException);
    }

    @Test
    void release_versionBehaviour_format_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        Library library = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        ILibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        List<String> badVersionBehaviors = Arrays.asList("not-a-valid-option", null);
        for (String versionBehaviour : badVersionBehaviors) {
            Exception maybeException = null;
            Parameters params = parameters(
                    part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType(versionBehaviour)));
            try {
                libraryAdapter.accept(releaseVisitor, params);
            } catch (FHIRException | UnprocessableEntityException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
    }

    @Test
    void release_preserves_extensions() {
        var bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        var originalLibrary = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var testLibrary = originalLibrary.copy();
        var libraryAdapter = new AdapterFactory().createLibrary(testLibrary);
        var params =
                parameters(part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType("force")));
        var returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        var releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        for (final var originalRelatedArtifact : originalLibrary.getRelatedArtifact()) {
            releasedLibrary.getRelatedArtifact().forEach(releasedRelatedArtifact -> {
                if (Canonicals.getUrl(releasedRelatedArtifact.getResource().getReference())
                                .equals(Canonicals.getUrl(
                                        originalRelatedArtifact.getResource().getReference()))
                        && originalRelatedArtifact.getType() == releasedRelatedArtifact.getType()) {
                    // Verify all original extensions are preserved
                    originalRelatedArtifact.getExtension().forEach(originalExt -> {
                        var matchingExtensions = releasedRelatedArtifact.getExtensionsByUrl(originalExt.getUrl());
                        assertTrue(
                                !matchingExtensions.isEmpty(),
                                "Original extension " + originalExt.getUrl() + " should be preserved");
                    });
                    // Verify extension count is at least as many as original (may have added CRMI extensions)
                    assertTrue(
                            releasedRelatedArtifact.getExtension().size()
                                    >= originalRelatedArtifact.getExtension().size(),
                            "Released artifact should have at least as many extensions as original");
                }
            });
        }
    }

    @Test
    void release_should_not_duplicate_components_as_dependencies() {
        var bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        var originalLibrary = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var testLibrary = originalLibrary.copy();
        var libraryAdapter = new AdapterFactory().createLibrary(testLibrary);
        var params =
                parameters(part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType("force")));
        var returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        Optional<BundleEntryComponent> maybeRCTCLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/rctc"))
                .findFirst();
        assertTrue(maybeRCTCLib.isPresent());
        var releasedRCTCLibrary = repo.read(
                Library.class, new IdType(maybeRCTCLib.get().getResponse().getLocation()));
        assertEquals(2, releasedRCTCLibrary.getRelatedArtifact().size());
        // 1 component
        assertTrue(releasedRCTCLibrary.getRelatedArtifact().stream()
                .anyMatch(ra -> ra.getType() == RelatedArtifactType.DEPENDSON));
        // 1 dependency
        assertTrue(releasedRCTCLibrary.getRelatedArtifact().stream()
                .anyMatch(ra -> ra.getType() == RelatedArtifactType.COMPOSEDOF));
    }

    @Test
    void release_should_pin_the_latest_version_of_dependencies() {
        var bundle = (Bundle) jsonParser.parseResource(
                ReleaseVisitorTests.class.getResourceAsStream("Bundle-unversioned-dependency.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        var originalLibrary = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var retiredLeaf = repo.read(ValueSet.class, new IdType("ValueSet/2.16.840.1.113762.1.4.1146.77-old"))
                .copy();
        var testLibrary = originalLibrary.copy();
        var libraryAdapter = new AdapterFactory().createLibrary(testLibrary);
        var params =
                parameters(part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType("force")));
        var returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        var maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        var releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        var maybeActiveLeafRA = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getResource().getReference().contains("2.16.840.1.113762.1.4.1146.6"))
                .findFirst();
        assertTrue(maybeActiveLeafRA.isPresent());
        assertEquals(
                "1.0.1",
                Canonicals.getVersion(maybeActiveLeafRA.get().getResource().getReference()));
        var maybeRetiredLeafRA = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getResource().getReference().contains("2.16.840.1.113762.1.4.1146.77"))
                .findFirst();
        assertTrue(maybeRetiredLeafRA.isPresent());
        assertNotEquals(
                retiredLeaf.getUrl() + "|" + retiredLeaf.getVersion(),
                maybeRetiredLeafRA.get().getResource().getReference());
        assertEquals(
                "3.2.3",
                Canonicals.getVersion(maybeRetiredLeafRA.get().getResource().getReference()));
    }

    @Test
    void release_should_capture_input_and_runtime_expansion_params() {
        var bundle = (Bundle) jsonParser.parseResource(
                org.opencds.cqf.fhir.cr.visitor.dstu3.ReleaseVisitorTests.class.getResourceAsStream(
                        "Bundle-versioned-and-unversioned-dependency.json"));
        repo.transaction(bundle);
        var releaseVisitor = new ReleaseVisitor(repo);
        var originalLibrary = repo.read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        var testLibrary = originalLibrary.copy();
        var libraryAdapter = new AdapterFactory().createLibrary(testLibrary);
        var params =
                parameters(part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType("force")));
        var returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, params);
        var maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        var releasedLibrary =
                repo.read(Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        var authoredExpansionParamsExt = releasedLibrary.getExtensionByUrl(Constants.CQF_INPUT_EXPANSION_PARAMETERS);
        var runtimeExpansionParamsExt = releasedLibrary.getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS);
        var authoredExpansionParams = (Parameters) releasedLibrary.getContained().stream()
                .filter(cont -> cont.getId()
                        .equals(((Reference) authoredExpansionParamsExt.getValue())
                                .getReference()
                                .substring(1)))
                .findFirst()
                .get();
        var runtimeExpansionParams = (Parameters) releasedLibrary.getContained().stream()
                .filter(cont -> cont.getId()
                        .equals(((Reference) runtimeExpansionParamsExt.getValue())
                                .getReference()
                                .substring(1)))
                .findFirst()
                .get();
        assertEquals(1, authoredExpansionParams.getParameter().size());
        assertEquals(3, runtimeExpansionParams.getParameter().size());
    }
}
