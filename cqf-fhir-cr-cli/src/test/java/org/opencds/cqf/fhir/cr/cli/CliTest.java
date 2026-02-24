package org.opencds.cqf.fhir.cr.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import jakarta.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opencds.cqf.fhir.test.Resources;

@SuppressWarnings("squid:S1135")
@TestInstance(Lifecycle.PER_CLASS)
class CliTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser();
    private static final String MEASUREREPORTS_FOLDER = "measurereports";
    private static final String TXTRESULTS_FOLDER = "txtresults";

    @TempDir
    private static Path tempDir;

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private static String testResourcePath = null;
    private static String testResultsPath = null;

    @BeforeAll
    void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        Resources.copyFromJar("/", tempDir);
        testResourcePath = tempDir.toAbsolutePath().toString();
        System.out.printf("Test resource directory: %s%n", testResourcePath);
        testResultsPath = testResourcePath + "/results";
    }

    @BeforeEach
    void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        String sysOut = outContent.toString();
        String sysError = errContent.toString();

        System.setOut(originalOut);
        System.setErr(originalErr);

        System.out.println(sysOut);
        System.err.println(sysError);
    }

    @Test
    void version() {
        String[] args = new String[] {"-V"};
        Main.run(args);
        assertTrue(outContent.toString().startsWith("cqf-fhir-cr-cli version:"));
    }

    @Test
    void help() {
        String[] args = new String[] {"-h"};
        Main.run(args);
        String output = outContent.toString();
        assertTrue(output.startsWith("Usage:"));
    }

    @Test
    void empty() {
        String[] args = new String[] {};
        Main.run(args);
        String output = errContent.toString();
        assertTrue(output.startsWith("Missing required subcommand"));
    }

    @Test
    void testNull() {
        assertThrows(NullPointerException.class, () -> Main.run(null));
    }

    @Test
    void argFile() {
        String[] args = new String[] {"argfile", testResourcePath + "/argfile/args.txt"};

        Main.run(args);

        String output = outContent.toString();

        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(output.contains("TestAdverseEvent=[AdverseEvent(id=example)]"));
    }

    @Test
    void r4() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/r4/input/cql",
            "-name=TestFHIR",
            "-data=" + testResourcePath + "/r4",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();

        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(output.contains("TestAdverseEvent=[AdverseEvent(id=example)]"));
        assertTrue(output.contains("TestPatientGender=Patient(id=example)"));
        assertTrue(output.contains("TestPatientActive=Patient(id=example)"));
        assertTrue(output.contains("TestPatientBirthDate=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMaritalStatusMembership=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMartialStatusComparison=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsBoolean=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsDateTime=null"));
        assertTrue(output.contains("TestSlices=[Observation(id=blood-pressure)]"));
        assertTrue(output.contains("TestSimpleExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestComplexExtensions=Patient(id=example)"));
    }

    @Test
    void r4WithOutputPath() {
        var outputPath = Path.of(testResultsPath, "TestFHIR");
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/r4/input/cql",
            "-name=TestFHIR",
            "-data=" + testResourcePath + "/r4",
            "-c=Patient",
            "-cv=example",
            "--output-path=" + outputPath.toString()
        };

        Main.run(args);

        assertTrue(outputPath.toFile().isDirectory());
    }

    @Test
    void r4WithHelpers() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/r4/input/cql",
            "-name=TestFHIRWithHelpers",
            "-data=" + testResourcePath + "/r4",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();

        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(output.contains("TestAdverseEvent=[AdverseEvent(id=example)]"));
        assertTrue(output.contains("TestPatientGender=Patient(id=example)"));
        assertTrue(output.contains("TestPatientActive=Patient(id=example)"));
        assertTrue(output.contains("TestPatientBirthDate=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMaritalStatusMembership=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMartialStatusComparison=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsBoolean=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsDateTime=null"));
        assertTrue(output.contains("TestSlices=[Observation(id=blood-pressure)]"));
        assertTrue(output.contains("TestSimpleExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestComplexExtensions=Patient(id=example)"));
    }

    @Test
    void uSCore() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/uscore/input/cql",
            "-name=TestUSCore",
            "-data=" + testResourcePath + "/uscore",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("TestPatientGender=Patient(id=example)"));
        assertTrue(output.contains("TestPatientActive=Patient(id=example)"));
        assertTrue(output.contains("TestPatientBirthDate=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMaritalStatusMembership=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMartialStatusComparison=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsBoolean=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsDateTime=null"));
        assertTrue(output.contains("TestSlices=[Observation(id=blood-pressure)]"));
        assertTrue(output.contains("TestSimpleExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestComplexExtensions=Patient(id=example)"));
    }

    @Test
    void qICore() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/qicore/input/cql",
            "-name=TestQICore",
            "-data=" + testResourcePath + "/qicore",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("TestPatientGender=Patient(id=example)"));
        assertTrue(output.contains("TestPatientActive=Patient(id=example)"));
        assertTrue(output.contains("TestPatientBirthDate=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMaritalStatusMembership=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMartialStatusComparison=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsBoolean=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsDateTime=null"));
        // TODO: This is because the engine is not validating on profile-based
        // retrieve...
        assertTrue(output.contains("TestSimpleExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestComplexExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestEncounterDiagnosisCardinality=true"));
        // NOTE: Testing combinations here because ordering is not guaranteed
        assertTrue(
                output.contains(
                                "TestGeneralDeviceNotRequested=[DeviceRequest(id=negation-example), DeviceRequest(id=negation-with-code-example)]")
                        || output.contains(
                                "TestGeneralDeviceNotRequested=[DeviceRequest(id=negation-with-code-example), DeviceRequest(id=negation-example)]"));
        assertTrue(
                output.contains(
                                "TestGeneralDeviceNotRequestedCode=[DeviceRequest(id=negation-example), DeviceRequest(id=negation-with-code-example)]")
                        || output.contains(
                                "TestGeneralDeviceNotRequestedCode=[DeviceRequest(id=negation-with-code-example), DeviceRequest(id=negation-example)]"));
        assertTrue(
                output.contains(
                                "TestGeneralDeviceNotRequestedValueSet=[DeviceRequest(id=negation-example), DeviceRequest(id=negation-with-code-example)]")
                        || output.contains(
                                "TestGeneralDeviceNotRequestedValueSet=[DeviceRequest(id=negation-with-code-example), DeviceRequest(id=negation-example)]"));
        assertTrue(
                output.contains(
                                "TestGeneralDeviceNotRequestedActual=[DeviceRequest(id=negation-example), DeviceRequest(id=negation-with-code-example)]")
                        || output.contains(
                                "TestGeneralDeviceNotRequestedActual=[DeviceRequest(id=negation-with-code-example), DeviceRequest(id=negation-example)]"));
        assertTrue(
                output.contains(
                                "TestGeneralDeviceNotRequestedExplicit=[DeviceRequest(id=negation-example), DeviceRequest(id=negation-with-code-example)]")
                        || output.contains(
                                "TestGeneralDeviceNotRequestedExplicit=[DeviceRequest(id=negation-with-code-example), DeviceRequest(id=negation-example)]"));
        assertTrue(output.contains(
                "TestGeneralDeviceNotRequestedCodeExplicit=[DeviceRequest(id=negation-with-code-example)]"));
        assertTrue(
                output.contains("TestGeneralDeviceNotRequestedValueSetExplicit=[DeviceRequest(id=negation-example)]"));
    }

    @Test
    void qICoreCommon() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/qicorecommon/input/cql",
            "-name=QICoreCommonTests",
            "-data=" + testResourcePath + "/qicorecommon",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("TestPatientGender=Patient(id=example)"));
        assertTrue(output.contains("TestPatientActive=Patient(id=example)"));
        assertTrue(output.contains("TestPatientBirthDate=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMaritalStatusMembership=Patient(id=example)"));
        assertTrue(output.contains("TestPatientMartialStatusComparison=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsBoolean=Patient(id=example)"));
        assertTrue(output.contains("TestPatientDeceasedAsDateTime=null"));
        assertTrue(output.contains("TestSlices=[Observation(id=blood-pressure)]"));
        assertTrue(output.contains("TestSimpleExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestComplexExtensions=Patient(id=example)"));
    }

    @Test
    void options() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/options/input/cql",
            "-name=FluentFunctions",
            "-data=" + testResourcePath + "/options",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(output.contains("Four=4"));
        assertTrue(output.contains("TestPlus=true"));
        assertTrue(output.contains("Testplus=-8"));
    }

    @Test
    void optionsFailure() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/optionsFailure/input/cql",
            "-name=FluentFunctions",
            "-data=" + testResourcePath + "/optionsFailure",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Library FluentFunctions loaded, but had errors"));
    }

    @Test
    void vSCastFunction14() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/vscast/input/cql",
            "-name=TestVSCastFunction",
            "-data=" + testResourcePath + "/vscast",
            "-c=Patient",
            "-cv=Patient-17"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("TestConditions=[Condition(id=Condition-17-94)]"));
        assertTrue(output.contains("TestConditionsViaFunction=[Condition(id=Condition-17-94)]"));
        assertTrue(output.contains("TestConditionsDirectly=[Condition(id=Condition-17-94)]"));
    }

    @Test
    void vSCastFunction15() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/vscast15/input/cql",
            "-name=TestVSCastFunction",
            "-data=" + testResourcePath + "/vscast15",
            "-c=Patient",
            "-cv=Patient-17"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("TestConditions=[Condition(id=Condition-17-94)]"));
        assertTrue(output.contains("TestConditionsViaFunction=[Condition(id=Condition-17-94)]"));
        assertTrue(output.contains("TestConditionsDirectly=[Condition(id=Condition-17-94)]"));
    }

    @Test
    void qICoreSupplementalDataElements() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/qicore/input/cql",
            "-name=SupplementalDataElements_QICore4",
            "-data=" + testResourcePath + "/qicore",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(
                output.contains(
                        "SDE Ethnicity=[Code { code: 2184-0, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Dominican }, Code { code: 2148-5, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Mexican }]"));
        assertTrue(
                output.contains(
                        "SDE Race=[Code { code: 1586-7, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Shoshone }, Code { code: 2036-2, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Filipino }, Code { code: 1735-0, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Alaska Native }]"));
        assertTrue(
                output.contains(
                        "SDE Payer=[Tuple {\n  code: Concept {\n\tCode { code: 59, system: urn:oid:2.16.840.1.113883.3.221.5, version: null, display: Other Private Insurance }\n}\n  period: Interval[2011-05-23, 2012-05-23]\n}]"));
        assertTrue(
                output.contains(
                        "SDE Sex=Code { code: M, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Male }"));
    }

    @Test
    void qICoreEXM124Example() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/qicore/input/cql",
            "-name=EXM124_QICore4",
            "-data=" + testResourcePath + "/qicore",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(
                output.contains(
                        "SDE Ethnicity=[Code { code: 2184-0, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Dominican }, Code { code: 2148-5, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Mexican }]"));
        assertTrue(
                output.contains(
                        "SDE Race=[Code { code: 1586-7, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Shoshone }, Code { code: 2036-2, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Filipino }, Code { code: 1735-0, system: urn:oid:2.16.840.1.113883.6.238, version: null, display: Alaska Native }]"));
        assertTrue(
                output.contains(
                        "SDE Payer=[Tuple {\n  code: Concept {\n\tCode { code: 59, system: urn:oid:2.16.840.1.113883.3.221.5, version: null, display: Other Private Insurance }\n}\n  period: Interval[2011-05-23, 2012-05-23]\n}]"));
        assertTrue(
                output.contains(
                        "SDE Sex=Code { code: M, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Male }"));
        assertTrue(output.contains("Initial Population=false"));
        assertTrue(output.contains("Denominator=false"));
        assertTrue(output.contains("Denominator Exclusion=false"));
        assertTrue(output.contains("Numerator=false"));
    }

    @Test
    void qICoreEXM124Denom() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/qicore/input/cql",
            "-name=EXM124_QICore4",
            "-data=" + testResourcePath + "/qicore",
            "-c=Patient",
            "-cv=denom-EXM124"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Patient=Patient(id=denom-EXM124)"));
        assertTrue(
                output.contains(
                        "SDE Sex=Code { code: F, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Female }"));
        assertTrue(output.contains("Initial Population=true"));
        assertTrue(output.contains("Denominator=true"));
        assertTrue(output.contains("Denominator Exclusion=false"));
        assertTrue(output.contains("Numerator=false"));
    }

    @Test
    void qICoreEXM124Numer() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/qicore/input/cql",
            "-name=EXM124_QICore4",
            "-data=" + testResourcePath + "/qicore",
            "-c=Patient",
            "-cv=numer-EXM124"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Patient=Patient(id=numer-EXM124)"));
        assertTrue(
                output.contains(
                        "SDE Sex=Code { code: F, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Female }"));
        assertTrue(output.contains("Initial Population=true"));
        assertTrue(output.contains("Denominator=true"));
        assertTrue(output.contains("Denominator Exclusion=false"));
        assertTrue(output.contains("Numerator=true"));
    }

    @Test
    void hedisCompatibilityModeTest() {
        // TODO: this test exposes an issue, which is that we don't support
        // evaluating arbitrary CQL libraries without context/data in the CLI.
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/hedis/input/cql",
            "-data=" + testResourcePath + "/hedis",
            "-name=ReturnTest",
            "-c=Patient",
            "-cv=ABC",
            "--enable-hedis-compatibility-mode",
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Return=[1, 1, 1, 2, 2]"));
    }

    @Test
    void expressionTest() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/expression/input/cql",
            "-data=" + testResourcePath + "/expression",
            "-name=ExpressionTest",
            "-c=Patient",
            "-cv=ABC",
            "--expression=One",
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("One=1"));
        // We do not expect "Two" to be evaluated because we specified a single expression
        assertFalse(output.contains("Two=2"));
    }

    @Test
    void compartmentalizedTests() {
        String[] args = new String[] {
            "cql",
            "-source=" + testResourcePath + "/compartment/input/cql",
            "-name=Example",
            "-data=" + testResourcePath + "/compartment",
            "-c=Patient",
            "-cv=123",
            "-c=Patient",
            "-cv=456"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Patient=Patient(id=123)"));
        assertTrue(output.contains("Encounters=[Encounter(id=ABC)]"));
        assertTrue(output.contains("Patient=Patient(id=456)"));
        assertTrue(output.contains("Encounters=[Encounter(id=DEF)]"));
    }

    @ParameterizedTest
    @CsvSource({"ABCLIB,ABC", "DEFLIB,DEF"})
    void measureEvaluationTest(String libraryName, String measureId) throws IOException {
        var expectedMeasureId = "http://example.com/Measure/%s".formatted(measureId);
        var subjectId1 = "Patient/123";
        var subjectId2 = "Patient/456";

        var expectedTxtResult123 = """
                Encounters=[Encounter(id=ABC)]
                Patient=Patient(id=123)
                """;

        var expectedTxtResult456 = """
                Encounters=[Encounter(id=DEF)]
                Patient=Patient(id=456)
                """;

        String[] args = new String[] {
            "measure",
            "-source=" + testResourcePath + "/compartment/input/cql",
            "-name=%s".formatted(libraryName),
            "-data=" + testResourcePath + "/compartment",
            "-c=Patient",
            "-cv=123",
            "-c=Patient",
            "-cv=456",
            "--measure=%s".formatted(measureId),
            "--output-path=" + Path.of(testResultsPath, libraryName, TXTRESULTS_FOLDER),
            "--report-path=" + Path.of(testResultsPath, libraryName, MEASUREREPORTS_FOLDER),
        };

        Main.run(args);

        var resultsMap = getFilenameToTxtResultsMap(libraryName, MEASUREREPORTS_FOLDER, TXTRESULTS_FOLDER);

        final List<Pair<Path, String>> measureReportJsons = resultsMap.get(MEASUREREPORTS_FOLDER);
        assertEquals(2, measureReportJsons.size());

        final Optional<MeasureReport> measureReport123 = getMeasureReportForSubject(measureReportJsons, "123.json");
        assertTrue(measureReport123.isPresent());
        assertMeasureReport(measureReport123.get(), expectedMeasureId, subjectId1);

        final Optional<MeasureReport> measureReport456 = getMeasureReportForSubject(measureReportJsons, "456.json");
        assertTrue(measureReport456.isPresent());
        assertMeasureReport(measureReport456.get(), expectedMeasureId, subjectId2);

        final List<Pair<Path, String>> txtResults = resultsMap.get(TXTRESULTS_FOLDER);
        assertEquals(2, txtResults.size());

        final Optional<String> txtResult123 = getTxtResultsForSubject(txtResults, "123.txt");
        assertTrue(txtResult123.isPresent());
        assertEquals(expectedTxtResult123.trim(), txtResult123.get().trim());

        final Optional<String> txtResult456 = getTxtResultsForSubject(txtResults, "456.txt");
        assertTrue(txtResult456.isPresent());
        assertEquals(expectedTxtResult456.trim(), txtResult456.get().trim());
    }

    @ParameterizedTest
    @CsvSource({"ABCLIB,ABC", "DEFLIB,DEF"})
    void measureEvaluationTestSystemOut(String libraryName, String measureId) {
        String[] args = new String[] {
            "measure",
            "-source=" + testResourcePath + "/compartment/input/cql",
            "-name=%s".formatted(libraryName),
            "-data=" + testResourcePath + "/compartment",
            "-c=Patient",
            "-cv=123",
            "-c=Patient",
            "-cv=456",
            "--apply-scoring=false",
            "--measure=%s".formatted(measureId),
        };

        Main.run(args);

        // Should be two MeasureReports printed to the console
        String output = outContent.toString();
        assertTrue(output.contains("\"resourceType\":\"MeasureReport\""));
        assertTrue(output.contains("\"subject\":{\"reference\":\"Patient/123\""));
        assertTrue(output.contains("\"subject\":{\"reference\":\"Patient/456\""));
    }

    @Test
    @Disabled("This test is failing on the CI Server for reasons unknown. Need to debug that.")
    void sampleContentIG() {
        String[] args = new String[] {
            "cql",
            "-root=" + testResourcePath + "/samplecontentig",
            "-ig=" + "input/mycontentig.xml",
            "-source=" + testResourcePath + "/samplecontentig/input/cql",
            "-name=DependencyExample",
            "-data=" + testResourcePath + "/samplecontentig/input/tests/DependencyExample",
            "-terminology=" + testResourcePath + "/samplecontentig/input/vocabulary/valueset",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(output.contains("Observation(id=example)"));
        assertTrue(output.contains("Observation(id=negation-example)"));
        assertTrue(output.contains("Observation(id=pediatric-bmi-example)"));
        assertTrue(output.contains("Observation(id=pediatric-wt-example)"));
        assertTrue(output.contains("Observation(id=satO2-fiO2)"));
        assertFalse(output.contains("Observation(id=blood-glucose)"));
        assertFalse(output.contains("Observation(id=blood-pressure)"));
    }

    @Nonnull
    private Optional<String> getTxtResultsForSubject(List<Pair<Path, String>> txtResults, String file) {
        return txtResults.stream()
                .filter(pair -> Path.of(file).equals(pair.getKey()))
                .map(Pair::getValue)
                .findFirst();
    }

    @Nonnull
    private Optional<MeasureReport> getMeasureReportForSubject(
            List<Pair<Path, String>> measureReportJsons, String first) {
        return measureReportJsons.stream()
                .filter(pair -> Path.of(first).equals(pair.getKey()))
                .map(Pair::getValue)
                .map(json -> JSON_PARSER.parseResource(MeasureReport.class, json))
                .findFirst();
    }

    private void assertMeasureReport(MeasureReport measureReport, String expectedMeasureId, String expectedSubjectId) {
        assertEquals(expectedMeasureId, measureReport.getMeasure());
        assertEquals(MeasureReportStatus.COMPLETE, measureReport.getStatus());
        assertEquals(expectedSubjectId, measureReport.getSubject().getReference());
        assertEquals(MeasureReportType.INDIVIDUAL, measureReport.getType());
    }

    private ListMultimap<String, Pair<Path, String>> getFilenameToTxtResultsMap(
            String libraryName, String... resultTypes) throws IOException {

        final ImmutableListMultimap.Builder<String, Pair<Path, String>> multimapBuilder =
                ImmutableListMultimap.builder();

        for (String resultType : resultTypes) {
            Path resultsPath = Path.of(testResultsPath, libraryName, resultType);
            if (!Files.exists(resultsPath) || !Files.isDirectory(resultsPath)) {
                throw new IOException("Missing or invalid directory: " + resultsPath);
            }

            try (Stream<Path> pathsStream = Files.walk(resultsPath)) {
                for (Path filePath : pathsStream.filter(Files::isRegularFile).toList()) {
                    try (Stream<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
                        String fileContents = lines.collect(Collectors.joining("\n"));
                        // Use relative path from resultsPath to avoid name collisions
                        Path relativePath = filePath.getFileName();
                        multimapBuilder.put(resultType, Pair.of(relativePath, fileContents));
                    }
                }
            }
        }

        return multimapBuilder.build();
    }
}
