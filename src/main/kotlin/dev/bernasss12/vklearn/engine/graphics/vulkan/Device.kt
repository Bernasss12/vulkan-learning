/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.OperatingSystem
import dev.bernasss12.vklearn.util.VulkanUtils.applyOnEachIndexed
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateIntWithBuffer
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreatePointer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.tinylog.kotlin.Logger

class Device(
    val physicalDevice: PhysicalDevice,
) : AutoCloseable {

    val vkDevice: VkDevice

    init {
        Logger.debug("Creating device")
        useMemoryStack { stack ->
            // Define required extensions
            val deviceExtensions = getDeviceExtensions(stack)
            val usePortability = deviceExtensions.contains(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME) && OperatingSystem.isMacOs

            val extensions = stack.mallocPointer(
                if (usePortability) 2 else 1
            ).apply {
                put(stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME))
                if (usePortability) {
                    put(stack.ASCII(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME))
                }
                flip()
            }


            // Set up required features
            val features = VkPhysicalDeviceFeatures.calloc(stack)

            // Enable all queue features
            val queueFamilyProperties = physicalDevice.vkQueueFamilyProperties
            val queueFamilyCount = queueFamilyProperties.capacity()
            val queueCreateInfoBuffer = VkDeviceQueueCreateInfo.calloc(queueFamilyCount, stack).applyOnEachIndexed { index ->
                val priorities = stack.callocFloat(queueFamilyProperties.get(index).queueCount())
                sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                queueFamilyIndex(index)
                pQueuePriorities(priorities)
            }

            // Create device info
            val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                ppEnabledExtensionNames(extensions)
                pEnabledFeatures(features)
                pQueueCreateInfos(queueCreateInfoBuffer)
            }

            // Create a device from info
            val pointer = stack.vkCreatePointer("Failed to create device") { buffer ->
                vkCreateDevice(
                    physicalDevice.vkPhysicalDevice,
                    deviceCreateInfo,
                    null,
                    buffer
                )
            }
            vkDevice = VkDevice(pointer, physicalDevice.vkPhysicalDevice, deviceCreateInfo)
        }
    }

    private fun getDeviceExtensions(stack: MemoryStack): Set<String> {
        // Get device extensions count
        val (extensionCount, extensionCountBuffer) = stack.vkCreateIntWithBuffer { buffer ->
            vkEnumerateDeviceExtensionProperties(
                physicalDevice.vkPhysicalDevice,
                null as String?,
                buffer,
                null
            )
        }
        Logger.debug("Device supports [$extensionCount] extension")

        // Get actual device extensions
        return VkExtensionProperties.calloc(extensionCount, stack).let { propertyBuffer ->
            vkEnumerateDeviceExtensionProperties(
                physicalDevice.vkPhysicalDevice,
                null as String?,
                extensionCountBuffer,
                propertyBuffer
            )
            propertyBuffer.map { extensionProperty ->
                val extensionName = extensionProperty.extensionNameString()
                Logger.debug("Supported device extension [$extensionName]")
                extensionName
            }.toSet()
        }
    }


    override fun close() {
        Logger.debug("Destroying Vulkan device")
        vkDestroyDevice(vkDevice, null)
    }

    fun waitIdle() {
        vkDeviceWaitIdle(vkDevice)
    }
}