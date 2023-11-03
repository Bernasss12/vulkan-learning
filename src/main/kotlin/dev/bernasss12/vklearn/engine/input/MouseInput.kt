/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.input

import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.*

class MouseInput(windowHandle: Long) {
    val currentPos: Vector2f
    private val previousPos: Vector2f
    val displVec: Vector2f
    private var inWindow = false
    var leftButtonPressed = false
        private set
    var rightButtonPressed = false
        private set

    init {
        previousPos = Vector2f(-1f, -1f)
        currentPos = Vector2f()
        displVec = Vector2f()

        glfwSetCursorPosCallback(windowHandle) { _, x, y ->
            currentPos.apply {
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
        displVec.zero()
        if (previousPos.x > 0 && previousPos.y > 0 && inWindow) {
            // TODO verify this is calculating correctly
            val (deltaX, deltaY) = currentPos.copy().sub(previousPos).pair()
            val (rotateX, rotateY) = (deltaX != 0f) to (deltaY != 0f)
            if (rotateX) {
                displVec.y = deltaX
            }
            if (rotateY) {
                displVec.x = deltaY
            }
            previousPos.set(currentPos)
        }
    }

    private fun Vector2f.pair() = x to y
    private fun Vector2f.copy() = Vector2f(x, y)
}
