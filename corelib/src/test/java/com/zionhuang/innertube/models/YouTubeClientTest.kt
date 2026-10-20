package com.zionhuang.innertube.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeClientTest {
    @Test fun `visionOS request context includes device identity and visitor`() {
        val context = YouTubeClient.VISIONOS.toContext(
            YouTubeLocale(gl = "US", hl = "en"),
            "CgtVisitor",
            null,
        )
        val body = Json.encodeToString(context)

        assertTrue(body.contains("\"clientName\":\"VISIONOS\""))
        assertTrue(body.contains("\"deviceModel\":\"RealityDevice17,1\""))
        assertTrue(body.contains("\"visitorData\":\"CgtVisitor\""))
    }
}
