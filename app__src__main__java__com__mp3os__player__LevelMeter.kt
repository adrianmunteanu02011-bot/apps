package com.mp3os.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/** Live loudness numbers (0..1) that the UI reads every frame to make the covers dance. */
object AudioLevels {
    @Volatile var bass = 0f
    @Volatile var level = 0f
    /** System.nanoTime() of the last update; lets the UI notice if the meter isn't running. */
    @Volatile var stamp = 0L
}

/**
 * Pass-through audio processor: the sound is not changed, it only measures the PCM that goes by.
 * No microphone / RECORD_AUDIO permission is needed because it listens inside the player.
 */
@OptIn(UnstableApi::class)
class LevelMeter : BaseAudioProcessor() {

    private var channels = 2
    private var alpha = 0.02f
    private var lowPass = 0f
    private var bassPeak = 0.05f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) return AudioProcessor.AudioFormat.NOT_SET
        channels = max(1, inputAudioFormat.channelCount)
        alpha = 1f - exp(-2f * PI.toFloat() * 140f / inputAudioFormat.sampleRate)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return

        val shorts = inputBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder()).asShortBuffer()
        val frames = shorts.remaining() / channels
        if (frames > 0) {
            var sumAll = 0.0
            var sumBass = 0.0
            for (f in 0 until frames) {
                var m = 0f
                for (c in 0 until channels) m += shorts.get(f * channels + c)
                m /= channels * 32768f
                sumAll += m * m
                lowPass += alpha * (m - lowPass)
                sumBass += lowPass * lowPass
            }
            val rmsAll = sqrt(sumAll / frames).toFloat()
            val rmsBass = sqrt(sumBass / frames).toFloat()

            // auto gain: track the recent bass peak so quiet and loud songs both move the same amount
            bassPeak = max(max(rmsBass, 0.03f), bassPeak * 0.998f)
            val norm = (rmsBass / bassPeak).coerceIn(0f, 1f)
            AudioLevels.bass = norm * norm
            AudioLevels.level = (rmsAll * 3.2f).coerceIn(0f, 1f)
            AudioLevels.stamp = System.nanoTime()
        }

        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    override fun onFlush() {
        lowPass = 0f
        AudioLevels.bass = 0f
        AudioLevels.level = 0f
    }
}
