package com.feyiuremote.libs.Cameras.abstracts.Connection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * This address is used for UPnP (Universal Plug and Play)/SSDP (Simple Service Discovery Protocol)
 * by various vendors to advertise the capabilities of (or discover) devices on a VLAN. MAC OS, Microsoft Windows,
 * IOS and other operating systems and applications use this protocol.
 * <p>
 * Since each client device will most likely be using this protocol to advertise its capabilities to other devices,
 * UPnP/SSDP traffic volume can quickly increase due to flooding of the traffic on a network proportional to the amount of new devices
 * and services connecting to the network.
 */
public class SSDPConnection {

    private static final String TAG = SSDPConnection.class.getSimpleName();

    private final int PACKET_BUFFER_SIZE = 4096;
    protected final int SSDP_PORT = 1900;
    protected final int SSDP_RECEIVE_TIMEOUT = 2000;
    protected final int SSDP_MX = 2;
    protected final String SSDP_ADDR = "239.255.255.250";
    protected final String SSDP_ST = "urn:schemas-upnp-org:device:MediaServer:1";
    private Context context;
    private final ExecutorService executor;
    private NetworkInterface wlanInterface;

    public SSDPConnection(Context context, ExecutorService executor) {
        this.context = context;
        this.executor = executor;
    }

    public void inquire(ISSDPDiscoveryListener listener) {
        String ssdpRequest = "M-SEARCH * HTTP/1.1\r\n";
        ssdpRequest += "HOST: " + SSDP_ADDR + ":" + SSDP_PORT + "\r\n";
        ssdpRequest += "MAN: \"ssdp:discover\"\r\n";
        ssdpRequest += "MX: " + SSDP_MX + "\r\n";
        ssdpRequest += "ST: " + SSDP_ST + "\r\n\r\n";

        Log.i(TAG, ssdpRequest);

        this.inquire(ssdpRequest, listener);
    }

    /**
     * First a magic packet needs to be sent to the network
     * to initialise response from devices
     *
     * @param ssdpRequest
     * @param listener
     */
    public void inquire(String ssdpRequest, ISSDPDiscoveryListener listener) {
        byte[] sendData = ssdpRequest.getBytes(StandardCharsets.UTF_8);

        executor.execute(() -> {
            DatagramSocket socket = null;
            DatagramPacket packet;
            ArrayList<String> responses = new ArrayList();

            try {
                socket = getDatagramSocket();

                InetSocketAddress iAddress = new InetSocketAddress(SSDP_ADDR, SSDP_PORT);
                packet = new DatagramPacket(sendData, sendData.length, iAddress);

                for (int i = 0; i < 3; i++) {
                    listener.onProgressUpdate("SSDP - Sending inquiry packet (" + i + ")");
                    Thread.sleep(500);
                    socket.send(packet);
                    Thread.sleep(500);
                }

                responses = getSSDPResponses(socket);
                socket.close();

                for (String response : responses) {
                    listener.onDeviceFound(response);
                }
            } catch (Exception e) {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

                listener.onFailure("SSDP - Inquiry failed, socket closed.");
                Log.e(TAG, e.toString());
                e.printStackTrace();
            }

            listener.onFinish();
        });
    }

    /**
     * After the packet is sent, we'll wait for the response from camera
     *  @param socket
     * @return
     */
    public ArrayList<String> getSSDPResponses(DatagramSocket socket) {
        DatagramPacket receivePacket;
        byte[] buffer = new byte[PACKET_BUFFER_SIZE];
        ArrayList<String> responses = new ArrayList<>();

        while (true) {
            try {
                receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(SSDP_RECEIVE_TIMEOUT);
                socket.receive(receivePacket);

                String ssdpReplyMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), Charset.forName("UTF-8"));
                Log.d(TAG, "SSDP:" + ssdpReplyMessage);

                if (ssdpReplyMessage.contains("HTTP/1.1 200")) {
                    if (!responses.contains(ssdpReplyMessage)) {
                        responses.add(ssdpReplyMessage);
                    }
                }
            } catch (SocketTimeoutException e) {
                // This always times out, so its fine...
                // Stop receiving packets after first timeout
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        return responses;
    }

    public static String findParameterValue(String ssdpMessage, String paramName) {
        String name = paramName;

        if (!name.endsWith(":")) {
            name = name + ":";
        }

        int start = ssdpMessage.indexOf(name);
        int end = ssdpMessage.indexOf("\r\n", start);

        if (start != -1 && end != -1) {
            start += name.length();
            try {
                return ssdpMessage.substring(start, end).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Send the datagram packet on all interfaces.
     * <p>
     * Falls back to the default send() if the interfaces list is not populated
     *
     * @param packet the datagram to send
     * @throws IOException from the MulticastSocket
     */
    private void sendOnAllInterfaces(DatagramPacket packet) throws IOException {
//        if (interfaces != null && interfaces.size() > 0) {
//            for (NetworkInterface iface : interfaces) {
//                clientSocket.setNetworkInterface(iface);
//                clientSocket.send(packet);
//            }
//        } else {
//            clientSocket.send(packet);
//        }
    }

    private DatagramSocket getDatagramSocket() throws SocketException {
        Log.d(TAG, "Creating SSDP socket");

        DatagramSocket socket = new DatagramSocket();
        socket.setReuseAddress(true);

// Get the Wi-Fi network information
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();

// Convert the IP address to a string
        @SuppressLint("DefaultLocale")
        String ipString = String.format(
                "%d.%d.%d.%d",
                (ipAddress & 0xFF),
                (ipAddress >> 8 & 0xFF),
                (ipAddress >> 16 & 0xFF),
                (ipAddress >> 24 & 0xFF)
        );

// Bind the DatagramSocket to the Wi-Fi network
        try {
            socket.bind(new InetSocketAddress(ipString, SSDP_PORT));
        } catch (SocketException e) {
            // Socket is already bound
            Log.d(TAG, "Socket is already bound");
        }


        return socket;
    }

    // Method to get and set the list of network interfaces
    private void acquireWlanInterface() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface iface = networkInterfaces.nextElement();
            // You might want to filter out loopback or inactive interfaces based on your requirements
            if (iface.isUp() && !iface.isLoopback() && Objects.equals(iface.getName(), "wlan0")) {
                this.wlanInterface = iface;
            }
        }
    }


}
