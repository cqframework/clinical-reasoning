package org.opencds.cqf.fhir.utility.npm;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.hl7.cql.model.NamespaceInfo;

// LUKETODO: javadoc
public class NpmNamespaceManagerFromList implements NpmNamespaceManager {

    private final List<NamespaceInfo> namespaceInfos;

    public NpmNamespaceManagerFromList(List<NamespaceInfo> namespaceInfos) {
        this.namespaceInfos = List.copyOf(namespaceInfos);
    }

    @Override
    public List<NamespaceInfo> getAllNamespaceInfos() {
        return namespaceInfos;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NpmNamespaceManagerFromList that = (NpmNamespaceManagerFromList) o;
        return Objects.equals(namespaceInfos, that.namespaceInfos);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(namespaceInfos);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NpmNamespaceManagerFromList.class.getSimpleName() + "[", "]")
                .add("namespaceInfos=" + namespaceInfos)
                .toString();
    }
}
