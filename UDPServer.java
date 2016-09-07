import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.sound.sampled.Port;

public class UDPServer {
    private static final int BUFFER_SIZE = 32 * 1024;
    private static DatagramSocket server;
    
    
    public static void main(String[] args) {
        
        ArrayList<Integer> packets = new ArrayList<Integer>();
        
        try {
            server = new DatagramSocket();
            System.out.println("Server listen on: " + InetAddress.getLocalHost().getHostAddress() + ":" + server.getLocalPort());
            server.setReceiveBufferSize(BUFFER_SIZE);
            InetAddress clientAddr = null;
            int clientPort = 0;
            
            while (true) {
                byte buffer[] = new byte[BUFFER_SIZE];
                DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
                server.receive(pack);
                
                //as the server is a single thread server, when client changed, clear the receved packets
                if (!pack.getAddress().equals(clientAddr) || pack.getPort() != clientPort) {
                    if (clientPort != 0) {
                        System.out.println("End receiving from: " + clientAddr.toString() + ":" + clientPort 
                                + ". Received valid pack: " + packets.size());
                    }
                    clientAddr = pack.getAddress();
                    clientPort = pack.getPort();
                    packets = new ArrayList<Integer>();
                    System.out.println("Start receiving from: " + clientAddr.toString() + ":" + pack.getPort());
                }
                
                int packetNum = decodeNumber(pack.getData());
//                System.out.print("Recv: " + packetNum + " ");
               
                
                int expectNum = packets.size();
//                System.out.println("\tExpecting: " + expectNum);
                
                boolean drop = true;
                if (expectNum == packetNum) {
                    drop = false;
                    packets.add(packetNum);
//                    System.out.println("\tMatch Expecting.");
                } else if (packets.contains(packetNum)) {
                    drop = false;
//                    System.out.println("\tContains packet.");
                }
                
                
                if (drop) {
//                  System.out.println("Drop the packet.");
                }
                
                byte[] sendData = new byte[2];
                sendData[0] = (byte)(packetNum & 0xFF);
                sendData[1] = (byte)((packetNum >> 8) & 0xFF);
                    
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, pack.getAddress(), pack.getPort());
                server.send(sendPacket);
//                System.out.println("Send ack " + packetNum + " to " + pack.getAddress() + ":" + pack.getPort());
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    private static int decodeNumber(byte[] arr) {
        if (arr.length < 2) {
            return -1;
        }
        
        byte[] result = new byte[2];
        result[0] = arr[0];
        result[1] = arr[1];
        
        int high = result[1] >=0 ? result[1] : 256 + result[1];
        int low = result[0] >=0 ? result[0] : 256 + result[0];
        int num = low | (high << 8);
        
        return num;
    }
    
}
