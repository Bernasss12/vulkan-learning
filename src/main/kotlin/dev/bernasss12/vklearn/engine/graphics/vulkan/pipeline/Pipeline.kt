/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.pipeline

import dev.bernasss12.vklearn.util.VulkanUtils.applyOnEach
import dev.bernasss12.vklearn.util.VulkanUtils.applyOnEachWith
import dev.bernasss12.vklearn.util.VulkanUtils.applyOnFirst
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.tinylog.kotlin.Logger


class Pipeline(
    pipelineCache: PipelineCache,
    pipelineCreationInfo: PipelineCreationInfo
) : AutoCloseable {
    val device = pipelineCache.device
    val vkPipelineLayout: Long
    val vkPipeline: Long

    init {
        useMemoryStack { stack ->
            val main = stack.UTF8("main")
            val shaderModules = pipelineCreationInfo.shaderProgram.shaderModules
            val moduleCount = shaderModules.size
            val shaderStages = VkPipelineShaderStageCreateInfo.calloc(moduleCount, stack).applyOnEachWith(shaderModules) { (shaderStage, handle) ->
                sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                stage(shaderStage)
                module(handle)
                pName(main)
            }

            val vkPipelineInputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            }

            val vkPipelineViewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                viewportCount(1)
                scissorCount(1)
            }


            val vkPipelineRasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                polygonMode(VK_POLYGON_MODE_FILL)
                cullMode(VK_CULL_MODE_NONE)
                frontFace(VK_FRONT_FACE_CLOCKWISE)
                lineWidth(1.0f)
            }

            val vkPipelineMultisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
            }


            val blendAttachmentState = VkPipelineColorBlendAttachmentState.calloc(
                pipelineCreationInfo.colorAttachmentCount,
                stack,
            ).applyOnEach {
                colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT or
                            VK_COLOR_COMPONENT_G_BIT or
                            VK_COLOR_COMPONENT_B_BIT or
                            VK_COLOR_COMPONENT_A_BIT
                )
            }

            val colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                pAttachments(blendAttachmentState)
            }

            val vkPipelineDynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                pDynamicStates(
                    stack.ints(
                        VK_DYNAMIC_STATE_VIEWPORT,
                        VK_DYNAMIC_STATE_SCISSOR
                    )
                )
            }

            val pPipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                if (pipelineCreationInfo.pushConstantSize > 0) {
                    pPushConstantRanges(
                        VkPushConstantRange.calloc(1, stack).applyOnFirst {
                            stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                            offset(0)
                            size(pipelineCreationInfo.pushConstantSize)
                        }
                    )
                }
            }

            vkPipelineLayout = stack.vkCreateLong("pipeline layout") { buffer ->
                vkCreatePipelineLayout(device.vkDevice, pPipelineLayoutCreateInfo, null, buffer)
            }

            val pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack).applyOnFirst {
                sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                pStages(shaderStages)
                pVertexInputState(pipelineCreationInfo.vertexInputStateInfo.vertexCreateInfo)
                pInputAssemblyState(vkPipelineInputAssemblyStateCreateInfo)
                pViewportState(vkPipelineViewportStateCreateInfo)
                pRasterizationState(vkPipelineRasterizationStateCreateInfo)
                pMultisampleState(vkPipelineMultisampleStateCreateInfo)
                pColorBlendState(colorBlendState)
                pDynamicState(vkPipelineDynamicStateCreateInfo)
                layout(vkPipelineLayout)
                renderPass(pipelineCreationInfo.vkRenderPass)
                if (pipelineCreationInfo.hasDepthAttachment) {
                    pDepthStencilState(
                        VkPipelineDepthStencilStateCreateInfo.calloc(stack).apply {
                            sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                            depthTestEnable(true)
                            depthWriteEnable(true)
                            depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
                            depthBoundsTestEnable(false)
                            stencilTestEnable(false)
                        }
                    )
                }
            }

            vkPipeline = stack.vkCreateLong("graphics pipeline") { buffer ->
                vkCreateGraphicsPipelines(
                    device.vkDevice,
                    pipelineCache.vkPipelineCache,
                    pipeline,
                    null,
                    buffer,
                )
            }
        }
    }

    override fun close() {
        Logger.debug("Destroying pipeline")
        vkDestroyPipelineLayout(
            device.vkDevice,
            vkPipelineLayout,
            null
        )
        vkDestroyPipeline(
            device.vkDevice,
            vkPipeline,
            null
        )
    }
}