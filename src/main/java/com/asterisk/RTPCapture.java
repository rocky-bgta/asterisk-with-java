package com.asterisk;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.packet.Payload;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RTPCapture {
    public static void main(String[] args) {
        // List all available network interfaces
        List<PcapIf> allDevs = new ArrayList<>();
        StringBuilder errbuf = new StringBuilder();
        int result = Pcap.findAllDevs(allDevs, errbuf);

        if (result != Pcap.OK) {
            System.err.printf("Error occurred: %s%n", errbuf.toString());
            return;
        }

        // Prompt user to select a network interface
        System.out.println("Available Network Interfaces:");
        int index = 0;
        for (PcapIf dev : allDevs) {
            System.out.println(index++ + ": " + dev.getName() + " : " + dev.getDescription());
        }
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the index of the network interface to capture from: ");
        int selectedIndex = scanner.nextInt();
        
        if (selectedIndex < 0 || selectedIndex >= allDevs.size()) {
            System.err.println("Invalid interface index.");
            return;
        }

        // Get the selected network interface name
        String interfaceName = allDevs.get(selectedIndex).getName();

        // Open the selected network interface for packet capture
        StringBuilder errbuf2 = new StringBuilder();
        Pcap pcap = Pcap.openLive(interfaceName, 65536, Pcap.MODE_PROMISCUOUS, 1000, errbuf2);

        if (pcap == null) {
            System.err.println("Error opening adapter: " + errbuf2.toString());
            return;
        }

        // Perform packet capture and processing indefinitely
        pcap.loop(Pcap.LOOP_INFINITE, new MyPacketHandler(), null);

        // Close the pcap handle (This will not be reached because the loop is infinite)
        pcap.close();
    }

    private static class MyPacketHandler implements JPacketHandler<StringBuilder> {
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
            }
        }
    }
}
