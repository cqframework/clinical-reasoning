package org.opencds.cqf.cql.evaluator.engine.execution;

import java.util.List;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.cql.model.NamespaceInfo;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;

/**
 * This interface extends LibraryLoader to expose the set of translator options the LibraryLoader is
 * currently configured with.
 */
public interface TranslatorOptionAwareLibraryLoader extends LibraryLoader {
  public CqlTranslatorOptions getCqlTranslatorOptions();

  public void loadNamespaces(List<NamespaceInfo> namespaceInfos);

  public boolean translatorOptionsMatch(Library library);
}
