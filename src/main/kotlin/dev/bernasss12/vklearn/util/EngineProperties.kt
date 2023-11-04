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

    private val props = Properties()

    private const val FILENAME = "engine.properties"

    private const val DEFAULT_UPS = 30
    private const val DEFAULT_VALIDATE = true
    private const val DEFAULT_REQUESTED_IMAGES = 3
    private const val DEFAULT_VSYNC = true

    // Updates per second
    val ups: Int
    val validate: Boolean
    val physicalDeviceName: String
    val requestedImages: Int
    val vsync: Boolean

    init {
        props.apply {
            try {
                File(FILENAME)
                    .inputStream().use { stream ->
                        try {
                            load(stream)
                            Logger.debug("Loading configs from disk.")
                        } catch (e: IOException) {
                            Logger.warn("Could not read $FILENAME properties file. Using defaults.")
                        }
                    }
            } catch (e: FileNotFoundException) {
                Logger.warn("File [$FILENAME] not found. Using defaults.")
            }
        }

        ups = props.getPropertyOrDefault("updates_per_second", DEFAULT_UPS, String::toInt)
        validate = props.getPropertyOrDefault("vulkan_validate", DEFAULT_VALIDATE, String::toBoolean)
        physicalDeviceName = props.getPropertyOrDefault("physical_device_name", "") { it }
        requestedImages = props.getPropertyOrDefault("requested_images", DEFAULT_REQUESTED_IMAGES, String::toInt)
        vsync = props.getPropertyOrDefault("vsync", DEFAULT_VSYNC, String::toBoolean)
    }

    fun saveOnClose() {
        props.apply {
            try {
                File(FILENAME)
                    .outputStream().use { stream ->
                        try {
                            store(stream, "Automatically saved, if you change these while the app is running it could be overwritten")
                            Logger.debug("Saving configs to disk.")
                        } catch (e: IOException) {
                            Logger.warn("Could not write $FILENAME properties file. Changed settings may be lost.")
                        }
                    }
            } catch (e: FileNotFoundException) {
                Logger.warn("File [$FILENAME] not found. Changed settings may be lost.")
            }
        }
    }

    private fun <T : Any> Properties.getPropertyOrDefault(key: String, default: T, convert: (String) -> T): T {
        return convert.invoke(
            getOrPut(key) { default.toString() }.toString()
        )
    }
}
