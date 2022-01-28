
// package org.opencds.cqf.cql.evaluator.engine.execution;

// import static org.testng.Assert.assertNotNull;
// import static org.testng.Assert.assertEquals;
// import static org.mockito.Mockito.times;

// import java.io.InputStream;
// import java.io.InputStreamReader;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.Map;

// import org.cqframework.cql.cql2elm.CqlTranslator;
// import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
// import org.cqframework.cql.cql2elm.ModelManager;
// import org.cqframework.cql.elm.execution.Library;
// import org.cqframework.cql.elm.execution.VersionedIdentifier;
// import org.hl7.fhir.instance.model.api.IBaseResource;
// import org.testng.annotations.BeforeClass;
// import org.testng.annotations.BeforeMethod;
// import org.testng.annotations.Test;
// import org.mockito.Mockito;
// import org.opencds.cqf.cql.engine.execution.LibraryLoader;
// import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BaseFhirLibraryContentProvider;
// import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;

// import ca.uhn.fhir.context.FhirContext;
// import ca.uhn.fhir.context.FhirVersionEnum;
// import ca.uhn.fhir.parser.IParser;

// public class CacheAwareLibraryLoaderDecoratorTests {
//     private FhirContext fhirContext;
//     private IParser parser;
//     private ModelManager modelManger;
//     private TranslatorOptionAwareLibraryLoader libraryLoader;
//     private Map<VersionedIdentifier, Library> libraryCache;
//     private CacheAwareLibraryLoaderDecorator libraryLoaderDecorator;

//     @BeforeClass
//     public void setup() {
//         fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
//         modelManger = new ModelManager();
//         parser = fhirContext.newJsonParser();
//     }

//     private TranslatorOptionAwareLibraryLoader createLibraryLoader(CqlTranslatorOptions translatorOptions) {
//         BaseFhirLibraryContentProvider testFhirLibraryContentProvider = new BaseFhirLibraryContentProvider(
//                 new AdapterFactory()) {
//             @Override
//             public IBaseResource getLibrary(org.hl7.elm.r1.VersionedIdentifier versionedIdentifier) {
//                 String name = versionedIdentifier.getId();

//                 InputStream libraryStream = CacheAwareLibraryLoaderDecoratorTests.class
//                         .getResourceAsStream(name + ".json");

//                 return parser.parseResource(new InputStreamReader(libraryStream));
//             }
//         };

//         return new TranslatingLibraryLoader(modelManger, Collections.singletonList(testFhirLibraryContentProvider),
//                 translatorOptions);
//     }

//     @BeforeMethod
//     public void initialize() {
//         this.libraryLoader = Mockito.spy(this.createLibraryLoader(CqlTranslatorOptions.defaultOptions()));

//         this.libraryCache = Mockito.spy(new HashMap<>());

//         this.libraryLoaderDecorator = Mockito.spy(new CacheAwareLibraryLoaderDecorator(libraryLoader, libraryCache));
//     }

//     @Test
//     public void cachesLibrary() {
//         VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId("LibraryJson");
//         Library library = this.libraryLoaderDecorator.load(libraryIdentifier);
//         assertNotNull(library);

//         Mockito.verify(this.libraryLoader, times(1)).load(libraryIdentifier);

//         assertEquals(this.libraryCache.size(), 1);

//         // Load from cache, should not have requested another load
//         library = this.libraryLoaderDecorator.load(libraryIdentifier);
//         assertNotNull(library);

//         Mockito.verify(this.libraryLoader, times(1)).load(libraryIdentifier);
//     }

//     @Test
//     public void recachesAfterInvalidation() {
//         VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId("LibraryJson");
//         Library library = this.libraryLoaderDecorator.load(libraryIdentifier);
//         assertNotNull(library);

//         Mockito.verify(this.libraryLoader, times(1)).load(libraryIdentifier);

//         assertEquals(this.libraryCache.size(), 1);

//         this.libraryCache.clear();
//         ;

//         // Cache is empty, should have reloaded.
//         library = this.libraryLoaderDecorator.load(libraryIdentifier);
//         assertNotNull(library);

//         Mockito.verify(this.libraryLoader, times(2)).load(libraryIdentifier);
//     }

//     @Test
//     public void usesCacheIfSupplied() {
//         VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId("LibraryJson");
//         Library library = this.libraryLoader.load(libraryIdentifier);
//         assertNotNull(library);

//         this.libraryCache.put(libraryIdentifier, library);

//         // Cache is not empty, should use it
//         library = this.libraryLoaderDecorator.load(libraryIdentifier);
//         assertNotNull(library);

//         Mockito.verify(this.libraryLoader, times(1)).load(libraryIdentifier);
//     }

//     @Test
//     public void doesNotUseCacheIfTranslatorMismatch() {
//         VersionedIdentifier libraryIdentifier = new VersionedIdentifier().withId("LibraryBoth");

//         LibraryLoader nonDefaultLibraryLoader = this.createLibraryLoader(
//                 CqlTranslatorOptions.defaultOptions().withOptions(CqlTranslator.Options.RequireFromKeyword));

//         Library library = nonDefaultLibraryLoader.load(libraryIdentifier);
//         assertNotNull(library);

//         this.libraryCache.put(libraryIdentifier, library);

//         // Cache has a bad library, so this should still call the library loader
//         library = this.libraryLoaderDecorator.load(libraryIdentifier);
//         assertNotNull(library);

//         Mockito.verify(this.libraryLoader, times(1)).load(libraryIdentifier);
//     }
// }