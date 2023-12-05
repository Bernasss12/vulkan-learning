/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.pipeline

import dev.bernasss12.vklearn.engine.graphics.vulkan.shader.ShaderProgram
import dev.bernasss12.vklearn.engine.graphics.vulkan.vertex.VertexInputStateInfo

data class PipelineCreationInfo(
    val vkRenderPass: Long,
    val shaderProgram: ShaderProgram,
    val colorAttachmentCount: Int,
    val hasDepthAttachment: Boolean,
    val pushConstantSize: Int,
    val vertexInputStateInfo: VertexInputStateInfo,
) : AutoCloseable {
    override fun close() {
        vertexInputStateInfo.close()
    }
}
