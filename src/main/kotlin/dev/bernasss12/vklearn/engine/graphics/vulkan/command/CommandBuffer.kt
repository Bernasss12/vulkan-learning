/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.command

import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandBufferBeginInfo
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo
import org.tinylog.kotlin.Logger

class CommandBuffer(
    private val commandPool: CommandPool,
    private var primary: Boolean,
    private val oneTimeSubmit: Boolean,
) {

    val vkCommandBuffer: VkCommandBuffer

    init {
        Logger.trace("Creating command buffer")
        MemoryStack.stackPush().use { stack ->
            val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                commandPool(commandPool.vkCommandPool)
                level(
                    if (primary) {
                        VK_COMMAND_BUFFER_LEVEL_PRIMARY
                    } else {
                        VK_COMMAND_BUFFER_LEVEL_SECONDARY
                    }
                )
                commandBufferCount(1)
            }

            val vkCommandBufferBuffer = stack.mallocPointer(1)
            vkAllocateCommandBuffers(
                commandPool.device.vkDevice,
                commandBufferAllocateInfo,
                vkCommandBufferBuffer
            ).vkAssertSuccess("Failed to allocate render command buffer")

            vkCommandBuffer = VkCommandBuffer(
                vkCommandBufferBuffer.get(0),
                commandPool.device.vkDevice
            )
        }
    }

    fun beginRecording() {
        beginRecording(null)
    }

    fun beginRecording(inheritanceInfo: InheritanceInfo?) {
        MemoryStack.stackPush().use { stack ->
            val commandBufferInfo = VkCommandBufferBeginInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                if (oneTimeSubmit) {
                    flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
                }
                if (!primary) {
                    inheritanceInfo?.let { notNullInheritanceInfo ->
                        val vkCommandBufferInheritanceInfo = VkCommandBufferInheritanceInfo.calloc(stack).apply {
                            sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
                            renderPass(notNullInheritanceInfo.vkRenderPass)
                            framebuffer(notNullInheritanceInfo.vkFrameBuffer)
                            subpass(notNullInheritanceInfo.subPass)
                        }
                        pInheritanceInfo(vkCommandBufferInheritanceInfo)
                        flags(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
                    } ?: throw RuntimeException("Secondary buffers must declare inheritance info")
                }
            }
            vkBeginCommandBuffer(
                vkCommandBuffer,
                commandBufferInfo,
            ).vkAssertSuccess("Failed to begin command buffer")
        }
    }

    fun cleanup() {
        Logger.trace("Destroying command buffer")
        vkFreeCommandBuffers(
            commandPool.device.vkDevice,
            commandPool.vkCommandPool,
            vkCommandBuffer
        )
    }

    fun endRecording() {
        vkEndCommandBuffer(
            vkCommandBuffer
        ).vkAssertSuccess("Failed to end command buffer")
    }

    fun reset() {
        vkResetCommandBuffer(vkCommandBuffer, VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)
    }

    data class InheritanceInfo(
        var vkRenderPass: Long,
        var vkFrameBuffer: Long,
        var subPass: Int,
    )

}