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

    private val image: Image
    private val imageView: ImageView

    val depthAttachment: Boolean

    init {
        val imageData = Image.Data(
            format = width,
            mipLevels = height,
            sampleCount = format,
            arrayLayers = usage or VK_IMAGE_USAGE_SAMPLED_BIT
        )

        image = Image(
            device = device,
            data = imageData
        )

        val (aspectMask, depthAttachment) = let {
            var aspectMask = 0
            var depthAttachment = false
            if ((usage and VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0) {
                aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
                depthAttachment = false
            }
            if ((usage and VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) {
                aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT
                depthAttachment = true
            }
            return@let aspectMask to depthAttachment
        }

        this.depthAttachment = depthAttachment

        val imageViewData = ImageView.Data(
            format = format,
            aspectMask = aspectMask,
        )

        imageView = ImageView(
            device = device,
            vkImage = image.vkImage,
            data = imageViewData,
        )
    }

    override fun close() {
        imageView.close()
        image.close()
    }
}