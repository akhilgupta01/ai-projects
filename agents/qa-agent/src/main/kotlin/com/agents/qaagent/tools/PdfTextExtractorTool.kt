package com.agents.qaagent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

/**
 * Koog [Tool] that extracts plain text from a PDF file on the local filesystem.
 *
 * The tool is invoked by the koog agent when it needs to read the content of a
 * regulatory document in order to identify reportable attributes.
 */
object PdfTextExtractorTool : Tool<PdfTextExtractorTool.Args, ToolResult.Text>() {

    @Serializable
    data class Args(
        @SerialName("file_path")
        val filePath: String
    ) : Tool.Args

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "extract_pdf_text",
        description = """
            Extract all text content from a PDF file stored at the given path.
            Use this tool to read a regulatory reporting specification document
            before analyzing it for reportable attributes.
        """.trimIndent(),
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "file_path",
                description = "Absolute or relative path to the PDF file on the filesystem.",
                type = ToolParameterType.String
            )
        )
    )

    override suspend fun execute(args: Args): ToolResult.Text {
        val file = File(args.filePath)
        require(file.exists()) { "PDF file not found: ${args.filePath}" }
        require(file.extension.lowercase() == "pdf") { "File is not a PDF: ${args.filePath}" }

        Loader.loadPDF(file).use { document ->
            val stripper = PDFTextStripper()
            return ToolResult.Text(stripper.getText(document))
        }
    }
}
