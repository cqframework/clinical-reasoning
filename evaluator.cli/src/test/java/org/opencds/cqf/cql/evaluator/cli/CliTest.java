package org.opencds.cqf.cql.evaluator.cli;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CliTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private static final String testResourceRelativePath = "src/test/resources";
    private static String testResourcePath = null;

    @BeforeClass
    public static void setup(){
        File file = new File(testResourceRelativePath);
        testResourcePath = file.getAbsolutePath();
        System.out.println(String.format("Test resource directory: %s", testResourcePath));
    }

    @Before
    public void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        // errContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));
        // System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        String sysOut = outContent.toString();
        // String sysError = errContent.toString();

        System.setOut(originalOut);
        // System.setErr(originalErr);

        System.out.println(sysOut);
        // System.err.println(sysError);
    }

    @Test 
    public void testVersion() {
        String[] args = new String[] { "-v" };
        Main.main(args);
        assertTrue(outContent.toString().startsWith("cql-evaluator cli version:"));
    }

    //@Test
    public void testHelp() {
        String[] args = new String[] { "-h" };
        Main.main(args);
        String output = outContent.toString();
        assertTrue(output.startsWith("cql-evaluator cli version:"));
        // assertTrue(output.endsWith("Patient=123\n"));
    }

    @Test 
    public void testEmpty() {
        String[] args = new String[] { };
        Main.main(args);
        String output = outContent.toString();
        assertTrue(output.startsWith("cql-evaluator cli version:"));
        // assertTrue(output.endsWith("Patient=123\n"));
    }

    @Test 
    public void testNull() {
        Main.main(null);
        String output = outContent.toString();
        assertTrue(output.startsWith("cql-evaluator cli version:"));
        // assertTrue(output.endsWith("Patient=123\n"));
    }

   
    @Test
    public void testDstu3() {

    }

    @Test
    public void testR4() {
        String[] args = new String[]{
                "-lu",
                testResourcePath + "/r4",
                "-ln",
                "TestFHIR",
                "-m",
                "FHIR="+ testResourcePath + "/r4",
                "-t",
                testResourcePath + "/r4/vocabulary/ValueSet",
                "-c",
                "Patient=example"
            };
    
        Main.main(args);

        String output = outContent.toString();

        assertTrue(output.contains("Patient="));
        assertTrue(output.contains("TestAdverseEvent="));
    }

    @Test
    public void testUSCore() {

    }

    @Test
    public void testQICore() {

    }
}
