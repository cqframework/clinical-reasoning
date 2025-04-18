package org.opencds.cqf.fhir.cr.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class JPTest {
    @Test
    public void hedis() {
        this.run(
                "AABReporting", "/Users/jp/hedis", List.of()
                // List.of(
                //         "patient.2024.aab.0.95115",
                //         "patient.2024.aab.0.95116",
                //         "patient.2024.aab.0.95117",
                //         "patient.2024.aab.0.95118",
                //         "patient.2024.aab.0.95119",
                //         "patient.2024.aab.0.95120",
                //         "patient.2024.aab.0.95121",
                //         "patient.2024.aab.0.95122",
                //         "patient.2024.aab.0.95123",
                //         "patient.2024.aab.0.95124",
                //         "patient.2024.aab.0.95125",
                //         "patient.2024.aab.0.95127",
                //         "patient.2024.aab.0.95128"),
                );
    }

    public void run(String libraryName, String patientPath, List<String> patientIds) {
        var args = new ArrayList<>(List.of(
                "cql",
                "-fv=R4",
                "-rd=/Users/jp/repos/ncqa-hedis-discovery",
                "-lu=/Users/jp/repos/ncqa-hedis-discovery/input/cql",
                "-ln=" + libraryName,
                "-lv=2024.0.0",
                "-m=FHIR",
                "-mu=" + patientPath,
                "-t=/Users/jp/repos/ncqa-hedis-discovery/input/vocabulary/valueset"));

        var patientArgs = idsToArgs(!patientIds.isEmpty() ? patientIds : allPatients(patientPath));

        args.addAll(patientArgs);
        Main.run(args.toArray(String[]::new));
    }

    private List<String> idsToArgs(List<String> patientIds) {
        return patientIds.stream()
                .flatMap(id -> List.of("-c=Patient", "-cv=" + id).stream())
                .toList();
    }

    private List<String> allPatients(String dataPath) {
        return Arrays.stream(Path.of(dataPath).resolve("tests/Patient").toFile().listFiles())
                .filter(file -> file.isDirectory())
                .map(File::getName)
                .toList();
    }
}
