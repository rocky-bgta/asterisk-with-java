package com.asterisk.util;

import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;

import java.util.List;

public class Utility {

    public static PcapNetworkInterface selectNetworkInterface() throws PcapNativeException {
        List<PcapNetworkInterface> interfaces = Pcaps.findAllDevs();
        for (PcapNetworkInterface iface : interfaces) {
            if (iface.getAddresses().stream().anyMatch(address -> address.getAddress() instanceof java.net.Inet4Address)) {
                return iface;
            }
        }
        throw new RuntimeException("No suitable network interface found");
    }

    private static void processPacket(Packet packet) {
        if (packet.contains(EthernetPacket.class)) {
            EthernetPacket ethPacket = packet.get(EthernetPacket.class);
            // Handle Ethernet packet
            System.out.println("Ethernet packet: " + ethPacket);
        } else if (packet.contains(IpV4Packet.class)) {
            IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
            // Handle IPv4 packet
            System.out.println("IPv4 packet: " + ipV4Packet);
        } else if (packet.contains(IpV6Packet.class)) {
            IpV6Packet ipV6Packet = packet.get(IpV6Packet.class);
            // Handle IPv6 packet
            System.out.println("IPv6 packet: " + ipV6Packet);
        } else {
            // Handle other packet types
            System.out.println("Unknown packet type: " + packet);
            //System.out.println("Raw packet data: " + byteArrayToHexString(packet.getRawData()));
        }
    }
}
