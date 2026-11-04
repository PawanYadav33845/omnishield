package com.omnishield.utility.domain.model

enum class IrDeviceCategory(val displayName: String) {
    TV("Television"),
    AIR_CONDITIONER("Air Conditioner"),
    SOUNDBAR("Sound System"),
    PROJECTOR("Projector"),
    CUSTOM("Custom IR Transmitter")
}

data class IrCarrierFrequencyRange(
    val minFrequencyHz: Int,
    val maxFrequencyHz: Int
)

data class IrCommand(
    val id: String,
    val name: String,
    val frequencyHz: Int = 38000,
    val pattern: IntArray,
    val isPrimary: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as IrCommand
        return id == other.id && name == other.name && frequencyHz == other.frequencyHz && pattern.contentEquals(other.pattern)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + frequencyHz
        result = 31 * result + pattern.contentHashCode()
        return result
    }
}

data class IrDeviceProfile(
    val id: String,
    val brandName: String,
    val category: IrDeviceCategory,
    val commands: List<IrCommand>
)

data class IrHardwareStatus(
    val hasEmitter: Boolean,
    val supportedCarrierRanges: List<IrCarrierFrequencyRange> = emptyList()
)
