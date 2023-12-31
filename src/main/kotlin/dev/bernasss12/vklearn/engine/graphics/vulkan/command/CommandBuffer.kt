/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.command

import dev.bernasss12.vklearn.util.VulkanUtils.notNullOrThrow
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreatePointer
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
) : AutoCloseable {

    val vkCommandBuffer: VkCommandBuffer

    init {
        Logger.debug("Creating command buffer")
        useMemoryStack { stack ->
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

            val vkCommandBufferPointer = stack.vkCreatePointer(
                "Failed to allocate render command buffer"
            ) { buffer ->
                vkAllocateCommandBuffers(
                    commandPool.device.vkDevice,
                    commandBufferAllocateInfo,
                    buffer
                )
            }

            vkCommandBuffer = VkCommandBuffer(
                vkCommandBufferPointer,
                commandPool.device.vkDevice
            )
        }
    }

    fun beginRecording() {
        beginRecording(null)
    }

    private fun beginRecording(inheritanceInfo: InheritanceInfo?) {
        useMemoryStack { stack ->
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
                    }.notNullOrThrow("Secondary buffers must declare inheritance info")
                }
            }
            vkBeginCommandBuffer(
                vkCommandBuffer,
                commandBufferInfo,
            ).vkAssertSuccess("Failed to begin command buffer")
        }
    }

    fun endRecording() {
        vkEndCommandBuffer(
            vkCommandBuffer
        ).vkAssertSuccess("Failed to end command buffer")
    }

    fun reset() {
        vkResetCommandBuffer(vkCommandBuffer, VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)
    }

    override fun close() {
        Logger.debug("Destroying command buffer")
        vkFreeCommandBuffers(
            commandPool.device.vkDevice,
            commandPool.vkCommandPool,
            vkCommandBuffer
        )
    }
}