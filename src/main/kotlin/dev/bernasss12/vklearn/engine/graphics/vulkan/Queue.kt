/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkQueue
import org.tinylog.kotlin.Logger

open class Queue(
    device: Device,
    queueFamilyIndex: Int,
    queueIndex: Int,
) {
    val vkQueue: VkQueue

    init {
        Logger.debug("Creating queue")

        MemoryStack.stackPush().use { stack ->
            val pQueue = stack.mallocPointer(1)
            vkGetDeviceQueue(device.vkDevice, queueFamilyIndex, queueIndex, pQueue)
            val queue = pQueue[0]
            vkQueue = VkQueue(queue, device.vkDevice)
        }
    }

    fun waitIdle() {
        vkQueueWaitIdle(vkQueue)
    }

    class GraphicsQueue(device: Device, queueIndex: Int) : Queue(device, getGraphicsQueueFamilyIndex(device), queueIndex) {
        companion object {
            private fun getGraphicsQueueFamilyIndex(device: Device): Int {
                return device.physicalDevice.vkQueueFamilyProperties.indexOfFirst { queueFamilyProperty ->
                    queueFamilyProperty.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0
                }.also { index ->
                    if (index < 0) {
                        throw RuntimeException("Failed to get graphics Queue family index")
                    }
                }
            }
        }
    }
}