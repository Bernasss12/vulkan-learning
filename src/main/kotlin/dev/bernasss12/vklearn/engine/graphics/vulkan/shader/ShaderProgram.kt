/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.shader

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.IOException
import kotlin.io.path.readBytes

class ShaderProgram(
    private val device: Device,
    shaderModuleData: List<ShaderModuleData>
) : AutoCloseable {
    val shaderModules: List<ShaderModule>

    init {
        try {
            shaderModules = shaderModuleData.map { moduleData ->
                ShaderModule(
                    shaderStage = moduleData.shaderStage,
                    handle = createShaderModule(
                        content = File(moduleData.shaderSpvFile).toPath().readBytes()
                    )
                )
            }
        } catch (e: IOException) {
            Logger.error("Error loading shader files", e)
            throw RuntimeException(e)
        }
    }

    private fun createShaderModule(content: ByteArray): Long {
        useMemoryStack { stack ->
            val contentBuffer = stack.malloc(content.size).put(0, content)
            val moduleCreateInfo = VkShaderModuleCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                pCode(contentBuffer)
            }

            return stack.vkCreateLong("Failed to create shader module") { buffer ->
                vkCreateShaderModule(
                    device.vkDevice,
                    moduleCreateInfo,
                    null,
                    buffer
                )
            }
        }
    }

    override fun close() {
        shaderModules.forEach { module ->
            vkDestroyShaderModule(
                device.vkDevice,
                module.handle,
                null,
            )
        }
    }
}