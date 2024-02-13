package org.opencds.cqf.fhir.utility.repository.ig;

/*
 * This enum represents whether or not an IG file structure has a directory per resource category or a flat structure.
 *
 * Resource categories are defined by the ResourceCategory enum.
 */
enum CategoryLayout {
    DIRECTORY_PER_CATEGORY,
    FLAT
}
