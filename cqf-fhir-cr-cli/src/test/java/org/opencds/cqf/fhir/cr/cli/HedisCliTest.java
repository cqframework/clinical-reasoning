package org.opencds.cqf.fhir.cr.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testParseBundles() throws Exception {
        var test_deck_dir = "/Users/justinmckelvy/Documents/DCSv2/";
        List<String> measureCodes = new ArrayList<>();
//        measureCodes.add("DAE");
//        measureCodes.add("DBO"); // partially unwrapped
//        measureCodes.add("DDE");
//        measureCodes.add("DMH");
//        measureCodes.add("DMSE");
//        measureCodes.add("DRRE");
//        measureCodes.add("DSFE");
//        measureCodes.add("DSU");
//        measureCodes.add("EDH");
//        measureCodes.add("EDU");

        for (String measureCode : measureCodes) {
            String patientBundles = test_deck_dir + measureCode + "/Sample/v0_tests/tests/Patient";
            //"/Users/justinmckelvy/Documents/BulkImport_raw/AAB/AAB-Sample/test";
            //String libraryName = measureCode + "_Reporting";
            // Data staging
            Path dirPath = Paths.get(test_deck_dir + measureCode + "/Sample/deck"); // ← replace with your directory

            processNDJSON(patientBundles, dirPath);
        }


    }

    public void processNDJSON(String patientBundles, Path dirPath) throws Exception {
        Path output = Paths.get(patientBundles);
        try (Stream<Path> paths = Files.list(dirPath)) {
            paths
                .filter(Files::isRegularFile) // skip subdirectories
                .forEach(path -> {
                    Path fullPath = path.toAbsolutePath();
                    try {
                        NdjsonBundleExtractor.extractBundlesToDirs(fullPath, output);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }

    }

    @Test
    public void testOnePatient() throws Exception {
        String measureCode = "COA";
        var testPatientId = "patient.2024.coa.0.95635";
        var directory = "/Users/justinmckelvy/Documents/DCSv2/";
        var suffix = "/Sample/v0_tests/tests/Patient";
        String patientBundles = directory + measureCode + suffix;
        String libraryName = measureCode + "_Reporting";
        singleRun(libraryName, patientBundles, testPatientId, 100000, measureCode);



        }
    void singleRun(String libraryName, String patientPath, String patientId, int count, String measureCode) throws IOException {

        var baseArgs = Stream.of(
            "cql",
            "-fv=R4",
            "-rd=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2",
            "-lu=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2/input/cql",
            "-ln=" + libraryName,
            "-lv=2024.2.0",
            "-m=FHIR",
            "-mu=" + patientPath + "/" + patientId,
            "-c=Patient",
            "-cv=" + patientId,
            "-resultsPath=/Users/justinmckelvy/Documents/DCSv2/_Results8/",
            "-singleFile=" + true,
//            "-measurePath=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2/input/resources/Measure/",
//            "-measure=" + measureCode + "-Reporting",
//            "-periodStart=2024-01-01",
//            "-periodEnd=2024-12-31",
            "-t=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2/input/vocabulary/valueset");

        //var patientArgs = allPatients(patientPath, count, patientId).flatMap(id -> Stream.of("-c=Patient", "-cv=" + id));

        //var args = Stream.concat(baseArgs, patientArgs).toArray(String[]::new);
        var args = baseArgs.toArray(String[]::new);
        Main.run(args);
    }

    Stream<String> allPatients(String dataPath, int count, String patientId) throws IOException {
        return Files.list(Path.of(dataPath, ""))
            .filter(Files::isDirectory)
            .limit(count)
            .map(Path::getFileName)
            .map(Path::toString)
            .filter(string -> string.equals(patientId));
    }
}
