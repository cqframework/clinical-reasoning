package org.opencds.cqf.fhir.utility.monad

import java.util.Optional
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * Represents a value of one of two possible types (a disjoint union). An instance of Either is
 * either an instance of Left or Right, only one of which can be present at a time.
 *
 * This type is right-biased, meaning that operations like map and flatMap will apply to the right
 * value if it is present. By convention, the preferred type for a given context should be the right
 * value.
 *
 * By convention, the left value is used for error handling, when used in a context where one of the
 * values is an error or exception.
 *
 * While this class does provide methods for accessing the left and right values, it is generally
 * recommended to use the map, flatMap, and fold methods to work with the values. These methods are
 * more idiomatic and less error-prone, and they are less likely to result in
 * IllegalStateExceptions.
 *
 * @param <L> the left type
 * @param <R> the right type </R></L>
 */
open class Either<L : Any, R : Any> internal constructor(left: L?, right: R?) {
    private val left: L?
    private val right: R?

    /*
     * Private constructor. Use static factory methods for instantiation. See {@link Eithers}.
     */
    init {
        require((left == null) xor (right == null)) { "left and right are mutually exclusive" }
        this.left = left
        this.right = right
    }

    /**
     * Swaps the left and right types. The value is unchanged.
     *
     * @return a new Either instance with the left and right types swapped
     */
    fun swap(): Either<R, L> {
        if (isRight()) {
            return Eithers.forLeft(right!!)
        }

        return Eithers.forRight(left!!)
    }

    /**
     * Returns true if the value is the left type, false if it is right.
     *
     * @return true if left is present
     */
    fun isLeft(): Boolean {
        return left != null
    }

    /**
     * Returns true if the value is the right type, false if it is left.
     *
     * @return true is right is present
     */
    fun isRight(): Boolean {
        return right != null
    }

    /**
     * Returns the left value if it is present, otherwise throws an IllegalStateException.
     *
     * It's generally preferred to pass functions to the Either using [.fold]
     *
     * @return the left value
     * @throws IllegalStateException if the left value is not present
     */
    fun leftOrThrow(): L {
        check(isLeft())
        return left!!
    }

    /**
     * Returns the right value if it is present, otherwise throws an IllegalStateException.
     *
     * It's generally preferred to pass functions to the Either using [.fold] Alternatively, use
     * [.orElse] or [.orElseGet]
     *
     * @return the right value
     * @throws IllegalStateException if the right value is not present
     */
    fun rightOrThrow(): R {
        check(isRight())
        return right!!
    }

    val orThrow: R
        /**
         * Alias for [.rightOrThrow].
         *
         * It's generally preferred to pass functions to the Either using [.fold] Alternatively, use
         * [.orElse] or [.orElseGet]
         *
         * @return the right value
         * @throws IllegalStateException if the right value is not present
         */
        get() = rightOrThrow()

    /**
     * Returns the right value if it is present, otherwise returns the provided default value.
     *
     * @param defaultValue the value to return if the right value is not present
     * @return the right value if it is present, otherwise the default value
     */
    fun orElse(defaultValue: R?): R? {
        if (isRight()) {
            return right
        } else {
            return defaultValue
        }
    }

    /**
     * Returns the right value if it is present, otherwise returns the result of the provided
     * supplier.
     *
     * @param defaultSupplier the supplier to provide a default value if the right value is not
     *   present
     * @return the right value if it is present, otherwise the result of the supplier
     */
    fun orElseGet(defaultSupplier: Supplier<R?>): R? {
        if (isRight()) {
            return right
        } else {
            return defaultSupplier.get()
        }
    }

    /**
     * Executes the provided consumer if the right value is present
     *
     * @param forRight the consumer to execute if the right value is present
     */
    fun forEach(forRight: Consumer<in R>) {
        if (isRight()) {
            forRight.accept(right!!)
        }
    }

    /**
     * Executes the provided consumer if the right value is present, returning the Either unchanged.
     *
     * @param forRight the consumer to execute if the right value is present
     * @return the Either unchanged
     */
    fun peek(forRight: Consumer<in R>): Either<L, R> {
        if (isRight()) {
            forRight.accept(right!!)
        }

        return this
    }

    /**
     * Maps the right value to a new value using the provided function.
     *
     * If the right value is not present, returns the left value unchanged.
     *
     * @param <T> the new right type
     * @param mapRight the function to map the right value to a new value
     * @return the Either with the right value mapped to a new value, or the left value unchanged
     *   </T>
     */
    open fun <T : Any> map(mapRight: Function<in R, out T>): Either<L, T> {
        if (isLeft()) {
            return this as Either<L, T>
        }

        return Eithers.forRight(mapRight.apply(right!!))
    }

    /**
     * Maps the right value to a new Either using the provided function.
     *
     * If the right value is not present, returns the left value unchanged.
     *
     * @param <T> the new right type
     * @param flatMapRight the function to map the right value to a new Either
     * @return a new Either instance with the right value mapped to a new Either, or the left value
     *   unchanged </T>
     */
    open fun <T : Any> flatMap(flatMapRight: Function<in R, out Either<L, out T>>): Either<L, T> {
        if (isLeft()) {
            return this as Either<L, T>
        }

        return flatMapRight.apply(right!!) as Either<L, T>
    }

    /**
     * Maps the left or right value to a new value using the provided functions.
     *
     * The function is sometimes known as "reduce".
     *
     * If the right value is present, the foldRight function is applied to the right value. If the
     * left value is present, the foldLeft function is applied to the left value.
     *
     * @param <T> the type of the new value
     * @param foldLeft the function to map the left value to a new value, if present
     * @param foldRight the function to map the right value to a new value, if present
     * @return the new value </T>
     */
    fun <T> fold(foldLeft: Function<in L, out T>, foldRight: Function<in R, out T>): T {
        if (isRight()) {
            return foldRight.apply(right!!)
        } else {
            return foldLeft.apply(left!!)
        }
    }

    /**
     * Transforms the Either to a new value using the provided function. The function is applied to
     * the entire Either, regardless of which value is present. This is in contrast to the map,
     * flatMap, and fold functions, which only apply when the right value is present.
     *
     * @param <U> the type of the new value
     * @param transform the function to transform the Either to a new value
     * @return the new value </U>
     */
    fun <U> transform(transform: Function<in Either<in L, in R>, out U>): U {
        return transform.apply(this)
    }

    /**
     * Returns a stream of the right value if present, otherwise an empty stream. Mainly useful for
     * converting to a stream for further processing with standard Java APIs like Stream.map,
     * Stream.filter, etc.
     *
     * @return the stream of the right value if present
     */
    fun stream(): Stream<R> {
        if (isRight()) {
            return Stream.of(right)
        } else {
            return Stream.of()
        }
    }

    /**
     * Returns an Optional containing the right value if present, otherwise an empty Optional.
     * Mainly useful for converting to an Optional for further processing with standard Java APIs,
     * like Optional.map, Optional.filter, etc.
     *
     * @return an Optional containing the right value if present
     */
    fun optional(): Optional<R> {
        if (isRight()) {
            return Optional.of(right!!)
        }

        return Optional.empty()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is Either<*, *>) {
            return (this.left === obj.left && this.right === obj.right) ||
                (this.left != null && obj.left != null && this.left == obj.left) ||
                (this.right != null && obj.right != null && this.right == obj.right)
        }

        return false
    }

    override fun hashCode(): Int {
        if (this.left != null) {
            return this.left.hashCode()
        }

        return this.right.hashCode()
    }
}
