/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.util

import dev.bernasss12.vklearn.engine.graphics.vulkan.PhysicalDevice
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Struct
import org.lwjgl.system.StructBuffer
import org.lwjgl.vulkan.VK10.VK_MAX_MEMORY_TYPES
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.tinylog.kotlin.Logger
import java.nio.IntBuffer
import java.nio.LongBuffer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Assortment of methods that help avoid repeated code.
 * @since 0.0.1
 */
object VulkanUtils {

    /**
     * Hard fails in case the given code does not match 'VK_SUCCESS'
     * @since 0.0.1
     */
    private fun vkCheckSuccess(error: Int, errorMessage: String) {
        if (error != VK_SUCCESS) {
            throw RuntimeException("$errorMessage: $error")
        }
    }

    /**
     * Calls vkCheckSuccess on error code.
     * @since 0.0.1
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
     * @since 0.0.1
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
     * @since 0.0.1
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
     * @since 0.0.1
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
     * @param name default is null, but if an error message is given, the result of the operation will be checked.
     * @param block this is the code that will be run that will take the pre-allocated long buffer and hopefully fill it with the desired value.
     * @return the first, and only, element of the buffer.
     * @since 0.0.1
     */
    inline fun MemoryStack.vkCreateLong(
        name: String? = null,
        block: (buffer: LongBuffer) -> Any
    ): Long {
        val longBuffer: LongBuffer = this.mallocLong(1)
        // Debug creation of named handle.
        name?.let {
            Logger.trace("Creating $name")
        }
        block.invoke(longBuffer).also { result ->
            name?.let {
                (result as? Int)?.vkAssertSuccess("Failed to create $name")
            }
        }
        return longBuffer.get(0).also { handle ->
            name?.let {
                Logger.trace("Created $name successfully [$handle]")
            }
        }
    }

    /**
     * Use this method to automatically allocate a pointer buffer of size 1, run the given code, and return the resulting long value.
     * @param error default is null, but if an error message is given, the result of the operation will be checked.
     * @param block this is the code that will be run that will take the pre-allocated pointer buffer and hopefully fill it with the desired value.
     * @return the first, and only, element of the buffer.
     * @since 0.0.1
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
     * Grab the first element in Iterable<> and apply block on it, and return the original iterable itself.
     * @since 0.0.1
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <P, T : Iterable<P>> T.applyOnFirst(block: P.() -> Unit): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        firstOrNull()
            ?.block()
            ?: throw IllegalStateException("There has to be something in the buffer for this to work.")
        return this
    }

    /**
     * Grab the nth element and apply block on it, and return the StructBuffer itself.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <P: Struct, T : StructBuffer<P, T>> T.applyOn(index: Int, block: P.() -> Unit): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        try {
            get(index).block()
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("There has to be something in the $index position for this to work.")
        }
        return this
    }

    /**
     * Iterates through all the elements in the buffer and applies block on each.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <P: Struct, T : StructBuffer<P, T>> T.applyOnEach(block: P.() -> Unit): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        forEach { it.block() }
        return this
    }

    /**
     * Zips the Struct buffer with the given Iterable and runs apply on each Struct with the zipped being provided.
     * As per the zip inner workings if the Iterables (StructBuffer is an Iterable) don't have the same size, the smallest becomes the zipped size.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <P: Struct, T : StructBuffer<P, T>, V> T.applyOnEachWith(with: Iterable<V>, block: P.(V) -> Unit): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        zip(with).forEach { (currentThis, currentWith) -> currentThis.block(currentWith) }
        return this
    }

    /**
     * Applies on each Struct providing the constructor with the current index.
     */
    @OptIn(ExperimentalContracts::class)
    inline fun <P: Struct, T : StructBuffer<P, T>> T.applyOnEachIndexed(block: P.(Int) -> Unit): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        withIndex().forEach { block.invoke(it.value, it.index) }
        return this
    }

    /**
     * Pushes the memory stack and passes it to the runnable block.
     * The runnable block can return a value but that's just because the original use method also does it.
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

    /**
     * Finds the index of the corresponding memoryType by its bits and propertyFlags.
     * @throws RuntimeException when no memory type matches the filter.
     */
    fun memoryTypeFromProperties(physicalDevice: PhysicalDevice, memoryTypeBits: Int, requirementMask: Int): Int {
        return (0..VK_MAX_MEMORY_TYPES).zip(physicalDevice.vkMemoryProperties.memoryTypes()).firstOrNull { (index, memoryType) ->
            (((memoryTypeBits shr index) and 1) == 1 && (memoryType.propertyFlags() and requirementMask) == requirementMask)
        }
            ?.first
            ?: throw RuntimeException("Failed to find memory type")
    }
}
