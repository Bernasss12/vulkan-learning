/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.common.model

import dev.bernasss12.vklearn.engine.graphics.vulkan.Device
import dev.bernasss12.vklearn.engine.graphics.vulkan.Fence
import dev.bernasss12.vklearn.engine.graphics.vulkan.Queue
import dev.bernasss12.vklearn.engine.graphics.vulkan.VulkanBuffer
import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandBuffer
import dev.bernasss12.vklearn.engine.graphics.vulkan.command.CommandPool
import dev.bernasss12.vklearn.util.GraphConstants
import dev.bernasss12.vklearn.util.Utils
import dev.bernasss12.vklearn.util.VulkanUtils.applyOnFirst
import dev.bernasss12.vklearn.util.VulkanUtils.useMemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCopy


class VulkanModel private constructor(
    val modelId: String
) : AutoCloseable {
    val vulkanMeshList = mutableListOf<VulkanMesh>()

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
            fence.close()
            commandBuffer.close()

            stagingBufferList.forEach(VulkanBuffer::close)

            return vulkanModelList
        }

        private fun createVerticesBuffers(
            device: Device,
            meshData: MeshData,
        ): TransferBuffers {
            val bufferSize = (meshData.positions.size + meshData.textureCoords.size) * GraphConstants.FLOAT_LENGTH.toLong()

            return TransferBuffers(
                VulkanBuffer(
                    device = device,
                    size = bufferSize,
                    usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    requirementsMask = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                ).applyWithMemory { mappedMemory ->
                    MemoryUtil.memFloatBuffer(
                        mappedMemory,
                        requestedSize.toInt(),
                    ).apply {
                        put(
                            Utils.interleaveFloatArrays(
                                3 to meshData.positions,
                                2 to meshData.textureCoords
                            ).let {
                                println()
                                it.forEach (::print)
                                println()
                                it
                            }
                        )
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
            val bufferSize = meshData.indices.size * GraphConstants.INT_LENGTH.toLong()

            return TransferBuffers(
                VulkanBuffer(
                    device = device,
                    size = bufferSize,
                    usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    requirementsMask = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                ).applyWithMemory { mappedMemory ->
                    MemoryUtil.memIntBuffer(
                        mappedMemory,
                        requestedSize.toInt(),
                    ).apply {
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
                    VkBufferCopy.calloc(1, stack).applyOnFirst {
                        srcOffset(0)
                        dstOffset(0)
                        size(transferBuffers.source.requestedSize)
                    }
                )
            }
        }

    }

    override fun close() {
        vulkanMeshList.forEach(VulkanMesh::close)
    }
}
