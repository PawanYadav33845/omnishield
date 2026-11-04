package com.omnishield.utility.domain.repository

import android.net.Uri
import com.omnishield.utility.domain.model.CryptoMode
import com.omnishield.utility.domain.model.CryptoProgress
import com.omnishield.utility.domain.model.CryptoResult
import kotlinx.coroutines.flow.Flow

interface ICryptoRepository {

    fun executeCryptoOperation(
        inputUri: Uri,
        outputDestinationUri: Uri?,
        passphrase: CharArray,
        mode: CryptoMode
    ): Flow<Pair<CryptoProgress, CryptoResult?>>

    suspend fun isEncryptedOmniShieldFile(inputUri: Uri): Boolean
}
