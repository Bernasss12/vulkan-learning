/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class Entity(
    val id: String,
    val modelId: String,
    val position: Vector3f,
) {
    val modelMatrix: Matrix4f
    val rotation: Quaternionf
    var scale: Float = 1f
        set(value) {
            field = value
            updateModelMatrix()
        }

    init {
        modelMatrix = Matrix4f()
        rotation = Quaternionf()
        updateModelMatrix()
    }

    fun resetRotation() {
        rotation.apply {
            x = 0f
            y = 0f
            z = 0f
            w = 1f
        }
        updateModelMatrix()
    }

    fun setPosition(newX: Float, newY: Float, newZ: Float) {
        position.apply {
            x = newX
            y = newY
            z = newZ
        }
        updateModelMatrix()
    }

    fun rotate(amount: Float, axis: Vector3f) {
        rotation.identity().rotateAxis(amount, axis)
        updateModelMatrix()
    }

    private fun updateModelMatrix() {
        modelMatrix.translationRotateScale(
            position,
            rotation,
            scale,
        )
    }
}