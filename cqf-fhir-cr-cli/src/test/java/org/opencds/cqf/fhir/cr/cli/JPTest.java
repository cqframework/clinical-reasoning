package org.opencds.cqf.fhir.cr.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class JPTest {

    @Test
    void hedis() throws IOException {
        run("AABReporting", "/Users/jp/hedis", 500);
    }

    void run(String libraryName, String patientPath, int count) throws IOException {
        var baseArgs = Stream.of(
                "cql",
                "-fv=R4",
                "-rd=/Users/jp/repos/ncqa-hedis-discovery",
                "-lu=/Users/jp/repos/ncqa-hedis-discovery/input/cql",
                "-ln=" + libraryName,
                "-lv=2024.0.0",
                "-m=FHIR",
                "-mu=" + patientPath,
                "-t=/Users/jp/repos/ncqa-hedis-discovery/input/vocabulary/valueset");

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
