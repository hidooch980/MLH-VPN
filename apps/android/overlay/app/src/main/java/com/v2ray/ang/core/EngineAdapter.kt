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
class XrayCoreEngineAdapter(
    private val controller: CoreController,
) : EngineAdapter {
    override val engineId: String = "xray-compatible"

    override fun start(config: String, tunFileDescriptor: Int) {
        require(config.isNotBlank()) { "Engine config must not be blank" }
        controller.startLoop(config, tunFileDescriptor)
    }

    override fun stop() {
        if (controller.isRunning) controller.stopLoop()
    }

    override fun isRunning(): Boolean = controller.isRunning
}
