package com.rapid.prominence.minty.notification

object NotificationPermissionPolicy {
    const val MAX_REQUEST_COUNT = 2

    enum class NextStep { GRANTED, REQUEST, NONE }

    data class Result(val denials: Int, val nextStep: NextStep)

    fun afterResult(granted: Boolean, previousDenials: Int): Result {
        if (granted) return Result(0, NextStep.GRANTED)
        val denials = previousDenials.coerceAtLeast(0) + 1
        return Result(
            denials,
            if (denials >= MAX_REQUEST_COUNT) NextStep.NONE else NextStep.REQUEST,
        )
    }

    fun shouldRequestAtLaunch(denials: Int): Boolean =
        denials.coerceAtLeast(0) < MAX_REQUEST_COUNT

    fun shouldRequestAtHome(denials: Int, systemShowsRationale: Boolean): Boolean =
        denials == 1 || systemShowsRationale

    fun canLaunchRequest(isInFlight: Boolean, requestIsAllowed: Boolean): Boolean =
        !isInFlight && requestIsAllowed

    fun shouldDeferHomeNavigation(permissionRequestInFlight: Boolean): Boolean =
        permissionRequestInFlight
}
