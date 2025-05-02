package org.opencds.cqf.fhir.cr.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.opencds.cqf.fhir.test.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    @Disabled("This test is failing on the CI Server for reasons unknown. Need to debug that.")
    void dqicIG() {
        String[] args = new String[] {
            "cql",
            //fhir version
            "-fv=R4",
            //root directory
            "-rd=/Users/justinmckelvy/alphora/ncqa-hedis-discovery",
            //igPath
            "-ig=/input/mycontentig.xml",
            //libraryUrl
            "-lu=/Users/justinmckelvy/alphora/ncqa-hedis-discovery/input/cql",
            //libraryName
            "-ln=LSCReporting",
            //libraryVersion
            "-lv=2024.0.0",
            //model
            "-m=FHIR",
            //model Url, Patient Bundle directory
            "-mu=/Users/justinmckelvy/alphora/dqic-ig/input/tests/measure/LSC_Reporting/95006",
            //terminologyUrl
            "-t=/Users/justinmckelvy/alphora/ncqa-hedis-discovery/input/vocabulary/valueset",
            //context
            "-c=Patient",
            //expressions
            "-e=Num",
            "-e=Epop",
            "-e=RExclD",
            //context value
            "-cv=95006"
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

    public ByteArrayOutputStream run(String patientResources, String patientId, String libraryName) {
        // Count of base args without -cv
        int baseArgCount = 12;

        // Total args = base + one -cv=... per patientId
        String[] args = new String[baseArgCount];

        // Base args
        args[0] = "cql";
        args[1] = "-fv=R4";
        args[2] = "-rd=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2";
        args[3] = "-ig=/input/mycontentig.xml";
        //args[4] = "-lu=/Users/justinmckelvy/alphora/ncqa-hedis-discovery/input/cql";
        args[4] = "-lu=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2/input/cql";
        args[5] = "-ln=" + libraryName;
        //args[6] = "-lv=2024.0.0";
        args[6] = "-lv=2024.2.0";
        args[7] = "-m=FHIR";
        args[8] = "-mu=" + patientResources;
        //args[9] = "-t=/Users/justinmckelvy/alphora/ncqa-hedis-discovery/input/vocabulary/valueset";
        args[9] = "-t=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2/input/vocabulary/valueset";
        args[10] = "-c=Patient";
        args[11] = "-cv=" + patientId;

        Main.run(args);
        return outContent;
    }
@Test
public void unbundleNDJSON() throws Exception {
       List<String> measureCodes = new ArrayList<>();
//    measureCodes.add("AAB");
//    measureCodes.add("AAP");
//    measureCodes.add("AMR");
//    measureCodes.add("BPD");
//    measureCodes.add("BCSE");
//    measureCodes.add("CBP");
//    measureCodes.add("COLE");
//    measureCodes.add("PBH");
    measureCodes.add("CCS");
    //LSC

    for (String measureCode : measureCodes) {
        String patientBundles = "/Users/justinmckelvy/Documents/DCSv2/" + measureCode + "/Sample/v0_tests/tests/Patient";
        // Data staging
        Path dirPath = Paths.get("/Users/justinmckelvy/Documents/DCSv2/" + measureCode + "/Sample/deck");
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

}

    @Test
    public void testParseBundlesAndWriteEachToOwnDirectory() throws Exception {

        //String patientBundles = "/Users/justinmckelvy/Documents/DCSv2/" + measureCode + "/Sample/v0_tests/tests/Patient";
        //"/Users/justinmckelvy/Documents/BulkImport_raw/AAB/AAB-Sample/test";
        String libraryName = "LSC_Reporting";
        // Data staging

//
//        List<String> subDirs = Files.list(output)
//            .filter(Files::isDirectory)
//            .map(p -> p.getFileName().toString())
//            .collect(Collectors.toList());
//
//        var patientIds = subDirs.stream().map(t -> t.split("/"))
//            .map(x -> x[x.length - 1])
//            .toList();
//
//        for (String patientId : patientIds) {
//            Path outputFile = Paths.get(
//                "/Users/justinmckelvy/Documents/DCSv2/_Results3/" + libraryName + "/" + patientId + ".txt");
//
//            // ✅ Skip if already written
//            if (Files.exists(outputFile)) {
//                System.out.println("⏭️ Skipping " + patientId + " (already processed)");
//                continue;
//            }
//
//            String patientPath = output + "/" + patientId;
//            ByteArrayOutputStream result = run(patientPath, patientId, libraryName);
//
//            // ✅ Ensure output directory exists
//            Files.createDirectories(outputFile.getParent());
//
//            // ✅ Write result
//            Files.write(outputFile, result.toByteArray());
//            System.out.println("✅ Processed " + patientId);
//            outContent.reset();
//        }
    }

    @Test
    public void testParseBundles() throws Exception {

        var measure_code = "COLE";


        String patientBundles = "/Users/justinmckelvy/Documents/DCSv2-Certification/" + measure_code + "/A/Patients-v0";
        //"/Users/justinmckelvy/Documents/BulkImport_raw/AAB/AAB-Sample/test";
        String libraryName = measure_code + "_Reporting";
        // Data staging

        Path dirPath = Paths.get("/Users/justinmckelvy/Documents/DCSv2-Certification/" + measure_code + "/A/deck"); // ← replace with your directory
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
        String patientBundles = "/Users/justinmckelvy/Documents/DCSv2/AMR/Sample/v1_tests/tests/Patient";
        //"/Users/justinmckelvy/Documents/BulkImport_raw/AAB/AAB-Sample/test";
        String libraryName = "AMR_Reporting";
        // Data staging

//        Path dirPath = Paths.get("/Users/justinmckelvy/Documents/DCSv2/LSC/Sample/deck"); // ← replace with your directory
        Path output = Paths.get(patientBundles);
//        try (Stream<Path> paths = Files.list(dirPath)) {
//            paths
//                .filter(Files::isRegularFile) // skip subdirectories
//                .forEach(path -> {
//                    Path fullPath = path.toAbsolutePath();
//                    try {
//                        NdjsonBundleExtractor.extractBundlesToDirs(fullPath, output);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//        }
        var testPatientId = "patient.2024.amr.0.101314";

        Path root = Paths.get(patientBundles + "");

        List<String> patientIds = Files.list(root)
            .filter(Files::isDirectory)
            .map(path -> path.getFileName().toString())
            .collect(Collectors.toList());

        for (String patientId : patientIds) {
            if(patientId.equals(testPatientId)) {
                Path outputFile = Paths.get(
                    "/Users/justinmckelvy/Documents/DCSv2/_Results7/" + libraryName + "/"
                        + patientId + "v15.txt");

                // ✅ Skip if already written
                if (Files.exists(outputFile)) {
                    System.out.println("⏭️ Skipping " + patientId + " (already processed)");
                    continue;
                }

                String patientPath = output + "/" + patientId;
                ByteArrayOutputStream result = run(patientPath, patientId, libraryName);

                // ✅ Ensure output directory exists
                Files.createDirectories(outputFile.getParent());

                // ✅ Write result
                Files.write(outputFile, result.toByteArray());
                System.out.println("✅ Processed " + patientId);
                outContent.reset();
            }
        }
    }
}
