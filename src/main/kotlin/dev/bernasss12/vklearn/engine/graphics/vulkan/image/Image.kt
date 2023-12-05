/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.image

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import dev.bernasss12.vklearn.util.VulkanUtils
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements

class Image(
    private val device: Device,
    imageData: ImageData
) : AutoCloseable {

    val format: Int
    private val mipLevels: Int
    val vkImage: Long
    private val vkMemory: Long

    init {
        useMemoryStack { stack ->
            format = imageData.format
            mipLevels = imageData.mipLevels

            val imageCreateInfo = VkImageCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                imageType(VK_IMAGE_TYPE_2D)
                format(format)
                extent { extent ->
                    extent.apply {
                        width(imageData.width)
                        height(imageData.height)
                        depth(1)
                    }
                }
                mipLevels(mipLevels)
                arrayLayers(imageData.arrayLayers)
                samples(imageData.sampleCount)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                tiling(VK_IMAGE_TILING_OPTIMAL)
                usage(imageData.usage)
            }

            vkImage = stack.vkCreateLong("image") { buffer ->
                vkCreateImage(
                    device.vkDevice,
                    imageCreateInfo,
                    null,
                    buffer,
                )
            }

            // Get memory requirements for this object
            val memoryRequirements = VkMemoryRequirements.calloc(stack)
            vkGetImageMemoryRequirements(
                device.vkDevice,
                vkImage,
                memoryRequirements,
            )

            // Select memory size and type
            val memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                allocationSize(memoryRequirements.size())
                memoryTypeIndex(
                    VulkanUtils.memoryTypeFromProperties(
                        physicalDevice = device.physicalDevice,
                        memoryTypeBits = memoryRequirements.memoryTypeBits(),
                        requirementMask = 0,
                    )
                )
            }

            vkMemory = stack.vkCreateLong("image memory handle") { buffer ->
                vkAllocateMemory(
                    device.vkDevice,
                    memoryAllocateInfo,
                    null,
                    buffer,
                )
            }

            vkBindImageMemory(
                device.vkDevice,
                vkImage,
                vkMemory,
                0
            ).vkAssertSuccess("Failed to bind image memory")
        }
    }

    override fun close() {
        vkDestroyImage(
            device.vkDevice,
            vkImage,
            null
        )
        vkFreeMemory(
            device.vkDevice,
            vkMemory,
            null
        )
    }

    data class ImageData(
        internal var arrayLayers: Int = 1,
        internal var format: Int = VK_FORMAT_R8G8B8A8_SRGB,
        internal var mipLevels: Int = 1,
        internal var sampleCount: Int = 1,
        internal var width: Int = 0,
        internal var height: Int = 0,
        internal var usage: Int = 0,
    )
}
