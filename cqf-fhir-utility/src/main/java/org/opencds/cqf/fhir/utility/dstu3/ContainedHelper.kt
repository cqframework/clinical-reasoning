package org.opencds.cqf.fhir.utility.dstu3

import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream
import org.hl7.fhir.dstu3.model.DomainResource
import org.hl7.fhir.dstu3.model.Resource
import org.opencds.cqf.fhir.utility.dstu3.ContainedHelper.getContainedResourcesInContainedResources

/**
 * This class consists exclusively of static methods that assist with managing contained FHIR
 * Resources.
 *
 * The FHIR specification does not allow contained resources to contain additional resources: &gt;
 * Contained resources SHALL NOT contain additional contained resources.
 * https://www.hl7.org/fhir/references.html#contained
 *
 * When returning a resource from an annotated method that responds to an incoming request any
 * resources contained in another resource will be removed. `liftContainedResourcesToParent` in this
 * class will move all contained resources to the parent so they are present in responses to
 * clients.
 */
object ContainedHelper {
    /**
     * Adds all contained resources in resources contained on the parent to the parent.
     *
     * @param resource the parent resource where contained resources should be
     * @return the modified parent resource
     */
    @JvmStatic
    fun liftContainedResourcesToParent(resource: DomainResource): DomainResource {
        getContainedResourcesInContainedResources(resource)
            .forEach(Consumer { t: Resource? -> resource.addContained(t) }) // add

        // them
        // to
        // the
        // parent
        return resource // Return the resource to allow for method chaining
    }

    /**
     * Returns all contained resources that are not already directly present on the parent resource.
     *
     * @param resource the parent resource
     * @return list of the contained resources
     */
    private fun getContainedResourcesInContainedResources(
        resource: Resource?
    ): MutableList<Resource?> {
        if (resource !is DomainResource) {
            return ArrayList<Resource?>()
        }
        return streamContainedResourcesInContainedResources(resource).collect(Collectors.toList())
    }

    /**
     * Returns all contained resources, including resources directly contained on the parent and
     * resources contained on any other contained resource
     *
     * @param resource the parent resource
     * @return list of all contained resources
     */
    @JvmStatic
    fun getAllContainedResources(resource: Resource?): MutableList<Resource?> {
        if (resource !is DomainResource) {
            return mutableListOf()
        }
        return streamAllContainedResources(resource).collect(Collectors.toList())
    }

    /** @see getContainedResourcesInContainedResources */
    private fun streamContainedResourcesInContainedResources(
        resource: Resource?
    ): Stream<Resource?> {
        if (resource !is DomainResource) {
            return Stream.empty<Resource?>()
        }
        return resource
            .getContained() // We don't need to re-add any resources
            // that are already on the parent.
            .stream()
            .flatMap { obj -> ContainedHelper.streamAllContainedResources(obj) } // Get the
        // resources
        // contained
    }

    /** @see getAllContainedResources */
    private fun streamAllContainedResources(resource: Resource?): Stream<Resource?> {
        if (resource !is DomainResource) {
            return Stream.empty<Resource?>()
        }
        val contained = resource.getContained()

        return Stream.concat<Resource?>(
            contained.stream(),
            contained.stream().flatMap { obj -> ContainedHelper.streamAllContainedResources(obj) },
        )
    }
}
