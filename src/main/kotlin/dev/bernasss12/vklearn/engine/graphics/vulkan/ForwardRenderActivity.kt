/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandBuffer
import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandPool
import dev.bernasss12.vklearn.engine.graphics.vulkan.model.VulkanModel
import dev.bernasss12.vklearn.engine.graphics.vulkan.pipeline.Pipeline
import dev.bernasss12.vklearn.engine.graphics.vulkan.pipeline.PipelineCache
import dev.bernasss12.vklearn.engine.graphics.vulkan.pipeline.PipelineCreationInfo
import dev.bernasss12.vklearn.engine.graphics.vulkan.shader.ShaderCompiler
import dev.bernasss12.vklearn.engine.graphics.vulkan.shader.ShaderModuleData
import dev.bernasss12.vklearn.engine.graphics.vulkan.shader.ShaderProgram
import dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain.RenderPass
import dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain.SwapChain
import dev.bernasss12.vklearn.engine.graphics.vulkan.vertex.VertexBufferStructure
import dev.bernasss12.vklearn.util.EngineProperties
import dev.bernasss12.vklearn.util.VulkanUtils.applyOnFirst
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderPassBeginInfo
import org.lwjgl.vulkan.VkViewport


class ForwardRenderActivity(
    private val swapChain: SwapChain,
    commandPool: CommandPool,
    pipelineCache: PipelineCache,
) {

    companion object {
        private const val FRAGMENT_SHADER_FILE_GLSL = "shaders/fwd_fragment.glsl"
        private const val FRAGMENT_SHADER_FILE_SPV = "$FRAGMENT_SHADER_FILE_GLSL.spv"
        private const val VERTEX_SHADER_FILE_GLSL = "shaders/fwd_vertex.glsl"
        private const val VERTEX_SHADER_FILE_SPV = "$VERTEX_SHADER_FILE_GLSL.spv"
    }

    private val commandBuffers: List<CommandBuffer>
    private val fences: List<Fence>
    private val frameBuffers: List<FrameBuffer>
    private val forwardShaderProgram: ShaderProgram
    private val pipeline: Pipeline
    private val renderPass: RenderPass

    init {
        val swapChainExtent = swapChain.swapChainExtent
        val imageViews = swapChain.imageViews
        useMemoryStack { stack ->
            renderPass = RenderPass(swapChain)

            frameBuffers = List(swapChain.imageViews.size) { index ->
                FrameBuffer(
                    device = swapChain.device,
                    width = swapChainExtent.width(),
                    height = swapChainExtent.height(),
                    pAttachments = stack.longs(imageViews[index].vkImageView),
                    renderPass = renderPass.vkRenderPass,
                )
            }

            if (EngineProperties.shaderRecompilation) {
                ShaderCompiler.compileIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader)
                ShaderCompiler.compileIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader)
            }

            forwardShaderProgram = ShaderProgram(
                device = swapChain.device,
                listOf(
                    ShaderModuleData(
                        VK_SHADER_STAGE_VERTEX_BIT,
                        VERTEX_SHADER_FILE_SPV
                    ),
                    ShaderModuleData(
                        VK_SHADER_STAGE_FRAGMENT_BIT,
                        FRAGMENT_SHADER_FILE_SPV
                    )
                )
            )

            val pipelineCreationInfo = PipelineCreationInfo(
                vkRenderPass = renderPass.vkRenderPass,
                shaderProgram = forwardShaderProgram,
                colorAttachmentCount = 1,
                vertexInputStateInfo = VertexBufferStructure()
            )

            pipeline = Pipeline(
                pipelineCache = pipelineCache,
                pipelineCreationInfo = pipelineCreationInfo,
            )
            pipelineCreationInfo.cleanup()

            commandBuffers = List(swapChain.imageViews.size) {
                CommandBuffer(
                    commandPool = commandPool,
                    primary = true,
                    oneTimeSubmit = false
                )
            }

            fences = List(swapChain.imageViews.size) {
                Fence(
                    device = swapChain.device,
                    signaled = true
                )
            }
        }
    }

    fun submit(queue: Queue) {
        useMemoryStack { stack ->
            val index = swapChain.currentFrame
            val syncSemaphores = swapChain.syncSemaphores[index]
            queue.submit(
                stack.pointers(commandBuffers[index].vkCommandBuffer),
                stack.longs(syncSemaphores.imageAcquisitionSemaphore.vkSemaphore),
                stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                stack.longs(syncSemaphores.renderCompleteSemaphore.vkSemaphore),
                fences[index]
            )
        }
    }

    fun recordCommandBuffer(
        vulkanModelList: List<VulkanModel>
    ) {
        useMemoryStack { stack ->
            val swapChainExtent = swapChain.swapChainExtent
            val width = swapChainExtent.width()
            val height = swapChainExtent.height()
            val index = swapChain.currentFrame

            val fence = fences[index]
            val commandBuffer = commandBuffers[index]
            val frameBuffer = frameBuffers[index]

            fence.fenceWait()
            fence.reset()

            commandBuffer.reset()
            val clearValues = VkClearValue.calloc(1, stack).apply(0) { value ->
                value.color().apply {
                float32(0, 0.5f) // R
                float32(1, 0.7f) // G
                float32(2, 0.9f) // B
                float32(3, 1f)   // A
                }
            }

            val renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                renderPass(renderPass.vkRenderPass)
                pClearValues(clearValues)
                renderArea { it.extent().set(width, height) }
                framebuffer(frameBuffer.vkFrameBuffer)
            }

            commandBuffer.beginRecording()
            val commandBufferHandle = commandBuffer.vkCommandBuffer
            vkCmdBeginRenderPass(
                commandBufferHandle,
                renderPassBeginInfo,
                VK_SUBPASS_CONTENTS_INLINE
            )

            vkCmdBindPipeline(
                commandBufferHandle,
                VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipeline.vkPipeline,
            )

            val viewport = VkViewport.calloc(1, stack).applyOnFirst {
                x(0f)
                y(height.toFloat())
                height(-height.toFloat())
                width(width.toFloat())
                minDepth(0.0f)
                maxDepth(1.0f)
            }
            vkCmdSetViewport(
                commandBufferHandle,
                0,
                viewport
            )

            val scissor = VkRect2D.calloc(1, stack).applyOnFirst {
                extent {
                    it.set(width, height)
                }
                offset {
                    it.set(0, 0)
                }
            }
            vkCmdSetScissor(
                commandBufferHandle,
                0,
                scissor
            )

            for (vulkanModel in vulkanModelList) {
                for (mesh in vulkanModel.vulkanMeshList) {
                    vkCmdBindVertexBuffers(commandBufferHandle, 0, stack.longs(mesh.verticesBuffer.vkBuffer), stack.longs(0L))
                    vkCmdBindIndexBuffer(commandBufferHandle, mesh.indicesBuffer.vkBuffer, 0, VK_INDEX_TYPE_UINT32)
                    vkCmdDrawIndexed(commandBufferHandle, mesh.indicesCount, 1, 0, 0, 0)
                }
            }

            vkCmdEndRenderPass(commandBufferHandle)
            commandBuffer.endRecording()
        }
    }

    fun cleanup() {
        frameBuffers.forEach(FrameBuffer::cleanup)
        renderPass.cleanup()
        commandBuffers.forEach(CommandBuffer::cleanup)
        fences.forEach(Fence::cleanup)
    }
}
