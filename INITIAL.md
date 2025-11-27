# Acceptance Criteria

* Identify all unit tests annotated @ParameterizedTest and @MethodSource
* Look at the method parameters for the test
* Create a custom inner record containing all of the parameters
* Convert the method to take this new record class instead of the arguments
* In the method identified by the @MethodSource, convert the method to return a Stream of the custom record instead of Argument
* Convert each Argument to a constructor call to the new record
* Ensure all code compiles
* Ensure all tests in that module pass
* Run spotless
* Generate a commit message
* Do not commit yet
