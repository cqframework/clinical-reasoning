package org.opencds.cqf.cql.evaluator.cli;

import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CliTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private static final String testResourceRelativePath = "src/test/resources";
    private static String testResourcePath = null;

    @BeforeClass
    public void setup(){
        File file = new File(testResourceRelativePath);
        testResourcePath = file.getAbsolutePath();
        System.out.println(String.format("Test resource directory: %s", testResourcePath));
    }

    @BeforeMethod
    public void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterMethod
    public void restoreStreams() {
        String sysOut = outContent.toString();
        String sysError = errContent.toString();

        System.setOut(originalOut);
        System.setErr(originalErr);

        System.out.println(sysOut);
        System.err.println(sysError);
    }

    @Test 
    public void testVersion() {
        String[] args = new String[] { "-V" };
        Main.run(args);
        assertTrue(outContent.toString().startsWith("cql-evaluator cli version:"));
    }

    @Test
    public void testHelp() {
        String[] args = new String[] { "-h" };
        Main.run(args);
        String output = outContent.toString();
        assertTrue(output.startsWith("Usage:"));
        // assertTrue(output.endsWith("Patient=123\n"));
    }

    @Test 
    public void testEmpty() {
        String[] args = new String[] { };
        Main.run(args);
        String output = errContent.toString();
        assertTrue(output.startsWith("Missing required subcommand"));
        // assertTrue(output.endsWith("Patient=123\n"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNull() {
        Main.run(null);
    }

   
    @Test
    public void testDstu3() {

    }

    @Test
    public void testArgFile() {
        String [] args = new String[] {
            "argfile",
            testResourcePath + "/argfile/args.txt"
        };

        Main.run(args);

        String output = outContent.toString();

        assertTrue(output.contains("Patient=Patient(id=example)"));
        assertTrue(output.contains("TestAdverseEvent=[AdverseEvent(id=example)]"));
    }

    @Test
    public void testR4() {
        String[] args = new String[]{
                "cql",
                "-fv=R4",
                "-lu="+ testResourcePath + "/r4",
                "-ln=TestFHIR",
                "-m=FHIR",
                "-mu=" + testResourcePath + "/r4",
                "-t=" + testResourcePath + "/r4/vocabulary/ValueSet",
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
    public void testR4WithHelpers() {
        String[] args = new String[] {
                "cql",
                "-fv=R4",
                "-lu=" + testResourcePath + "/r4",
                "-ln=TestFHIRWithHelpers",
                "-m=FHIR",
                "-mu=" + testResourcePath + "/r4",
                "-t=" + testResourcePath + "/r4/vocabulary/ValueSet",
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
    public void testUSCore() {
        String[] args = new String[]{
                "cql",
                "-fv=R4",
                "-lu="+ testResourcePath + "/uscore",
                "-ln=TestUSCore",
                "-m=FHIR",
                "-mu=" + testResourcePath + "/uscore",
                "-t=" + testResourcePath + "/uscore/vocabulary/ValueSet",
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
    public void testQICore() {
        String[] args = new String[]{
                "cql",
                "-fv=R4",
                "-lu="+ testResourcePath + "/qicore",
                "-ln=TestQICore",
                "-m=FHIR",
                "-mu=" + testResourcePath + "/qicore",
                "-t=" + testResourcePath + "/qicore/vocabulary/ValueSet",
                "-c=Patient",
                "-cv=example"
        };

        Main.run(args);

        String output = outContent.toString();
    }
}
