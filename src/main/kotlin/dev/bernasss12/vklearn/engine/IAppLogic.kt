/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine

import dev.bernasss12.vklearn.engine.graphics.Render

interface IAppLogic {

    /**
     * Invoked when application is finished.
     * Releases resources.
     */
    fun cleanup()

    /**
     * Invoked on launch to create immediately needed resources.
     */
    fun init(window: Window, scene: Scene, render: Render)

    /**
     * Invoked periodically to update app states and react to user input.
     * @param delta difference in milliseconds since last invoked.
     */
    fun input(window: Window, scene: Scene, delta: Long)

    /**
     * Invoked periodically to update app states.
     * @param delta difference in milliseconds since last invoked.
     */
    fun update(window: Window, scene: Scene, delta: Long)

}