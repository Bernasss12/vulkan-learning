/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */
@file:Suppress("MemberVisibilityCanBePrivate")

package dev.bernasss12.vklearn.engine.input

import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.*

class MouseInput(windowHandle: Long) {
    val currentPosition: Vector2f = Vector2f()
    private val previousPosition: Vector2f = Vector2f(-1f, -1f)
    val displacementVector: Vector2f = Vector2f()
    private var inWindow = false
    var leftButtonPressed = false
        private set
    var rightButtonPressed = false
        private set

    init {
        glfwSetCursorPosCallback(windowHandle) { _, x, y ->
            currentPosition.apply {
                this.x = x.toFloat()
                this.y = y.toFloat()
            }
        }

        glfwSetCursorEnterCallback(windowHandle) { _, entered ->
            inWindow = entered
        }

        glfwSetMouseButtonCallback(windowHandle) { _, button, action, _ ->
            leftButtonPressed = button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS
            rightButtonPressed = button == GLFW_MOUSE_BUTTON_2 && action == GLFW_PRESS
        }
    }

    fun input() {
        displacementVector.zero()
        if (previousPosition.x > 0 && previousPosition.y > 0 && inWindow) {
            // TODO verify this is calculating correctly
            val (deltaX, deltaY) = currentPosition.copy().sub(previousPosition).pair()
            val (rotateX, rotateY) = (deltaX != 0f) to (deltaY != 0f)
            if (rotateX) {
                displacementVector.y = deltaX
            }
            if (rotateY) {
                displacementVector.x = deltaY
            }
            previousPosition.set(currentPosition)
        }
    }

    private fun Vector2f.pair() = x to y
    private fun Vector2f.copy() = Vector2f(x, y)
}
