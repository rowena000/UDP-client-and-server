import java.util.TimerTask;

public class PacketTimerTask extends TimerTask {

	UDPClient client;
	int packNum;
	
	public PacketTimerTask(int packNum, UDPClient c) {
		this.client = c;
		this.packNum = packNum;
	}
	
	public void run() {
		//if 30 ms reach, the current packet is timeout
		
		if (!client.ackSet.contains(packNum)) {
			client.debug("Timeout for packet " + packNum + ". ");
			client.dropCongWin(packNum);
		}
		
//		if (client.head <= packNum) {
//			client.dropCongWin();
//			client.debug("Timeout for packet " + packNum + ". " + this);
//		} else {
//			if (client.head > packNum) {
//				client.debug("\t\tDEBUG - current packet " + packNum + " has received before timeout");
//			}
//		}
	}
}
