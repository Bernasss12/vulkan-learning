/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLongErrorChecking
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkFenceCreateInfo

class Fence(
    private val device: Device,
    signaled: Boolean,
) {
    val vkFence: Long

    init {
        useMemoryStack { stack ->
            val flags = if (signaled) {
                VK_FENCE_CREATE_SIGNALED_BIT
            } else {
                0
            }

            val vkFenceCreateInfo = VkFenceCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                flags(flags)
            }

            vkFence = stack.vkCreateLongErrorChecking(
                "Failed to create fence"
            ) { buffer ->
                vkCreateFence(
                    device.vkDevice,
                    vkFenceCreateInfo,
                    null,
                    buffer,
                )
            }
        }
    }

    fun cleanup() {
        vkDestroyFence(
            device.vkDevice,
            vkFence,
            null,
        )
    }

    fun fenceWait() {
        vkWaitForFences(
            device.vkDevice,
            vkFence,
            true,
            Long.MAX_VALUE,
        )
    }

    fun reset() {
        vkResetFences(
            device.vkDevice,
            vkFence,
        )
    }
}