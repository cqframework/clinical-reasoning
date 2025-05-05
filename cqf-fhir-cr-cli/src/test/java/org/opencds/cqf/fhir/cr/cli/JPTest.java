package org.opencds.cqf.fhir.cr.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class JPTest {

    @Test
    void hedis_AAB() throws IOException {
        List<String> measureCodes = new ArrayList<>();
//        measureCodes.add("AAB"); // other team
//        measureCodes.add("AAP");
//        measureCodes.add("AMR"); // other team
//        measureCodes.add("BPD");
//        measureCodes.add("BCSE"); done
//        measureCodes.add("CBP");
//        measureCodes.add("CCS"); done
//        measureCodes.add("COLE"); done
        measureCodes.add("LSC");
//        measureCodes.add("PBH");
        //LSC

        for (String measureCode : measureCodes) {
            run(measureCode + "_Reporting",
                "/Users/justinmckelvy/Documents/DCSv2/" + measureCode + "/Sample/v2_tests", 100000);
        }
    }

//    @Test
//    void hedis_LSC() throws IOException {
//        run("LSC_Reporting", "/Users/justinmckelvy/Documents/DCSv2/LSC/Sample/v2_tests", 100000);
//    }

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
