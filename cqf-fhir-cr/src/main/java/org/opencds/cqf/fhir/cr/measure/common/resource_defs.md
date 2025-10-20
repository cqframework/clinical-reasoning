


# LibraryDef

* VersionedIdentifier
* Content
  * Base64 encoded CQL
  * Reference to CQL file (testing only)
  * automagically resolve this in a version-agnostic way though private methods that return an InputStream

# MeasureDef

* id
* url
* version
* LibraryDef (Collection)
* groups
  * populations
  * stratifiers
  * scoring
  * etc...
* sdes
* errors

# Etc...


# IDefRepo (need better name)

* Top-level interface for all defs (IDef.java)
