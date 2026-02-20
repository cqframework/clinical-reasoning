package cqf

/**
 * A known linked build that can be included as a composite build.
 *
 * @param slug GitHub repo slug (e.g. "cqframework/clinical_quality_language")
 * @param propertyKey the key in local.properties that holds the path to the local checkout
 * @param buildRoot subdirectory within the repo containing settings.gradle.kts (empty for root)
 * @param substitutions map of Maven module coordinate to Gradle project path.
 *   All consumed modules must be listed — providing any explicit substitution
 *   disables Gradle's automatic substitution for the entire included build.
 */
data class LinkedBuild(
    val slug: String,
    val propertyKey: String,
    val buildRoot: String = "",
    val substitutions: Map<String, String> = emptyMap()
)

/**
 * Registry of all known linked builds. Add new repos here — both the settings plugin
 * (includeBuild) and the CI plugin (resolveLinkedBuilds) read from this list.
 */
object LinkedBuildRegistry {

    private val CQL_ENGINE = LinkedBuild(
        slug = "cqframework/clinical_quality_language",
        propertyKey = "cql.engine.path",
        buildRoot = "Src/java",
        substitutions = buildMap {
            // Kotlin/JVM modules — artifact name matches project name
            for (name in listOf("engine", "engine-fhir", "ucum", "elm-fhir", "cqf-fhir", "cqf-fhir-npm", "quick")) {
                put("org.cqframework:$name", ":$name")
            }
            // KMP module publishes JVM artifact with -jvm suffix
            put("org.cqframework:cql-to-elm-jvm", ":cql-to-elm")
        }
    )

    val builds: List<LinkedBuild> = listOf(CQL_ENGINE)

    /** Lookup by repo slug. */
    fun forSlug(slug: String): LinkedBuild? = builds.find { it.slug == slug }

    /** Lookup by property key. */
    fun forProperty(key: String): LinkedBuild? = builds.find { it.propertyKey == key }
}
