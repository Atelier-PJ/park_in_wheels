package com.parkonwheels.demo

/** Optional read-only context; never replaces the main vehicle safety gate. */
interface TravelContextGateway {
    interface Listener {
        fun charging(value: Boolean?, observedAt: Long)
        fun arrived(opaqueId: String, observedAt: Long)
        fun clearArrival()
        fun destination(name: String?, observedAt: Long) { }
        fun status(message: String)
    }
    fun start(charging: Boolean, navigation: Boolean, listener: Listener)
    fun stop()
    fun close() = stop()
}
