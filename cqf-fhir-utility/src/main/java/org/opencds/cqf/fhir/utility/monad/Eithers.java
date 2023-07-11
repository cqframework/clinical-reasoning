package org.opencds.cqf.fhir.utility.monad;

// Companion class for Either. Defines factories, interfaces, functions.
public class Eithers {

  private Eithers() {
    // intentionally empty
  }

  public static <L, R> Either<L, R> for2(L left, R right) {
    return left != null ? forLeft(left) : forRight(right);
  }

  public static <L, R> Either<L, R> forLeft(L left) {
    return new Either<>(left, null);
  }

  public static <L, R> Either<L, R> forRight(R right) {
    return new Either<>(null, right);
  }

  public static <L, M, R> Either3<L, M, R> for3(L left, M middle, R right) {
    if (left != null) {
      return forLeft3(left);
    }

    if (middle != null) {
      return forMiddle3(middle);
    }

    return forRight3(right);
  }

  public static <L, M, R> Either3<L, M, R> forLeft3(L left) {
    return new Either3<>(left, null, null);
  }

  public static <L, M, R> Either3<L, M, R> forRight3(R right) {
    return new Either3<>(null, null, right);
  }

  public static <L, M, R> Either3<L, M, R> forMiddle3(M middle) {
    return new Either3<>(null, middle, null);
  }
}
