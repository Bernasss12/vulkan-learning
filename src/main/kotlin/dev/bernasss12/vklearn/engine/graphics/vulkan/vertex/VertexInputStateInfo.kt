/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.vertex

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo

abstract class VertexInputStateInfo : AutoCloseable {

    abstract val vertexCreateInfo: VkPipelineVertexInputStateCreateInfo

    override fun close() {
        vertexCreateInfo.free()
    }
}