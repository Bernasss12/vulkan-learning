/*
 * Copyright (c) 2023 Bernardo Antunes
 *
 * For the purpose of not bloating every single file in the project, refer to the version of the MIT license provided in the project in `LICENCE.md`
 */

package dev.bernasss12.vklearn.engine.graphics.vulkan.shader

import org.lwjgl.util.shaderc.Shaderc
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.IOException

object ShaderCompiler {

    /**
     * Compile shader if spv counterpart does not exist or if gls file was last modified after the compiled spv version.
     */
    fun compileIfChanged(
        glslShaderFile: String,
        shaderType: Int
    ) {
        try {
            val (glslFile, spvFile) = getFiles(glslShaderFile)
            if (!spvFile.exists() || glslFile.lastModified() > spvFile.lastModified()) {
                Logger.debug("Compiling ${glslFile.path} to ${spvFile.path}")
                val shaderCode = String(glslFile.readBytes())
                spvFile.writeBytes(compileShader(shaderCode, shaderType))
            } else {
                Logger.debug("Shader ${glslFile.path} already compiled. Loading compiled version: ${spvFile.path}")
            }
        } catch (e: IOException) {
            Logger.error("Could not find: $glslShaderFile")
            throw RuntimeException(e)
        }
    }

    private fun compileShader(shaderCode: String, shaderType: Int): ByteArray {
        var compiler: Long = 0
        var options: Long = 0

        return try {
            compiler = Shaderc.shaderc_compiler_initialize()
            options = Shaderc.shaderc_compile_options_initialize()

            val result = Shaderc.shaderc_compile_into_spv(
                compiler,
                shaderCode,
                shaderType,
                "shader.glsl",
                "main",
                options
            )

            if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                throw RuntimeException("Shader compilation failed: ${Shaderc.shaderc_result_get_error_message(result)}")
            }

            val resultBuffer = Shaderc.shaderc_result_get_bytes(result)
                ?: throw RuntimeException("Shader compilation failed: ${Shaderc.shaderc_result_get_error_message(result)}")

            ByteArray(resultBuffer.capacity()).also { array ->
                resultBuffer.get(array)
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options)
            Shaderc.shaderc_compiler_release(compiler)
            byteArrayOf()
        }
    }

    private fun getFiles(glsShaderFile: String): Pair<File, File> = File(glsShaderFile) to File("$glsShaderFile.spv")
}