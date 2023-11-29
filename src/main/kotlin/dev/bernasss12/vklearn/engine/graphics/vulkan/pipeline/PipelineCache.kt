/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.pipeline

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo
import org.tinylog.kotlin.Logger

class PipelineCache(
    val device: Device
) : AutoCloseable {

    val vkPipelineCache: Long

    init {
        Logger.debug("Creating pipeline cache")
        useMemoryStack { stack ->
            val vkPipelineCacheCreateInfo = VkPipelineCacheCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO)
            }

            vkPipelineCache = stack.vkCreateLong("Error creating pipeline cache") { buffer ->
                vkCreatePipelineCache(
                    device.vkDevice,
                    vkPipelineCacheCreateInfo,
                    null,
                    buffer
                )
            }
        }
    }

    override fun close() {
        Logger.debug("Destroying pipeline cache")
        vkDestroyPipelineCache(
            device.vkDevice,
            vkPipelineCache,
            null
        )
    }
}