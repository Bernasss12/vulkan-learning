/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreatePointer
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements

class VulkanBuffer(
    val device: Device,
    size: Long,
    usage: Int,
    requirementsMask: Int,
) : AutoCloseable {

    val vkBuffer: Long
    private val memory: Long
    private val pointerBuffer: PointerBuffer = MemoryUtil.memAllocPointer(1)
    private val allocationSize: Long

    val requestedSize = size

    private var _mappedMemory: Long? = null

    init {
        useMemoryStack { stack ->
            val vkBufferCreateInfo = VkBufferCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                size(size)
                usage(usage)
                sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            }

            vkBuffer = stack.vkCreateLong("Failed to create buffer") { buffer ->
                vkCreateBuffer(
                    device.vkDevice,
                    vkBufferCreateInfo,
                    null,
                    buffer
                )
            }

            val memoryRequirements = VkMemoryRequirements.malloc(stack)
            vkGetBufferMemoryRequirements(device.vkDevice, vkBuffer, memoryRequirements)

            val memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                allocationSize(memoryRequirements.size())
                memoryTypeIndex(
                    VulkanUtils.memoryTypeFromProperties(
                        device.physicalDevice,
                        memoryRequirements.memoryTypeBits(),
                        requirementsMask,
                    )
                )
            }

            memory = stack.vkCreateLong("Failed to allocate memory") { buffer ->
                vkAllocateMemory(
                    device.vkDevice,
                    memoryAllocateInfo,
                    null,
                    buffer,
                )
            }

            allocationSize = memoryAllocateInfo.allocationSize()

            vkBindBufferMemory(
                device.vkDevice,
                vkBuffer,
                memory,
                0,
            ).vkAssertSuccess("Failed to bind buffer memory")
        }
    }

    fun map(): Long {
        return _mappedMemory ?: MemoryStack.stackPush().vkCreatePointer("Failed to map buffer") {
            vkMapMemory(
                device.vkDevice,
                memory,
                0,
                allocationSize,
                0,
                it
            )
        }.also {
            _mappedMemory = it
        }
    }

    fun unMap() {
        if (_mappedMemory != null) {
            vkUnmapMemory(
                device.vkDevice,
                memory
            )
            _mappedMemory = null
        }
    }

    /**
     * Runs the given block of code giving access to the buffer and the handle for the mapped memory.
     */
    inline fun use(block: (VulkanBuffer, mappedMemory: Long) -> Unit): VulkanBuffer {
        block(this, map())
        unMap()
        return this
    }

    override fun close() {
        MemoryUtil.memFree(pointerBuffer)
        vkDestroyBuffer(device.vkDevice, vkBuffer, null)
        vkFreeMemory(device.vkDevice, memory, null)
    }
}