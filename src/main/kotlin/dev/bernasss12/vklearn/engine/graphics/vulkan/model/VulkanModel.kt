/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.model

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import dev.bernasss12.vklearn.engine.graphics.vulkan.Fence
import dev.bernasss12.vklearn.engine.graphics.vulkan.Queue
import dev.bernasss12.vklearn.engine.graphics.vulkan.VulkanBuffer
import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandBuffer
import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandPool
import dev.bernasss12.vklearn.util.GraphConstants
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy
import kotlin.apply as standardApply


class VulkanModel private constructor(
    val modelId: String
) {
    val vulkanMeshList = mutableListOf<VulkanMesh>()

    fun cleanup() {
        vulkanMeshList.forEach(VulkanMesh::cleanup)
    }

    companion object {
        fun transformModels(
            modelDataList: List<ModelData>,
            commandPool: CommandPool,
            queue: Queue
        ): List<VulkanModel> {
            val device = commandPool.device
            val commandBuffer = CommandBuffer(
                commandPool = commandPool,
                primary = true,
                oneTimeSubmit = true
            )

            val vulkanModelList = modelDataList.map { (modelId) ->
                VulkanModel(modelId)
            }

            commandBuffer.beginRecording()

            val stagingBufferList = modelDataList.zip(vulkanModelList).flatMap { (modelData, vulkanModel) ->
                modelData.meshDataList.flatMap { meshData ->
                    val verticesBuffers: TransferBuffers = createVerticesBuffers(device, meshData)
                    val indicesBuffers: TransferBuffers = createIndicesBuffers(device, meshData)
                    recordTransferCommand(commandBuffer, verticesBuffers)
                    recordTransferCommand(commandBuffer, indicesBuffers)
                    vulkanModel.vulkanMeshList.add(
                        VulkanMesh(
                            verticesBuffer = verticesBuffers.destination,
                            indicesBuffer = indicesBuffers.destination,
                            indicesCount = meshData.indices.size,
                        )
                    )
                    listOf(verticesBuffers.source, indicesBuffers.source)
                }
            }

            commandBuffer.endRecording()
            val fence = Fence(device, true)
            fence.reset()

            useMemoryStack { stack ->
                queue.submit(
                    commandBuffers = stack.pointers(
                        commandBuffer.vkCommandBuffer
                    ),
                    waitSemaphores = null,
                    dstStageMasks = null,
                    signalSemaphores = null,
                    fence = fence
                )
            }

            fence.fenceWait()
            fence.cleanup()
            commandBuffer.cleanup()

            stagingBufferList.forEach(VulkanBuffer::cleanup)

            return vulkanModelList
        }

        private fun createVerticesBuffers(
            device: Device,
            meshData: MeshData,
        ): TransferBuffers {
            val bufferSize = meshData.positions.size * GraphConstants.FLOAT_LENGTH.toLong()

            return TransferBuffers(
                VulkanBuffer(
                    device = device,
                    size = bufferSize,
                    usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    requirementsMask = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                ).use { source, mappedMemory ->
                    MemoryUtil.memFloatBuffer(
                        mappedMemory,
                        source.requestedSize.toInt()
                    ).standardApply {
                        put(meshData.positions)
                    }
                },
                VulkanBuffer(
                    device = device,
                    size = bufferSize,
                    usage = VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    requirementsMask = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                )
            )
        }

        private fun createIndicesBuffers(
            device: Device,
            meshData: MeshData
        ): TransferBuffers {
            val bufferSize = meshData.positions.size * GraphConstants.INT_LENGTH.toLong()

            return TransferBuffers(
                VulkanBuffer(
                    device = device,
                    size = bufferSize,
                    usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    requirementsMask = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                ).use { source, mappedMemory ->
                    MemoryUtil.memIntBuffer(
                        mappedMemory,
                        source.requestedSize.toInt()
                    ).standardApply {
                        put(meshData.indices)
                    }
                },
                VulkanBuffer(
                    device = device,
                    size = bufferSize,
                    usage = VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    requirementsMask = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
                )
            )
        }

        private fun recordTransferCommand(
            commandBuffer: CommandBuffer,
            transferBuffers: TransferBuffers,
        ) {
            useMemoryStack { stack ->
                vkCmdCopyBuffer(
                    commandBuffer.vkCommandBuffer,
                    transferBuffers.source.vkBuffer,
                    transferBuffers.destination.vkBuffer,
                    VkBufferCopy.calloc(1, stack).standardApply {
                        srcOffset(0)
                        dstOffset(0)
                        size(transferBuffers.source.requestedSize)
                    }
                )
            }
        }
    }
}