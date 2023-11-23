/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.moreThanZeroOrThrow
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateIntWithBuffer
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.NativeResource
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.tinylog.kotlin.Logger


class PhysicalDevice(
    val vkPhysicalDevice: VkPhysicalDevice,
) {

    private val vkPhysicalDeviceProperties: VkPhysicalDeviceProperties by lazy {
        // Get device properties
        VkPhysicalDeviceProperties.calloc().also { propertyBuffer ->
            vkGetPhysicalDeviceProperties(vkPhysicalDevice, propertyBuffer)
        }
    }

    private val vkMemoryProperties: VkPhysicalDeviceMemoryProperties by lazy {
        // Get device memory information and properties
        VkPhysicalDeviceMemoryProperties.calloc().also { propertyBuffer ->
            vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, propertyBuffer)
        }
    }

    private val vkPhysicalDeviceFeatures: VkPhysicalDeviceFeatures by lazy {
        // Get device memory information and properties
        VkPhysicalDeviceFeatures.calloc().also { propertyBuffer ->
            vkGetPhysicalDeviceFeatures(vkPhysicalDevice, propertyBuffer)
        }
    }

    private val vkDeviceExtensionProperties: VkExtensionProperties.Buffer by lazy {
        useMemoryStack { stack ->
            val (deviceExtensionCount, deviceExtensionCountBuffer) = stack.vkCreateIntWithBuffer("Error getting device extension count") { buffer ->
                vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as String?, buffer, null)
            }
            VkExtensionProperties.calloc(deviceExtensionCount).also { vkExtensionProperties ->
                vkEnumerateDeviceExtensionProperties(vkPhysicalDevice, null as String?, deviceExtensionCountBuffer, vkExtensionProperties)
                    .vkAssertSuccess("Error getting device extension properties")
            }
        }
    }

    val vkQueueFamilyProperties: VkQueueFamilyProperties.Buffer by lazy {
        useMemoryStack { stack ->
            val (queueFamilyPropertiesCount, queueFamilyPropertiesCountBuffer) = stack.vkCreateIntWithBuffer { buffer ->
                vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, buffer, null)
            }
            VkQueueFamilyProperties.calloc(queueFamilyPropertiesCount).also { vkQueueFamilyProperties ->
                vkGetPhysicalDeviceQueueFamilyProperties(vkPhysicalDevice, queueFamilyPropertiesCountBuffer, vkQueueFamilyProperties)
            }
        }
    }

    private val deviceName: String
        get() = vkPhysicalDeviceProperties.deviceNameString()

    companion object {
        fun createPhysicalDevice(instance: Instance, preferredDeviceName: String?): PhysicalDevice {
            Logger.debug("Selecting physical devices")
            useMemoryStack { stack ->
                // Get available devices
                val pPhysicalDevices = getPhysicalDevices(instance, stack)
                val numberPhysicalDevices = pPhysicalDevices.capacity()
                    .moreThanZeroOrThrow("No physical devices found")

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

                // Get selected device if no preferred device defaults to first in the list.
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
            val (deviceCount, deviceCountBuffer) = stack.vkCreateIntWithBuffer("Failed to get physical device count") { buffer ->
                vkEnumeratePhysicalDevices(instance.vkInstance, buffer, null)
            }
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
        vkPhysicalDeviceFeatures.freeInitialized()
        vkMemoryProperties.freeInitialized()
        vkQueueFamilyProperties.freeInitialized()
        vkDeviceExtensionProperties.freeInitialized()
        vkPhysicalDeviceProperties.freeInitialized()
    }

    private fun NativeResource.freeInitialized() {
        if (this is Lazy<*> && this.isInitialized()) {
            this.free()
        }
    }

}