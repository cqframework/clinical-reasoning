package org.opencds.cqf.fhir.cr.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;

@TestInstance(Lifecycle.PER_CLASS)
class CliTest {

    @TempDir
    private static Path tempDir;

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private static String testResourcePath = null;

    @BeforeAll
    void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        Resources.copyFromJar("/", tempDir);
        testResourcePath = tempDir.toAbsolutePath().toString();
        System.out.println(String.format("Test resource directory: %s", testResourcePath));
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
    void testVersion() {
        String[] args = new String[] {"-V"};
        Main.run(args);
        assertTrue(outContent.toString().startsWith("cqf-fhir-cr-cli version:"));
    }

    @Test
    void testHelp() {
        String[] args = new String[] {"-h"};
        Main.run(args);
        String output = outContent.toString();
        assertTrue(output.startsWith("Usage:"));
        // assertTrue(output.endsWith("Patient=123\n"));
    }

    @Test
    void testEmpty() {
        String[] args = new String[] {};
        Main.run(args);
        String output = errContent.toString();
        assertTrue(output.startsWith("Missing required subcommand"));
        // assertTrue(output.endsWith("Patient=123\n"));
    }

    @Test
    void testNull() {
        assertThrows(NullPointerException.class, () -> {
            Main.run(null);
        });
    }

    @Test
    void testDstu3() {}

    @Test
    void testArgFile() {
        String[] args = new String[] {"argfile", testResourcePath + "/argfile/args.txt"};

        Main.run(args);

        String output = outContent.toString();

        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(output.contains("TestAdverseEvent=[AdverseEvent(id=example)]"));
    }

    @Test
    void testR4() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/r4",
            "-ln=TestFHIR",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/r4/example",
            "-t=" + testResourcePath + "/r4/vocabulary/valueset",
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
    void testR4WithHelpers() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/r4",
            "-ln=TestFHIRWithHelpers",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/r4/example",
            "-t=" + testResourcePath + "/r4/vocabulary/valueset",
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
    void testUSCore() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/uscore",
            "-ln=TestUSCore",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/uscore/example",
            "-t=" + testResourcePath + "/uscore/vocabulary/valueset",
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
    void testQICore() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/qicore",
            "-ln=TestQICore",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/qicore/example",
            "-t=" + testResourcePath + "/qicore/vocabulary/valueset",
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
        // assertTrue(output.contains("TestSlices=[Observation(id=blood-pressure)]"));
        assertTrue(output.contains("TestSimpleExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestComplexExtensions=Patient(id=example)"));
        assertTrue(output.contains("TestEncounterDiagnosisCardinality=true"));
        // assertTrue(output.contains("TestProcedureNotDoneElements=[Procedure(id=negation-example),
        // Procedure(id=negation-with-code-example)]"));
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
    void testQICoreCommon() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/qicorecommon",
            "-ln=QICoreCommonTests",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/qicorecommon/example",
            "-t=" + testResourcePath + "/qicorecommon/vocabulary/valueset",
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
    void testOptions() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-op=" + testResourcePath + "/options/cql-options.json",
            "-lu=" + testResourcePath + "/options",
            "-ln=FluentFunctions",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/options/example",
            "-t=" + testResourcePath + "/options/vocabulary/valueset",
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
    void testOptionsFailure() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-op=" + testResourcePath + "/optionsFailure/cql-options.json",
            "-lu=" + testResourcePath + "/optionsFailure",
            "-ln=FluentFunctions",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/optionsFailure",
            "-t=" + testResourcePath + "/optionsFailure/vocabulary/valueset",
            "-c=Patient",
            "-cv=example"
        };

        Main.run(args);

        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("library FluentFunctions loaded, but had errors"));
    }

    @Test
    void testVSCastFunction14() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-op=" + testResourcePath + "/vscast/cql-options.json",
            "-lu=" + testResourcePath + "/vscast",
            "-ln=TestVSCastFunction",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/vscast/Patient-17",
            "-t=" + testResourcePath + "/vscast/vocabulary/valueset",
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
    void testVSCastFunction15() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-op=" + testResourcePath + "/vscast15/cql-options.json",
            "-lu=" + testResourcePath + "/vscast15",
            "-ln=TestVSCastFunction",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/vscast15/Patient-17",
            "-t=" + testResourcePath + "/vscast15/vocabulary/valueset",
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
    void testQICoreSupplementalDataElements() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/qicore",
            "-ln=SupplementalDataElements_QICore4",
            "-lv=2.0.0",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/qicore/example",
            "-t=" + testResourcePath + "/qicore/vocabulary/valueset",
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
        assertTrue(output.contains("SDE Payer=[Tuple {\n" + "\t\"code\": Concept {\n"
                + "\tCode { code: 59, system: urn:oid:2.16.840.1.113883.3.221.5, version: null, display: Other Private Insurance }\n"
                + "}\n" + "\t\"period\": Interval[2011-05-23, 2012-05-23]\n" + "}]"));
        assertTrue(
                output.contains(
                        "SDE Sex=Code { code: M, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Male }"));
    }

    @Test
    void testQICoreEXM124Example() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/qicore",
            "-ln=EXM124_QICore4",
            "-lv=8.2.000",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/qicore/example",
            "-t=" + testResourcePath + "/qicore/vocabulary/valueset",
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
        assertTrue(output.contains("SDE Payer=[Tuple {\n" + "\t\"code\": Concept {\n"
                + "\tCode { code: 59, system: urn:oid:2.16.840.1.113883.3.221.5, version: null, display: Other Private Insurance }\n"
                + "}\n" + "\t\"period\": Interval[2011-05-23, 2012-05-23]\n" + "}]"));
        assertTrue(
                output.contains(
                        "SDE Sex=Code { code: M, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Male }"));
        assertTrue(output.contains("Initial Population=false"));
        assertTrue(output.contains("Denominator=false"));
        assertTrue(output.contains("Denominator Exclusion=false"));
        assertTrue(output.contains("Numerator=false"));
    }

    @Test
    void testQICoreEXM124Denom() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/qicore",
            "-ln=EXM124_QICore4",
            "-lv=8.2.000",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/qicore/denom-EXM124",
            "-t=" + testResourcePath + "/qicore/vocabulary/valueset",
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
    void testQICoreEXM124Numer() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-lu=" + testResourcePath + "/qicore",
            "-ln=EXM124_QICore4",
            "-lv=8.2.000",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/qicore/numer-EXM124",
            "-t=" + testResourcePath + "/qicore/vocabulary/valueset",
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
    @Disabled("This test is failing on the CI Server for reasons unknown. Need to debug that.")
    void testSampleContentIG() {
        String[] args = new String[] {
            "cql",
            "-fv=R4",
            "-rd=" + testResourcePath + "/samplecontentig",
            "-ig=" + "input/mycontentig.xml",
            "-lu=" + testResourcePath + "/samplecontentig/input/cql",
            "-ln=DependencyExample",
            "-lv=0.1.0",
            "-m=FHIR",
            "-mu=" + testResourcePath + "/samplecontentig/input/tests/DependencyExample",
            "-t=" + testResourcePath + "/samplecontentig/input/vocabulary/valueset",
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
}
