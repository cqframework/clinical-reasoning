package org.opencds.cqf.cql.evaluator.engine.execution;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.hl7.cql.model.NamespaceInfo;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

import java.util.List;

/** This interface extends LibraryLoader to expose the set of translator options the
 * LibraryLoader is currently configured with.
 */
public interface TranslatorOptionAwareLibraryLoader extends LibraryLoader {
    public CqlTranslatorOptions getCqlTranslatorOptions();
    public void loadNamespaces(List<NamespaceInfo> namespaceInfos);
}
