/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandBuffer
import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandPool
import dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain.RenderPass
import dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain.SwapChain
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRenderPassBeginInfo

class ForwardRenderActivity(
    private val swapChain: SwapChain,
    commandPool: CommandPool
) {
    private val renderPass: RenderPass = RenderPass(swapChain)
    private val commandBuffers: List<CommandBuffer> = List(swapChain.imageViews.size) { CommandBuffer(commandPool, primary = true, oneTimeSubmit = false) }
    private val fences: List<Fence> = List(swapChain.imageViews.size) { Fence(swapChain.device, signaled = true) }
    private val frameBuffers: List<FrameBuffer>

    init {
        val swapChainExtent = swapChain.swapChainExtent
        val imageViews = swapChain.imageViews

        useMemoryStack { stack ->
            frameBuffers = List(swapChain.imageViews.size) { index ->
                FrameBuffer(
                    device = swapChain.device,
                    width = swapChainExtent.width(),
                    height = swapChainExtent.height(),
                    pAttachments = stack.longs(imageViews[index].vkImageView),
                    renderPass = renderPass.vkRenderPass,
                )
            }
            commandBuffers.zip(frameBuffers).forEach { pair ->
                recordCommandBuffer(
                    stack,
                    pair.first,
                    pair.second,
                    swapChainExtent.width(),
                    swapChainExtent.height(),
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
                fences[index].apply {
                    fenceWait()
                    reset()
                }
            )
        }
    }

    private fun recordCommandBuffer(
        stack: MemoryStack,
        commandBuffer: CommandBuffer,
        frameBuffer: FrameBuffer,
        width: Int,
        height: Int
    ) {
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
        vkCmdBeginRenderPass(
            commandBuffer.vkCommandBuffer,
            renderPassBeginInfo,
            VK_SUBPASS_CONTENTS_INLINE,
        )
        vkCmdEndRenderPass(
            commandBuffer.vkCommandBuffer
        )
        commandBuffer.endRecording()
    }

    fun cleanup() {
        frameBuffers.forEach(FrameBuffer::cleanup)
        renderPass.cleanup()
        commandBuffers.forEach(CommandBuffer::cleanup)
        fences.forEach(Fence::cleanup)
    }
}
