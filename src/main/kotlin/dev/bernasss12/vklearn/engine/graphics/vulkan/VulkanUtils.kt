/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import org.lwjgl.vulkan.VK10.VK_SUCCESS

object VulkanUtils {

    /**
     * Hard fails in case the given code does not match 'VK_SUCCESS'
     */
    private fun vkCheckSuccess(error: Int, errorMessage: String) {
        if (error != VK_SUCCESS) {
            throw RuntimeException("$errorMessage: $error")
        }
    }

    /**
     * Calls vkCheckSuccess on error code.
     */
    fun Int.vkAssertSuccess(errorMessage: String = "Error occurred") = vkCheckSuccess(this, errorMessage)

    fun Int.moreThanZeroOrThrow(errorMessage: String): Int {
        if (this <= 0) {
            throw RuntimeException(errorMessage)
        }
        return this
    }

    fun getOperatingSystem(): OperatingSystem {
        val system = System.getProperty("os.name", "generic").lowercase()
        return when {
            (system.contains("mac") || system.contains("darwin")) -> OperatingSystem.MACOS
            system.contains("nux") -> OperatingSystem.LINUX
            system.contains("win") -> OperatingSystem.WINDOWS
            else -> OperatingSystem.OTHER
        }
    }

    enum class OperatingSystem {
        LINUX,
        WINDOWS,
        MACOS,
        OTHER;
    }
}
