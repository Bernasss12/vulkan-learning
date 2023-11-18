/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import java.nio.LongBuffer

class FrameBuffer(private val device: Device, width: Int, height: Int, pAttachments: LongBuffer, renderPass: Long) {
    val vkFrameBuffer: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val frameBufferCreateInfo = VkFramebufferCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                pAttachments(pAttachments)
                width(width)
                height(height)
                layers(1)
                renderPass(renderPass)
            }

            val vkFramebufferBuffer = stack.mallocLong(1)
            vkCreateFramebuffer(
                device.vkDevice,
                frameBufferCreateInfo,
                null,
                vkFramebufferBuffer
            ).vkAssertSuccess("Failed to create FrameBuffer")

            vkFrameBuffer = vkFramebufferBuffer.get(0)
        }
    }

    fun cleanup() {
        vkDestroyFramebuffer(device.vkDevice, vkFrameBuffer, null)
    }
}