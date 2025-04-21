package org.opencds.cqf.fhir.cr.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class JPTest {

    @Test
    void hedis() {
        run("AABReporting", "/Users/jp/hedis", 500);
    }

    void run(String libraryName, String patientPath, int count) {
        var baseArgs = List.of(
                "cql",
                "-fv=R4",
                "-rd=/Users/jp/repos/ncqa-hedis-discovery",
                "-lu=/Users/jp/repos/ncqa-hedis-discovery/input/cql",
                "-ln=" + libraryName,
                "-lv=2024.0.0",
                "-m=FHIR",
                "-mu=" + patientPath,
                "-t=/Users/jp/repos/ncqa-hedis-discovery/input/vocabulary/valueset");

        var patientArgs = allPatients(patientPath, count).stream()
                .flatMap(id -> Stream.of("-c=Patient", "-cv=" + id))
                .toList();

        var args = Stream.concat(baseArgs.stream(), patientArgs.stream()).toArray(String[]::new);
        Main.run(args);
    }

    List<String> allPatients(String dataPath, int count) {
        return Stream.of(Path.of(dataPath, "tests/Patient").toFile().listFiles(File::isDirectory))
                .limit(count)
                .map(File::getName)
                .toList();
    }
}
