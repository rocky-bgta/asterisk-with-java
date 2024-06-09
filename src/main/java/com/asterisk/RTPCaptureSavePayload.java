package com.asterisk;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.packet.Payload;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RTPCaptureSavePayload {
    public static void main(String[] args) throws PcapNativeException {
        // List all available network interfaces
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        if (allDevs == null || allDevs.isEmpty()) {
            System.out.println("No network interfaces found.");
            return;
        }

        System.out.println("Available Network Interfaces:");
        for (int i = 0; i < allDevs.size(); i++) {
            System.out.println(i + ": " + allDevs.get(i).getName() + " [" + allDevs.get(i).getDescription() + "]");
        }

        // Select a network interface
        Scanner scanner = new Scanner(System.in);
        System.out.print("Select a network interface (index): ");
        int index = scanner.nextInt();

        PcapNetworkInterface nif = allDevs.get(index);

        // Get the selected network interface name
        String interfaceName = nif.getName();

        // Open the selected network interface for packet capture
        StringBuilder errbuf2 = new StringBuilder();
        Pcap pcap = Pcap.openLive(interfaceName, 65536, Pcap.MODE_PROMISCUOUS, 1000, errbuf2);

        if (pcap == null) {
            System.err.println("Error opening adapter: " + errbuf2.toString());
            return;
        }

        // Create an AudioFormat for G.711 (PCMU or PCMA) audio
        AudioFormat audioFormat = new AudioFormat(8000, 16, 1, true, false);

        // Perform packet capture and processing indefinitely
        pcap.loop(Pcap.LOOP_INFINITE, new MyPacketHandler(audioFormat), null);

        // Close the pcap handle (This will not be reached because the loop is infinite)
        pcap.close();
    }

    private static class MyPacketHandler implements JPacketHandler<StringBuilder> {
        private AudioFormat audioFormat;
        private ByteArrayOutputStream byteArrayOutputStream;

        public MyPacketHandler(AudioFormat audioFormat) {
            this.audioFormat = audioFormat;
            this.byteArrayOutputStream = new ByteArrayOutputStream();
        }

        public void nextPacket(JPacket packet, StringBuilder user) {
            // Check if it's an RTP packet (usually UDP port range 16384-32767)
            if (packet.hasHeader(Payload.ID)) {
                Payload payload = packet.getHeader(new Payload());
                byte[] payloadData = payload.getByteArray(0, payload.size());


                // Convert each byte to hexadecimal
                StringBuilder payloadHex = new StringBuilder();
                for (byte b : payloadData) {
                    payloadHex.append(String.format("%02X", b));
                }

                System.out.println("RTP Payload (hex): " + payloadHex.toString());

                // Decode RTP payload if needed (assuming it's already G.711 encoded)
                byte[] decodedData = payloadData;

                // Write decoded payload to ByteArrayOutputStream
                byteArrayOutputStream.write(decodedData, 0, decodedData.length);
            }
        }

        private void saveAsWav(byte[] audioData) {
            // Write audioData to a WAV file
            try {
                AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(audioData), audioFormat, audioData.length);
                File wavFile = new File("captured_audio.wav");
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile);
                System.out.println("Audio saved to: " + wavFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            // Save captured audio as a WAV file
            saveAsWav(byteArrayOutputStream.toByteArray());
        }
    }
}
