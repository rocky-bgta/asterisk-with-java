package com.asterisk;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Udp;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CaptureRtpPacketsJnet {

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
        int snaplen = 64 * 1024;           // Capture all packets, no truncation
        int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
        int timeout = 10 * 1000;           // 10 seconds in millis
        Pcap pcap = Pcap.openLive(device.getName(), snaplen, flags, timeout, errbuf);

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
                        System.out.println("RTP Packet captured:");
                        System.out.println(packet);
                    }
                }
            }
        };

        // Capture packets
        System.out.println("Capturing RTP packets...");
        int packetCount = 10; // Number of packets to capture
        pcap.loop(packetCount, jpacketHandler, "jNetPcap");

        // Close the pcap handle
        pcap.close();
        System.out.println("Capture finished.");
    }
}
