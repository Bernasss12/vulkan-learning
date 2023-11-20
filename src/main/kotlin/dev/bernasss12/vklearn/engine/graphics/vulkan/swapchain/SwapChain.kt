/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain

import dev.bernasss12.vklearn.engine.Window
import dev.bernasss12.vklearn.engine.graphics.vulkan.*
import dev.bernasss12.vklearn.util.VulkanUtils.moreThanZeroOrThrow
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import dev.bernasss12.vklearn.util.VulkanUtils.vkAssertSuccess
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateInt
import dev.bernasss12.vklearn.util.VulkanUtils.vkCreateLong
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.tinylog.kotlin.Logger

class SwapChain(
    val device: Device,
    val surface: Surface,
    val window: Window,
    requestedImages: Int,
    vsync: Boolean,
    presentQueue: Queue.PresentQueue,
    concurrentQueues: List<Queue>,
) {
    val imageViews: List<ImageView>
    val surfaceFormat: SurfaceFormat = calculateSurfaceFormat(device.physicalDevice, surface)
    val swapChainExtent: VkExtent2D
    private val vkSwapChain: Long
    val syncSemaphores: List<SyncSemaphores>
    var currentFrame: Int

    init {
        Logger.debug("Creating Vulkan SwapChain")
        useMemoryStack { stack ->
            val physicalDevice = device.physicalDevice

            // Get surface capabilities
            val surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack)
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                physicalDevice.vkPhysicalDevice,
                surface.vkSurface,
                surfaceCapabilities
            ).vkAssertSuccess("Failed to get surface capabilities")


            val imageCount = calculateImageCount(surfaceCapabilities, requestedImages)

            swapChainExtent = calculateSwapChainExtent(window, surfaceCapabilities)

            val vkSwapChainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack).apply {
                sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                surface(surface.vkSurface)
                minImageCount(imageCount)
                imageFormat(surfaceFormat.imageFormat)
                imageColorSpace(surfaceFormat.colorSpace)
                imageExtent(swapChainExtent)
                imageArrayLayers(1)
                imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                preTransform(surfaceCapabilities.currentTransform())
                compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                clipped(true)
                presentMode(
                    if (vsync) {
                        KHRSurface.VK_PRESENT_MODE_FIFO_KHR
                    } else {
                        KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR
                    }
                )

                concurrentQueues.map { queue ->
                    queue.queueFamilyIndex
                }.filterNot { index ->
                    index == presentQueue.queueFamilyIndex
                }.also { indices ->
                    if (indices.isNotEmpty()) {
                        val indicesBuffer = stack.mallocInt(indices.size + 1).apply {
                            indices.forEach { put(it) }
                            put(presentQueue.queueFamilyIndex)
                            flip()
                        }
                        imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        queueFamilyIndexCount(indicesBuffer.capacity())
                        pQueueFamilyIndices(indicesBuffer)
                    } else {
                        imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    }
                }
            }

            // Create swap chain
            vkSwapChain = stack.vkCreateLong("Failed to create swap chain") { buffer ->
                KHRSwapchain.vkCreateSwapchainKHR(
                    device.vkDevice,
                    vkSwapChainCreateInfo,
                    null,
                    buffer
                )
            }

            // Setting up semaphores
            syncSemaphores = List(imageCount) { _ -> SyncSemaphores(device) }
            currentFrame = 0

            imageViews = createImageViews(stack, device, vkSwapChain, surfaceFormat.imageFormat)
        }
    }

    fun presentImage(queue: Queue): Boolean {
        useMemoryStack { stack ->
            val present = VkPresentInfoKHR.calloc(stack).apply {
                sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                pWaitSemaphores(
                    stack.longs(
                        syncSemaphores[currentFrame].renderCompleteSemaphore.vkSemaphore
                    )
                )
                swapchainCount(1)
                pSwapchains(stack.longs(vkSwapChain))
                pImageIndices(stack.ints(currentFrame))
            }
            return KHRSwapchain.vkQueuePresentKHR(queue.vkQueue, present).let { result ->
                when (result) {
                    KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR -> true
                    KHRSwapchain.VK_SUBOPTIMAL_KHR, VK_SUCCESS -> false
                    else -> throw RuntimeException("Failed to acquire image: $result")
                }
            }.also {
                currentFrame = (currentFrame + 1) % imageViews.size
            }
        }
    }

    fun acquireNextImage(): Boolean {
        useMemoryStack {  stack ->
            currentFrame = stack.vkCreateInt { buffer ->
                KHRSwapchain.vkAcquireNextImageKHR(
                    device.vkDevice,
                    vkSwapChain,
                    0L,
                    syncSemaphores[currentFrame].imageAcquisitionSemaphore.vkSemaphore,
                    MemoryUtil.NULL,
                    buffer,
                ).also { result ->
                    return when (result) {
                        KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR -> true
                        KHRSwapchain.VK_SUBOPTIMAL_KHR, VK_SUCCESS -> false
                        else -> throw RuntimeException("Failed to acquire image: $result")
                    }
                }
            }
        }
        return false
    }

    fun cleanup() {
        Logger.debug("Destroying Vulkan SwapChain")
        swapChainExtent.free()
        imageViews.forEach(ImageView::cleanup)
        syncSemaphores.forEach(SyncSemaphores::cleanup)
        KHRSwapchain.vkDestroySwapchainKHR(device.vkDevice, vkSwapChain, null)
    }

    private fun createImageViews(stack: MemoryStack, device: Device, vkSwapChain: Long, imageFormat: Int): List<ImageView> {
        // Get number of surface images
        // Not using vkCreateInt because the IntBuffer is necessary at a later point.
        val swapChainImageCountBuffer = stack.mallocInt(1)
        KHRSwapchain.vkGetSwapchainImagesKHR(
            device.vkDevice,
            vkSwapChain,
            swapChainImageCountBuffer,
            null
        ).vkAssertSuccess("Failed to get number of surface images")
        val swapChainImageCount = swapChainImageCountBuffer.get(0)

        // Get surface images
        val swapChainImages = stack.mallocLong(swapChainImageCount)
        KHRSwapchain.vkGetSwapchainImagesKHR(
            device.vkDevice,
            vkSwapChain,
            swapChainImageCountBuffer,
            swapChainImages
        ).vkAssertSuccess("Failed to get surface images")

        // Map to ImageView
        return (0..<swapChainImageCount).map { index ->
            ImageView(
                device,
                swapChainImages.get(index),
                ImageView.ImageViewData(
                    aspectMask = VK_IMAGE_ASPECT_COLOR_BIT,
                    format = imageFormat,
                )
            )
        }
    }

    private fun calculateSwapChainExtent(window: Window, surfaceCapabilities: VkSurfaceCapabilitiesKHR): VkExtent2D {
        // Check if surface size is already defined
        return VkExtent2D.calloc().apply {
            if (surfaceCapabilities.currentExtent().width() == 0xFFFFFFFF.toInt()) {
                // Surface size is undefined. Set to window size if possible.
                width(
                    window.width.coerceIn(
                        surfaceCapabilities.minImageExtent().width(),
                        surfaceCapabilities.maxImageExtent().width()
                    )
                )
                height(
                    window.height.coerceIn(
                        surfaceCapabilities.minImageExtent().height(),
                        surfaceCapabilities.maxImageExtent().height()
                    )
                )
            } else {
                // Surface size is already defined, so go with that.
                set(
                    surfaceCapabilities.currentExtent()
                )
            }
        }
    }

    private fun calculateSurfaceFormat(physicalDevice: PhysicalDevice, surface: Surface): SurfaceFormat {
        MemoryStack.stackPush().use { stack ->
            // Get surface format count
            val surfaceFormatCountBuffer = stack.mallocInt(1)
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(
                physicalDevice.vkPhysicalDevice,
                surface.vkSurface,
                surfaceFormatCountBuffer,
                null,
            ).vkAssertSuccess("Failed to get surface format count")
            val surfaceFormatCount = surfaceFormatCountBuffer.get(0)
                .moreThanZeroOrThrow("No surface formats retrieved")

            // Get surface formats
            val surfaceFormatBuffer = VkSurfaceFormatKHR.calloc(surfaceFormatCount, stack)
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(
                physicalDevice.vkPhysicalDevice,
                surface.vkSurface,
                surfaceFormatCountBuffer,
                surfaceFormatBuffer,
            ).vkAssertSuccess("Failed to get surface formats")

            // Find a first surface format that supports the format and color space specified. If not return what the first surface format allows.
            return surfaceFormatBuffer.firstOrNull { surfaceFormat ->
                surfaceFormat.format() == VK_FORMAT_B8G8R8A8_SRGB && surfaceFormat.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
            }?.let { surfaceFormat ->
                SurfaceFormat(surfaceFormat.format(), surfaceFormat.colorSpace())
            } ?: SurfaceFormat(VK_FORMAT_B8G8R8A8_SRGB, surfaceFormatBuffer[0].colorSpace())
        }
    }

    private fun calculateImageCount(surfaceCapabilities: VkSurfaceCapabilitiesKHR, requestedImages: Int): Int {
        val maxImages = surfaceCapabilities.maxImageCount()
        val minImages = surfaceCapabilities.minImageCount()
        return requestedImages
            .coerceIn(minImages, maxImages)
            .also {
                Logger.debug("Requested [$requestedImages], got [$it] images. Surface capabilities: [$minImages, $maxImages]")
            }
    }

    data class SurfaceFormat(
        val imageFormat: Int,
        val colorSpace: Int
    )

    data class SyncSemaphores(
        val imageAcquisitionSemaphore: Semaphore, val renderCompleteSemaphore: Semaphore
    ) {
        constructor(device: Device) : this(Semaphore(device), Semaphore(device))

        fun cleanup() {
            imageAcquisitionSemaphore.cleanup()
            renderCompleteSemaphore.cleanup()
        }
    }
}