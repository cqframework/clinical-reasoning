package org.opencds.cqf.cql.evaluator.cli.version;

import picocli.CommandLine.IVersionProvider;

public class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        String version = VersionProvider.class.getPackage().getImplementationVersion();
        String[] values = {
            "cql-evaluator cli version: " + version, 
            "Copyright 2019+ Dynamic Content Group, LLC (dba Alphora)",
            "Apache License Version 2.0 <http://www.apache.org/licenses/>",
            "There is NO WARRANTY, to the extent permitted by law."
                    };

        return values;
    }
    
}
