package org.opencds.cqf.fhir.cr.cli.version;

import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        String version = VersionProvider.class.getPackage().getImplementationVersion();
        return new String[] {
            "cqf-fhir-cr-cli version: " + version,
            "Copyright 2019+ Smile Digital Health",
            "Apache License Version 2.0 <http://www.apache.org/licenses/>",
            "There is NO WARRANTY, to the extent permitted by law."
        };
    }
}
