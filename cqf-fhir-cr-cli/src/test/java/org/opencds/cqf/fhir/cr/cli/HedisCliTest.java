package org.opencds.cqf.fhir.cr.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(Lifecycle.PER_CLASS)
class HedisCliTest {

    @TempDir
    private static Path tempDir;

    protected static final Logger ourLog = LoggerFactory.getLogger(HedisCliTest.class);

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);

    private static String testResourcePath = null;

    @BeforeAll
    void setup() throws URISyntaxException, IOException, ClassNotFoundException {
        Resources.copyFromJar("/", tempDir);
        testResourcePath = tempDir.toAbsolutePath().toString();
        ourLog.info("Test resource directory: {}", testResourcePath);
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
    void testOnePatient() {
        String measureCode = "GSD";
        var testPatientId = "patient.2024.gsd.0.100009";
        var directory = "/Volumes/ExternalDrive/DCSv2/";
        var suffix = "/Sample/v0_tests/tests/Patient/";
        var patientPath = "/Volumes/ExternalDrive/DCSv2/";
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2";
        var resultsPath = patientPath + "_Results8/";
        var measurePath = cqlContentDirectory + "/input/resources/Measure/";
        var measureId = measureCode + "-Reporting";
        var periodStart = "2024-01-01";
        var periodEnd = "2024-12-31";
        String patientBundles = directory + measureCode + suffix;

        run(
                measureCode + "_Reporting",
                patientBundles + testPatientId,
                cqlContentDirectory,
                resultsPath,
                measurePath,
                measureId,
                periodStart,
                periodEnd,
                testPatientId);
    }

    void run(
            String libraryName,
            String patientPath,
            String cqlContentDirectory,
            String resultsPath,
            String measurePath,
            String measure,
            String periodStart,
            String periodEnd,
            String patientId) {
        var baseArgs = Stream.of(
                "cql",
                "-fv=R4",
                "-rd=" + cqlContentDirectory,
                "-lu=" + cqlContentDirectory + "/input/cql",
                "-ln=" + libraryName,
                "-lv=2024.2.0",
                "-m=FHIR",
                "-mu=" + patientPath,
                "-resultsPath=" + resultsPath,
                "-singleFile=" + true,
                "-t=" + cqlContentDirectory + "/input/vocabulary/valueset",
                "-measurePath=" + measurePath,
                "-measure=" + measure,
                "-periodStart=" + periodStart,
                "-periodEnd=" + periodEnd,
                "-c=Patient",
                "-cv=" + patientId);

        var args = baseArgs.toArray(String[]::new);
        Main.run(args);
    }
}
