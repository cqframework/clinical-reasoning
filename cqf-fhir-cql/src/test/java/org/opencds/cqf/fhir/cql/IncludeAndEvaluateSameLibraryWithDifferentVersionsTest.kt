package org.opencds.cqf.fhir.cql

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.util.ParametersUtil
import kotlin.jvm.optionals.getOrNull
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.hl7.elm.r1.VersionedIdentifier
import org.hl7.fhir.r4.model.Library
import org.opencds.cqf.cql.engine.exception.CqlException
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository

class IncludeAndEvaluateSameLibraryWithDifferentVersionsTest {

    /**
     * Expression x is defined in LibD version 1.0.0 and version 2.0.0 and evaluates to 1 and 2,
     * respectively.
     *
     * LibB re-exports expression x from LibD version 1.0.0.
     *
     * LibC re-exports expression x from LibD version 2.0.0.
     *
     * LibA accesses LibB.x and LibC.x.
     *
     * LibE directly includes both versions of LibD.
     */
    val LIB_D_V1 =
        """
            library LibD version '1.0.0'
            
            define x: 1
        """
            .trimIndent()

    val LIB_D_V2 =
        """
            library LibD version '2.0.0'
            
            define x: 2
        """
            .trimIndent()

    val LIB_B =
        """
            library LibB
            
            include LibD version '1.0.0'
            
            define x: LibD.x
        """
            .trimIndent()

    val LIB_C =
        """
            library LibC
            
            include LibD version '2.0.0'
            
            define x: LibD.x
        """
            .trimIndent()

    val LIB_A =
        """
            library LibA
            
            include LibB
            include LibC
            
            define xFromLibB: LibB.x
            define xFromLibC: LibC.x
        """
            .trimIndent()

    val LIB_E =
        """
            library LibE
            
            include LibD version '1.0.0' called LibDVersion1
            include LibD version '2.0.0' called LibDVersion2
            
            define xFromLibDVersion1: LibDVersion1.x
            define xFromLibDVersion2: LibDVersion2.x
        """
            .trimIndent()

    fun createLibrary(resourceId: String, name: String, version: String?, cql: String): Library {
        val library = Library()
        library.setId(resourceId)
        library.setName(name)
        library.setVersion(version)
        library.setUrl(
            "http://example.com/Library/$name" + (if (version == null) "" else "|$version")
        )
        library.addContent().setContentType("text/cql").setData(cql.toByteArray())
        return library
    }

    fun createRepository(): IRepository {
        return InMemoryFhirRepository(FhirContext.forR4Cached())
    }

    fun LibraryEngine.evaluate(libraryId: VersionedIdentifier): Map<String, Int?> {
        val results = this.evaluate(libraryId, null, null, null, null, null, null)
        val fhirContext = this.repository.fhirContext()
        return ParametersUtil.getNamedParameters(fhirContext, results).asMap().mapValues {
            (expression, result) ->
            ParametersUtil.getNamedParameterValueAsInteger(fhirContext, results, expression)
                .getOrNull()
        }
    }

    @Test
    fun includeSameLibraryWithDifferentVersionsTransitivelyTest() {
        val repository =
            createRepository().apply {
                update(createLibrary("LibAResourceId", "LibA", null, LIB_A))
                update(createLibrary("LibBResourceId", "LibB", null, LIB_B))
                update(createLibrary("LibCResourceId", "LibC", null, LIB_C))
                update(createLibrary("LibDVersion1ResourceId", "LibD", "1.0.0", LIB_D_V1))
                update(createLibrary("LibDVersion2ResourceId", "LibD", "2.0.0", LIB_D_V2))
            }

        val engine = LibraryEngine(repository, EvaluationSettings.default)

        val results = engine.evaluate(VersionedIdentifier().apply { id = "LibA" })

        assertEquals(1, results["xFromLibB"])
        assertEquals(2, results["xFromLibC"])
    }

    @Test
    fun includeSameLibraryWithDifferentVersionsDirectlyTest() {
        val repository =
            createRepository().apply {
                update(createLibrary("LibDVersion1ResourceId", "LibD", "1.0.0", LIB_D_V1))
                update(createLibrary("LibDVersion2ResourceId", "LibD", "2.0.0", LIB_D_V2))
                update(createLibrary("LibEResourceId", "LibE", null, LIB_E))
            }

        val engine = LibraryEngine(repository, EvaluationSettings.default)

        val results = engine.evaluate(VersionedIdentifier().apply { id = "LibE" })

        assertEquals(1, results["xFromLibDVersion1"])
        assertEquals(2, results["xFromLibDVersion2"])
    }

    @Test
    fun evaluateSameLibraryWithDifferentVersionsTest() {
        val repository =
            createRepository().apply {
                update(createLibrary("LibDVersion1ResourceId", "LibD", "1.0.0", LIB_D_V1))
                update(createLibrary("LibDVersion2ResourceId", "LibD", "2.0.0", LIB_D_V2))
            }

        val engine = LibraryEngine(repository, EvaluationSettings.default)

        assertEquals(
            1,
            engine
                .evaluate(
                    VersionedIdentifier().apply {
                        id = "LibD"
                        version = "1.0.0"
                    }
                )["x"],
        )
        assertEquals(
            2,
            engine
                .evaluate(
                    VersionedIdentifier().apply {
                        id = "LibD"
                        version = "2.0.0"
                    }
                )["x"],
        )
    }

    @Test
    fun evaluateSameLibraryWithDifferentVersionsButSameResourceIdTest() {
        val repository =
            createRepository().apply {
                update(createLibrary("LibDResourceId", "LibD", "1.0.0", LIB_D_V1))
                update(createLibrary("LibDResourceId", "LibD", "2.0.0", LIB_D_V2))
                update(createLibrary("LibBResourceId", "LibB", null, LIB_B))
                update(createLibrary("LibCResourceId", "LibC", null, LIB_C))
            }

        val engine = LibraryEngine(repository, EvaluationSettings.default)

        val exception =
            assertFailsWith<CqlException> {
                engine.evaluate(VersionedIdentifier().apply { id = "LibB" })
            }
        assertContains(exception.message!!, "Could not load source for library LibD, version 1.0.0")

        assertEquals(2, engine.evaluate(VersionedIdentifier().apply { id = "LibC" })["x"])
    }
}
