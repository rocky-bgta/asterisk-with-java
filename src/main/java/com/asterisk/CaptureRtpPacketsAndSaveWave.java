package com.asterisk;

import com.asterisk.util.AudioUtils;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.tcpip.Udp;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CaptureRtpPacketsAndSaveWave {

    private static final int SNAPLEN = 64 * 1024;           // Capture all packets, no truncation
    private static final int FLAGS = Pcap.MODE_PROMISCUOUS; // capture all packets
    private static final int TIMEOUT = 10 * 1000;           // 10 seconds in millis
    private static final int PACKET_COUNT = 10;             // Number of packets to capture in each loop
    private static final ByteArrayOutputStream audioStream = new ByteArrayOutputStream();

    public static void main(String[] args) {
        List<PcapIf> alldevs = new ArrayList<>();
        StringBuilder errbuf = new StringBuilder();

        // Get a list of devices
        int r = Pcap.findAllDevs(alldevs, errbuf);
        if (r == Pcap.NOT_OK || alldevs.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errbuf.toString());
            return;
        }

        System.out.println("Available Network Interfaces:");
        int i = 0;
        for (PcapIf device : alldevs) {
            String description = (device.getDescription() != null) ? device.getDescription() : "No description available";
            System.out.printf("%d: %s [%s]\n", i++, device.getName(), description);
        }

        // Select a network interface
        Scanner scanner = new Scanner(System.in);
        System.out.print("Select a network interface (index): ");
        int index = scanner.nextInt();
        PcapIf device = alldevs.get(index);

        // Open the selected network interface
        Pcap pcap = Pcap.openLive(device.getName(), SNAPLEN, FLAGS, TIMEOUT, errbuf);
        if (pcap == null) {
            System.err.printf("Error while opening device for capture: %s", errbuf.toString());
            return;
        }

        // Set a filter to capture only RTP packets
        String filter = "udp portrange 10000-20000";
        PcapBpfProgram bpf = new PcapBpfProgram();
        if (pcap.compile(bpf, filter, 0, 0xFFFFFF00) != Pcap.OK) {
            System.err.println(pcap.getErr());
            return;
        }
        if (pcap.setFilter(bpf) != Pcap.OK) {
            System.err.println(pcap.getErr());
            return;
        }

        // Create packet handler to receive packets
        PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
            public void nextPacket(PcapPacket packet, String user) {
                Udp udp = new Udp();
                if (packet.hasHeader(udp)) {
                    int dstPort = udp.destination();
                    if (dstPort >= 10000 && dstPort <= 20000) {
                        byte[] payload = udp.getPayload();
                        byte[] decodedAudio = AudioUtils.decodeULaw(payload);
                        try {
                            audioStream.write(decodedAudio);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("RTP Packet captured:");
                        System.out.println(packet);
                    }
                }
            }
        };

        // Use ExecutorService to capture packets continuously
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            System.out.println("Capturing RTP packets...");
            while (!Thread.currentThread().isInterrupted()) {
                pcap.loop(PACKET_COUNT, jpacketHandler, "jNetPcap");
            }
            // Close the pcap handle after stopping
            pcap.close();
            System.out.println("Capture finished.");
        });

        // Shutdown the executor service gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Executor service did not terminate in the specified time.");
                }
                byte[] audioData = audioStream.toByteArray();
                AudioUtils.saveAsWav(audioData, "captured_audio.wav");
            } catch (InterruptedException | IOException | UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
        }));
    }
}
