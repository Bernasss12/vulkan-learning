/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine

import dev.bernasss12.vklearn.engine.input.mouse.MouseInput
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
    val size: Size
    val width: Int get() = size.width
    val height: Int get() = size.height

    init {
        if (!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        if (!glfwVulkanSupported()) {
            throw IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)")
        }

        val videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor())!! // This is unsafe TODO make sure video mode is not null.
        size = Size(
            videoMode.width(),
            videoMode.height(),
        )

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE)

        // Create the window
        handle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
            .notNullOrThrow("Failed to create the GLFW window")

        glfwSetFramebufferSizeCallback(handle) { _, w, h ->
            size.resize(w, h)
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

    fun pollEvents() {
        glfwPollEvents()
        mouseInput.input()
    }

    fun isKeyPressed(keyCode: Int) = glfwGetKey(handle, keyCode) == GLFW_PRESS

    fun close() {
        glfwFreeCallbacks(handle)
        glfwDestroyWindow(handle)
        glfwTerminate()
    }

    private fun setShouldClose() = glfwSetWindowShouldClose(handle, true)

    fun shouldClose() = glfwWindowShouldClose(handle)


    /**
     * Holds window size and keeps track whether it has been changed
     */
    data class Size(
        private var _width: Int,
        private var _height: Int,
    ) {
        /**
         * If Size was changed and not yet cleared, this will be 'true'.
         * This is so other parts of the code that use Window can be sure the size hasn't changed or in case it did, it can be managed accordingly.
         */
        var dirty: Boolean = false; private set
        val valid: Boolean get() = _width > 0 && _height > 0

        val width: Int get() = _width
        val height: Int get() = _height

        /**
         * Sets the width and height of the Size and marks itself as dirty for further processing.
         */
        fun resize(
            width: Int,
            height: Int,
        ) {
            this.markDirty()
            this._width = width
            this._height = height
        }

        /**
         * Defines that the size has been changed.
         */
        fun markDirty() {
            dirty = true
        }

        /**
         * Defines that the size is now clean after being changed.
         */
        fun markClean() {
            dirty = false
        }
    }
}
