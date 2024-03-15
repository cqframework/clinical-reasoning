package org.opencds.cqf.fhir.utility.visitor.r4;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory;
import org.opencds.cqf.fhir.utility.r4.MetadataResourceHelper;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactReleaseVisitor;
import org.slf4j.LoggerFactory;

public class KnowledgeArtifactReleaseVisitorTests {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    private Repository spyRepository;
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
            "",
            null);

    @BeforeEach
    public void setup() {
        SearchParameter sp = (SearchParameter)
                jsonParser.parseResource(KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream(
                        "SearchParameter-artifactAssessment.json"));
        spyRepository = spy(new InMemoryFhirRepository(fhirContext));
        spyRepository.update(sp);
        doAnswer(new Answer<Bundle>() {
                    @Override
                    public Bundle answer(InvocationOnMock a) throws Throwable {
                        Bundle b = a.getArgument(0);
                        return InMemoryFhirRepository.transactionStub(b, spyRepository);
                    }
                })
                .when(spyRepository)
                .transaction(any());
    }

    @Test
    void visitLibraryTest() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-release-bundle.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/ReleaseSpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        String version = "1.0.1.23";
        String existingVersion = "1.2.3";
        Parameters params = new Parameters();
        params.addParameter("version", version);
        params.addParameter("versionBehavior", new CodeType("default"));

        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, spyRepository, params);
        assertNotNull(returnResource);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary = spyRepository.read(
                Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        // versionBehaviour == 'default' so version should be
        // existingVersion and not the new version provided in
        // the parameters
        assertTrue(releasedLibrary.getVersion().equals(existingVersion));
        List<String> ersdTestArtifactDependencies = Arrays.asList(
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-dxtc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-ostc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-lotc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-lrtc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-mrtc|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/ValueSet/release-sdtc|" + existingVersion,
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.6|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1063|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.360|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.120|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.362|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.528|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.408|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.409|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1469|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1866|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1906|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.480|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.481|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.761|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1223|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1182|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1181|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1184|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1601|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1600|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1603|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1602|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1082|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1439|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1436|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1435|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1446|2022-10-19",
                "http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.1438|2022-10-19",
                "http://notOwnedTest.com/Library/notOwnedRoot|0.1.1",
                "http://notOwnedTest.com/Library/notOwnedLeaf|0.1.1",
                "http://notOwnedTest.com/Library/notOwnedLeaf1|0.1.1");
        List<String> ersdTestArtifactComponents = Arrays.asList(
                "http://ersd.aimsplatform.org/fhir/PlanDefinition/release-us-ecr-specification|" + existingVersion,
                "http://ersd.aimsplatform.org/fhir/Library/release-rctc|" + existingVersion,
                "http://notOwnedTest.com/Library/notOwnedRoot|0.1.1");
        List<String> dependenciesOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.DEPENDSON))
                .map(ra -> ra.getResource())
                .collect(Collectors.toList());
        List<String> componentsOnReleasedArtifact = releasedLibrary.getRelatedArtifact().stream()
                .filter(ra -> ra.getType().equals(RelatedArtifact.RelatedArtifactType.COMPOSEDOF))
                .map(ra -> ra.getResource())
                .collect(Collectors.toList());
        // check that the released artifact has all the required dependencies
        for (String dependency : ersdTestArtifactDependencies) {
            assertTrue(dependenciesOnReleasedArtifact.contains(dependency));
        }
        // and components
        for (String component : ersdTestArtifactComponents) {
            assertTrue(componentsOnReleasedArtifact.contains(component));
        }
        // TODO: update the list of dependencies to be in line with $data-requirements
        // assertTrue(ersdTestArtifactDependencies.size() == dependenciesOnReleasedArtifact.size());
        assertTrue(ersdTestArtifactComponents.size() == componentsOnReleasedArtifact.size());
    }

    @Test
    void releaseResource_force_version() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        spyRepository.transaction(bundle);
        // Existing version should be "1.2.3";
        String newVersionToForce = "1.2.7.23";
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(
                part("version", new StringType(newVersionToForce)), part("versionBehavior", new CodeType("force")));

        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, spyRepository, params);

        assertNotNull(returnResource);
        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary = spyRepository.read(
                Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        assertTrue(releasedLibrary.getVersion().equals(newVersionToForce));
    }

    @Test
    void releaseResource_require_non_experimental_error() {
        // SpecificationLibrary - root is experimentalbut HAS experimental children
        Bundle bundle =
                (Bundle) jsonParser.parseResource(KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream(
                        "Bundle-small-approved-draft-experimental.json"));
        spyRepository.transaction(bundle);
        // SpecificationLibrary2 - root is NOT experimental but HAS experimental children
        Bundle bundle2 =
                (Bundle) jsonParser.parseResource(KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream(
                        "Bundle-small-approved-draft-experimental-children.json"));
        spyRepository.transaction(bundle2);
        Parameters params = parameters(
                part("version", new StringType("1.2.3")),
                part("versionBehavior", new CodeType("default")),
                part("requireNonExperimental", new CodeType("error")));
        Exception notExpectingAnyException = null;
        // no Exception if root is experimental
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        try {
            libraryAdapter.accept(releaseVisitor, spyRepository, params);
        } catch (Exception e) {
            notExpectingAnyException = e;
        }
        assertTrue(notExpectingAnyException == null);

        UnprocessableEntityException nonExperimentalChildException = null;
        Library library2 = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary2"))
                .copy();
        LibraryAdapter libraryAdapter2 = new AdapterFactory().createLibrary(library2);
        try {
            libraryAdapter2.accept(releaseVisitor, spyRepository, params);
        } catch (UnprocessableEntityException e) {
            nonExperimentalChildException = e;
        }
        assertTrue(nonExperimentalChildException != null);
        assertTrue(nonExperimentalChildException.getMessage().contains("not Experimental"));
    }

    @Test
    void releaseResource_require_non_experimental_warn() {
        // SpecificationLibrary - root is experimentalbut HAS experimental children
        Bundle bundle =
                (Bundle) jsonParser.parseResource(KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream(
                        "Bundle-small-approved-draft-experimental.json"));
        spyRepository.transaction(bundle);
        // SpecificationLibrary2 - root is NOT experimental but HAS experimental children
        Bundle bundle2 =
                (Bundle) jsonParser.parseResource(KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream(
                        "Bundle-small-approved-draft-experimental-children.json"));
        spyRepository.transaction(bundle2);

        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        Library library2 = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary2"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        LibraryAdapter libraryAdapter2 = new AdapterFactory().createLibrary(library2);

        Appender<ILoggingEvent> myMockAppender = mock(Appender.class);
        List<String> warningMessages = new ArrayList<>();
        doAnswer(t -> {
                    ILoggingEvent evt = (ILoggingEvent) t.getArguments()[0];
                    // we only care about warning messages here
                    if (evt.getLevel().equals(Level.WARN)) {
                        // instead of appending to logs, we just add it to a list
                        warningMessages.add(evt.getFormattedMessage());
                    }
                    return null;
                })
                .when(myMockAppender)
                .doAppend(any());
        org.slf4j.Logger logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ch.qos.logback.classic.Logger loggerRoot = (ch.qos.logback.classic.Logger) logger;
        // add the mocked appender, make sure it is detached at the end
        loggerRoot.addAppender(myMockAppender);

        Parameters params = parameters(
                part("version", new StringType("1.2.3")),
                part("versionBehavior", new CodeType("default")),
                part("requireNonExperimental", new CodeType("warn")));
        libraryAdapter.accept(releaseVisitor, spyRepository, params);
        // no warning if the root is Experimental
        assertTrue(warningMessages.size() == 0);

        libraryAdapter2.accept(releaseVisitor, spyRepository, params);

        // SHOULD warn if the root is not experimental
        assertTrue(warningMessages.stream()
                .anyMatch(message ->
                        message.contains("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113762.1.4.1146.7")));
        assertTrue(warningMessages.stream()
                .anyMatch(message -> message.contains("http://ersd.aimsplatform.org/fhir/Library/rctc2")));
        // cleanup
        loggerRoot.detachAppender(myMockAppender);
    }

    @Test
    void releaseResource_propagate_effective_period() {
        Bundle bundle =
                (Bundle) jsonParser.parseResource(KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream(
                        "Bundle-ersd-no-child-effective-period.json"));
        spyRepository.transaction(bundle);
        String effectivePeriodToPropagate = "2020-12-11";

        Parameters params =
                parameters(part("version", new StringType("1.2.7")), part("versionBehavior", new CodeType("default")));
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, spyRepository, params);
        assertNotNull(returnResource);
        MetadataResourceHelper.forEachMetadataResource(
                returnResource.getEntry(),
                resource -> {
                    assertNotNull(resource);
                    if (!resource.getClass().getSimpleName().equals("ValueSet")) {
                        KnowledgeArtifactAdapter adapter = new AdapterFactory().createLibrary(library);
                        assertTrue(((Period) adapter.getEffectivePeriod()).hasStart());
                        Date start = ((Period) adapter.getEffectivePeriod()).getStart();
                        Calendar calendar = new GregorianCalendar();
                        calendar.setTime(start);
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH) + 1;
                        int day = calendar.get(Calendar.DAY_OF_MONTH);
                        String startString = year + "-" + month + "-" + day;
                        assertTrue(startString.equals(effectivePeriodToPropagate));
                    }
                },
                spyRepository);
    }

    @Test
    void releaseResource_latestFromTx_NotSupported_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        spyRepository.transaction(bundle);

        String actualErrorMessage = "";

        Parameters params = parameters(
                part("version", "1.2.3"),
                part("versionBehavior", new CodeType("default")),
                part("latestFromTxServer", new BooleanType(true)));
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        try {
            libraryAdapter.accept(releaseVisitor, spyRepository, params);
        } catch (Exception e) {
            actualErrorMessage = e.getMessage();
        }
        assertTrue(actualErrorMessage.contains("not yet implemented"));
    }

    @Test
    void release_missing_approvalDate_validation_test() {
        Bundle bundle =
                (Bundle) jsonParser.parseResource(KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream(
                        "Bundle-release-missing-approvalDate.json"));
        spyRepository.transaction(bundle);

        String versionData = "1.2.3.23";
        String actualErrorMessage = "";

        Parameters params1 = parameters(part("version", versionData), part("versionBehavior", new CodeType("default")));
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/ReleaseSpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        try {
            libraryAdapter.accept(releaseVisitor, spyRepository, params1);
        } catch (Exception e) {
            actualErrorMessage = e.getMessage();
        }
        assertTrue(actualErrorMessage.contains("approvalDate"));
    }

    @Test
    void release_version_format_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        for (String version : badVersionList) {
            UnprocessableEntityException maybeException = null;
            Parameters params = parameters(
                    part("version", new StringType(version)), part("versionBehavior", new CodeType("force")));
            try {
                libraryAdapter.accept(releaseVisitor, spyRepository, params);

            } catch (UnprocessableEntityException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
    }

    @Test
    void release_releaseLabel_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        spyRepository.transaction(bundle);
        String releaseLabel = "release label test";
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        Parameters params = parameters(
                part("releaseLabel", new StringType(releaseLabel)),
                part("version", "1.2.3.23"),
                part("versionBehavior", new CodeType("default")));
        Bundle returnResource = (Bundle) libraryAdapter.accept(releaseVisitor, spyRepository, params);

        Optional<BundleEntryComponent> maybeLib = returnResource.getEntry().stream()
                .filter(entry -> entry.getResponse().getLocation().contains("Library/SpecificationLibrary"))
                .findFirst();
        assertTrue(maybeLib.isPresent());
        Library releasedLibrary = spyRepository.read(
                Library.class, new IdType(maybeLib.get().getResponse().getLocation()));
        Optional<Extension> maybeReleaseLabel = releasedLibrary.getExtension().stream()
                .filter(ext -> ext.getUrl().equals(KnowledgeArtifactAdapter.releaseLabelUrl))
                .findFirst();
        assertTrue(maybeReleaseLabel.isPresent());
        assertTrue(((StringType) maybeReleaseLabel.get().getValue()).getValue().equals(releaseLabel));
    }

    @Test
    void release_version_active_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream("Bundle-ersd-small-active.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);

        PreconditionFailedException maybeException = null;
        Parameters params =
                parameters(part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType("force")));
        try {
            libraryAdapter.accept(releaseVisitor, spyRepository, params);
        } catch (PreconditionFailedException e) {
            maybeException = e;
        }
        assertNotNull(maybeException);
    }

    @Test
    void release_versionBehaviour_format_test() {
        Bundle bundle = (Bundle) jsonParser.parseResource(
                KnowledgeArtifactReleaseVisitorTests.class.getResourceAsStream("Bundle-small-approved-draft.json"));
        spyRepository.transaction(bundle);
        KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
        Library library = spyRepository
                .read(Library.class, new IdType("Library/SpecificationLibrary"))
                .copy();
        LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
        List<String> badVersionBehaviors = Arrays.asList("not-a-valid-option", null);
        for (String versionBehaviour : badVersionBehaviors) {
            Exception maybeException = null;
            Parameters params = parameters(
                    part("version", new StringType("1.2.3")), part("versionBehavior", new CodeType(versionBehaviour)));
            try {
                libraryAdapter.accept(releaseVisitor, spyRepository, params);
            } catch (FHIRException e) {
                maybeException = e;
            } catch (UnprocessableEntityException e) {
                maybeException = e;
            }
            assertNotNull(maybeException);
        }
    }

    // @Test
    // void release_test_artifactComment_updated() {
    //     Bundle bundle = (Bundle)
    // jsonParser.parseResource(KnowledgeArtifactAdapterReleaseVisitorTests.class.getResourceAsStream("Bundle-release-missing-approvalDate.json"));
    //     spyRepository.transaction(bundle);
    //     KnowledgeArtifactReleaseVisitor releaseVisitor = new KnowledgeArtifactReleaseVisitor();
    //     Library library = spyRepository.read(Library.class, new IdType("Library/SpecificationLibrary")).copy();
    //     r4LibraryAdapter libraryAdapter = new AdapterFactory().createLibrary(library);
    // 	String versionData = "1.2.3";
    // 	Parameters approveParams = parameters(
    // 		part("approvalDate", new DateType(new Date(),TemporalPrecisionEnum.DAY))
    // 	);
    //     Bundle approvedBundle = (Bundle) libraryAdapter.accept(releaseVisitor, spyRepository, params);

    // 	Optional<BundleEntryComponent> maybeArtifactAssessment = approvedBundle.getEntry().stream().filter(entry ->
    // entry.getResponse().getLocation().contains("Basic")).findAny();
    // 	assertTrue(maybeArtifactAssessment.isPresent());
    // 	ArtifactAssessment artifactAssessment =
    // spyRepository.read(ArtifactAssessment.class,maybeArtifactAssessment.get().getResponse().getLocation());
    //
    //	assertTrue(artifactAssessment.getDerivedFromContentRelatedArtifact().get().getResourceElement().getValue().equals("http://ersd.aimsplatform.org/fhir/Library/ReleaseSpecificationLibrary|1.2.3-draft"));
    // 	Parameters releaseParams = parameters(
    // 		part("version", versionData),
    // 		part("versionBehavior", new CodeType("default"))
    // 	);
    // 	Bundle releasedBundle = getClient().operation()
    // 			.onInstance("Library/ReleaseSpecificationLibrary")
    // 			.named("$release")
    // 			.withParameters(releaseParams)
    // 			.useHttpGet()
    // 			.returnResourceType(Bundle.class)
    // 			.execute();
    // 	Optional<BundleEntryComponent> maybeReleasedArtifactAssessment = releasedBundle.getEntry().stream().filter(entry
    // -> entry.getResponse().getLocation().contains("Basic")).findAny();
    // 	assertTrue(maybeReleasedArtifactAssessment.isPresent());
    // 	ArtifactAssessment releasedArtifactAssessment =
    // getClient().fetchResourceFromUrl(ArtifactAssessment.class,maybeReleasedArtifactAssessment.get().getResponse().getLocation());
    //
    //	assertTrue(releasedArtifactAssessment.getDerivedFromContentRelatedArtifact().get().getResourceElement().getValue().equals("http://ersd.aimsplatform.org/fhir/Library/ReleaseSpecificationLibrary|1.2.3"));
    // }
}
