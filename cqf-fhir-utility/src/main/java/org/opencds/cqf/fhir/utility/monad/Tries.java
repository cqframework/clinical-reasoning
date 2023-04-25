package org.opencds.cqf.fhir.utility.monad;

import java.util.function.Supplier;

public class Tries {

  private Tries() {
    // intentionally empty
  }

  public static <T> Try<T> of(
      Exception error, T value) {
    return error != null ? ofException(error) : of(value);
  }

  public static <T> Try<T> ofException(Exception error) {
    return new Try<>(error, null);
  }

  public static <T> Try<T> of(T value) {
    return new Try<>(null, value);
  }

  public static <T> Try<T> of(Supplier<T> valueSupplier) {
    try {
      return of(valueSupplier.get());
    } catch (Exception e) {
      return ofException(e);
    }
  }

}
