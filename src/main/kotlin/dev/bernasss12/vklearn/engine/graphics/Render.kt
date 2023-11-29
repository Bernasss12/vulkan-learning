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
import dev.bernasss12.vklearn.engine.graphics.vulkan.model.ModelData
import dev.bernasss12.vklearn.engine.graphics.vulkan.model.VulkanModel
import dev.bernasss12.vklearn.engine.graphics.vulkan.pipeline.PipelineCache
import dev.bernasss12.vklearn.engine.graphics.vulkan.swapchain.SwapChain
import dev.bernasss12.vklearn.util.EngineProperties
import org.tinylog.kotlin.Logger

class Render(
    window: Window,
    scene: Scene
) : AutoCloseable {

    private val instance: Instance
    private val physicalDevice: PhysicalDevice
    private val device: Device
    private val surface: Surface
    private val graphicsQueue: Queue.GraphicsQueue
    private val presentQueue: Queue.PresentQueue
    private val swapChain: SwapChain
    private val commandPool: CommandPool
    private val forwardRenderActivity: ForwardRenderActivity
    private val vulkanModels: MutableList<VulkanModel>
    private val pipelineCache: PipelineCache

    init {
        instance = Instance(validate = EngineProperties.validate)
        physicalDevice = PhysicalDevice.createPhysicalDevice(
            instance = instance,
            preferredDeviceName = EngineProperties.physicalDeviceName
        )
        device = Device(physicalDevice = physicalDevice)
        surface = Surface(
            physicalDevice = physicalDevice,
            windowHandle = window.handle
        )
        graphicsQueue = Queue.GraphicsQueue(
            device = device,
            queueIndex = 0
        )
        presentQueue = Queue.PresentQueue(device, surface, 0)
        swapChain = SwapChain(
            device = device,
            surface = surface,
            window = window,
            requestedImages = EngineProperties.requestedImages,
            vsync = EngineProperties.vsync,
            presentQueue = presentQueue,
            concurrentQueues = emptyList()
        )
        commandPool = CommandPool(
            device = device,
            queueFamilyIndex = graphicsQueue.queueFamilyIndex
        )
        pipelineCache = PipelineCache(
            device = device
        )
        forwardRenderActivity = ForwardRenderActivity(
            swapChain = swapChain,
            commandPool = commandPool,
            pipelineCache = pipelineCache
        )
        vulkanModels = mutableListOf()
    }

    override fun close() {
        presentQueue.waitIdle()
        graphicsQueue.waitIdle()
        device.waitIdle()
        vulkanModels.forEach(VulkanModel::close)
        pipelineCache.close()
        forwardRenderActivity.close()
        commandPool.close()
        swapChain.close()
        surface.close()
        device.close()
        physicalDevice.close()
        instance.close()
    }

    fun loadModels(modelDataList: List<ModelData>) {
        Logger.debug("Loading ${modelDataList.size} model(s).")
        vulkanModels.addAll(VulkanModel.transformModels(modelDataList, commandPool, graphicsQueue))
        Logger.debug("Loaded ${modelDataList.size} model(s).")
    }

    fun render(window: Window, scene: Scene) {
        swapChain.acquireNextImage()

        forwardRenderActivity.recordCommandBuffer(vulkanModels)
        forwardRenderActivity.submit(graphicsQueue)

        swapChain.presentImage(presentQueue)
    }

}
