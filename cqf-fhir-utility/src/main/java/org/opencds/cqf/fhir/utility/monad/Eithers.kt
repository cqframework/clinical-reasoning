package org.opencds.cqf.fhir.utility.monad

// Companion class for Either. Defines factory methods for creating Either instances.
object Eithers {
    /**
     * Creates an Either instance from two values. If the left value is not null, the Either will be
     * left. Otherwise, it will be right. Exactly one of the values must be present.
     *
     * @param <L> the left type
     * @param <R> the right type
     * @param left the left value
     * @param right the right value
     * @return an Either instance containing either left or right value </R></L>
     */
    @JvmStatic
    fun <L : Any, R : Any> for2(left: L?, right: R?): Either<L, R> {
        return if (left != null) forLeft(left) else forRight(right!!)
    }

    /**
     * Creates an Either instance from a left value. Left cannot be null.
     *
     * @param <L> the left type
     * @param <R> the right type
     * @param left the left value
     * @return an Either instance containing the left value </R></L>
     */
    @JvmStatic
    fun <L : Any, R : Any> forLeft(left: L): Either<L, R> {
        return Either(left, null)
    }

    /**
     * Creates an Either instance from a right value. Right cannot be null.
     *
     * @param <L> the left type
     * @param <R> the right type
     * @param right the right value
     * @return an Either instance containing the right value </R></L>
     */
    @JvmStatic
    fun <L : Any, R : Any> forRight(right: R): Either<L, R> {
        return Either(null, right)
    }

    /**
     * Creates an Either3 instance from three values. If the left value is not null, the Either3
     * will be left. If the middle value is not null, the Either3 will be middle. Otherwise, it will
     * be right. Exactly of the values must be present.
     *
     * @param <L> the left type
     * @param <M> the middle type
     * @param <R> the right type
     * @param left the left value
     * @param middle the middle value
     * @param right the right value
     * @return the Either3 instance containing either left, middle, or right value </R></M></L>
     */
    @JvmStatic
    fun <L : Any, M : Any, R : Any> for3(left: L?, middle: M?, right: R?): Either3<L, M, R> {
        if (left != null) {
            return forLeft3(left)
        }

        if (middle != null) {
            return forMiddle3(middle)
        }

        return forRight3(right!!)
    }

    /**
     * Creates an Either3 instance from a left value. Left cannot be null.
     *
     * @param <L> the left type
     * @param <M> the middle type
     * @param <R> the right type
     * @param left the left value
     * @return the Either3 instance containing the left value </R></M></L>
     */
    @JvmStatic
    fun <L : Any, M : Any, R : Any> forLeft3(left: L): Either3<L, M, R> {
        return Either3(left, null, null)
    }

    /**
     * Creates an Either3 instance from a middle value. Middle cannot be null.
     *
     * @param <L> the left type
     * @param <M> the middle type
     * @param <R> the right type
     * @param middle the middle value
     * @return the Either3 instance containing the middle value </R></M></L>
     */
    @JvmStatic
    fun <L : Any, M : Any, R : Any> forMiddle3(middle: M): Either3<L, M, R> {
        return Either3(null, middle, null)
    }

    /**
     * Creates an Either3 instance from a right value. Right cannot be null.
     *
     * @param <L> the left type
     * @param <M> the middle type
     * @param <R> the right type
     * @param right the right value
     * @return the Either3 instance containing the right value </R></M></L>
     */
    @JvmStatic
    fun <L : Any, M : Any, R : Any> forRight3(right: R): Either3<L, M, R> {
        return Either3(null, null, right)
    }
}
