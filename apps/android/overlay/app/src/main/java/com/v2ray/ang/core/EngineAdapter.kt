package com.v2ray.ang.core

import libv2ray.CoreController

/**
 * Narrow lifecycle boundary for VPN engines. Implementations own only tunnel start/stop/status;
 * profile parsing, TUN ownership and platform VPN permission remain with the Android service.
 */
interface EngineAdapter {
    val engineId: String
    fun start(config: String, tunFileDescriptor: Int)
    fun stop()
    fun isRunning(): Boolean
}

/**
 * The only engine adapter enabled by current build evidence. Other protocol families must not be
 * registered as independent engines until their native runtime and device acceptance are proven.
 */
enum class EngineOperation { START, STOP, STATUS }

class EngineAdapterException(
    val engineId: String,
    val operation: EngineOperation,
    cause: Exception,
) : IllegalStateException("Engine '$engineId' $operation failed", cause)

/**
 * Adapter failures carry a stable engine/operation identity for diagnostics. Config contents,
 * credentials and server addresses are deliberately excluded from the exception message.
 */
class XrayCoreEngineAdapter(
    private val controller: CoreController,
) : EngineAdapter {
    override val engineId: String = "xray-compatible"

    override fun start(config: String, tunFileDescriptor: Int) {
        require(config.isNotBlank()) { "Engine config must not be blank" }
        try {
            controller.startLoop(config, tunFileDescriptor)
        } catch (e: Exception) {
            throw EngineAdapterException(engineId, EngineOperation.START, e)
        }
    }

    override fun stop() {
        try {
            if (controller.isRunning) controller.stopLoop()
        } catch (e: Exception) {
            throw EngineAdapterException(engineId, EngineOperation.STOP, e)
        }
    }

    override fun isRunning(): Boolean = try {
        controller.isRunning
    } catch (e: Exception) {
        throw EngineAdapterException(engineId, EngineOperation.STATUS, e)
    }
}
