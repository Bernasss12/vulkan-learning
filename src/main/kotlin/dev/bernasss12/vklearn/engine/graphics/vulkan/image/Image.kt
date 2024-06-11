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
    val device: Device,
    data: Data
) : AutoCloseable {

    val vkImage: Long
    val vkMemory: Long

    val format: Int
    val mipLevels: Int

    init {
        useMemoryStack { stack ->
            this.format = data.format
            this.mipLevels = data.mipLevels

            val imageCreateInfo = VkImageCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                imageType(VK_IMAGE_TYPE_2D)
                format(format)
                extent {
                    it.apply {
                        width(data.width)
                        height(data.height)
                        depth(1)
                    }
                }
                mipLevels(mipLevels)
                arrayLayers(data.arrayLayers)
                samples(data.sampleCount)
                initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                tiling(VK_IMAGE_TILING_OPTIMAL)
                usage(data.usage)
            }

            vkImage = stack.vkCreateLong("Failed to create image") { buffer ->
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

            // Allocate the memory for this image
            vkMemory = stack.vkCreateLong("Failed to allocate memory") { buffer ->
                vkAllocateMemory(
                    device.vkDevice,
                    memoryAllocateInfo,
                    null,
                    buffer
                )
            }

            // Bind image with memory
            vkBindImageMemory(
                device.vkDevice,
                vkImage,
                vkMemory,
                0
            ).vkAssertSuccess("Failed to bind image to memory")
        }
    }

    override fun close() {
        vkDestroyImage(device.vkDevice, vkImage, null)
        vkFreeMemory(device.vkDevice, vkMemory, null)
    }

    data class Data(
        var format: Int = VK_FORMAT_R8G8B8A8_SRGB,
        var mipLevels: Int = 1,
        var sampleCount: Int = 1,
        var arrayLayers: Int = 1,
        var usage: Int = 0,
        var width: Int = 0,
        var height: Int = 0,
    )
}
