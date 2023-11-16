/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.engine.graphics.vulkan.VulkanUtils.vkAssertSuccess
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageViewCreateInfo

class ImageView(
    private val device: Device,
    vkImage: Long,
    imageViewData: ImageViewData,
) {

    private val aspectMask: Int = imageViewData.aspectMask
    private val mipLevels: Int = imageViewData.mipLevels
    private val vkImageView: Long

    fun cleanup() {
        vkDestroyImageView(device.vkDevice, vkImageView, null)
    }

    init {
        MemoryStack.stackPush().use { stack ->
            // Allocate and define the info structure
            val imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                image(vkImage)
                viewType(imageViewData.viewType)
                format(imageViewData.format)
                subresourceRange {
                    it.apply {
                        aspectMask(aspectMask)
                        baseMipLevel(0)
                        levelCount(mipLevels)
                        baseArrayLayer(imageViewData.baseArrayLayer)
                        layerCount(imageViewData.layerCount)
                    }
                }
            }

            // Allocate long buffer and create image view from info
            val vkImageViewBuffer = stack.mallocLong(1)
            vkCreateImageView(device.vkDevice, imageViewCreateInfo, null, vkImageViewBuffer)
                .vkAssertSuccess("Failed to create image view")
            vkImageView = vkImageViewBuffer.get(0)
        }
    }


    data class ImageViewData(
        internal var aspectMask: Int,
        internal var format: Int,
        internal var baseArrayLayer: Int = 0,
        internal var layerCount: Int = 1,
        internal var mipLevels: Int = 1,
        internal var viewType: Int = VK_IMAGE_VIEW_TYPE_2D,
    )
}