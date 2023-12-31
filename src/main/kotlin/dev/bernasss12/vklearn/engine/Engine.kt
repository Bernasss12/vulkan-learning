/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine

import dev.bernasss12.vklearn.engine.graphics.VulkanRenderer
import dev.bernasss12.vklearn.util.EngineProperties

class Engine(
    windowTitle: String,
    private val appLogic: IAppLogic
) : AutoCloseable {

    private val window: Window
    private val render: VulkanRenderer
    private val scene: Scene
    private var running: Boolean = false

    init {
        this.window = Window(windowTitle)
        this.scene = Scene(window)
        this.render = VulkanRenderer(window, scene)
        appLogic.init(window, scene, render)
    }

    override fun close() {
        appLogic.close()
        render.close()
        window.close()
    }

    private fun run() {
        val timePerUpdate = 1000f / EngineProperties.ups

        var initialTime = System.currentTimeMillis()
        var deltaUpdate = 0.0

        var updateTime = initialTime
        while (running && !window.shouldClose()) {
            window.pollEvents()

            val currentTime = System.currentTimeMillis()
            deltaUpdate += (currentTime - initialTime) / timePerUpdate

            appLogic.input(window, scene, currentTime - initialTime)

            if (deltaUpdate >= 1) {
                val timeDiff = currentTime - updateTime
                appLogic.update(window, scene, timeDiff)
                updateTime = currentTime
                deltaUpdate--
            }

            render.render(window, scene)

            initialTime = currentTime
        }

        close()
    }

    fun start() {
        running = true
        run()
    }

    fun stop() {
        running = false
        EngineProperties.save()
    }

}
