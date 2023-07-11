package org.opencds.cqf.fhir.utility.monad;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Either<L, R> {

  private final L left;
  private final R right;

  Either(L left, R right) {
    checkArgument(left == null ^ right == null, "left and right are mutually exclusive");
    this.left = left;
    this.right = right;
  }

  public Either<R, L> swap() {
    if (isLeft()) {
      return Eithers.forRight(left());
    }

    return Eithers.forLeft(right());
  }

  public boolean isLeft() {
    return left != null;
  }

  public boolean isRight() {
    return right != null;
  }

  public L left() {
    checkState(isLeft());
    return left;
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

  public Either<L, R> forEach(
      Consumer<? super R> forRight) {
    checkNotNull(forRight);
    if (isRight()) {
      forRight.accept(right());
    }

    return this;
  }

  @SuppressWarnings("unchecked")
  public <T> Either<L, T> map(Function<? super R, ? extends T> mapRight) {
    checkNotNull(mapRight);
    if (isLeft()) {
      return (Either<L, T>) this;
    }

    return Eithers.forRight(mapRight.apply(right()));
  }

  @SuppressWarnings("unchecked")
  public <T> Either<L, T> flatMap(
      Function<? super R, ? extends Either<L, ? extends T>> flatMapRight) {
    checkNotNull(flatMapRight);
    if (isLeft()) {
      return (Either<L, T>) this;
    }

    return (Either<L, T>) flatMapRight.apply(right());
  }

  public R fold(
      Function<? super L, ? extends R> foldLeft) {
    checkNotNull(foldLeft);
    if (isRight()) {
      return right;
    } else {
      return foldLeft.apply(left());
    }
  }

  public <T> T fold(
      Function<? super L, ? extends T> foldLeft,
      Function<? super R, ? extends T> foldRight) {
    checkNotNull(foldLeft);
    checkNotNull(foldRight);
    if (isRight()) {
      return foldRight.apply(right());
    } else {
      return foldLeft.apply(left());
    }
  }

  public <U> U transform(Function<? super Either<? super L, ? super R>, ? extends U> transform) {
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

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Either<?, ?>) {
      Either<?, ?> other = (Either<?, ?>) obj;
      return (this.left == other.left && this.right == other.right)
          || (this.left != null && other.left != null && this.left.equals(other.left))
          || (this.right != null && other.right != null && this.right.equals(other.right));
    }

    return false;
  }

  @Override
  public int hashCode() {
    if (this.left != null) {
      return this.left.hashCode();
    }

    return this.right.hashCode();
  }
}
