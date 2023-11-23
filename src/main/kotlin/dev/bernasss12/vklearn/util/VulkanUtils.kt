/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.util

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import java.nio.IntBuffer
import java.nio.LongBuffer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object VulkanUtils {

    /**
     * Hard fails in case the given code does not match 'VK_SUCCESS'
     */
    private fun vkCheckSuccess(error: Int, errorMessage: String) {
        if (error != VK_SUCCESS) {
            throw RuntimeException("$errorMessage: $error")
        }
    }

    /**
     * Calls vkCheckSuccess on error code.
     */
    fun Int.vkAssertSuccess(errorMessage: String = "Error occurred") = vkCheckSuccess(this, errorMessage)

    fun Int.moreThanZeroOrThrow(errorMessage: String): Int {
        if (this < 0) {
            throw RuntimeException(errorMessage)
        }
        return this
    }

    /**
     * This method checks for null objects as well as Long handles with "NULL" value.
     */
    fun <T> T?.notNullOrThrow(errorMessage: String): T = when {
        (this is Long && (this as Long) != MemoryUtil.NULL) || this != null -> this
        else -> throw RuntimeException(errorMessage)
    }

    /*
     * Memory stack related stuff
     */

    /**
     * Use this method to automatically allocate an int buffer of size 1, run the given code, and return the resulting int value.
     * @param error default is null, but if an error message is given, the result of the operation will be checked.
     * @param block this is the code that will be run that will take the pre-allocated int buffer and hopefully fill it with the desired value.
     * @return the first, and only, element of the buffer.
     */
    inline fun MemoryStack.vkCreateInt(
        error: String? = null,
        block: (buffer: IntBuffer) -> Any
    ): Int {
        val intBuffer: IntBuffer = this.mallocInt(1)
        block.invoke(intBuffer).also { result ->
            error?.let {
                (result as? Int)?.vkAssertSuccess(error)
            }
        }
        return intBuffer.get(0)
    }

    /**
     * Use this method to automatically allocate an int buffer of size 1, run the given code, and return the resulting int value and the buffer itself.
     * @param error default is null, but if an error message is given, the result of the operation will be checked.
     * @param block this is the code that will be run that will take the pre-allocated int buffer and hopefully fill it with the desired value.
     * @return the first, and only, element of the buffer.
     */
    inline fun MemoryStack.vkCreateIntWithBuffer(
        error: String? = null,
        block: (buffer: IntBuffer) -> Any
    ): Pair<Int, IntBuffer> {
        val intBuffer: IntBuffer = this.mallocInt(1)
        block.invoke(intBuffer).also { result ->
            error?.let {
                (result as? Int)?.vkAssertSuccess(error)
            }
        }
        return intBuffer.get(0) to intBuffer
    }

    /**
     * Use this method to automatically allocate a long buffer of size 1, run the given code, and return the resulting long value.
     * @param error default is null, but if an error message is given, the result of the operation will be checked.
     * @param block this is the code that will be run that will take the pre-allocated long buffer and hopefully fill it with the desired value.
     * @return the first, and only, element of the buffer.
     */
    inline fun MemoryStack.vkCreateLong(
        error: String? = null,
        block: (buffer: LongBuffer) -> Any
    ): Long {
        val longBuffer: LongBuffer = this.mallocLong(1)
        block.invoke(longBuffer).also { result ->
            error?.let {
                (result as? Int)?.vkAssertSuccess(error)
            }
        }
        return longBuffer.get(0)
    }

    /**
     * Use this method to automatically allocate a pointer buffer of size 1, run the given code, and return the resulting long value.
     * @param error default is null, but if an error message is given, the result of the operation will be checked.
     * @param block this is the code that will be run that will take the pre-allocated pointer buffer and hopefully fill it with the desired value.
     * @return the first, and only, element of the buffer.
     */
    inline fun MemoryStack.vkCreatePointer(
        error: String? = null,
        block: (buffer: PointerBuffer) -> Any
    ): Long {
        val pointerBuffer: PointerBuffer = this.mallocPointer(1)
        block.invoke(pointerBuffer).also { result ->
            error?.let {
                (result as? Int)?.vkAssertSuccess(error)
            }
        }
        return pointerBuffer.get(0)
    }

    /**
     * Allows to create a buffer, grab the first element and apply something on it, and return the original buffer itself.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <P, T : Iterable<P>> T.applyInFirst(block: P.() -> Unit): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        firstOrNull()
            ?.block()
            ?: throw Exception("There has to be something in the buffer for this to work.")
        return this
    }

    /**
     * Pushes the memory stack and passes it to the runnable block.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <T> useMemoryStack(runnable: (stack: MemoryStack) -> T): T {
        contract {
            callsInPlace(runnable, InvocationKind.EXACTLY_ONCE)
        }
        return MemoryStack.stackPush().use { stack ->
            runnable(stack)
        }
    }
}
