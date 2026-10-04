package uk.krodity.blinkword.data.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsVoiceTest {

    @Test
    fun `reads the variant after the hash`() {
        assertEquals("Male 1", voiceVariantLabel("en-us-x-sfg#male_1-local"))
        assertEquals("Female 3", voiceVariantLabel("en-gb-x-gba#female_3-local"))
    }

    @Test
    fun `drops the network suffix too`() {
        assertEquals("Male 2", voiceVariantLabel("en-us-x-tpd#male_2-network"))
    }

    @Test
    fun `falls back to the trailing segment when there is no variant`() {
        assertEquals("Local", voiceVariantLabel("en-us-x-sfg-local"))
    }

    @Test
    fun `handles a bare name`() {
        assertEquals("Fred", voiceVariantLabel("fred"))
    }
}
