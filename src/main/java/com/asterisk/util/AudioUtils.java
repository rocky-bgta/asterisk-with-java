package com.asterisk.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class AudioUtils {

    private static final int SAMPLE_RATE = 8000;
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;

    public static byte[] decodeULaw(byte[] ulawData) {
        byte[] pcmData = new byte[ulawData.length * 2];
        for (int i = 0; i < ulawData.length; i++) {
            short s = ulawDecode(ulawData[i]);
            pcmData[i * 2] = (byte) (s & 0xFF);
            pcmData[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return pcmData;
    }

    public static short ulawDecode(byte ulawByte) {
        ulawByte = (byte) ~ulawByte;
        int sign = (ulawByte & 0x80);
        int exponent = (ulawByte & 0x70) >> 4;
        int mantissa = (ulawByte & 0x0F);
        int sample = (mantissa << 3) + 0x84;
        sample <<= (exponent + 2);
        sample -= 0x84;
        return (short) ((sign == 0) ? sample : -sample);
    }

    public static void saveAsWav(byte[] audioData, String filename) throws IOException, UnsupportedAudioFileException {
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, true, false);
        AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / 2);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
        ais.close();
    }
}
