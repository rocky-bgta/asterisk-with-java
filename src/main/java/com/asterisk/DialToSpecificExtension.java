package com.asterisk;

import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerResponse;

public class DialToSpecificExtension {

    private static final String ASTERISK_SERVER_HOST = "192.168.0.180";
    private static final int ASTERISK_SERVER_PORT = 5038;
    private static final String ASTERISK_USERNAME = "mediaoffice";
    private static final String ASTERISK_PASSWORD = "mediaoffice";

    public static void main(String[] args) {
        try {
            // Create a connection factory
            ManagerConnectionFactory factory = new ManagerConnectionFactory(
                    ASTERISK_SERVER_HOST, ASTERISK_SERVER_PORT, ASTERISK_USERNAME, ASTERISK_PASSWORD);

            // Create a manager connection
            ManagerConnection managerConnection = factory.createManagerConnection();

            // Connect to the Asterisk Manager
            managerConnection.login();

            // Originate the call to extension 6003
            OriginateAction originateAction = new OriginateAction();
            originateAction.setChannel("SIP/6003"); // SIP channel for extension 6003
            originateAction.setContext("from-internal"); // Context where extension 6003 is defined
            originateAction.setExten("6003"); // Extension to call
            originateAction.setPriority(1); // Priority of the extension
            //originateAction.setTimeout(30000); // Timeout in milliseconds (optional)
            originateAction.setAsync(true);
            ManagerResponse response = managerConnection.sendAction(originateAction,30000);
            System.out.println("Originate Response: " + response.getResponse());

            // Disconnect from the Asterisk Manager
            managerConnection.logoff();
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any exceptions gracefully
        }
    }
}
