/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.command

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import org.tinylog.kotlin.Logger

class CommandPool(
    val device: Device, queueFamilyIndex: Int
) {
    val vkCommandPool: Long

    init {
        Logger.debug("Creating Vulkan CommandPool")
        MemoryStack.stackPush().use { stack ->
            val commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                queueFamilyIndex(queueFamilyIndex)
            }

            val vkCommandPoolBuffer = stack.mallocLong(1)
            vkCreateCommandPool(
                device.vkDevice,
                commandPoolCreateInfo,
                null,
                vkCommandPoolBuffer
            ).vkAssertSuccess("Failed to create command pool")

            vkCommandPool = vkCommandPoolBuffer.get(0)
        }
    }

    fun cleanup() {
        vkDestroyCommandPool(device.vkDevice, vkCommandPool, null)
    }
}