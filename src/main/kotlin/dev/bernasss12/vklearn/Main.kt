/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn

import dev.bernasss12.vklearn.engine.*
import dev.bernasss12.vklearn.engine.graphics.Renderer
import dev.bernasss12.vklearn.engine.graphics.common.model.MeshData
import dev.bernasss12.vklearn.engine.graphics.common.model.ModelData
import dev.bernasss12.vklearn.util.EngineProperties
import org.joml.Math
import org.joml.Vector3f
import org.tinylog.kotlin.Logger


class Main : IAppLogic {

    private var angle: Float = 0f
    private var cubeEntity: Entity? = null
    private val rotatingAngle = Vector3f(1f, 1f, 1f)


    override fun init(window: Window, scene: Scene, render: Renderer) {
        val modelId = "CubeModel"
        val meshDataList = listOf(
            MeshData(
                floatArrayOf(
                    -0.5f, 0.5f, 0.5f,
                    -0.5f, -0.5f, 0.5f,
                    0.5f, -0.5f, 0.5f,
                    0.5f, 0.5f, 0.5f,
                    -0.5f, 0.5f, -0.5f,
                    0.5f, 0.5f, -0.5f,
                    -0.5f, -0.5f, -0.5f,
                    0.5f, -0.5f, -0.5f,
                ),
                floatArrayOf(
                    0.0f, 0.0f,
                    0.5f, 0.0f,
                    1.0f, 0.0f,
                    1.0f, 0.5f,
                    1.0f, 1.0f,
                    0.5f, 1.0f,
                    0.0f, 1.0f,
                    0.0f, 0.5f,
                ),
                intArrayOf(
                    // Front face
                    0, 1, 3, 3, 1, 2,
                    // Top Face
                    4, 0, 3, 5, 4, 3,
                    // Right face
                    3, 2, 7, 5, 3, 7,
                    // Left face
                    6, 1, 0, 6, 0, 4,
                    // Bottom face
                    2, 1, 6, 2, 6, 7,
                    // Back face
                    7, 6, 4, 7, 4, 5,
                )
            )
        )

        val modelDataList = listOf(
            ModelData(
                modelId,
                meshDataList
            )
        )

        render.loadModels(modelDataList)

        cubeEntity = Entity(
            id = "CubeEntity",
            modelId = modelId,
            position = Vector3f(
                0.0f,
                0.0f,
                0.0f,
            )
        ).apply {
            setPosition(0f, 0f, -2f)
        }
        scene.addEntity(cubeEntity!!)
    }

    override fun input(window: Window, scene: Scene, delta: Long) {

    }

    override fun update(window: Window, scene: Scene, delta: Long) {
        angle += 1.0f
        if (angle >= 360) {
            angle -= 360
        }
        cubeEntity!!.rotate(Math.toRadians(angle), rotatingAngle)
    }

    override fun close() {
        EngineProperties.save()
    }

}

fun main(args: Array<String>) {
    Logger.info("Starting application")
    val engine = Engine("Vulkan book", Main())
    engine.start()
}

