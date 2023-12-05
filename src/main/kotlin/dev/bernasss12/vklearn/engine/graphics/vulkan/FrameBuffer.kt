/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import java.nio.LongBuffer

class FrameBuffer(
    private val device: Device,
    width: Int,
    height: Int,
    pAttachments: LongBuffer,
    renderPass: Long
) : AutoCloseable {
    val vkFrameBuffer: Long

    init {
        useMemoryStack { stack ->
            val frameBufferCreateInfo = VkFramebufferCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                pAttachments(pAttachments)
                width(width)
                height(height)
                layers(1)
                renderPass(renderPass)
            }

            vkFrameBuffer = stack.vkCreateLong("frame buffer") { buffer ->
                vkCreateFramebuffer(
                    device.vkDevice,
                    frameBufferCreateInfo,
                    null,
                    buffer
                )
            }
        }
    }

    override fun close() {
        vkDestroyFramebuffer(device.vkDevice, vkFrameBuffer, null)
    }
}