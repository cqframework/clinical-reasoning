package org.opencds.cqf.fhir.utility.npm;

import java.util.List;
import org.hl7.cql.model.NamespaceInfo;

/**
 * Load all {@link NamespaceInfo}s capturing package ID to URL mappings associated with the NPM
 * packages maintained for clinical-reasoning NPM package users to be used to resolve cross-package
 * Library/CQL dependencies.  See {@link R4NpmPackageLoader}.
 */
public interface NpmNamespaceManager {
    List<NamespaceInfo> getAllNamespaceInfos();
}
