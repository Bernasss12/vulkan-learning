/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.util

object Utils {
    internal fun getOperatingSystem(): OperatingSystems {
        val system = System.getProperty("os.name", "generic").lowercase()
        return when {
            (system.contains("mac") || system.contains("darwin")) -> OperatingSystems.MACOS
            system.contains("nux") -> OperatingSystems.LINUX
            system.contains("win") -> OperatingSystems.WINDOWS
            else -> OperatingSystems.OTHER
        }
    }

    internal enum class OperatingSystems {
        LINUX,
        WINDOWS,
        MACOS,
        OTHER;
    }
}