<<<<<<< HEAD:evaluator.translation/src/main/java/org/opencds/cqf/cql/evaluator/translation/provider/InMemoryLibrarySourceProvider.java
package org.opencds.cqf.cql.evaluator.translation.provider;
=======
package org.opencds.cqf.cql.evaluator.provider;
>>>>>>> 69178e5... Updates to evaluator:evaluator/src/main/java/org/opencds/cqf/cql/evaluator/provider/InMemoryLibrarySourceProvider.java

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;

public class InMemoryLibrarySourceProvider implements LibrarySourceProvider {

    private List<String> libraries;

    public InMemoryLibrarySourceProvider(List<String> libraries) {
        this.libraries = libraries;
    }

    @Override
    public InputStream getLibrarySource(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier) {
        String id = libraryIdentifier.getId();
        String version = libraryIdentifier.getVersion();

        String matchText = "(?s).*library\\s+" + id;
        if (version != null) {
            matchText += ("\\s+version\\s+'" + version + "'\\s+(?s).*");
        }
        else {
            matchText += "\\s+(?s).*";
        }

        for(String library : this.libraries){

            if(library.matches(matchText)){
                return new ByteArrayInputStream(library.getBytes());
            }
        }

        return null;
    }
}