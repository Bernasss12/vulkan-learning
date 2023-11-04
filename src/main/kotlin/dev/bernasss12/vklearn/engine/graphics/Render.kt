/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics

import dev.bernasss12.vklearn.engine.Scene
import dev.bernasss12.vklearn.engine.Window
import dev.bernasss12.vklearn.engine.graphics.vulkan.*
import dev.bernasss12.vklearn.util.EngineProperties

class Render(window: Window, scene: Scene) {

    private val instance = Instance(EngineProperties.validate)
    private val physicalDevice: PhysicalDevice = PhysicalDevice.createPhysicalDevice(instance, EngineProperties.physicalDeviceName)
    private val device: Device = Device(physicalDevice)
    private val surface: Surface = Surface(physicalDevice, window.handle)
    private val graphicsQueue: Queue.GraphicsQueue = Queue.GraphicsQueue(device, 0)

    fun cleanup() {
        surface.cleanup()
        device.cleanup()
        physicalDevice.cleanup()
        instance.cleanup()
    }

    fun render(window: Window, scene: Scene) {

    }

}
