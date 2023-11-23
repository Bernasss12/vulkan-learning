/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine

import dev.bernasss12.vklearn.engine.input.MouseInput
import dev.bernasss12.vklearn.util.VulkanUtils.notNullOrThrow
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryUtil

class Window(
    title: String,
    keyCallback: GLFWKeyCallbackI? = null
) {

    private val mouseInput: MouseInput
    val handle: Long
    var height: Int
    var width: Int
    private var resized: Boolean = false

    init {
        resized = false

        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        if (!glfwVulkanSupported()) {
            throw IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)")
        }

        val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!! // This is unsafe TODO make sure video mode is not null.
        width = videoMode.width()
        height = videoMode.height()

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE)

        // Create the window
        handle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
            .notNullOrThrow("Failed to create the GLFW window")

        glfwSetFramebufferSizeCallback(handle) { _, w, h ->
            resize(w, h)
        }

        glfwSetKeyCallback(handle) { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                setShouldClose()
            }

            keyCallback?.invoke(
                window,
                key,
                scancode,
                action,
                mods
            )
        }

        mouseInput = MouseInput(handle)
    }

    private fun resize(width: Int, height: Int) {
        resized = true
        this.width = width
        this.height = height
    }

    fun pollEvents() {
        glfwPollEvents()
        mouseInput.input()
    }

    fun isKeyPressed(keyCode: Int) = glfwGetKey(handle, keyCode) == GLFW_PRESS

    fun resetResized() {
        resized = false
    }

    fun cleanup() {
        glfwFreeCallbacks(handle)
        glfwDestroyWindow(handle)
        glfwTerminate()
    }

    private fun setShouldClose() = glfwSetWindowShouldClose(handle, true)

    fun shouldClose() = glfwWindowShouldClose(handle)
}
