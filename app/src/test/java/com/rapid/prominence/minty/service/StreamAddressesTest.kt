package com.rapid.prominence.minty.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamAddressesTest {
    @Test fun `keeps an address only before expiry`() {
        val addresses = StreamAddresses()
        addresses.keep("song", "https://example.com/audio", 2_000L)

        assertEquals("https://example.com/audio", addresses.of("song", 1_999L))
        assertNull(addresses.of("song", 2_000L))
    }

    @Test fun `forgets a failed address so replay resolves again`() {
        val addresses = StreamAddresses()
        addresses.keep("song", "https://example.com/audio", 2_000L)

        assertTrue(addresses.playerFailedOn("song"))
        assertNull(addresses.of("song", 1_000L))
    }
}
