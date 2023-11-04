/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn

import dev.bernasss12.vklearn.engine.Engine
import dev.bernasss12.vklearn.engine.IAppLogic
import dev.bernasss12.vklearn.engine.Scene
import dev.bernasss12.vklearn.engine.Window
import dev.bernasss12.vklearn.engine.graphics.Render
import dev.bernasss12.vklearn.util.EngineProperties
import org.tinylog.kotlin.Logger

class Main : IAppLogic {

    override fun cleanup() {
        EngineProperties.saveOnClose()
    }

    override fun init(window: Window, scene: Scene, render: Render) {
        TODO("Not yet implemented")
    }

    override fun input(window: Window, scene: Scene, delta: Long) {

    }

    override fun update(window: Window, scene: Scene, delta: Long) {

    }

}

fun main(args: Array<String>) {
    Logger.info("Starting application")
    val engine = Engine("Vulkan book", Main())
    engine.start()
}

