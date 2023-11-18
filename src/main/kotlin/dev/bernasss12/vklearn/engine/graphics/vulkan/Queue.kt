/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateInt
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreatePointer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkQueue
import org.tinylog.kotlin.Logger

open class Queue(
    device: Device,
    val queueFamilyIndex: Int,
    queueIndex: Int,
) {
    private val vkQueue: VkQueue

    init {
        Logger.debug("Creating queue")

        useMemoryStack { stack ->
            val queue = stack.vkCreatePointer { buffer ->
                vkGetDeviceQueue(device.vkDevice, queueFamilyIndex, queueIndex, buffer)
            }
            vkQueue = VkQueue(queue, device.vkDevice)
        }
    }

    fun waitIdle() {
        vkQueueWaitIdle(vkQueue)
    }

    class GraphicsQueue(
        device: Device,
        queueIndex: Int
    ) : Queue(
        device,
        getGraphicsQueueFamilyIndex(device),
        queueIndex,
    ) {
        companion object {
            private fun getGraphicsQueueFamilyIndex(device: Device): Int {
                return device.physicalDevice.vkQueueFamilyProperties.indexOfFirst { queueFamilyProperty ->
                    queueFamilyProperty.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0
                }.also { index ->
                    if (index < 0) {
                        throw RuntimeException("Failed to get Graphics Queue family index")
                    }
                }
            }
        }
    }

    class PresentQueue(
        device: Device,
        surface: Surface,
        queueIndex: Int,
    ): Queue(
        device,
        getPresentQueuFamilyIndex(device, surface),
        queueIndex,
    ) {
        companion object {
            private fun getPresentQueuFamilyIndex(device: Device, surface: Surface): Int {
                useMemoryStack { stack ->
                    return device.physicalDevice.vkQueueFamilyProperties.withIndex().indexOfFirst { indexedVkQueueFamilyProperty ->
                        stack.vkCreateInt { buffer ->
                            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(
                                device.physicalDevice.vkPhysicalDevice,
                                indexedVkQueueFamilyProperty.index,
                                surface.vkSurface,
                                buffer,
                            )
                        } == VK_TRUE
                    }.also { index ->
                        if (index < 0) {
                            throw RuntimeException("Failed to get Presentation Queue family index")
                        }
                    }
                }
            }
        }
    }
}