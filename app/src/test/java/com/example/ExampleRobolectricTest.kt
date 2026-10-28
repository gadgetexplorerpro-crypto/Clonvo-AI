package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AudioStudioEngine
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Clonvo AI", appName)
    }

    @Test
    fun `audio studio engine analyzes 180Hz wav file accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AudioStudioEngine(context)
        val tempWav = File(context.cacheDir, "test_180hz.wav")
        val sampleRate = 44100
        val seconds = 16
        val totalSamples = sampleRate * seconds
        val pcmBytes = ByteArray(totalSamples * 2)
        val buf = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val sampleVal = (sin(2.0 * Math.PI * 180.0 * t) * 14000).toInt().toShort()
            buf.putShort(sampleVal)
        }
        writeTestWav(tempWav, pcmBytes, sampleRate)

        val report = engine.analyzeAudioFile(tempWav)
        assertTrue(report.isDurationValid)
        assertTrue("Expected F0 near 180Hz, got ${report.fundamentalFreqHz}", report.fundamentalFreqHz in 160.0..200.0)
        tempWav.delete()
    }

    private fun writeTestWav(file: File, pcm: ByteArray, sampleRate: Int) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeBytes("RIFF")
            raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36 + pcm.size).array())
            raf.writeBytes("WAVEfmt ")
            raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array())
            raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array())
            raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array())
            raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array())
            raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate * 2).array())
            raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2).array())
            raf.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(16).array())
            raf.writeBytes("data")
            raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(pcm.size).array())
            raf.write(pcm)
        }
    }
}
