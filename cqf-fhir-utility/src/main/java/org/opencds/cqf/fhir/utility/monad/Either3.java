package org.opencds.cqf.fhir.utility.monad;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Either3<L, M, R> {

  private final L left;
  private final M middle;
  private final R right;

  Either3(L left, M middle, R right) {
    checkArgument(left == null ^ middle == null ^ right == null,
        "left, middle, and right are mutually exclusive");
    this.left = left;
    this.middle = middle;
    this.right = right;
  }


  public Either3<R, M, L> swap() {
    if (isLeft()) {
      return Eithers.forRight3(left());
    } else if (isMiddle()) {
      return Eithers.forMiddle3(middle());
    } else {
      return Eithers.forLeft3(right());
    }
  }

  public Either3<R, L, M> rotate() {
    if (isLeft()) {
      return Eithers.forMiddle3(left());
    } else if (isMiddle()) {
      return Eithers.forRight3(middle());
    } else {
      return Eithers.forLeft3(right());
    }
  }

  public L left() {
    checkState(isLeft());
    return left;
  }

  public M middle() {
    checkState(isMiddle());
    return middle;
  }

  public R right() {
    checkState(isRight());
    return right;
  }

  public R get() {
    return right();
  }

  public R orElse(R defaultValue) {
    if (isRight()) {
      return right;
    } else {
      return defaultValue;
    }
  }

  public R orElseGet(Supplier<R> defaultSupplier) {
    if (isRight()) {
      return right;
    } else {
      return defaultSupplier.get();
    }
  }

  public boolean isLeft() {
    return left != null;
  }

  public boolean isMiddle() {
    return middle != null;
  }

  public boolean isRight() {
    return right != null;
  }

  public void forEach(
      Consumer<? super R> forRight) {
    checkNotNull(forRight);
    if (isRight()) {
      forRight.accept(right());
    }
  }

  public Either3<L, M, R> peek(
      Consumer<? super R> forRight) {
    checkNotNull(forRight);
    if (isRight()) {
      forRight.accept(right());
    }

    return this;
  }

  public <T> Either3<L, M, T> map(Function<? super R, ? extends T> mapRight) {
    checkNotNull(mapRight);
    if (isRight()) {
      return Eithers.forRight3(mapRight.apply(right()));
    }

    return propagate();
  }

  public <T> Either3<L, M, T> flatMap(
      Function<? super R, Either3<L, M, ? extends T>> flatMapRight) {
    checkNotNull(flatMapRight);
    if (isRight()) {
      return narrow(flatMapRight.apply(right()));
    }

    return propagate();
  }

  public <T> T fold(
      Function<? super L, ? extends T> foldLeft,
      Function<? super M, ? extends T> foldMiddle,
      Function<? super R, ? extends T> foldRight) {
    checkNotNull(foldLeft);
    checkNotNull(foldRight);
    if (isRight()) {
      return foldRight.apply(right());
    } else if (isMiddle()) {
      return foldMiddle.apply(middle());
    }

    return foldLeft.apply(left());
  }

  public <U> U transform(
      Function<? super Either3<? super L, ? super M, ? super R>, ? extends U> transform) {
    return transform.apply(this);
  }

  public Stream<R> stream() {
    if (isRight()) {
      return Stream.of(right());
    } else {
      return Stream.of();
    }
  }

  public Optional<R> optional() {
    if (isRight()) {
      return Optional.of(right());
    }

    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  protected <T> Either3<L, M, T> narrow(Either3<L, M, ? extends T> wide) {
    return (Either3<L, M, T>) wide;
  }

  @SuppressWarnings("unchecked")
  protected <T> Either3<L, M, T> propagate() {
    return (Either3<L, M, T>) this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Either3<?, ?, ?>) {
      Either3<?, ?, ?> other = (Either3<?, ?, ?>) obj;
      return (this.left == other.left && this.right == other.right && this.middle == other.middle)
          || (this.left != null && other.left != null && this.left.equals(other.left))
          || (this.middle != null && other.middle != null && this.middle.equals(other.middle))
          || (this.right != null && other.right != null && this.right.equals(other.right));
    }

    return false;
  }


  @Override
  public int hashCode() {
    if (this.left != null) {
      return this.left.hashCode();
    }

    if (this.middle != null) {
      return this.middle.hashCode();
    }

    return this.right.hashCode();
  }
}
