package com.omnishield.utility.domain.repository

import android.net.Uri
import com.omnishield.utility.domain.model.AudioContainerInfo
import com.omnishield.utility.domain.model.BatchConversionProgress
import com.omnishield.utility.domain.model.ConversionResult
import com.omnishield.utility.domain.model.DataFormat
import com.omnishield.utility.domain.model.ImageFormat
import kotlinx.coroutines.flow.Flow

interface IFileConverterRepository {

    fun convertImages(
        inputUris: List<Uri>,
        targetFormat: ImageFormat,
        quality: Int
    ): Flow<Pair<BatchConversionProgress, ConversionResult?>>

    suspend fun convertData(
        inputUri: Uri,
        sourceFormat: DataFormat,
        targetFormat: DataFormat
    ): Result<Uri>

    suspend fun inspectAudio(inputUri: Uri): Result<AudioContainerInfo>

    suspend fun compressFileLossless(inputUri: Uri): Result<Uri>
}
