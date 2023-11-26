/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain

import dev.bernasss12.vklearn.util.VulkanUtils.applyOnFirst
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*

class RenderPass(
    private val swapChain: SwapChain
) {
    val vkRenderPass: Long

    init {
        useMemoryStack { stack ->
            val attachments = VkAttachmentDescription.calloc(1, stack).applyOnFirst {
                format(swapChain.surfaceFormat.imageFormat)
                samples(VK_SAMPLE_COUNT_1_BIT)
                loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            }

            val colorReference = VkAttachmentReference.calloc(1, stack).applyOnFirst {
                attachment(0)
                layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            }

            val subPass = VkSubpassDescription.calloc(1, stack).applyOnFirst {
                pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                colorAttachmentCount(colorReference.remaining())
                pColorAttachments(colorReference)
            }


            val subpassDependencies = VkSubpassDependency.calloc(1, stack).applyOnFirst {
                srcSubpass(VK_SUBPASS_EXTERNAL)
                dstSubpass(0)
                srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                srcAccessMask(0)
                dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            }

            val renderPassCreateInfo = VkRenderPassCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                pAttachments(attachments)
                pSubpasses(subPass)
                pDependencies(subpassDependencies)
            }

            vkRenderPass = stack.vkCreateLong(
                "Failed to create render pass"
            ) { buffer ->
                vkCreateRenderPass(
                    swapChain.device.vkDevice,
                    renderPassCreateInfo,
                    null,
                    buffer
                )
            }
        }
    }

    fun cleanup() {
        vkDestroyRenderPass(swapChain.device.vkDevice, vkRenderPass, null)
    }
}