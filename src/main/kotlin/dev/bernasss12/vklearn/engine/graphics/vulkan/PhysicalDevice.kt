/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.engine.graphics.vulkan.VulkanUtils.vkAssertSuccess
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.tinylog.kotlin.Logger


class PhysicalDevice(val vkPhysicalDevice: VkPhysicalDevice) {

    private val vkPhysicalDeviceProperties: VkPhysicalDeviceProperties by lazy {
        // Get device properties
        VkPhysicalDeviceProperties.calloc().use { propertyBuffer ->
            vkGetPhysicalDeviceProperties(vkPhysicalDevice, propertyBuffer)
            propertyBuffer
        }
    }

    private val vkMemoryProperties: VkPhysicalDeviceMemoryProperties by lazy {
        // Get device memory information and properties
        VkPhysicalDeviceMemoryProperties.calloc().use { propertyBuffer ->
            vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, propertyBuffer)
            propertyBuffer
        }
    }

    private val vkPhysicalDeviceFeatures: VkPhysicalDeviceFeatures by lazy {
        // Get device memory information and properties
        VkPhysicalDeviceFeatures.calloc().use { propertyBuffer ->
            vkGetPhysicalDeviceFeatures(vkPhysicalDevice, propertyBuffer)
            propertyBuffer
        }
    }

    private val vkDeviceExtensionProperties: VkExtensionProperties.Buffer by lazy {
        MemoryStack.stackPush().use { stack ->
            val deviceExtensionCountBuffer = stack.mallocInt(1)
            vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as String?, deviceExtensionCountBuffer, null)
                .vkAssertSuccess("Error getting device extension count")
            val deviceExtensionCount = deviceExtensionCountBuffer[0]
            VkExtensionProperties.calloc(deviceExtensionCount).use { vkExtensionProperties ->
                vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as String?, deviceExtensionCountBuffer, vkExtensionProperties)
                    .vkAssertSuccess("Error getting device extension properties")
                vkExtensionProperties
            }
        }
    }

    val vkQueueFamilyProperties: VkQueueFamilyProperties.Buffer by lazy {
        MemoryStack.stackPush().use { stack ->
            val queueFamilyPropertiesCountBuffer = stack.mallocInt(1)
            vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, queueFamilyPropertiesCountBuffer, null)
            val queueFamilyPropertiesCount = queueFamilyPropertiesCountBuffer[0]
            VkQueueFamilyProperties.calloc(queueFamilyPropertiesCount).use { vkQueueFamilyProperties ->
                vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, queueFamilyPropertiesCountBuffer, vkQueueFamilyProperties)
                vkQueueFamilyProperties
            }
        }
    }

    private val deviceName: String
        get() = vkPhysicalDeviceProperties.deviceNameString()

    companion object {
        fun createPhysicalDevice(instance: Instance, preferredDeviceName: String?): PhysicalDevice {
            Logger.debug("Selecting physical devices")
            MemoryStack.stackPush().use { stack ->
                // Get available devices
                val pPhysicalDevices = getPhysicalDevices(instance, stack)
                val numberPhysicalDevices = pPhysicalDevices.capacity()
                if (numberPhysicalDevices <= 0) throw RuntimeException("No physical devices found.")

                // Gather available devices
                val devices = (0..<numberPhysicalDevices).mapNotNull { index ->
                    val vkPhysicalDevice = VkPhysicalDevice(pPhysicalDevices[index], instance.vkInstance)
                    val physicalDevice = PhysicalDevice(vkPhysicalDevice)
                    val deviceName = physicalDevice.deviceName
                    if (physicalDevice.hasGraphicsQueueFamily() && physicalDevice.hasKHRSwapChainExtension()) {
                        Logger.debug("Device [$deviceName] supports required extensions")
                        return@mapNotNull physicalDevice
                    } else {
                        Logger.debug("Device [$deviceName] does not support required extensions")
                        physicalDevice.cleanup()
                        return@mapNotNull null
                    }
                }

                // Get selected device, if no preferred device default to first in list.
                val selectedPhysicalDevice = preferredDeviceName?.let { preferredDeviceName ->
                    devices.find { it.deviceName == preferredDeviceName }
                } ?: devices.firstOrNull() ?: throw RuntimeException("No suitable physical devices found")
                Logger.debug("Selected device: [${selectedPhysicalDevice.deviceName}]")

                // Clean up all unused devices
                devices.filterNot { device -> device == selectedPhysicalDevice }.forEach(PhysicalDevice::cleanup)

                return selectedPhysicalDevice
            }
        }

        private fun getPhysicalDevices(instance: Instance, stack: MemoryStack): PointerBuffer {
            // Get number of devices
            val deviceCountBuffer = stack.mallocInt(1)
            vkEnumeratePhysicalDevices(instance.vkInstance, deviceCountBuffer, null)
                .vkAssertSuccess("Failed to get physical device count")
            val deviceCount = deviceCountBuffer.get(0)
            Logger.debug("Detected $deviceCount physical device(s)")
            // Allocate physical device property buffer
            val physicalDevicesBuffer = stack.mallocPointer(deviceCount)
            vkEnumeratePhysicalDevices(instance.vkInstance, deviceCountBuffer, physicalDevicesBuffer)
                .vkAssertSuccess("Failed to get physical devices")
            return physicalDevicesBuffer
        }
    }

    private fun hasKHRSwapChainExtension(): Boolean =
        vkDeviceExtensionProperties.any { it.extensionNameString() == KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME }

    private fun hasGraphicsQueueFamily(): Boolean =
        vkQueueFamilyProperties.any { (it.queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0 }

    fun cleanup() {
        if (Logger.isDebugEnabled()) {
            Logger.debug("Destroying physical device [$deviceName]")
        }
        vkPhysicalDeviceFeatures.free()
        vkMemoryProperties.free()
        vkQueueFamilyProperties.free()
        vkDeviceExtensionProperties.free()
        vkPhysicalDeviceProperties.free()
    }

}