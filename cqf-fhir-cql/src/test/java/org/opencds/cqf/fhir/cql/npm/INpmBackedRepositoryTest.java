package org.opencds.cqf.fhir.cql.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.cqframework.fhir.npm.NpmPackageManager;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opencds.cqf.fhir.cql.EvaluationSettings;

public interface INpmBackedRepositoryTest {
    FhirContext getFhirContext();

    IBaseResource createLibraryResource(String name, String canonicalUrl);

    <T extends IBaseResource> Class<T> getResourceClass(String resourceName);

    default String getCanonicalUrlFromResource(IBaseResource resource) {
        IBase val = getFhirContext()
                .getResourceDefinition(resource)
                .getChildByName("url")
                .getAccessor()
                .getValues(resource)
                .get(0);
        if (val instanceof IPrimitiveType<?> pt) {
            return pt.getValueAsString();
        }
        return null;
    }

    @Test
    default void resolveByUrl_withUrl_returnsOnlyResourcesThatMatch(@TempDir Path tempDir) throws IOException {
        // setup
        String name = "testIg";
        int count = 3;
        String urlBase = "http://example.com/";

        createPackage(tempDir, name, count, (Function<Integer, IBaseResource>) val -> {
            return createLibraryResource("Library" + val, String.format(urlBase + "%s/%s", "Library", val + ""));
        });

        // create the repo
        NpmBackedRepository repo = new NpmBackedRepository(getFhirContext(), EvaluationSettings.getDefault());

        // test
        repo.loadIg(tempDir.toString(), name);
        String urlToSearch = urlBase + "Library/1";
        List<IBaseResource> libraries = repo.resolveByUrl(getResourceClass("Library"), urlToSearch);

        // verify
        assertEquals(1, libraries.size());
        IBaseResource library = libraries.get(0);
        assertEquals("Library", library.fhirType());
        String url = getCanonicalUrlFromResource(library);
        assertEquals(urlToSearch, url);
    }

    @Test
    default void resolveByUrl_withoutUrl_returnsAllResourcesOfType(@TempDir Path tempDir) throws IOException {
        // setup
        String name = "testIg";
        int count = 3;
        String urlBase = "http://example.com/";

        createPackage(tempDir, name, count, (Function<Integer, IBaseResource>) val -> {
            return createLibraryResource("Library" + val, String.format(urlBase + "%s/%s", "Library", val + ""));
        });

        // create the repo
        NpmBackedRepository repo = new NpmBackedRepository(getFhirContext(), EvaluationSettings.getDefault());

        // test
        repo.loadIg(tempDir.toString(), name);
        List<IBaseResource> libraries = repo.resolveByUrl(getResourceClass("Library"), null);

        // verify
        assertEquals(count, libraries.size());
        for (IBaseResource library : libraries) {
            assertEquals("Library", library.fhirType());
            String url = getCanonicalUrlFromResource(library);
            assertTrue(url.startsWith(urlBase), urlBase);
        }
    }

