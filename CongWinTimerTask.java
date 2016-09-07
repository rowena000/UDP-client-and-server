import java.util.TimerTask;

public class CongWinTimerTask extends TimerTask  {
	UDPClient udpClient;
	long totalTime;
	boolean loss;
	
	public CongWinTimerTask(UDPClient udpClient) {
		this.udpClient = udpClient;
		totalTime = 0L;
		loss = false;
	}
	
	public void run() {
    	if (!loss) {
    		//if there is no loss in the RTT, increase to next prime number
			String str = "----- " + totalTime + " ms, congwin increase from " + udpClient.congwin;
	    	udpClient.congwin = getNextPrime(udpClient.congwin);
		    udpClient.debug(str + " to " + udpClient.congwin);
		    
		} else {
    		//if there is loss, decrease. If window size is 3, decrease to 1 (start with 1)
			String str = "----- " + totalTime + " ms, congwin drop from " + udpClient.congwin;
    		udpClient.congwin = udpClient.getPrimeFiftyLess(udpClient.congwin);
    		udpClient.debug(str + " to " + udpClient.congwin); 
    		udpClient.tail = udpClient.head;
    		loss = false;
//    		udpClient.printCongList();
    	}
    	
    	udpClient.fillCongWindow("Timer");
    	
//    	StringBuilder sb = new StringBuilder();
//    	sb.append("Send: ");
//    	
//    	while (udpClient.tail - udpClient.head < udpClient.congwin) {
//    		udpClient.tail++;
//    		try {
//				udpClient.send(udpClient.tail - 1, "TIMER");
//				sb.append((udpClient.tail -1) + ", ");
//			} catch (Exception e) {
//
//			}
//		}
//    	
//    	if (!"Send: ".equals(sb.toString())) {
//    		sb.append("\n");
//    		udpClient.debug(sb.toString());
//    	}
    	
    	totalTime += 10L;
    }
	
    private int getNextPrime(int start) {
		for (int i = start + 1; i < udpClient.maxwin; i++) {
			if (udpClient.inPrimeList(i)) {
				return i;
			}
		}
		return udpClient.maxwin;
	}
}
