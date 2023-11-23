/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.OperatingSystem
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateIntWithBuffer
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK11.VK_API_VERSION_1_1
import org.tinylog.kotlin.Logger


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
        useMemoryStack { stack ->
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
            var supportValidation = validate
            if (validate && validationLayersCount == 0) {
                supportValidation = false
                Logger.warn("Request validation but no supported validation layers found. Falling back to no validation.")
            }
            Logger.debug("Validation: $supportValidation")

            // Put validation layers in allocated memory
            val requiredLayers = if (supportValidation) {
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
            val useMacOsPortability = instanceExtensions.contains(portabilityExtension) && OperatingSystem.isMacOs

            // Find how many pointers are needed in buffer, starting with all the glfw required extensions
            var extensionsCount = glfwExtensions.remaining()
            // If validation is supported, one more space is needed for vkDebugUtilsExtension
            if (supportValidation) extensionsCount++
            if (useMacOsPortability) extensionsCount++

            // Create a buffer with the required extensions based on previously defined values
            val requiredExtensions = stack.mallocPointer(extensionsCount).apply {
                put(glfwExtensions)
                if (supportValidation) {
                    // Add a debugging extension in case validation is supported
                    put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))
                    if (useMacOsPortability) {
                        put(stack.UTF8(portabilityExtension))
                    }
                } else if (useMacOsPortability) {
                    put(stack.UTF8(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME))
                }
                flip()
            }

            // Define an extension based on debugUtils and if validation is supported
            val extension: Long = if (supportValidation) {
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
            vkDebugHandle = if (supportValidation && debugUtils != null) {
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
        useMemoryStack { stack ->
            // Allocate int buffer of size 1 to store the number of available extensions.
            val (numExtensions, numExtensionBuffer) = stack.vkCreateIntWithBuffer("Error getting extension count") { buffer ->
                vkEnumerateInstanceExtensionProperties(null as String?, buffer, null)
            }
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
        useMemoryStack { stack ->
            // Allocate int buffer of size 1 to store the number of available layers.
            val (numLayers, numLayersBuffer) = stack.vkCreateIntWithBuffer("Error getting layer count") { buffer ->
                vkEnumerateInstanceLayerProperties(buffer, null)
            }
            Logger.debug("Instance supports [$numLayers] layers")

            // Allocate buffer with retrieved number of property layers
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