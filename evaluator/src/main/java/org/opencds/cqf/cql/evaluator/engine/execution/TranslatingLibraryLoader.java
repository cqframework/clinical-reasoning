package org.opencds.cqf.cql.evaluator.engine.execution;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.converter.VersionedIdentifierConverter.toElmIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlCompilerException.ErrorSeverity;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.LibraryBuilder;
import org.cqframework.cql.cql2elm.LibraryBuilder.SignatureLevel;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionDef;
import org.cqframework.cql.elm.execution.FunctionRef;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.serializing.CqlLibraryReaderFactory;
import org.opencds.cqf.cql.evaluator.engine.elm.LibraryMapper;
import org.opencds.cqf.cql.evaluator.engine.util.TranslatorOptionsUtil;


/**
 * The TranslatingLibraryLoader attempts to load a library from a set of LibrarySourceProviders. If
 * pre-existing ELM is found for the requested library and the ELM was generated using the same set
 * of translator options as is provided to the TranslatingLibraryLoader, it will use that ELM. If
 * the ELM is not found, or the ELM translation options do not match, the TranslatingLibraryLoader
 * will attempt to regenerate the ELM by translating CQL content with the requested options. If
 * neither matching ELM content nor CQL content is found for the requested Library, null is
 * returned.
 */
public class TranslatingLibraryLoader implements TranslatorOptionAwareLibraryLoader {

  protected CqlTranslatorOptions cqlTranslatorOptions;
  protected List<LibrarySourceProvider> librarySourceProviders;

  protected LibraryManager libraryManager;

  private static EnumSet<LibraryBuilder.SignatureLevel> OVERLOAD_SAFE_SIGNATURE_LEVELS =
      EnumSet.of(SignatureLevel.All, SignatureLevel.Overloads);

  private final EnumSet<CqlTranslatorOptions.Options> binaryOptionSet;

  public TranslatingLibraryLoader(ModelManager modelManager,
      List<LibrarySourceProvider> librarySourceProviders, CqlTranslatorOptions translatorOptions,
      NamespaceInfo namespaceInfo) {
    this.librarySourceProviders =
        requireNonNull(librarySourceProviders, "librarySourceProviders can not be null");

    this.cqlTranslatorOptions =
        translatorOptions != null ? translatorOptions : CqlTranslatorOptions.defaultOptions();

    if (namespaceInfo != null) {
      modelManager.getNamespaceManager().addNamespace(namespaceInfo);
    }

    this.libraryManager = new LibraryManager(modelManager);

    // TODO: Dual caching here between this layer and the LibraryManager.
    // The LibraryManager only allows loading one version at a time.
    // So disable that cache. That impacts compilation speed.
    // But since compilation is most likely a one-time expense, this
    // produces better performance overall.
    this.libraryManager.disableCache();
    for (LibrarySourceProvider provider : librarySourceProviders) {
      libraryManager.getLibrarySourceLoader().registerProvider(provider);
    }

    if (!OVERLOAD_SAFE_SIGNATURE_LEVELS.contains(this.cqlTranslatorOptions.getSignatureLevel())) {
      throw new IllegalArgumentException(
          "TranslatingLibraryLoader requires an overload-safe SignatureLevel: {All, Overloads}");
    }

    this.binaryOptionSet = this.cqlTranslatorOptions.getOptions().clone();
    binaryOptionSet.removeAll(TranslatorOptionsUtil.OPTIONAL_ENUM_SET);
  }

  public void loadNamespaces(List<NamespaceInfo> namespaceInfos) {
    for (NamespaceInfo ni : namespaceInfos) {
      libraryManager.getNamespaceManager().addNamespace(ni);
    }
  }

  public Library load(VersionedIdentifier libraryIdentifier) {
    ensureNamespaceUpdate(libraryIdentifier);
    if (this.cqlTranslatorOptions.getEnableCqlOnly()) {
      return this.translate(libraryIdentifier);
    }

    Library library = this.getLibraryFromElm(libraryIdentifier);
    if (checkBinaryCompatibility(library)) {
      return library;
    }

    return this.translate(libraryIdentifier);
  }

  private void ensureNamespaceUpdate(VersionedIdentifier libraryIdentifier) {
    // Need to ensure namespaces are preserved when recompiling
    if (libraryIdentifier.getSystem() != null && !libraryIdentifier.getSystem().isEmpty()
        && libraryManager.getNamespaceManager()
            .getNamespaceInfoFromUri(libraryIdentifier.getSystem()) == null) {
      libraryManager.getNamespaceManager().addNamespace(
          new NamespaceInfo(libraryIdentifier.getId(), libraryIdentifier.getSystem()));
    }
  }

  private boolean checkBinaryCompatibility(Library library) {
    if (library == null) {
      return false;
    }

    return this.isSignatureCompatible(library)
        && this.isVersionCompatible(library)
        && this.translatorOptionsMatch(library);
  }

