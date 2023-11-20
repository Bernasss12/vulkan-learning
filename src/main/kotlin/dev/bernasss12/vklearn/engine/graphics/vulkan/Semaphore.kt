/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

class Semaphore(
    private val device: Device
) {
    val vkSemaphore: Long

    init {
        MemoryStack.stackPush().use { stack ->
            val semaphoreCreateInfoBuffer = VkSemaphoreCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
            }

            //TODO check if this works
            vkSemaphore = stack.vkCreateLong { buffer ->
                vkCreateSemaphore(
                    device.vkDevice,
                    semaphoreCreateInfoBuffer,
                    null,
                    buffer
                ).vkAssertSuccess("Failed to create semaphore")
            }
        }
    }

    fun cleanup() {
        vkDestroySemaphore(
            device.vkDevice,
            vkSemaphore,
            null,
        )
    }
}