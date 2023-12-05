/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine

import dev.bernasss12.vklearn.engine.graphics.vulkan.Projection

class Scene(
    window: Window
) {
    private val entityMap: MutableMap<String, MutableList<Entity>>
    val projection: Projection

    init {
        entityMap = hashMapOf()
        projection = Projection(
            width = window.width,
            height = window.height,
        )
    }

    fun addEntity(entity: Entity) {
        entityMap.getOrPut(entity.modelId) {
            mutableListOf()
        }.add(entity)
    }

    fun getEntitiesByModeId(modelId: String) = entityMap[modelId] ?: emptyList()

    fun getEntityMap() = entityMap.toMap()

    fun cleanEntities() = entityMap.clear()

    fun removeEntity(entity: Entity) {
        entityMap[entity.modelId]
            ?.let { entities ->
                entities.removeIf { it.id == entity.id }
            }
    }
}
