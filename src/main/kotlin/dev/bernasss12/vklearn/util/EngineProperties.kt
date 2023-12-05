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
    private const val DEFAULT_SHADER_RECOMPILATION = false
    private const val DEFAULT_FOV = 60f
    private const val DEFAULT_Z_NEAR = 1f
    private const val DEFAULT_Z_FAR = 100f

    // Updates per second
    var ups: Int
    var validate: Boolean
    var physicalDeviceName: String
    var requestedImages: Int
    var vsync: Boolean
    var shaderRecompilation: Boolean
    var fov: Float
    var zNear: Float
    var zFar: Float

    init {
        load()

        val loadedProperties = props.size

        ups = props.getPropertyOrDefault("updates_per_second", DEFAULT_UPS, String::toInt)
        validate = props.getPropertyOrDefault("vulkan_validate", DEFAULT_VALIDATE, String::toBoolean)
        physicalDeviceName = props.getPropertyOrDefault("physical_device_name", "") { it }
        requestedImages = props.getPropertyOrDefault("requested_images", DEFAULT_REQUESTED_IMAGES, String::toInt)
        vsync = props.getPropertyOrDefault("vsync", DEFAULT_VSYNC, String::toBoolean)
        shaderRecompilation = props.getPropertyOrDefault("shader_recompilation", DEFAULT_SHADER_RECOMPILATION, String::toBoolean)
        fov = props.getPropertyOrDefault("fov", DEFAULT_FOV, String::toFloat)
        zNear = props.getPropertyOrDefault("z_near", DEFAULT_Z_NEAR, String::toFloat)
        zFar = props.getPropertyOrDefault("z_far", DEFAULT_Z_FAR, String::toFloat)

        // If new properties have been added override the file with the new properties, so they can be externally changed.
        // This is good in case the program crashes before gracefully shutting down.
        if (loadedProperties < props.size) {
            save()
        }
    }

    private fun load() {
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
    }

    fun save() {
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
