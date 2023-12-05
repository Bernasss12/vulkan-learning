/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.image

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageViewCreateInfo

class ImageView(
    private val device: Device,
    vkImage: Long,
    imageViewData: ImageViewData,
) : AutoCloseable {

    private val aspectMask: Int = imageViewData.aspectMask
    private val mipLevels: Int = imageViewData.mipLevels
    val vkImageView: Long

    init {
        useMemoryStack { stack ->
            // Allocate and define the info structure
            val imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                image(vkImage)
                viewType(imageViewData.viewType)
                format(imageViewData.format)
                subresourceRange { subresourceRange ->
                    subresourceRange.apply {
                        aspectMask(aspectMask)
                        baseMipLevel(0)
                        levelCount(mipLevels)
                        baseArrayLayer(imageViewData.baseArrayLayer)
                        layerCount(imageViewData.layerCount)
                    }
                }
            }

            // Allocate long buffer and create image view from info
            vkImageView = stack.vkCreateLong(
                "image view"
            ) { buffer ->
                vkCreateImageView(
                    device.vkDevice,
                    imageViewCreateInfo,
                    null,
                    buffer
                ).vkAssertSuccess("Failed to create image view")
            }
        }
    }

    override fun close() {
        vkDestroyImageView(device.vkDevice, vkImageView, null)
    }

    data class ImageViewData(
        internal var aspectMask: Int = 0,
        internal var format: Int = 0,
        internal var baseArrayLayer: Int = 0,
        internal var layerCount: Int = 1,
        internal var mipLevels: Int = 1,
        internal var viewType: Int = VK_IMAGE_VIEW_TYPE_2D,
    )
}