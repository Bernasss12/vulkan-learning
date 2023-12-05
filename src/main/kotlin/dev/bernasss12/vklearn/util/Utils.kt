/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.util

object Utils {
    internal fun getOperatingSystem(): OperatingSystems {
        val system = System.getProperty("os.name", "generic").lowercase()
        return when {
            (system.contains("mac") || system.contains("darwin")) -> OperatingSystems.MACOS
            system.contains("nux") -> OperatingSystems.LINUX
            system.contains("win") -> OperatingSystems.WINDOWS
            else -> OperatingSystems.OTHER
        }
    }

    /**
     * Interleave values of the given arrays; will respect order; integer is the number of values of each array to place in a row.
     */
    fun interleaveFloatArrays(vararg arrays: Pair<Int, FloatArray>): FloatArray {
        when {
            // If only one array is given, return it
            arrays.size == 1 -> arrays.first().second
            // Verify if amounts are compatible with
            arrays.any { (amount, array) ->
                array.size % amount != 0
            } -> {
                throw IllegalArgumentException("Array sizes must be multiple of amount of elements for each array")
            }
            // Confirm all arrays and amounts will result in the same number of float "groups"
            arrays.map { (amount, array) ->
                array.size / amount
            }.any { iterations ->
                iterations != arrays.first().second.size / arrays.first().first
            } -> {
                throw IllegalArgumentException("Array size divided by amount should be the same for all arrays")
            }
        }

        // All arguments **should** be good to work with now...
        return arrays.map { (amount, array) ->
            array.toList().chunked(amount)
        }.interleave().flatten().toFloatArray()
    }

    private fun <T> List<List<T>>.interleave(): List<T> {
        // If only one array is given, return it
        if (size == 1) return first()

        val result = mutableListOf<T>()
        val iterators = map { it.iterator() }
        while (iterators.any { it.hasNext() }) {
            iterators.forEach { iterator ->
                if (iterator.hasNext()) {
                    result += iterator.next()
                }
            }
        }
        return result
    }

    /**
     * Closes all elements of iterable.
     */
    fun <T: AutoCloseable> Iterable<T>.closeAll() = forEach(AutoCloseable::close)

    internal enum class OperatingSystems {
        LINUX,
        WINDOWS,
        MACOS,
        OTHER;
    }
}