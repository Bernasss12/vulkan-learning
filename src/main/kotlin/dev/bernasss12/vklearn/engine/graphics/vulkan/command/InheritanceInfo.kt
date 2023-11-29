/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.command

internal data class InheritanceInfo(
    var vkRenderPass: Long,
    var vkFrameBuffer: Long,
    var subPass: Int,
)