package org.opencds.cqf.fhir.utility.repository;

/*
 * This enum represents whether or not an IG file structure has a directory per resource category or a flat structure.
 *
 * Resource categories are defined by the ResourceCategory enum.
 */
public enum ResourceCategoryMode {
    DIRECTORY_PER_CATEGORY,
    FLAT
}
