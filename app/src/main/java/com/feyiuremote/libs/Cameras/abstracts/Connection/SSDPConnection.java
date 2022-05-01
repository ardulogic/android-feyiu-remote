package com.feyiuremote.libs.Cameras.abstracts.Connection;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private final ExecutorService executor;

    public SSDPConnection(ExecutorService executor) {
        this.executor = executor;
    }

    public void inquire(ISSDPDiscoveryListener listener) {
        String ssdpRequest = "M-SEARCH * HTTP/1.1\r\n";
        ssdpRequest += "HOST: " + SSDP_ADDR + ":" + SSDP_PORT + "\r\n";
        ssdpRequest += "MAN: \"ssdp:discover\"\r\n";
        ssdpRequest += "MX: " + SSDP_MX + "\r\n";
        ssdpRequest += "ST: " + SSDP_ST + "\r\n\r\n";

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

        executor.execute(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                DatagramPacket packet;
                ArrayList<String> responses = new ArrayList();

                try {
                    socket = new DatagramSocket();
                    socket.setReuseAddress(true);

                    InetSocketAddress iAddress = new InetSocketAddress(SSDP_ADDR, SSDP_PORT);
                    packet = new DatagramPacket(sendData, sendData.length, iAddress);

                    for (int i = 0; i < 3; i++) {
                        listener.onProgressUpdate("SSDP - Sending inquiry packet (" + i + ")");
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
                    e.printStackTrace();
                }

                listener.onFinish();
            }
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

}
