/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.util

import org.tinylog.kotlin.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

object EngineProperties {

    private const val FILENAME = "engine.properties"

    private const val DEFAULT_UPS = 30
    private const val VALIDATE = true

    // Updates per second
    val ups: Int
    val validate: Boolean
    val physicalDeviceName: String

    init {
        val props = Properties().apply {
            try {
                File("/$FILENAME").inputStream().use { stream ->
                    try {
                        load(stream)
                    } catch (e: IOException) {
                        Logger.error("Could not read $FILENAME properties file.")
                    }
                }
            } catch (e: FileNotFoundException) {
                Logger.error("File [$FILENAME] not found.")
            }
        }

        ups = props.getOrDefault("updates_per_second", DEFAULT_UPS).toString().toInt()
        validate = props.getOrDefault("vulkan_validate", VALIDATE.toString()).toString().toBoolean()
        physicalDeviceName = props.getOrDefault("physical_device_name", "").toString()
    }
}
