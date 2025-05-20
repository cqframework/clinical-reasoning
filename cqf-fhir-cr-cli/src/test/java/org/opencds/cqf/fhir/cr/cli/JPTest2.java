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
    void hedis_CERT_Deck() throws IOException {
        List<String> measureCodes = new ArrayList<>();
//        measureCodes.add("AAB"); // results partially processed with old FHIRCommon "has"
//        measureCodes.add("AAP");
//        measureCodes.add("AMR");
//        measureCodes.add("BPD");
//        measureCodes.add("BCSE");
//        measureCodes.add("CBP");
//        measureCodes.add("CCS");
//        measureCodes.add("COLE");
        measureCodes.add("LSC");
//        measureCodes.add("PBH");
        //LSC

        for (String measureCode : measureCodes) {
            run(measureCode + "_Reporting",
                "/Users/justinmckelvy/Documents/DCSv2-Certification/LSC/A/Patients-v0", 100000);
        }
    }

    @Test
    void hedis_DCSv2TestDeck() throws IOException {
        List<String> measureCodes = new ArrayList<>();
        measureCodes.add("FMC");


        for (String measureCode : measureCodes) {
            run(measureCode + "_Reporting",
                "/Users/justinmckelvy/Documents/DCSv2/" + measureCode + "/Sample/v0_tests", 100000);
        }
    }

    @Test
    void hedis_DCSv2TestDeck2() throws IOException {
        List<String> measureCodes = new ArrayList<>();

//        measureCodes.add("BPD");
        measureCodes.add("EDH");
//        measureCodes.add("LDM");
//        measureCodes.add("CRE");
//        measureCodes.add("ASFE");
//        measureCodes.add("AXR");
//        measureCodes.add("COU");


        for (String measureCode : measureCodes) {
            run(measureCode + "_Reporting",
                "/Users/justinmckelvy/Documents/DCSv2/" + measureCode + "/Sample/v0_tests", 100000);
        }
    }


    void run(String libraryName, String patientPath, int count) throws IOException {
        var baseArgs = Stream.of(
                "cql",
                "-fv=R4",
                "-rd=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2",
                "-lu=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2/input/cql",
                "-ln=" + libraryName,
                "-lv=2024.2.0",
                "-m=FHIR",
                "-mu=" + patientPath,
                "-resultsPath=/Users/justinmckelvy/Documents/DCSv2/_Results7/",
                "-singleFile=" + true,
                "-t=/Users/justinmckelvy/alphora/DCS-HEDIS-2024-v2/input/vocabulary/valueset");

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
