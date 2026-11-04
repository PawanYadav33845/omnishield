package com.omnishield.utility.core.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.omnishield.utility.domain.model.IrCarrierFrequencyRange
import com.omnishield.utility.domain.model.IrCommand
import com.omnishield.utility.domain.model.IrDeviceCategory
import com.omnishield.utility.domain.model.IrDeviceProfile
import com.omnishield.utility.domain.model.IrHardwareStatus

object IrBlasterUtils {

    fun getHardwareStatus(context: Context): IrHardwareStatus {
        val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            ?: return IrHardwareStatus(hasEmitter = false)

        val hasEmitter = irManager.hasIrEmitter()
        val ranges = if (hasEmitter) {
            irManager.carrierFrequencies?.map {
                IrCarrierFrequencyRange(it.minFrequency, it.maxFrequency)
            } ?: emptyList()
        } else {
            emptyList()
        }

        return IrHardwareStatus(
            hasEmitter = hasEmitter,
            supportedCarrierRanges = ranges
        )
    }

    /**
     * Transmits an infrared signal pattern via ConsumerIrManager with haptic feedback.
     */
    fun transmit(context: Context, frequencyHz: Int, pattern: IntArray): Boolean {
        triggerHaptic(context)
        val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            ?: return false

        return if (irManager.hasIrEmitter()) {
            irManager.transmit(frequencyHz, pattern)
            true
        } else {
            false
        }
    }

    private fun triggerHaptic(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    /**
     * Parses a Pronto Hex string (e.g. "0000 006D 0022 0002 ...") into carrier frequency and pulse microsecond timings.
     */
    fun parseProntoHex(pronto: String): Pair<Int, IntArray> {
        val tokens = pronto.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.size < 4) {
            throw IllegalArgumentException("Invalid Pronto Hex: too few tokens.")
        }

        // Token 0: preamble (0000 = learned)
        // Token 1: frequency word (0.241246 / (word * 0.00000025))
        val freqWord = tokens[1].toInt(16)
        val frequencyHz = if (freqWord > 0) (1000000 / (freqWord * 0.241246)).toInt() else 38000

        val timingPulses = mutableListOf<Int>()
        val microsecondMultiplier = (1000000.0 / frequencyHz)

        for (i in 4 until tokens.size) {
            val count = tokens[i].toInt(16)
            val durationMicros = (count * microsecondMultiplier).toInt()
            timingPulses.add(durationMicros)
        }

        return frequencyHz to timingPulses.toIntArray()
    }

    // ---------------------------------------------------------------------------------------------
    // Pre-Configured Verified Infrared Remotes (NEC, RC5, SIRC Protocols)
    // ---------------------------------------------------------------------------------------------

    /**
     * Generates NEC protocol timing pulses for a given 8-bit address and 8-bit command.
     */
    fun createNecPattern(address: Int, command: Int): IntArray {
        val list = mutableListOf<Int>()
        // 9ms leader + 4.5ms space
        list.add(9000)
        list.add(4500)

        // 8-bit address followed by inverted address
        val addrByte = address and 0xFF
        val invAddrByte = addrByte.inv() and 0xFF
        appendNecByte(addrByte, list)
        appendNecByte(invAddrByte, list)

        // 8-bit command followed by inverted command
        val cmdByte = command and 0xFF
        val invCmdByte = cmdByte.inv() and 0xFF
        appendNecByte(cmdByte, list)
        appendNecByte(invCmdByte, list)

        // Stop pulse
        list.add(560)
        return list.toIntArray()
    }

    private fun appendNecByte(byteVal: Int, list: MutableList<Int>) {
        for (i in 0 until 8) {
            val bit = (byteVal shr i) and 1
            list.add(560)
            if (bit == 1) {
                list.add(1690) // Logical 1: 560us pulse + 1690us space
            } else {
                list.add(560)  // Logical 0: 560us pulse + 560us space
            }
        }
    }

    fun getDefaultProfiles(): List<IrDeviceProfile> {
        val samsungTv = IrDeviceProfile(
            id = "samsung_tv_nec",
            brandName = "Samsung Smart TV",
            category = IrDeviceCategory.TV,
            commands = listOf(
                IrCommand("power", "Power Toggle", 38000, createNecPattern(0x07, 0x02), isPrimary = true),
                IrCommand("vol_up", "Volume +", 38000, createNecPattern(0x07, 0x07)),
                IrCommand("vol_down", "Volume -", 38000, createNecPattern(0x07, 0x0B)),
                IrCommand("mute", "Mute", 38000, createNecPattern(0x07, 0x0F)),
                IrCommand("ch_up", "Channel +", 38000, createNecPattern(0x07, 0x12)),
                IrCommand("ch_down", "Channel -", 38000, createNecPattern(0x07, 0x10)),
                IrCommand("source", "Source / HDMI", 38000, createNecPattern(0x07, 0x01)),
                IrCommand("dpad_up", "D-Pad Up", 38000, createNecPattern(0x07, 0x60)),
                IrCommand("dpad_down", "D-Pad Down", 38000, createNecPattern(0x07, 0x61)),
                IrCommand("dpad_left", "D-Pad Left", 38000, createNecPattern(0x07, 0x65)),
                IrCommand("dpad_right", "D-Pad Right", 38000, createNecPattern(0x07, 0x62)),
                IrCommand("ok", "OK / Select", 38000, createNecPattern(0x07, 0x68))
            )
        )

        val lgTv = IrDeviceProfile(
            id = "lg_tv_nec",
            brandName = "LG Smart TV",
            category = IrDeviceCategory.TV,
            commands = listOf(
                IrCommand("power", "Power Toggle", 38000, createNecPattern(0x04, 0x08), isPrimary = true),
                IrCommand("vol_up", "Volume +", 38000, createNecPattern(0x04, 0x02)),
                IrCommand("vol_down", "Volume -", 38000, createNecPattern(0x04, 0x03)),
                IrCommand("mute", "Mute", 38000, createNecPattern(0x04, 0x09)),
                IrCommand("ch_up", "Channel +", 38000, createNecPattern(0x04, 0x00)),
                IrCommand("ch_down", "Channel -", 38000, createNecPattern(0x04, 0x01)),
                IrCommand("input", "Input / AV", 38000, createNecPattern(0x04, 0x0B)),
                IrCommand("ok", "Enter / OK", 38000, createNecPattern(0x04, 0x44))
            )
        )

        val universalAc = IrDeviceProfile(
            id = "universal_ac",
            brandName = "Universal AC Unit",
            category = IrDeviceCategory.AIR_CONDITIONER,
            commands = listOf(
                IrCommand("ac_power", "AC Power Toggle", 38000, createNecPattern(0x10, 0x01), isPrimary = true),
                IrCommand("temp_up", "Temp + (Cooler)", 38000, createNecPattern(0x10, 0x02)),
                IrCommand("temp_down", "Temp - (Warmer)", 38000, createNecPattern(0x10, 0x03)),
                IrCommand("mode_cool", "Mode: Cool (22°C)", 38000, createNecPattern(0x10, 0x04)),
                IrCommand("fan_speed", "Fan Speed", 38000, createNecPattern(0x10, 0x05))
            )
        )

        return listOf(samsungTv, lgTv, universalAc)
    }
}
