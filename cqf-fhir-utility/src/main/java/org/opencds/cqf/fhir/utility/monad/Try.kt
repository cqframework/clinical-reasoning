package org.opencds.cqf.fhir.utility.monad

import java.util.function.Function

class Try<T : Any> internal constructor(left: Exception?, right: T?) :
    Either<Exception, T>(left, right) {
    fun hasException(): Boolean {
        return isLeft()
    }

    fun hasResult(): Boolean {
        return isRight()
    }

    fun exception(): Exception {
        return leftOrThrow()
    }

    override fun <X : Any> map(map: Function<in T, out X>): Try<X> {
        if (hasException()) {
            return this as Try<X>
        }

        try {
            return Tries.of(map.apply(rightOrThrow()))
        } catch (e: Exception) {
            return Tries.ofException(e)
        }
    }

    override fun <X : Any> flatMap(
        flatMapRight: Function<in T, out Either<Exception, out X>>
    ): Try<X> {
        if (hasException()) {
            return this as Try<X>
        }

        try {
            return flatMapRight.apply(rightOrThrow()).fold(Tries::ofException, Tries::of)
        } catch (e: Exception) {
            return Tries.ofException(e)
        }
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is Try<*>) {
            return super.equals(obj)
        }

        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
