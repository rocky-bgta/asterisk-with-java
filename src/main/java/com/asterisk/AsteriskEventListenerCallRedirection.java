package com.asterisk;

import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.RedirectAction;
import org.asteriskjava.manager.action.StatusAction;
import org.asteriskjava.manager.event.*;

import java.util.Timer;
import java.util.TimerTask;

public class AsteriskEventListenerCallRedirection implements ManagerEventListener {

    private ManagerConnection managerConnection;

    private static final String ASTERISK_IP = "192.168.0.180";
    private static final String AMI_USERNAME = "mediaoffice";
    private static final String AMI_PASSWORD = "mediaoffice";
    private static final long KEEP_ALIVE_INTERVAL = 30000; // 30 seconds

    public AsteriskEventListenerCallRedirection() {
        ManagerConnectionFactory factory = new ManagerConnectionFactory(
                ASTERISK_IP,  // Asterisk server hostname
                AMI_USERNAME,  // AMI username
                AMI_PASSWORD   // AMI password
        );
        managerConnection = factory.createManagerConnection();
    }

    public void run() throws Exception {
        managerConnection.addEventListener(this);
        managerConnection.login();

        // Schedule a task to send a dummy action periodically for keep-alive
        Timer keepAliveTimer = new Timer();
        keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendKeepAliveAction();
            }
        }, KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL);
    }

    private void sendKeepAliveAction() {
        try {
            // Send a dummy action to keep the connection alive
            managerConnection.sendAction(new StatusAction());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onManagerEvent(ManagerEvent event) {
        if (event instanceof NewChannelEvent) {
            NewChannelEvent newChannelEvent = (NewChannelEvent) event;
            String extension = newChannelEvent.getExten();
            // Check if call is from specific extension
            if (extension.equals("6000") || extension.equals("6001")) {
                String uniqueId = newChannelEvent.getUniqueId();
                System.out.println("Incoming call from extension " + extension + " with Unique ID: " + uniqueId);
                // Redirect the call to another extension
                redirectCall(uniqueId, "5000"); // Redirect to extension 5000
            }
        }
    }

    private void redirectCall(String uniqueId, String targetExtension) {
        try {
            System.out.println("call redirect to " + targetExtension + " with Unique ID: " + uniqueId);
            RedirectAction redirectAction = new RedirectAction();
            redirectAction.setChannel(uniqueId);
            redirectAction.setContext("from-internal"); // Change to the appropriate context
            redirectAction.setExten(targetExtension);
            redirectAction.setPriority(1);
            managerConnection.sendAction(redirectAction);
            System.out.println("Call redirected to extension " + targetExtension);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        AsteriskEventListenerCallRedirection eventListener = new AsteriskEventListenerCallRedirection();
        eventListener.run();
    }
}
