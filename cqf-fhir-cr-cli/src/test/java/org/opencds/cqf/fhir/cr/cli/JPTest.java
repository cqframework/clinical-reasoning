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
        this.run("AABReporting", "/Users/jp/hedis", 500);
    }

    public void run(String libraryName, String patientPath, int count) {
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

        var patientArgs = idsToArgs(allPatients(patientPath, count));

        args.addAll(patientArgs);
        Main.run(args.toArray(String[]::new));
    }

    private List<String> idsToArgs(List<String> patientIds) {
        return patientIds.stream()
                .flatMap(id -> List.of("-c=Patient", "-cv=" + id).stream())
                .toList();
    }

    private List<String> allPatients(String dataPath, int count) {
        return Arrays.stream(Path.of(dataPath).resolve("tests/Patient").toFile().listFiles())
                .limit(count)
                .filter(file -> file.isDirectory())
                .map(File::getName)
                .toList();
    }
}
