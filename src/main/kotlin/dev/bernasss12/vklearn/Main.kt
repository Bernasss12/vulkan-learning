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
import dev.bernasss12.vklearn.engine.graphics.vulkan.model.MeshData
import dev.bernasss12.vklearn.engine.graphics.vulkan.model.ModelData
import dev.bernasss12.vklearn.util.EngineProperties
import org.tinylog.kotlin.Logger


class Main : IAppLogic {

    override fun close() {
        EngineProperties.save()
    }

    override fun init(window: Window, scene: Scene, render: Render) {
        val modelId = "TriangleModel"
        val meshDataList = listOf(
            MeshData(
                floatArrayOf(
                    -0.33f, -0.33f, 0.0f,
                    0.0f, 0.5f, 0.0f,
                    0.33f, -0.33f, 0.0f
                ),
                intArrayOf(0, 1, 2)
            )
        )

        val modelDataList = listOf(
            ModelData(
                modelId,
                meshDataList
            )
        )
        render.loadModels(modelDataList)
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

