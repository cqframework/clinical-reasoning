package org.opencds.cqf.cql.evaluator.engine.execution;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.NamespaceInfo;

/**
 * The CacheAwareLibraryLoaderDecorator wraps a TranslatorOptionAwareLibraryLoader with a cache
 * layer. The cache can either be provided by the user (e.g. a global cache of libraries) or if one
 * is not provided it will be created by the CacheAwareLibraryLoaderDecorator. The cached Library is
 * checked to see if has been translated with the correct options before being returned. If it has
 * been translated with a different set of options the cache is invalidated and the Library is
 * loaded from the inner LibraryLoader.
 */
public class CacheAwareLibraryLoaderDecorator implements TranslatorOptionAwareLibraryLoader {

  private final TranslatorOptionAwareLibraryLoader innerLoader;

  private final Map<VersionedIdentifier, Library> libraryCache;

  public CacheAwareLibraryLoaderDecorator(TranslatorOptionAwareLibraryLoader libraryLoader,
      Map<VersionedIdentifier, Library> libraryCache) {
    this.innerLoader = libraryLoader;
    if (libraryCache == null) {
      this.libraryCache = new HashMap<>();
    } else {
      this.libraryCache = libraryCache;
    }
  }

  public CacheAwareLibraryLoaderDecorator(TranslatorOptionAwareLibraryLoader libraryLoader) {
    this(libraryLoader, null);
  }

  @Override
  public Library load(VersionedIdentifier libraryIdentifier) {
    Library library = this.libraryCache.get(libraryIdentifier);
    if (library != null && this.translatorOptionsMatch(library)) { // Bug on xml libraries not
      // getting annotations
      return library;
    }

    library = this.innerLoader.load(libraryIdentifier);
    if (library == null) {
      return null;
    }
    this.libraryCache.put(libraryIdentifier, library);

    return library;
  }

  @Override
  public CqlTranslatorOptions getCqlTranslatorOptions() {
    return this.innerLoader.getCqlTranslatorOptions();
  }

  public Map<VersionedIdentifier, Library> getLibraryCache() {
    return this.libraryCache;
  }

  public void loadNamespaces(List<NamespaceInfo> namespaceInfos) {
    this.innerLoader.loadNamespaces(namespaceInfos);
  }

  @Override
  public boolean translatorOptionsMatch(Library library) {
    return this.innerLoader.translatorOptionsMatch(library);
  }
}
