package com.omnishield.utility.domain.repository

import android.net.Uri
import com.omnishield.utility.domain.model.ExifMetadata
import com.omnishield.utility.domain.model.FileHashReport
import com.omnishield.utility.domain.model.SanitizerResult
import kotlinx.coroutines.flow.Flow

interface IMetadataRepository {

    suspend fun inspectExif(imageUri: Uri): Result<ExifMetadata>

    suspend fun sanitizeMetadata(inputUri: Uri): SanitizerResult

    fun computeChecksums(inputUri: Uri): Flow<Pair<Float, FileHashReport?>>
}
