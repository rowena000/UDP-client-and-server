import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class UDPClient {

	private static final int TIMEOUT = 30;
	private static final int BUFFER_SIZE = 32 * 1024;
	private static final int RUNNING_TIME = 60; //60 seconds, for one minute
	boolean debugEnable = false;
	
//	private static final int[] MAXWINS = {1, 2, 4, 8, 16, 32, 64, 128, 256};
	private static final int[] MAXWINS = {1};
//	private static final int[] PAYLOAD_SIZES = {32, 512, 1400, 8192};
	
	public static void main(String[] args) {
		
		if (args.length != 4) {
			System.out.println("Invalid args number");
			System.exit(0);
		}
		
		String ip = args[0];
		String sp = args[1];
		
		try {
			if (!validIP(ip)) {
				System.out.println("Invalid args");
				System.exit(0);
			}
			int port = Integer.parseInt(sp);
			int payloadSize = Integer.parseInt(args[2]);
			int MAXWIN = Integer.parseInt(args[3]);
			
//			UDPClient client = new UDPClient(ip, port, 8, 128);
//			client.run(1000);	
//			System.out.println(client.report);
			
			ArrayList<UDPClient> clients = new ArrayList<UDPClient>();
			
//			for (int count = 1; count <=1; count++) {
//				for (int j = 0; j < PAYLOAD_SIZES.length; j++) {
//					for (int i = 0; i < MAXWINS.length; i++) {
						System.out.println("Running with MAXWIN: " + MAXWIN 
								+ ", PAYLOAD_SIZE = " + payloadSize);
						UDPClient client = new UDPClient(ip, port, MAXWIN, payloadSize);
						client.run(RUNNING_TIME*1000);	
						System.out.println("-----------------------");
						clients.add(client);
//					}
//				}
//			}
			
			for (int i = 0; i < clients.size(); i++) {
				System.out.println("-----------------------");
				System.out.println(clients.get(i).report);	
			}

		} catch (Exception e) {
			System.out.println("Invalid args");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	String address;
	int port;
	DatagramSocket client;
	
	int head;
	int tail;
	
	int congwin;
	int maxwin;
	int payloadSize; //PAYLOAD SIZE OF: 32B, 512B, 1400B, and 8192B
	
	private ArrayList<Integer> primeList;
	private HashSet<Integer> congList;
	HashSet<Integer> ackSet;
	Hashtable<Integer, Integer> transmitCount;
	
	Timer timer;
	CongWinTimerTask congTask;
	
	String report;
	
	public UDPClient(String address, int port, int maxwin, int payloadSize) {
		this.address = address;
		this.port = port;
		this.maxwin = maxwin;
		this.payloadSize = payloadSize;
		
		congList = new HashSet<Integer>();
		ackSet = new HashSet<Integer>();
		timer = new Timer();
		transmitCount = new Hashtable<Integer, Integer>();	
		head = 0;
		
		try {
			client = new DatagramSocket();
			client.setSoTimeout(60*1000);
		} catch (Exception e) {
			return;
		}
		primeList = new ArrayList<Integer>();
		for (int i =0; i <= maxwin; i++) {
			if (UDPClient.isPrime(i)) {
				primeList.add(i);
			}
		}
	}
	
	public void run(int seconds) {
		long startTime, lastAckTime;
		
		//Secdule task for congwin, for every 10 ms, increase the cong window size
		congTask = new CongWinTimerTask(this);
		timer.schedule(congTask, 0, 10);
		lastAckTime = startTime = System.currentTimeMillis();
		
		//run for {seconds} seconds
 		for (long stop = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(seconds); stop>System.nanoTime();) { 
			try {
				int ack = receive(client);
				debug("Receive ack:" + ack);
		        lastAckTime = System.currentTimeMillis();
		        
		        //add packet num to ackSet, remove from congList
		        ackSet.add(ack);
		        congList.remove((Integer)ack);
		        
		        //move head to lowest unacked number
		        // for example, 1, 3,4 sent, 2 acked.
		        // when 1 acked, the head should move to 3
		        while (ackSet.contains(head)) {
		        	head++;
		        }
		        
		        //the received packet removed from congwin, send new packet if needed
		        fillCongWindow("ACK");
		        
		    } catch (Exception e) {
		    }
		 }
 		
 		timer.cancel();
 		
		long timeDiff = lastAckTime - startTime;
		
		int lostByte = 0;
		int lossCount = 0;
		for (Integer key : transmitCount.keySet()) {
			int count = transmitCount.get(key);
			if (count > 1) {
				lossCount += count - 1;
				lostByte++;
			}
		}
		lossCount += congList.size();
		lostByte += congList.size();
		BigInteger b1 = new BigInteger(Integer.toString(payloadSize));
		BigInteger b2 = new BigInteger(Integer.toString(lostByte));
		
		report = "Running Config is MAXWIN = " + maxwin + ", PAYLOAD_SIZE = " + payloadSize + ". "
				+ "\nLoss Packet Count: " + lossCount 
				+ ", Acked packet num: " + ackSet.size()
				+ "\nLostByte = " + lostByte + "*" + payloadSize + " = " + b1.multiply(b2)
				+ "\nTime needed " + timeDiff + " ms.";
	}
	
	public int getNextSendNumber() {
		/**
		 * For example,
		 * 1, 3, 4 in in cong window, 2 acked
		 * now 1 acked, the next sent num should be 5
		 */
		int num = head;
		while (congList.contains(num) || ackSet.contains(num)) {
			num++;
		}
		return num;
	}
	
	public void debug(String msg) {
		if (debugEnable) {
			System.out.println(msg);
		}
	}
	
	public void dropCongWin(int packNum) {
		//1. tell congwin timer task packet loss, need to drop window size this RTT
		congTask.loss = true;
		
		//2. remove the packet from congwin
		congList.remove((Integer)packNum);
		
		//3. fill congwindow if needed since the packet has removed from window
		fillCongWindow("timeout");
	}
	
	public void fillCongWindow(String source) {
		//if congwin is not filled, sent new data
        while (congList.size() < congwin) {
        	int sendNum = getNextSendNumber();
        	try {
        		send(sendNum, source);
        	} catch (Exception e) {
        		
        	}
        }
	}
	
	public void send(int packNum, String source) 
			throws Exception {
		//send a packet, create a timer for this packet, start counting down 30 ms
		byte[] sendData = encode(packNum, getPayload());
		InetAddress IPAddress = InetAddress.getByName(address);
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		PacketTimerTask task = new PacketTimerTask(packNum, this);
		
		//add the packet to congwin
		congList.add(packNum);
		//send packet, and schedule a timer for it, count down for 30ms
		timer.schedule(task, TIMEOUT);
		client.send(sendPacket);
		
//		String sendStr = "Send";
		if (transmitCount.containsKey((Integer)packNum)) {
			int count = transmitCount.get((Integer)packNum);
			transmitCount.put((Integer)packNum, count+1);
//			System.out.println("Retransmit packNum: " + packNum);
		} else {
			transmitCount.put((Integer)packNum, 1);
		}
		debug("Send packNum: " + packNum 
				+ ", source: " + source);
		
//		printCongList();
	}
	
	private int receive(DatagramSocket client) throws Exception {

		byte buffer[] = new byte[BUFFER_SIZE];
		DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
		client.receive(pack);
		int ackNum = decodeNumber(pack.getData());
		return ackNum;
	}
	
	public byte[] encode(int in, byte[] msgByte) {
		byte[] data = new byte[2]; // <- assuming "in" value in 0..65535 range and we can use 2 bytes only
		data[0] = (byte)(in & 0xFF);
		data[1] = (byte)((in >> 8) & 0xFF);
		
	
		byte[] concat = concat(data, msgByte);
		return concat;
	}
	
	private byte[] concat(byte[] a, byte[] b) {
		int aLen = a.length;
		int bLen = b.length;
		byte[] c= new byte[aLen+bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}
	
	public byte[] getPayload() {
		byte[] payload = new byte[payloadSize];
		for (int i = 0; i < payloadSize; i++) {
			payload[i] = 0;
		}
		
		return payload;
	}
	
	private static int decodeNumber(byte[] arr) {
		if (arr.length < 2) {
			return -1; 
		}
		
		byte[] data = new byte[2];
		data[0] = arr[0];
		data[1] = arr[1];
		
		int high = data[1] >= 0 ? data[1] : 256 + data[1];
		int low = data[0] >= 0 ? data[0] : 256 + data[0];
		int num = low | (high << 8);
		
		return num;
	}
	
	public int getPrimeFiftyLess(int currwin) {
		int half = currwin/2;
		for (int i = half; i > 1; i--) {
			if (inPrimeList(i)) {
				return i;
			}
		}
		return 1;
	}
	
	public static boolean isPrime(int n) {
		if (n <= 0) {
			return false;
		}
		
		if (n % 2 == 0 && n != 2) {
			return false;
		}
		
	    for (int i=2; 2*i < n; i++) {
	        if(n% i==0) {
	            return false;
	        }
	    }
	    return true;
	}
	
	public boolean inPrimeList(int n) {
		return primeList.contains(n);
	}
	
//	public void printCongList() {
//		String str = "\tCongList: [";
//		for (int i : congList) {
//			str += i + ", ";
//		}
//		str += "]";
//		debug(str);
//	}
	
	private static boolean validIP (String ip) {
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (Exception nfe) {
	        return false;
	    }
	}
}


