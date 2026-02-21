import cqf.LinkedBuildRegistry

// Settings plugin that reads local.properties and conditionally includes
// linked builds as composite builds with the correct dependency substitutions.

val localPropsFile = file("local.properties")
if (localPropsFile.exists()) {
    val localProps = java.util.Properties().apply { localPropsFile.inputStream().use(::load) }

    for (build in LinkedBuildRegistry.builds) {
        val path = localProps.getProperty(build.propertyKey) ?: continue

        includeBuild(path) {
            if (build.substitutions.isNotEmpty()) {
                dependencySubstitution {
                    for ((module, project) in build.substitutions) {
                        substitute(module(module)).using(project(project))
                    }
                }
            }
        }
    }
}
