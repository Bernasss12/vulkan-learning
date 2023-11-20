/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics

import dev.bernasss12.vklearn.engine.Scene
import dev.bernasss12.vklearn.engine.Window
import dev.bernasss12.vklearn.engine.graphics.vulkan.*
import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandPool
import dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain.SwapChain
import dev.bernasss12.vklearn.util.EngineProperties

class Render(
    window: Window,
    scene: Scene
) {

    private val instance = Instance(validate = EngineProperties.validate)
    private val physicalDevice: PhysicalDevice = PhysicalDevice.createPhysicalDevice(
        instance = instance,
        preferredDeviceName = EngineProperties.physicalDeviceName
    )
    private val device: Device = Device(physicalDevice = physicalDevice)
    private val surface: Surface = Surface(
        physicalDevice = physicalDevice,
        windowHandle = window.handle
    )
    private val graphicsQueue: Queue.GraphicsQueue = Queue.GraphicsQueue(
        device = device,
        queueIndex = 0
    )
    private val presentQueue: Queue.PresentQueue = Queue.PresentQueue(device, surface, 0)
    private val swapChain: SwapChain = SwapChain(
        device = device,
        surface = surface,
        window = window,
        requestedImages = EngineProperties.requestedImages,
        vsync = EngineProperties.vsync,
        presentQueue = presentQueue,
        concurrentQueues = emptyList()
    )
    private val commandPool: CommandPool = CommandPool(
        device = device,
        queueFamilyIndex = graphicsQueue.queueFamilyIndex
    )
    private val forwardRenderActivity: ForwardRenderActivity = ForwardRenderActivity(
        swapChain = swapChain,
        commandPool = commandPool
    )

    fun cleanup() {
        presentQueue.waitIdle()
        graphicsQueue.waitIdle()
        device.waitIdle()
        forwardRenderActivity.cleanup()
        commandPool.cleanup()
        swapChain.cleanup()
        surface.cleanup()
        device.cleanup()
        physicalDevice.cleanup()
        instance.cleanup()
    }

    fun render(window: Window, scene: Scene) {
        swapChain.acquireNextImage()
        forwardRenderActivity.submit(graphicsQueue)
        swapChain.presentImage(presentQueue)
    }

}
