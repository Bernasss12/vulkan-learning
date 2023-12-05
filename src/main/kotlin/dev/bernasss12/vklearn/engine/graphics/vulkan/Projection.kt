/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan

import dev.bernasss12.vklearn.util.EngineProperties
import org.joml.Matrix4f

class Projection(
    width: Int,
    height: Int,
) {
    val projectionMatrix: Matrix4f = Matrix4f()

    init {
        resize(
            width = width,
            height = height,
        )
    }

    fun resize(width: Int, height: Int) {
        projectionMatrix.identity()
        EngineProperties.apply {
            projectionMatrix.perspective(
                fov,
                width.toFloat() / height,
                zNear,
                zFar,
                true,
            )
        }
    }
}