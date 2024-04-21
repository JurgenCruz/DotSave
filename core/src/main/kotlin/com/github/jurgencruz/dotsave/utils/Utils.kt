package com.github.jurgencruz.dotsave.utils

import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Helper that allows composing functions that return Result as well.
 * @param R The type of the resulting encapsulated value.
 * @param T The type of the source encapsulated value.
 * @param transform The transformation function between values that returns a Result.
 * @return The composed Result with either the transformed value or the error in the chain.
 */
inline fun <R, T> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (isSuccess) {
  true  -> transform(getOrNull()!!)
  false -> Result.failure(exceptionOrNull()!!)
}

/**
 * Wrapper for Path that returns result instead of throwing.
 * @param base Base Path.
 * @param subPaths Any sub paths after the base path.
 * @return A Result object with the Path if successful or exception if failure.
 */
fun toSafePath(base: String, vararg subPaths: String): Result<Path> = runCatching { Path(base, *subPaths) }

/**
 * Helper that reduces a sequence of Results into a single Result to signal a success or failure of the entire operation.
 * If multiple errors, they will be merged using the "suppressed" exception making a chain.
 * @return A single Result signaling success or failure of the entire operation.
 */
fun <T> Sequence<Result<T>>.mergeFailures(): Result<Unit> {
  return this.filter(Result<T>::isFailure).reduceOrNull { l, r ->
    if (l != r) {
      l.exceptionOrNull()!!.addSuppressed(r.exceptionOrNull())
    }
    l
  }?.map { } ?: Result.success(Unit)
}
