package com.omnishield.utility.core.diagnostics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.DecimalFormat

data class BatteryReport(
    val percentage: Int,
    val isCharging: Boolean,
    val powerSource: String,
    val voltageVolts: String,
    val temperatureCelsius: String,
    val temperatureFahrenheit: String,
    val healthStatus: String,
    val technology: String
)

object BatteryDiagnosticsUtils {

    fun getBatteryReport(context: Context): BatteryReport {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val powerSource = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Qi Wireless Charging Pad"
            else -> if (isCharging) "Charging" else "Battery Discharging"
        }

        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val voltageStr = if (voltageMv > 0) "${DecimalFormat("#.##").format(voltageMv / 1000.0)} V ($voltageMv mV)" else "N/A"

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempC = if (tempTenths > 0) (tempTenths / 10.0) else 0.0
        val tempF = (tempC * 9.0 / 5.0) + 32.0

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal (Good)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating Warning"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded / Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over-Voltage Warning"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold Temperature"
            else -> "Normal"
        }

        val tech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        return BatteryReport(
            percentage = batteryPct,
            isCharging = isCharging,
            powerSource = powerSource,
            voltageVolts = voltageStr,
            temperatureCelsius = "${DecimalFormat("#.#").format(tempC)} °C",
            temperatureFahrenheit = "${DecimalFormat("#.#").format(tempF)} °F",
            healthStatus = healthStr,
            technology = tech
        )
    }
}
