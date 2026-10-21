package org.opencds.cqf.fhir.utility.monad

import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * Represents a value of one of three possible types (a disjoint union). An instance of Either3 is
 * either an instance of Left, Middle, or Right, only one of which can be present at a time.
 *
 * This type is right-biased, meaning that operations like map and flatMap will apply to the right
 * value if it is present. By convention, the preferred type for a given context should be the right
 * value.
 *
 * By convention, the left value is used for error handling, when used in a context where one of the
 * values is an error or exception.
 *
 * While this class does provide methods for accessing the left, middle, and right values, it is
 * generally recommended to use the map, flatMap, and fold methods to work with the values. These
 * methods are more idiomatic and less error-prone, and they are less likely to result in
 * IllegalStateExceptions.
 *
 * @param <L> the left value type
 * @param <M> the middle value type
 * @param <R> the right value type </R></M></L>
 */
class Either3<L : Any, M : Any, R : Any> internal constructor(left: L?, middle: M?, right: R?) {
    private val left: L?
    private val middle: M?
    private val right: R?

    /*
     * Private constructor. Use static factory methods for instantiation. See {@link Eithers}.
     */
    init {
        require((left != null) xor (middle != null) xor (right != null)) {
            "left, middle, and right are mutually exclusive"
        }
        this.left = left
        this.middle = middle
        this.right = right
    }

    /**
     * Swaps the position of the left and right types The middle type is unchanged. The value is
     * unchanged.
     *
     * @return a new Either3 with the left and right types swapped
     */
    fun swap(): Either3<R, M, L> {
        if (isRight()) {
            return Eithers.forLeft3<R, M, L>(right!!)
        } else if (isMiddle()) {
            return Eithers.forMiddle3<R, M, L>(middle!!)
        } else {
            return Eithers.forRight3<R, M, L>(left!!)
        }
    }

    /**
     * Rotates the types of the Either3 to the right. The right type becomes the left type, the left
     * type becomes the middle type, and the middle type becomes the right type. The values are
     * unchanged.
     *
     * @return new Either3 with the types rotated
     */
    fun rotate(): Either3<R, L, M> {
        if (isRight()) {
            return Eithers.forLeft3<R, L, M>(right!!)
        } else if (isMiddle()) {
            return Eithers.forRight3<R, L, M>(middle!!)
        } else {
            return Eithers.forMiddle3<R, L, M>(left!!)
        }
    }

    /**
     * Returns the left value. Throws an exception if the left value is not present.
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
     * Returns the middle value. Throws an exception if the middle value is not present.
     *
     * It's generally preferred to pass functions to the Either using [.fold]
     *
     * @return the middle value
     * @throws IllegalStateException if the middle value is not present
     */
    fun middleOrThrow(): M {
        check(isMiddle())
        return middle!!
    }

    /**
     * Returns the right value. Throws an exception if the right value is not present.
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
     * Returns the right value if present, otherwise return the provided default value.
     *
     * @param defaultValue the value to return if the right value is not present
     * @return the right value if present, otherwise the default value
     */
    fun orElse(defaultValue: R?): R? {
        if (isRight()) {
            return right!!
        } else {
            return defaultValue
        }
    }

    /**
     * Returns the right value if present, otherwise return the result of the provided supplier.
     *
     * @param defaultSupplier the supplier to provide a default value if the right value is not
     *   present
     * @return the right value if present, otherwise the result of the supplier
     */
    fun orElseGet(defaultSupplier: Supplier<R?>): R? {
        if (isRight()) {
            return right
        } else {
            return defaultSupplier.get()
        }
    }

    /**
     * Returns true if the value is the left type. Otherwise, returns false.
     *
     * @return true if left is present
     */
    fun isLeft(): Boolean {
        return left != null
    }

    /**
     * Returns true if value is the middle type. Otherwise, returns false.
     *
     * @return true if middle is present
     */
    fun isMiddle(): Boolean {
        return middle != null
    }

