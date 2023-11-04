/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import org.tinylog.kotlin.Logger

class Device(
    val physicalDevice: PhysicalDevice,
) {

    val vkDevice: VkDevice

    init {
        Logger.debug("Creating device")

        MemoryStack.stackPush().use { stack ->
            // Define required extensions
            val deviceExtensions = getDeviceExtensions()
            val usePortability = deviceExtensions.contains(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME) &&
                    VulkanUtils.getOperatingSystem() == VulkanUtils.OperatingSystem.MACOS
            val extensionCount = if (usePortability) 2 else 1
            val extensions = stack.mallocPointer(extensionCount)
            extensions.put(stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME))
            if (usePortability) {
                extensions.put(stack.ASCII(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME))
            }
            extensions.flip()

            // Set up required features
            val features = VkPhysicalDeviceFeatures.calloc(stack)

            // Enable all queue features
            val queueFamilyProperties = physicalDevice.vkQueueFamilyProperties
            val queueFamilyCount = queueFamilyProperties.capacity()
            val queueCreateInfoBuffer = VkDeviceQueueCreateInfo.calloc(queueFamilyCount, stack)
            queueCreateInfoBuffer.forEachIndexed { index, queue ->
                val priorities = stack.callocFloat(queueFamilyProperties.get(index).queueCount())
                queue.apply {
                    sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    queueFamilyIndex(index)
                    pQueuePriorities(priorities)
                }
            }

            // Create device info
            val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                ppEnabledExtensionNames(extensions)
                pEnabledFeatures(features)
                pQueueCreateInfos(queueCreateInfoBuffer)
            }

            // Create device from info
            val pointer = stack.mallocPointer(1)
            vkCreateDevice(physicalDevice.vkPhysicalDevice, deviceCreateInfo, null, pointer)
                .vkAssertSuccess("Failed to create device")
            vkDevice = VkDevice(pointer[0], physicalDevice.vkPhysicalDevice, deviceCreateInfo)
        }
    }

    private fun getDeviceExtensions(): Set<String> {
        MemoryStack.stackPush().use { stack ->
            // Get device extensions count
            val extensionCountBuffer = stack.mallocInt(1)
            vkEnumerateDeviceExtensionProperties(physicalDevice.vkPhysicalDevice, null as String?, extensionCountBuffer, null)
            val extensionCount = extensionCountBuffer[0]
            Logger.debug("Device supports [$extensionCount] extension")

            // Get actual device extensions
            val propertiesBuffer = VkExtensionProperties.calloc(extensionCount, stack)
            vkEnumerateDeviceExtensionProperties(physicalDevice.vkPhysicalDevice, null as String?, extensionCountBuffer, propertiesBuffer)
            return propertiesBuffer.map { extensionProperty ->
                val extensionName = extensionProperty.extensionNameString()
                Logger.debug("Supported device extension [$extensionName]")
                extensionName
            }.toSet()
        }
    }

    fun cleanup() {
        Logger.debug("Destroying Vulkan device")
        vkDestroyDevice(vkDevice, null)
    }

    fun waitIdle() {
        vkDeviceWaitIdle(vkDevice)
    }
}