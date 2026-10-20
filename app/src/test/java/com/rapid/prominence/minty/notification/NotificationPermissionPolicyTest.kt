package com.rapid.prominence.minty.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test fun firstDenialAllowsAnotherRequest() {
        assertEquals(NotificationPermissionPolicy.NextStep.REQUEST, NotificationPermissionPolicy.afterResult(false, 0).nextStep)
    }

    @Test fun secondDenialStopsFurtherRequestsWithoutShowingSettings() {
        val result = NotificationPermissionPolicy.afterResult(false, 1)
        assertEquals(2, result.denials)
        assertEquals(NotificationPermissionPolicy.NextStep.NONE, result.nextStep)
    }

    @Test fun onlyTheFirstDenialTriggersTheHomeRetry() {
        assertEquals(true, NotificationPermissionPolicy.shouldRequestAtHome(1, false))
        assertEquals(false, NotificationPermissionPolicy.shouldRequestAtHome(0, false))
        assertEquals(false, NotificationPermissionPolicy.shouldRequestAtHome(2, false))
    }

    @Test fun systemRationaleRecoversAFirstDenialWhoseSplashCallbackArrivedLate() {
        assertEquals(true, NotificationPermissionPolicy.shouldRequestAtHome(0, true))
    }

    @Test fun launchDoesNotAskAgainAfterTwoDenials() {
        assertEquals(true, NotificationPermissionPolicy.shouldRequestAtLaunch(0))
        assertEquals(true, NotificationPermissionPolicy.shouldRequestAtLaunch(1))
        assertEquals(false, NotificationPermissionPolicy.shouldRequestAtLaunch(2))
    }

    @Test fun inFlightRequestPreventsASecondSystemPrompt() {
        assertEquals(true, NotificationPermissionPolicy.canLaunchRequest(false, true))
        assertEquals(false, NotificationPermissionPolicy.canLaunchRequest(true, true))
        assertEquals(false, NotificationPermissionPolicy.canLaunchRequest(false, false))
    }

    @Test fun splashDefersHomeNavigationUntilTheFirstPermissionResultArrives() {
        assertEquals(true, NotificationPermissionPolicy.shouldDeferHomeNavigation(true))
        assertEquals(false, NotificationPermissionPolicy.shouldDeferHomeNavigation(false))
    }

    @Test fun grantResetsDenialCount() {
        val result = NotificationPermissionPolicy.afterResult(true, 2)
        assertEquals(0, result.denials)
        assertEquals(NotificationPermissionPolicy.NextStep.GRANTED, result.nextStep)
    }
}