    /**
     * Returns true if the value is the right type. Otherwise, returns false.
     *
     * @return true if right is present
     */
    fun isRight(): Boolean {
        return right != null
    }

    /**
     * Executes the provided consumer if the right value is present.
     *
     * @param forRight the consumer to execute if the right value is present
     */
    fun forEach(forRight: Consumer<in R>) {
        if (isRight()) {
            forRight.accept(right!!)
        }
    }

    /**
     * Executes the provided consumer if the right value is present, returning the Either3
     * unchanged.
     *
     * @param forRight the consumer to execute if the right value is present
     * @return the Either3 unchanged
     */
    fun peek(forRight: Consumer<in R>): Either3<L, M, R> {
        if (isRight()) {
            forRight.accept(right!!)
        }

        return this
    }

    /**
     * Maps the right value to a new value using the provided function.
     *
     * If the right value is not present, returns the left or middle value unchanged.
     *
     * @param <T> the type of the new value
     * @param mapRight the function to map the right value to a new value
     * @return a new Either3 with the right value mapped to a new value, or the left or middle value
     *   </T>
     */
    fun <T : Any> map(mapRight: Function<in R, out T>): Either3<L, M, T> {
        if (isRight()) {
            return Eithers.forRight3(mapRight.apply(right!!))
        }

        return propagate()
    }

    /**
     * Maps the Either to a new Either using the provided function.
     *
     * If the right value is present, the function is applied to the right value.
     *
     * If the right value is not present, the left or middle value is returned.
     *
     * @param <T> the type of the new right value
     * @param flatMapRight the function to map the Either to a new Either
     * @return a new Either3 with the right value mapped to a new Either, or the left or middle
     *   value </T>
     */
    fun <T : Any> flatMap(
        flatMapRight: Function<in R, out Either3<L, M, out T>>
    ): Either3<L, M, T> {
        if (isRight()) {
            return narrow(flatMapRight.apply(right!!))
        }

        return propagate()
    }

    /**
     * Maps the left, middle, or right value to a new value using the provided functions.
     *
     * The function is sometimes known as "reduce".
     *
     * If the right value is present, the foldRight function is applied to the right value. If the
     * middle value is present, the foldMiddle function is applied to the middle value. If the left
     * value is present, the foldLeft function is applied to the left value.
     *
     * @param <T> the type of the new value
     * @param foldLeft the function to map the left value to a new value, if present
     * @param foldMiddle the function to map the middle value to a new value, if present
     * @param foldRight the function to map the right value to a new value, if present
     * @return the new value </T>
     */
    fun <T> fold(
        foldLeft: Function<in L, out T>,
        foldMiddle: Function<in M, out T>,
        foldRight: Function<in R, out T>,
    ): T? {
        if (isRight()) {
            return foldRight.apply(right!!)
        } else if (isMiddle()) {
            return foldMiddle.apply(middle!!)
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
    fun <U> transform(transform: Function<in Either3<in L, in M, in R>, out U>): U {
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

    protected fun <T : Any> narrow(wide: Either3<L, M, out T>): Either3<L, M, T> {
        return wide as Either3<L, M, T>
    }

    protected fun <T : Any> propagate(): Either3<L, M, T> {
        return this as Either3<L, M, T>
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is Either3<*, *, *>) {
            return (this.left === obj.left &&
                this.right === obj.right &&
                this.middle === obj.middle) ||
                (this.left != null && obj.left != null && this.left == obj.left) ||
                (this.middle != null && obj.middle != null && this.middle == obj.middle) ||
                (this.right != null && obj.right != null && this.right == obj.right)
        }

        return false
    }

    override fun hashCode(): Int {
        if (this.left != null) {
            return this.left.hashCode()
        }

        if (this.middle != null) {
            return this.middle.hashCode()
        }

        return this.right.hashCode()
    }
}
