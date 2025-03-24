package org.opencds.cqf.fhir.utility.npm;

import java.util.List;
import org.hl7.cql.model.NamespaceInfo;

public class NpmNamespaceManagerFromList implements NpmNamespaceManager {

    private final List<NamespaceInfo> namespaceInfos;

    public NpmNamespaceManagerFromList(List<NamespaceInfo> namespaceInfos) {
        this.namespaceInfos = namespaceInfos;
    }

    @Override
    public List<NamespaceInfo> getAllNamespaceInfos() {
        return namespaceInfos;
    }
}
