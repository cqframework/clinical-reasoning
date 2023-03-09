package org.opencds.cqf.cql.evaluator.measure;


import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;

import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.ExpressionDef;
import org.cqframework.cql.elm.execution.FunctionRef;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.Mockito;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BaseFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;

public class TranslatingLibraryLoaderTests {

  private static FhirContext fhirContext;
  private static IParser parser;
  private static ModelManager modelManger;
  private BaseFhirLibrarySourceProvider testFhirLibrarySourceProvider;
  private LibraryLoader libraryLoader;

  @BeforeClass
  public void setup() {
    fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    modelManger = new ModelManager();
    parser = fhirContext.newJsonParser();
  }

  @BeforeMethod
  public void initialize() {
    this.testFhirLibrarySourceProvider = Mockito.spy(new BaseFhirLibrarySourceProvider(new
        AdapterFactory()) {
      @Override
      public IBaseResource getLibrary(org.hl7.elm.r1.VersionedIdentifier versionedIdentifier) {
        String name = versionedIdentifier.getId();

        InputStream libraryStream = TranslatingLibraryLoaderTests.class.getResourceAsStream(name +
            ".json");

        return parser.parseResource(new InputStreamReader(libraryStream));
      }
    });

    this.libraryLoader = new TranslatingLibraryLoader(modelManger,
        Collections.singletonList(testFhirLibrarySourceProvider),
        CqlTranslatorOptions.defaultOptions(), null);
  }

  @Test
  public void loadLibrary() {
    VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId("MethodOverload");
    Library library = this.libraryLoader.load(libraryIdentifier);

    assertNotNull(library);
    //stored elm had no signature but translated library has one
    assertTrue(hasSignature(library));
  }

  boolean hasSignature(Library library) {
    if (library != null && library.getStatements() != null) {
      for (ExpressionDef ed : library.getStatements().getDef()) {
        if (ed.getExpression() instanceof FunctionRef) {
          FunctionRef fr = (FunctionRef) ed.getExpression();
          if (fr.getSignature() != null) {
            return true;
          }
        }
      }
    }
    return false;
  }
}