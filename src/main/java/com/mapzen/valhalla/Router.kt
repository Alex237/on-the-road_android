package com.mapzen.valhalla

import retrofit.RestAdapter

interface Router {
    enum class Type(private val type: String) {
        WALKING("pedestrian"),
        BIKING("bicycle"),
        DRIVING("auto");

        override fun toString(): String {
            return type
        }
    }

    enum class DistanceUnits(private val units: String) {
        MILES("miles"),
        KILOMETERS("kilometers");

        override fun toString(): String {
            return units
        }
    }

    fun setApiKey(key: String): Router
    fun setWalking(): Router
    fun setDriving(): Router
    fun setBiking(): Router
    fun setLocation(point: DoubleArray): Router
    fun setLocation(point: DoubleArray, heading: Float): Router
    fun setLocation(point: DoubleArray,
            name: String? = null,
            street: String? = null,
            city: String? = null,
            state: String? = null): Router
    fun setDistanceUnits(units: DistanceUnits): Router
    fun clearLocations(): Router
    fun setEndpoint(url: String): Router
    fun getEndpoint(): String
    fun setCallback(callback: RouteCallback): Router
    fun fetch()
    fun getJSONRequest(): JSON
    fun setLogLevel(logLevel: RestAdapter.LogLevel): Router
    fun setDntEnabled(enabled: Boolean): Router
    fun isDntEnabled(): Boolean
}
