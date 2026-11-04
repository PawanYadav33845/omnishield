package com.omnishield.utility.presentation.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnishield.utility.core.pdf.PdfUtils
import com.omnishield.utility.core.storage.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfFileItem(
    val uri: Uri,
    val name: String,
    val sizeFormatted: String
)

data class PdfStudioState(
    val activeTab: Int = 0, // 0: Merge, 1: Split, 2: Render Page
    // Merge Tab
    val mergeInputFiles: List<PdfFileItem> = emptyList(),
    val mergedOutputFile: File? = null,
    // Split Tab
    val splitInputFile: PdfFileItem? = null,
    val splitStartPage: Int = 1,
    val splitEndPage: Int = 2,
    val splitOutputFile: File? = null,
    // Render Tab
    val renderInputFile: PdfFileItem? = null,
    val renderPageIndex: Int = 1,
    val totalPagesInRenderDoc: Int = 0,
    val renderedBitmap: Bitmap? = null,
    // Common
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class PdfStudioViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PdfStudioState())
    val state: StateFlow<PdfStudioState> = _state.asStateFlow()

    fun selectTab(tab: Int) {
        _state.update { it.copy(activeTab = tab, statusMessage = null, errorMessage = null) }
    }

    // Merge Actions
    fun addMergeFiles(uris: List<Uri>) {
        val newItems = uris.map { uri ->
            val name = StorageUtils.getFileName(context, uri)
            val size = StorageUtils.getFileSize(context, uri)
            PdfFileItem(
                uri = uri,
                name = name,
                sizeFormatted = StorageUtils.formatBytes(size)
            )
        }
        _state.update { it.copy(mergeInputFiles = it.mergeInputFiles + newItems) }
    }

    fun removeMergeFile(index: Int) {
        _state.update {
            val updated = it.mergeInputFiles.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            it.copy(mergeInputFiles = updated)
        }
    }

    fun clearMergeFiles() {
        _state.update { it.copy(mergeInputFiles = emptyList(), mergedOutputFile = null) }
    }

    fun executeMerge() {
        val files = _state.value.mergeInputFiles
        if (files.size < 2) {
            _state.update { it.copy(errorMessage = "Please select at least 2 PDF files to merge.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, statusMessage = "Merging ${files.size} PDF files...") }
            try {
                val cacheDir = File(context.cacheDir, "pdf_studio").apply { mkdirs() }
                val outFile = File(cacheDir, "Merged_${System.currentTimeMillis()}.pdf")

                val pagesWritten = withContext(Dispatchers.IO) {
                    FileOutputStream(outFile).use { outStream ->
                        PdfUtils.mergePdfs(context, files.map { it.uri }, outStream)
                    }
                }

                if (pagesWritten > 0) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            mergedOutputFile = outFile,
                            statusMessage = "Merged $pagesWritten pages into ${outFile.name} (${StorageUtils.formatBytes(outFile.length())})"
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Merge produced no pages."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // Split Actions
    fun setSplitFile(uri: Uri) {
        val name = StorageUtils.getFileName(context, uri)
        val size = StorageUtils.getFileSize(context, uri)
        val totalPages = PdfUtils.getPageCount(context, uri)

        _state.update {
            it.copy(
                splitInputFile = PdfFileItem(
                    uri = uri,
                    name = name,
                    sizeFormatted = StorageUtils.formatBytes(size)
                ),
                splitStartPage = 1,
                splitEndPage = totalPages.coerceAtLeast(1),
                splitOutputFile = null
            )
        }
    }

    fun setSplitPages(start: Int, end: Int) {
        _state.update { it.copy(splitStartPage = start, splitEndPage = end) }
    }

    fun executeSplit() {
        val file = _state.value.splitInputFile ?: run {
            _state.update { it.copy(errorMessage = "Please select a PDF file to split.") }
            return
        }
        val start = _state.value.splitStartPage
        val end = _state.value.splitEndPage

        if (start < 1 || end < start) {
            _state.update { it.copy(errorMessage = "Invalid page range.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, statusMessage = "Extracting pages $start to $end...") }
            try {
                val cacheDir = File(context.cacheDir, "pdf_studio").apply { mkdirs() }
                val outFile = File(cacheDir, "Split_p${start}-${end}_${System.currentTimeMillis()}.pdf")

                val extracted = withContext(Dispatchers.IO) {
                    FileOutputStream(outFile).use { outStream ->
                        PdfUtils.splitPdf(context, file.uri, start, end, outStream)
                    }
                }

                if (extracted > 0) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            splitOutputFile = outFile,
                            statusMessage = "Extracted $extracted pages into ${outFile.name}"
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to extract pages."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    // Render Actions
    fun setRenderFile(uri: Uri) {
        val name = StorageUtils.getFileName(context, uri)
        val size = StorageUtils.getFileSize(context, uri)
        val totalPages = PdfUtils.getPageCount(context, uri)

        _state.update {
            it.copy(
                renderInputFile = PdfFileItem(
                    uri = uri,
                    name = name,
                    sizeFormatted = StorageUtils.formatBytes(size)
                ),
                totalPagesInRenderDoc = totalPages,
                renderPageIndex = 1,
                renderedBitmap = null
            )
        }
        executeRender(1)
    }

    fun executeRender(pageNumber: Int) {
        val file = _state.value.renderInputFile ?: return
        val zeroIndex = (pageNumber - 1).coerceAtLeast(0)

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, renderPageIndex = pageNumber) }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    PdfUtils.renderPagePreview(context, file.uri, zeroIndex)
                }

                if (bitmap != null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            renderedBitmap = bitmap,
                            statusMessage = "Rendered page $pageNumber successfully"
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to render page $pageNumber."
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
