package org.opencds.cqf.fhir.cr.cli;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JPTest {

    protected static final Logger ourLog = LoggerFactory.getLogger(JPTest.class);

    @Test
    public void hedis() {
        this.run(
                "/Users/jp/repos/hedis",
                List.of(
                        "patient.2024.aab.0.95115",
                        "patient.2024.aab.0.95116",
                        "patient.2024.aab.0.95117",
                        "patient.2024.aab.0.95118",
                        "patient.2024.aab.0.95119",
                        "patient.2024.aab.0.95120",
                        "patient.2024.aab.0.95121",
                        "patient.2024.aab.0.95122",
                        "patient.2024.aab.0.95123",
                        "patient.2024.aab.0.95124",
                        "patient.2024.aab.0.95125",
                        "patient.2024.aab.0.95127",
                        "patient.2024.aab.0.95128"),
                "AABReporting");
    }

    public void run(String patientResources, List<String> patientIds, String libraryName) {
        var args = new ArrayList<>(List.of(
                "cql",
                "-fv=R4",
                "-rd=/Users/jp/repos/ncqa-hedis-discovery",
                "-lu=/Users/jp/repos/ncqa-hedis-discovery/input/cql",
                "-ln=" + libraryName,
                "-lv=2024.0.0",
                "-m=FHIR",
                "-mu=" + patientResources,
                "-t=/Users/jp/repos/ncqa-hedis-discovery/input/vocabulary/valueset"));

        for (String patientId : patientIds) {
            args.add("-c=Patient");
            args.add("-cv=" + patientId);
        }

        Main.run(args.toArray(String[]::new));
    }
}
