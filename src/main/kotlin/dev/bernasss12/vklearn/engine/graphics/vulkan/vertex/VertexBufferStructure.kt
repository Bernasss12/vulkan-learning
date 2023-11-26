/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.vertex

import dev.bernasss12.vklearn.util.GraphConstants
import dev.bernasss12.vklearn.util.VulkanUtils.applyOnFirst
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

class VertexBufferStructure : VertexInputStateInfo() {

    private val vertexCreateInfoAttributes: VkVertexInputAttributeDescription.Buffer
    private val vertexCreateInfoBindings: VkVertexInputBindingDescription.Buffer
    override val vertexCreateInfo: VkPipelineVertexInputStateCreateInfo

    init {
        vertexCreateInfoAttributes = VkVertexInputAttributeDescription.calloc(NUMBER_OF_ATTRIBUTES).applyOnFirst {
            binding(0)
            location(0)
            format(VK_FORMAT_R32G32B32_SFLOAT)
            offset(0)
        }
        vertexCreateInfoBindings = VkVertexInputBindingDescription.calloc(1).applyOnFirst {
            binding(0)
            stride(POSITION_COMPONENTS * GraphConstants.FLOAT_LENGTH)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        vertexCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc().apply {
            sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            pVertexBindingDescriptions(vertexCreateInfoBindings)
            pVertexAttributeDescriptions(vertexCreateInfoAttributes)
        }
    }

    override fun cleanup() {
        super.cleanup()
        vertexCreateInfoBindings.free()
        vertexCreateInfoAttributes.free()
    }

    companion object {
        const val NUMBER_OF_ATTRIBUTES = 1
        const val POSITION_COMPONENTS = 3
    }
}
