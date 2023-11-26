/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.model

import dev.bernasss12.vklearn.engine.graphics.vulkan.VulkanBuffer

data class VulkanMesh(
    val verticesBuffer: VulkanBuffer,
    val indicesBuffer: VulkanBuffer,
    val indicesCount: Int,
) {
    fun cleanup() {
        verticesBuffer.cleanup()
        indicesBuffer.cleanup()
    }
}
