/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.KHRSurface
import org.tinylog.kotlin.Logger

class Surface(
    private val physicalDevice: PhysicalDevice,
    windowHandle: Long,
) : AutoCloseable {

    val vkSurface: Long

    init {
        Logger.debug("Creating Vulkan surface")
        useMemoryStack { stack ->
            vkSurface = stack.vkCreateLong { buffer ->
                GLFWVulkan.glfwCreateWindowSurface(
                    physicalDevice.vkPhysicalDevice.instance,
                    windowHandle,
                    null,
                    buffer
                )
            }
        }
    }

    override fun close() {
        Logger.debug("Destroying Vulkan surface")
        KHRSurface.vkDestroySurfaceKHR(
            physicalDevice.vkPhysicalDevice.instance,
            vkSurface,
            null
        )
    }

}