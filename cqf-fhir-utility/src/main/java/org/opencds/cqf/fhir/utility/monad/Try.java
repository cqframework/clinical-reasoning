package org.opencds.cqf.fhir.utility.monad;

import java.util.function.Function;

public class Try<T> extends Either<Exception, T> {

    Try(Exception left, T right) {
        super(left, right);
    }

    public boolean hasException() {
        return isLeft();
    }

    public boolean hasResult() {
        return isRight();
    }

    public Exception exception() {
        return left();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Try<X> map(Function<? super T, ? extends X> map) {
        if (hasException()) {
            return (Try<X>) this;
        }

        try {
            return Tries.of(map.apply(right()));
        } catch (Exception e) {
            return Tries.ofException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Try<X> flatMap(Function<? super T, ? extends Either<Exception, ? extends X>> flatMapRight) {
        if (hasException()) {
            return (Try<X>) this;
        }

        try {
            return flatMapRight.apply(right()).fold(Tries::ofException, Tries::of);
        } catch (Exception e) {
            return Tries.ofException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Try<?>) {
            Try<?> other = (Try<?>) obj;
            return super.equals(other);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
