/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.moreThanZeroOrThrow
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateInt
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreatePointer
import org.lwjgl.PointerBuffer
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkQueue
import org.lwjgl.vulkan.VkSubmitInfo
import org.tinylog.kotlin.Logger
import java.nio.IntBuffer
import java.nio.LongBuffer

open class Queue(
    device: Device,
    val queueFamilyIndex: Int,
    queueIndex: Int,
) {
    val vkQueue: VkQueue

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

    fun submit(
        commandBuffers: PointerBuffer,
        waitSemaphores: LongBuffer?,
        dstStageMasks: IntBuffer?,
        signalSemaphores: LongBuffer?,
        fence: Fence
    ) {
        useMemoryStack { stack ->
            val submitInfo = VkSubmitInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                pCommandBuffers(commandBuffers)
                pSignalSemaphores(signalSemaphores)
                waitSemaphores?.let {
                    waitSemaphoreCount(waitSemaphores.capacity())
                    pWaitSemaphores(waitSemaphores)
                    pWaitDstStageMask(dstStageMasks)
                } ?: waitSemaphoreCount(0)
            }

            vkQueueSubmit(
                vkQueue,
                submitInfo,
                fence.vkFence, // If vkFence is nullable and null pass VK_NULL_HANDLE
            ).vkAssertSuccess("Failed to submit command queue")
        }
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
                }.moreThanZeroOrThrow("Failed to get Graphics Queue family index")
            }
        }
    }

    class PresentQueue(
        device: Device,
        surface: Surface,
        queueIndex: Int,
    ) : Queue(
        device,
        getPresentQueueFamilyIndex(device, surface),
        queueIndex,
    ) {
        companion object {
            private fun getPresentQueueFamilyIndex(device: Device, surface: Surface): Int {
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
                    }.moreThanZeroOrThrow("Failed to get Presentation Queue family index")
                }
            }
        }
    }
}