package org.opencds.cqf.fhir.cr.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class JPTest2 {
    @Test
    void hedis_DCSv2CertDeck() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Volumes/ExternalDrive/DCSv2-Certification/"; //where your test deck data is
        var testingPath = "/A/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results2/"; // the path where results will save
        var measurePath = cqlContentDirectory + "/input/resources/Measure/";
        var measureCode = "CCS";
        var measureId = measureCode + "-Reporting";
        var periodStart = "2024-01-01";
        var periodEnd = "2024-12-31";
        measureCodes.add(measureCode); //measure(s) to evaluate, add more if wanting to test multiple


        for (String measure : measureCodes) {
            run(measureCode + "_Reporting",
                patientPath + measure + testingPath, 100000, cqlContentDirectory, resultsPath, measurePath, measureId, periodStart, periodEnd);
        }
    }

    @Test
    void hedis_DCSv2CertDeck2() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Volumes/ExternalDrive/DCSv2-Certification/"; //where your test deck data is
        var testingPath = "/A/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results2/"; // the path where results will save
        var measurePath = cqlContentDirectory + "/input/resources/Measure/";
        var measureCode = "CCSE";
        var measureId = measureCode + "-Reporting";
        var periodStart = "2024-01-01";
        var periodEnd = "2024-12-31";
        measureCodes.add(measureCode); //measure(s) to evaluate, add more if wanting to test multiple


        for (String measure : measureCodes) {
            run(measureCode + "_Reporting",
                patientPath + measure + testingPath, 100000, cqlContentDirectory, resultsPath, measurePath, measureId, periodStart, periodEnd);
        }
    }

    @Test
    void hedis_DCSv2TestDeck() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2/"; //where your test deck data is
        var testingPath = "/Sample/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results7/"; // the path where results will save
        measureCodes.add("LBP"); //measure(s) to evaluate, add more if wanting to test multiple


        for (String measureCode : measureCodes) {
            run(measureCode + "_Reporting",
                patientPath + measureCode + testingPath, 250000, cqlContentDirectory, resultsPath, null, null, null, null);
        }
    }

    @Test
    void hedis_DCSv2TestDeck2() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2/";
        var testingPath = "/Sample/v0_tests";
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2";
        var resultsPath = patientPath + "_Results7/";
        measureCodes.add("IMAE");

        for (String measureCode : measureCodes) {
            run(measureCode + "_Reporting",
                patientPath + measureCode + testingPath, 250000, cqlContentDirectory, resultsPath, null, null, null, null);
        }
    }

    @Test
    void hedis_DCSv2TestDeck3() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2/";
        var testingPath = "/Sample/v0_tests";
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2";
        var resultsPath = patientPath + "_Results7/";
        measureCodes.add("HFS");

        for (String measureCode : measureCodes) {
            run(measureCode + "_Reporting",
                patientPath + measureCode + testingPath, 250000, cqlContentDirectory, resultsPath, null, null, null, null);
        }
    }


    void run(String libraryName, String patientPath, int count, String cqlContentDirectory, String resultsPath, String measurePath, String measure, String periodStart, String periodEnd) throws IOException {
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
            "-periodEnd=" + periodEnd
        );

        var patientArgs = allPatients(patientPath, count).flatMap(id -> Stream.of("-c=Patient", "-cv=" + id));

        var args = Stream.concat(baseArgs, patientArgs).toArray(String[]::new);
        Main.run(args);
    }

    Stream<String> allPatients(String dataPath, int count) throws IOException {
        return Files.list(Path.of(dataPath, "tests/Patient"))
                .filter(Files::isDirectory)
                .limit(count)
                .map(Path::getFileName)
                .map(Path::toString);
    }
}
