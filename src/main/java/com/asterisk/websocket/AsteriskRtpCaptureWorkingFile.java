package com.asterisk.websocket;

import org.asteriskjava.manager.*;
import org.asteriskjava.manager.event.*;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class AsteriskRtpCaptureWorkingFile {

    private static final String ASTERISK_IP = "192.168.0.132";
    private static final String AMI_USERNAME = "mediaoffice";
    private static final String AMI_PASSWORD = "mediaoffice";

    public static void main(String[] args) throws Exception {
        AsteriskServer asteriskServer = new AsteriskServer();
        PacketCapture packetCapture = new PacketCapture();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(asteriskServer);
        executor.execute(packetCapture);

        // Shutdown executor on program termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdownNow();
        }));
    }

    static class AsteriskServer implements Runnable {
        @Override
        public void run() {
            ManagerConnectionFactory factory = new ManagerConnectionFactory(ASTERISK_IP, AMI_USERNAME, AMI_PASSWORD);
            ManagerConnection managerConnection = factory.createManagerConnection();
            try {
                managerConnection.addEventListener(new ManagerEventListener() {
                    @Override
                    public void onManagerEvent(ManagerEvent event) {
                        if (event instanceof NewChannelEvent) {
                            NewChannelEvent newChannelEvent = (NewChannelEvent) event;
                            String callId = newChannelEvent.getUniqueId();
                            System.out.println("New call started. Call ID: " + callId);
                        } else if (event instanceof HangupEvent) {
                            HangupEvent hangupEvent = (HangupEvent) event;
                            String callId = hangupEvent.getUniqueId();
                            System.out.println("Call ended. Call ID: " + callId);
                        }
                    }
                });

                managerConnection.login();
                Thread.sleep(Long.MAX_VALUE); // Keep the thread alive
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    managerConnection.logoff();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class PacketCapture implements Runnable {
        @Override
        public void run() {
            try {
                List<PcapNetworkInterface> interfaces = Pcaps.findAllDevs();
                PcapNetworkInterface networkInterface = selectNetworkInterface(interfaces);

                PcapHandle handle = networkInterface.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
                handle.setFilter("udp and port 5060 or udp port 10000", BpfProgram.BpfCompileMode.OPTIMIZE);

                while (true) {
                    Packet packet = handle.getNextPacket();
                    if (packet != null) {
                        System.out.println("Raw packet: " + packet);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static PcapNetworkInterface selectNetworkInterface(List<PcapNetworkInterface> interfaces) {
        for (PcapNetworkInterface iface : interfaces) {
            if (iface.getAddresses().stream().anyMatch(address -> address.getAddress() instanceof java.net.Inet4Address)) {
                return iface;
            }
        }
        throw new RuntimeException("No suitable network interface found");
    }
}