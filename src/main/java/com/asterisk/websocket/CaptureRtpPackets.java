package com.asterisk.websocket;

import org.pcap4j.core.*;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.UdpPort;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class CaptureRtpPackets {

    public static void main(String[] args) throws PcapNativeException, NotOpenException {
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

        // Open the selected network interface
        int snapLen = 65536;
        PromiscuousMode mode = PromiscuousMode.PROMISCUOUS;
        int timeout = 10;
        PcapHandle handle = nif.openLive(snapLen, mode, timeout);

        // Set a filter to capture only RTP packets
        String filter = "udp portrange 10000-20000";
        handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);

        // Capture packets
        System.out.println("Capturing RTP packets...");
        int packetCount = 10; // Number of packets to capture
        for (int i = 0; i < packetCount; i++) {
            Packet packet = handle.getNextPacket();
            if (packet != null) {
                UdpPacket udpPacket = packet.get(UdpPacket.class);
                if (udpPacket != null) {
                    System.out.println("RTP Packet captured:");
                    System.out.println(packet);
                }
            }
        }

        // Close the handle
        handle.close();
        System.out.println("Capture finished.");
    }
}