  private boolean isSignatureCompatible(Library library) {
    return !hasOverloadedFunctions(library) || hasSignature(library);
  }

  @Override
  public CqlTranslatorOptions getCqlTranslatorOptions() {
    return this.cqlTranslatorOptions;
  }

  protected Library getLibraryFromElm(VersionedIdentifier libraryIdentifier) {
    org.hl7.elm.r1.VersionedIdentifier versionedIdentifier = toElmIdentifier(libraryIdentifier);
    for (var type : new LibraryContentType[] {LibraryContentType.JSON, LibraryContentType.XML}) {
      InputStream is = this.getLibraryContent(versionedIdentifier, type);
      if (is != null) {
        try {
          return CqlLibraryReaderFactory.getReader(type.mimeType()).read(is);
        } catch (IOException e) {
          return null;
        }
      }
    }

    return null;
  }

  public boolean translatorOptionsMatch(Library library) {
    EnumSet<CqlTranslatorOptions.Options> options =
        TranslatorOptionsUtil.getTranslatorOptions(library, true);
    if (options == null) {
      return false;
    }

    return options.equals(this.binaryOptionSet);
  }

  protected InputStream getLibraryContent(org.hl7.elm.r1.VersionedIdentifier libraryIdentifier,
      LibraryContentType libraryContentType) {
    for (LibrarySourceProvider librarySourceProvider : librarySourceProviders) {
      InputStream content =
          librarySourceProvider.getLibraryContent(libraryIdentifier, libraryContentType);
      if (content != null) {
        return content;
      }
    }

    return null;
  }

  protected Library translate(VersionedIdentifier libraryIdentifier) {
    CompiledLibrary library;
    List<CqlCompilerException> errors = new ArrayList<>();

    // TODO: Huh. Big ole issue here. Need to update the LibraryManager to
    // to be able to have all the same tests for binary compatibility as the
    // translating library loader. In the meantime, fake it and tell it
    // to only use CQL when we resolve a library. And then reset
    // to our default state.
    boolean enableCql = this.cqlTranslatorOptions.getEnableCqlOnly();
    try {
      this.cqlTranslatorOptions.setEnableCqlOnly(true);
      library = this.libraryManager.resolveLibrary(toElmIdentifier(libraryIdentifier),
          this.cqlTranslatorOptions, errors);
    } catch (Exception e) {
      throw new CqlException(String.format("Unable to resolve library (%s): %s",
          libraryIdentifier.getId(), e.getMessage()), e);
    } finally {
      this.cqlTranslatorOptions.setEnableCqlOnly(enableCql);
    }

    if (!errors.isEmpty()) {
      for (CqlCompilerException e : errors) {
        if (e.getSeverity() == ErrorSeverity.Error) {
          throw new CqlException(
              String.format("Translation of library %s failed with the following message: %s",
                  libraryIdentifier.getId(), e.getMessage()));
        }
      }
    }

    try {
      return LibraryMapper.INSTANCE.map(library.getLibrary());
    } catch (Exception e) {
      throw new CqlException(
          String.format("Mapping of library %s failed", libraryIdentifier.getId()), e);
    }
  }

  private boolean isVersionCompatible(Library library) {
    if (!StringUtils.isEmpty(cqlTranslatorOptions.getCompatibilityLevel())) {
      if (library.getAnnotation() != null) {
        String version = TranslatorOptionsUtil.getTranslationVersion(library);
        if (version != null) {
          return version.equals(cqlTranslatorOptions.getCompatibilityLevel());
        }
      }
    }

    return false;
  }

  private boolean hasOverloadedFunctions(Library library) {
    if (library == null || library.getStatements() == null) {
      return false;
    }

    Set<FunctionSig> functionNames = new HashSet<>();
    for (ExpressionDef ed : library.getStatements().getDef()) {
      if (ed instanceof FunctionDef) {
        FunctionDef fd = (FunctionDef) ed;
        var sig = new FunctionSig(fd.getName(),
            fd.getOperand() == null ? 0 : fd.getOperand().size());
        if (functionNames.contains(sig)) {
          return true;
        } else {
          functionNames.add(sig);
        }
      }
    }
    return false;
  }

  boolean hasSignature(Library library) {
    if (library != null && library.getStatements() != null) {
      // Just a quick top-level scan for signatures. To fully verify we'd have to recurse all
      // the way down. At that point, let's just translate.
      for (ExpressionDef ed : library.getStatements().getDef()) {
        if (ed.getExpression() instanceof FunctionRef) {
          FunctionRef fr = (FunctionRef) ed.getExpression();
          if (fr.getSignature() != null && !fr.getSignature().isEmpty()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  static class FunctionSig {

    private final String name;
    private final int numArguments;

    public FunctionSig(String name, int numArguments) {
      this.name = name;
      this.numArguments = numArguments;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + name.hashCode();
      result = prime * result + numArguments;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      FunctionSig other = (FunctionSig) obj;
      return other.name.equals(this.name) && other.numArguments == this.numArguments;
    }
  }

}
