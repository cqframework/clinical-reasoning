* Look at R4MeasureService, R4MultiMeasureService, R4MeasureProcessor, DSTU3MeasureService and DSTU3MeasureProcessor, and how they interact with each other, as well as the other code they call, such as MeasureEvaluator
* Gain a full understanding of this architecture
* Figure out how to fulfill new requirements:
    * Build a self-contained workflow encompassing the entry points of the various services above
      right up to the creation and update of MeasureDef and related classes: so, for example, right
      up end of execution in method R4MeasureProcessor#processResults
    * Build another self-contained workflow that immediately follows this line, taking a fully
      updated MeasureDef and outputting a MeasureReport or Parameters
* Feel free to break any contracts, including expectations of public/protected/package-private APIs
* Feel free to break any non-integration style unit tests
* Feel free to break the inner workings of Measure/MultiMeasure test frameworks, so long as the
  assertions in the client tests are not broken, both for the Defs and the MeasureReports/Parameters
