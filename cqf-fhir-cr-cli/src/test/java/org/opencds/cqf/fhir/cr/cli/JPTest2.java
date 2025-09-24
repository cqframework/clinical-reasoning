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
//        measureCodes.add("APME");
//        measureCodes.add("BPD");
//        measureCodes.add("CBP");
//        measureCodes.add("COLE");
//        measureCodes.add("CRE");
        measureCodes.add("DMH");
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2-Cert-A/"; //where your test deck data is
        var testingPath = "/A/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results/"; // the path where results will save
        var measurePath = cqlContentDirectory + "/src/Measure/";

        var periodStart = "2024-01-01";
        var periodEnd = "2024-12-31";

        for (String measure : measureCodes) {
            var measureId = measure + "Reporting";

            run(measureId,
                    patientPath + measure + testingPath, 200000, cqlContentDirectory, resultsPath, measurePath, measureId, periodStart, periodEnd);
        }
    }

    @Test
    void hedis_DCSv2CertDeck2() throws IOException {
        List<String> measureCodes = new ArrayList<>();
//        measureCodes.add("DAE");
//        measureCodes.add("DDE");
//        measureCodes.add("DSU");
//        measureCodes.add("EED");
//        measureCodes.add("FMC");
        measureCodes.add("APP");
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2-Cert-A/"; //where your test deck data is
        var testingPath = "/A/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results/"; // the path where results will save
        var measurePath = cqlContentDirectory + "/src/Measure/";

        var periodStart = "2024-01-01";
        var periodEnd = "2024-12-31";

        for (String measure : measureCodes) {
            var measureId = measure + "Reporting";

            run(measureId,
                patientPath + measure + testingPath, 200000, cqlContentDirectory, resultsPath, measurePath, measureId, periodStart, periodEnd);
        }
    }

    @Test
    void hedis_DCSv2CertDeck3() throws IOException {
        List<String> measureCodes = new ArrayList<>();
//        measureCodes.add("FUM");
//        measureCodes.add("KED");
//        measureCodes.add("OMW");
//        measureCodes.add("OSW");
//        measureCodes.add("PBH");
        measureCodes.add("AHU");
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2-Cert-A/"; //where your test deck data is
        var testingPath = "/A/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results/"; // the path where results will save
        var measurePath = cqlContentDirectory + "/src/Measure/";

        var periodStart = "2024-01-01";
        var periodEnd = "2024-12-31";

        for (String measure : measureCodes) {
            var measureId = measure + "Reporting";

            run(measureId,
                patientPath + measure + testingPath, 200000, cqlContentDirectory, resultsPath, measurePath, measureId, periodStart, periodEnd);
        }
    }

    @Test
    void hedis_DCSv2CertDeck4() throws IOException {
        List<String> measureCodes = new ArrayList<>();
//        measureCodes.add("SMC");
//        measureCodes.add("SMD");
//        measureCodes.add("SSD");
//        measureCodes.add("URI");
//        measureCodes.add("W30");
//        measureCodes.add("WCC");
        measureCodes.add("WCV");
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2-Cert-A/"; //where your test deck data is
        var testingPath = "/A/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results/"; // the path where results will save
        var measurePath = cqlContentDirectory + "/src/Measure/";

        var periodStart = "2024-01-01";
        var periodEnd = "2024-12-31";

        for (String measure : measureCodes) {
            var measureId = measure + "Reporting";

            run(measureId,
                patientPath + measure + testingPath, 200000, cqlContentDirectory, resultsPath, measurePath, measureId, periodStart, periodEnd);
        }
    }

    @Test
    void hedis_DCSv2TestDeck() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2/"; //where your test deck data is
        var testingPath = "/Sample/v0_tests"; //test deck data path suffix
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2"; // the CQL that will be used for test
        var resultsPath = patientPath + "_Results/"; // the path where results will save
        measureCodes.add("PSA"); //measure(s) to evaluate, add more if wanting to test multiple


        for (String measureCode : measureCodes) {
            run(measureCode + "Reporting",
                patientPath + measureCode + testingPath, 250000, cqlContentDirectory, resultsPath, null, null, null, null);
        }
    }

    @Test
    void hedis_DCSv2TestDeck2() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2/";
        var testingPath = "/Sample/v0_tests";
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2";
        var resultsPath = patientPath + "_Results/";
        measureCodes.add("LDM");

        for (String measureCode : measureCodes) {
            run(measureCode + "Reporting",
                patientPath + measureCode + testingPath, 250000, cqlContentDirectory, resultsPath, null, null, null, null);
        }
    }

    @Test
    void hedis_DCSv2TestDeck3() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        var patientPath = "/Users/justinmckelvy/Documents/DCSv2/";
        var testingPath = "/Sample/v0_tests";
        var cqlContentDirectory = "/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2";
        var resultsPath = patientPath + "_Results/";
        measureCodes.add("SPD");

        for (String measureCode : measureCodes) {
            run(measureCode + "Reporting",
                patientPath + measureCode + testingPath, 250000, cqlContentDirectory, resultsPath, null, null, null, null);
        }
    }


    void run(String libraryName, String patientPath, int count, String cqlContentDirectory, String resultsPath, String measurePath, String measure, String periodStart, String periodEnd) throws IOException {
        var baseArgs = Stream.of(
                "cql",
                "-fv=R4",
                "-rd=" + cqlContentDirectory,
                "-lu=" + cqlContentDirectory + "/src/cql",
                "-ln=" + libraryName,
                "-lv=2024.2.0",
                "-m=FHIR",
                "-mu=" + patientPath,
                "-resultsPath=" + resultsPath,
                "-singleFile=" + true,
                "-t=" + cqlContentDirectory + "/src/valueset",
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