    @Test
    default void resolveByUrl_noMatchingUrl_returnsNothing(@TempDir Path tempDir) throws IOException {
        // setup
        String name = "testIg";
        createPackage(tempDir, name, 1, (Function<Integer, IBaseResource>) val -> {
            return createLibraryResource("Library" + val, "http://some.place.com/lib");
        });

        // create the repo
        NpmBackedRepository repo = new NpmBackedRepository(getFhirContext(), EvaluationSettings.getDefault());

        // test
        repo.loadIg(tempDir.toString(), name);
        List<IBaseResource> libraries = repo.resolveByUrl(getResourceClass("Library"), "http://example.com");

        // verify
        assertTrue(libraries.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://some.place.com/"})
    @NullSource
    default void resolveByUrl_incorrectResource_returnsNothing(String urlToSearch, @TempDir Path tempDir)
            throws IOException {
        // setup
        String name = "testIg";
        createPackage(tempDir, name, 1, (Function<Integer, IBaseResource>) val -> {
            return createLibraryResource("Library" + val, urlToSearch);
        });

        // create the repo
        NpmBackedRepository repo = new NpmBackedRepository(getFhirContext(), EvaluationSettings.getDefault());

        // test
        repo.loadIg(tempDir.toString(), name);
        List<IBaseResource> questionnaires = repo.resolveByUrl(getResourceClass("Questionnaire"), urlToSearch);

        // verify
        // should be no questionnaire resources at all (regardless of url)
        assertTrue(questionnaires.isEmpty());
    }

    @Test
    default void resolveByUrl_validUrlButResourceHasNoCanonical_returnsNothing(@TempDir Path tempDir)
            throws IOException {
        // setup
        String name = "testIg";
        createPackage(tempDir, name, 1, (Function<Integer, IBaseResource>) val -> {
            return createLibraryResource("Library" + val, null);
        });

        // create the repo
        NpmBackedRepository repo = new NpmBackedRepository(getFhirContext(), EvaluationSettings.getDefault());

        // test
        repo.loadIg(tempDir.toString(), name);
        List<IBaseResource> libraries = repo.resolveByUrl(getResourceClass("Library"), "http://example.com");

        // validate
        assertTrue(libraries.isEmpty());
    }

    @Test
    default void loadIg_nonExistentPackage_failsToLoad() {
        // setup
        EvaluationSettings settings = EvaluationSettings.getDefault();
        NpmProcessor processor = mock(NpmProcessor.class);
        settings.setNpmProcessor(processor);
        NpmBackedRepository repo = new NpmBackedRepository(getFhirContext(), settings);

        // test
        try {
            // non-existent path
            repo.loadIg("any", "thing");
            fail();
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Could not load package"), ex.getMessage());
        }
    }

    @Test
    @SuppressWarnings("resource")
    default void resolveByUrl_cannotReadFile_coverageTest() throws IOException {
        // setup
        Map<String, List<String>> types = new HashMap<>();
        types.put("Library", List.of("someFile.json"));

        NpmPackage pkgMock = mock(NpmPackage.class);
        NpmProcessor processor = mock(NpmProcessor.class);
        org.hl7.fhir.r5.model.ImplementationGuide guide = new org.hl7.fhir.r5.model.ImplementationGuide();
        guide.setName("default");
        NpmPackageManager pkgMgr = new NpmPackageManager(guide);

        // when
        when(processor.getPackageManager()).thenReturn(pkgMgr);

        try (MockedStatic<NpmPackage> npmPackageMock = mockStatic(NpmPackage.class)) {
            npmPackageMock
                    .when(() -> NpmPackage.fromFolder(anyString(), anyBoolean()))
                    .thenReturn(pkgMock);

            EvaluationSettings settings = EvaluationSettings.getDefault();
            settings.setNpmProcessor(processor);
            NpmBackedRepository repo = new NpmBackedRepository(getFhirContext(), settings);

            // when
            when(pkgMock.getTypes()).thenReturn(types);
            doThrow(new IOException("Exception")).when(pkgMock).loadResource(anyString());

            // test
            repo.loadIg("any", "thing");
            repo.resolveByUrl(getResourceClass("Library"), null);
            fail();
        } catch (RuntimeException ex) {
            assertTrue(
                    ex.getLocalizedMessage().contains("Could not parse resource from package"),
                    ex.getLocalizedMessage());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    default void resolveByUrl_cannotParseResource_coverageTest(@TempDir Path tempDir) throws IOException {
        // setup
        String name = "testIg";
        createPackage(tempDir, name, 1, (Function<Integer, IBaseResource>) val -> {
            return createLibraryResource("Library" + val, null);
        });

        FhirContext spy = Mockito.spy(getFhirContext());

        // when
        IParser parser = mock(IParser.class);
        doReturn(parser).when(spy).newJsonParser();
        doThrow(new DataFormatException("exceptional")).when(parser).parseResource(any(Class.class), anyString());

        // create the repo
        NpmBackedRepository repo = new NpmBackedRepository(spy, EvaluationSettings.getDefault());

        repo.loadIg(tempDir.toString(), name);

        // test
        try {
            repo.resolveByUrl(getResourceClass("Library"), null);
            fail();
        } catch (Exception ex) {
            assertTrue(ex.getLocalizedMessage().contains("Could not parse resource from package"), ex.getMessage());
        }
    }

    // TODO - we might want to put this into a test util if we're going to use it over and over
    private void createPackage(
            Path tempDir, String pkgName, int resourceCount, Function<Integer, IBaseResource> resourceCreator)
            throws IOException {
        IParser parser = getFhirContext().newJsonParser();

        // create the default guide...
        // for some reason we require a 'base' sourceig...
        // and this base *must be* R5
        org.hl7.fhir.r5.model.ImplementationGuide guide = new org.hl7.fhir.r5.model.ImplementationGuide();
        guide.addFhirVersion(org.hl7.fhir.r5.model.Enumerations.FHIRVersion._4_0_0);
        guide.setName("default");
        IGContext igContext = new IGContext();
        igContext.setSourceIg(guide);

        // NpmManager expects:
        // (tempdir)/something/package/package.json
        // "version" = file://(tempdir)

        // create named file
        Path igNamePath = Files.createDirectory(Paths.get(tempDir.toString(), pkgName));
        // create package path
        Path packagePath = Files.createDirectory(Paths.get(igNamePath.toString(), "package"));

        for (int i = 0; i < resourceCount; i++) {
            IBaseResource resource = resourceCreator.apply(i);

            // add to package
            File f = new File(packagePath.toString(), String.format("resource%s.json", Integer.toString(i)));
            Files.writeString(f.toPath().toAbsolutePath(), parser.encodeResourceToString(resource));
        }

        // create the package speck (package.json)
        String pkgJson = String.format("""
            {
                "packageUrl": "file://%s",
                "name": "%s",
                "version": "1.0.0",
                "installMode": "STORE_ONLY"
            }
            """, packagePath.toString(), pkgName);

        File packagejson = new File(packagePath.toString(), "package.json");
        Files.writeString(packagejson.toPath(), pkgJson);
    }
}
