/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils
import dev.bernasss12.vklearn.util.VulkanUtils.getOperatingSystem
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1
import org.tinylog.kotlin.Logger
import java.nio.IntBuffer


class Instance(
    validate: Boolean,
) {

    // Main validation layer
    private val vkLayerKhronosValidation = "VK_LAYER_KHRONOS_validation"

    // Fallback validation layer
    private val vkLayerLunargStandardValidation = "VK_LAYER_LUNARG_standard_validation"

    // Last hope validation layers
    private val vkLayerFallback = listOf(
        "VK_LAYER_GOOGLE_threading",
        "VK_LAYER_LUNARG_parameter_validation",
        "VK_LAYER_LUNARG_object_tracker",
        "VK_LAYER_LUNARG_core_validation",
        "VK_LAYER_GOOGLE_unique_objects",
    )

    // Macos portability
    private val portabilityExtension: String = "VK_KHR_portability_enumeration"

    // Define debug level bitmasks
    private val messageSeverityBitmask: Int = VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
    private val messageTypeBitmask: Int = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
            VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
            VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT

    val vkInstance: VkInstance

    private val debugUtils: VkDebugUtilsMessengerCreateInfoEXT?
    private val vkDebugHandle: Long

    init {
        Logger.debug("Creating Vulkan instance")
        MemoryStack.stackPush().use { stack ->
            // Create app info
            val appShortName = stack.UTF8("VulkanBook")
            val appInfo = VkApplicationInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                pApplicationName(appShortName)
                applicationVersion(VK_VERSION_MAJOR(1))
                pEngineName(appShortName)
                engineVersion(0)
                apiVersion(VK_API_VERSION_1_1)
            }

            // Get supported validation layers
            val validationLayers = getSupportedValidationLayers()
            val validationLayersCount = validationLayers.size
            var supportsValidation = validate
            if (validate && validationLayersCount == 0) {
                supportsValidation = false
                Logger.warn("Request validation but no supported validation layers found. Falling back to no validation.")
            }
            Logger.debug("Validation: $supportsValidation")

            // Put validation layers in allocated memory
            val requiredLayers = if (supportsValidation) {
                stack.mallocPointer(validationLayersCount).also { requiredLayers ->
                    validationLayers.forEachIndexed { index, layer ->
                        Logger.debug("Using validation layer [{}]", layer)
                        requiredLayers.put(index, stack.ASCII(layer))
                    }
                }
            } else {
                null
            }

            // Get instance extensions
            val instanceExtensions = getInstanceExtensions()

            // GLFW Extension
            val glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions().also { glfwExtensions ->
                if (glfwExtensions == null) {
                    throw RuntimeException("Failed to find GLFW platform surface extensions")
                }
            } as PointerBuffer

            // Check for mac portability extension
            val useMacOsPortability = instanceExtensions.contains(portabilityExtension) && getOperatingSystem() == VulkanUtils.OperatingSystem.MACOS

            // Find how many pointers are needed in buffer, starting with all the glfw required extensions
            var extensionsCount = glfwExtensions.remaining()
            // If validation is supported one more space is needed for vkDebugUtilsExtension
            if (supportsValidation) extensionsCount++
            if (useMacOsPortability) extensionsCount++

            // Create a buffer with the required extensions based on previously defined values
            val requiredExtensions = stack.mallocPointer(extensionsCount).apply {
                put(glfwExtensions)
                if (supportsValidation) {
                    // Add debugging extension in case validation is supported
                    put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))
                    if (useMacOsPortability) {
                        put(stack.UTF8(portabilityExtension))
                    }
                } else if (useMacOsPortability) {
                    put(stack.UTF8(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME))
                }
                flip()
            }

            // Define extension based on debugUtils and if validation is supported
            val extension: Long = if (supportsValidation) {
                debugUtils = createDebugCallBack()
                debugUtils.address()
            } else {
                debugUtils = null
                MemoryUtil.NULL
            }

            //Create instance info
            val instanceInfo = VkInstanceCreateInfo.calloc().apply {
                sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                pNext(extension)
                pApplicationInfo(appInfo)
                ppEnabledLayerNames(requiredLayers)
                ppEnabledExtensionNames(requiredExtensions)
            }

            if (useMacOsPortability) {
                instanceInfo.flags(0x00000001)
            }

            // Allocate instance in memory
            val pInstance = stack.mallocPointer(1)
            // Create the instance and verify that it created successfully
            vkCreateInstance(instanceInfo, null, pInstance)
                .vkAssertSuccess("Error creating instance")

            // Instantiate instance
            vkInstance = VkInstance(pInstance.get(0), instanceInfo)

            // Create debug utils and define handle
            vkDebugHandle = if (supportsValidation && debugUtils != null) {
                stack.mallocLong(1).let { longBuffer ->
                    vkCreateDebugUtilsMessengerEXT(vkInstance, debugUtils, null, longBuffer)
                        .vkAssertSuccess("Error creating debug utils")
                    longBuffer.get(0)
                }
            } else {
                VK_NULL_HANDLE
            }
        }
    }

    fun cleanup() {
        Logger.debug("Destroying Vulkan instance")

        // Destroy utils messenger if available
        if (vkDebugHandle != VK_NULL_HANDLE) {
            vkDestroyDebugUtilsMessengerEXT(vkInstance, vkDebugHandle, null)
        }

        // Destroy user callback
        debugUtils?.also {
            debugUtils.pfnUserCallback().free()
            debugUtils.free()
        }

        vkDestroyInstance(vkInstance, null)
    }

    /**
     * Gets available instance extensions.
     * @return set of extension names.
     */
    private fun getInstanceExtensions(): Set<String> {
        MemoryStack.stackPush().use { stack ->
            // Allocate int buffer of size 1 to store number of available extensions.
            val numExtensionBuffer = stack.callocInt(1)
            // Call vkEnumerateInstanceExtensionProperties with null pLayerName and pProperties to fill buffer with extension count.
            vkEnumerateInstanceExtensionProperties(null as String?, numExtensionBuffer, null)
                .vkAssertSuccess("Error getting extension count")
            // Get amount value from buffer
            val numExtensions = numExtensionBuffer.get(0)
            Logger.debug("Instance supports [$numExtensions] extensions")
            // Allocate extension property buffer
            val extensionPropsBuffer = VkExtensionProperties.calloc(numExtensions, stack)
            vkEnumerateInstanceExtensionProperties(null as String?, numExtensionBuffer, extensionPropsBuffer)
                .vkAssertSuccess("Error getting extension properties")

            // Map property to name. Return set of all the extension names.
            return extensionPropsBuffer.map { extensionProperty ->
                extensionProperty.extensionNameString().also { propertyName ->
                    Logger.debug("Supported layer $propertyName")
                }
            }.toSet()
        }
    }

    /**
     * Gets the supported validation layers.
     * @return list of selected validation layers.
     */
    private fun getSupportedValidationLayers(): List<String> {
        MemoryStack.stackPush().use { stack ->
            // Allocate int buffer of size 1 to store the number of available layers.
            val numLayersBuffer: IntBuffer = stack.callocInt(1)

            // Call vkEnumerateInstanceLayerProperties with null pProperties to fill IntBuffer with amount of property layers
            vkEnumerateInstanceLayerProperties(numLayersBuffer, null)
                .vkAssertSuccess("Error getting layer count")

            // Get amount of property layers from buffer
            val numLayers = numLayersBuffer[0]
            Logger.debug("Instance supports [$numLayers] layers")

            // Allocate buffer with retrieved amount of property layers
            val layerPropsBuff = VkLayerProperties.calloc(numLayers, stack)

            // Full LayerProperty buffer with Instance layers
            vkEnumerateInstanceLayerProperties(numLayersBuffer, layerPropsBuff)
                .vkAssertSuccess("Error getting layer properties")

            // Get all available layer names
            val supportedLayers = layerPropsBuff.map { layerProperty ->
                layerProperty.layerNameString().also { layerName ->
                    Logger.debug("Supported layer $layerName")
                }
            }

            // Select which layers to activate:
            return when {
                supportedLayers.contains(vkLayerKhronosValidation) -> listOf(vkLayerKhronosValidation) // Main validation layer
                supportedLayers.contains(vkLayerLunargStandardValidation) -> listOf(vkLayerLunargStandardValidation) // Fallback 1
                else -> supportedLayers.intersect(vkLayerFallback).toList() // Fallback 2
            }
        }
    }

    /**
     * Create new debug messenger
     */
    private fun createDebugCallBack() = VkDebugUtilsMessengerCreateInfoEXT.calloc().apply {
        sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
        messageSeverity(messageSeverityBitmask)
        messageType(messageTypeBitmask)
        pfnUserCallback { messageSeverity, _, pCallbackData, _ ->
            val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
            if (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT != 0) {
                Logger.info("VkDebugUtilsCallback, ${callbackData.pMessageString()}")
            } else if (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT != 0) {
                Logger.warn("VkDebugUtilsCallback, ${callbackData.pMessageString()}")
            } else if (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT != 0) {
                Logger.error("VkDebugUtilsCallback, ${callbackData.pMessageString()}")
            } else {
                Logger.debug("VkDebugUtilsCallback, ${callbackData.pMessageString()}")
            }
            return@pfnUserCallback VK_FALSE
        }
    }
}