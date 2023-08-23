package org.opencds.cqf.fhir.utility.repository;

/**
 * This enum represents whether or not an IG is structure using ResourceType as a prefix for
 * filename or if it's structured to use a directory hierarchy that includes the resource type in
 * the path
 */
public enum IGLayoutMode {
  DIRECTORY,
  TYPE_PREFIX
}
