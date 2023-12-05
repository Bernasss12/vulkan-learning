/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.image

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import org.lwjgl.vulkan.VK10.*

class Attachment(
    device: Device,
    width: Int,
    height: Int,
    format: Int,
    usage: Int,
) : AutoCloseable {
    val image: Image
    val imageView: ImageView

    private val depthAttachment: Boolean

    init {
        val imageData = Image.ImageData().apply {
            this.width = width
            this.height = height
            this.format = format
            this.usage = usage or VK_IMAGE_USAGE_SAMPLED_BIT
        }

        image = Image(
            device = device,
            imageData = imageData,
        )

        val (depthAttachment, aspectMask) = when {
            usage and VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT > 0 -> false to VK_IMAGE_ASPECT_COLOR_BIT
            usage and VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT > 0 -> true to VK_IMAGE_ASPECT_DEPTH_BIT
            else -> false to 0
        }

        this.depthAttachment = depthAttachment

        val imageViewData = ImageView.ImageViewData().apply {
            this.format = image.format
            this.aspectMask = aspectMask
        }

        imageView = ImageView(
            device,
            image.vkImage,
            imageViewData
        )
    }

    override fun close() {
        imageView.close()
        image.close()
    }
}