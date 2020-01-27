package cpsLib;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Finnbo
 *
 */
public abstract class CPSApplication {
	protected String myName;
	protected MQTTWrapper mq;
	
	public CPSApplication(String name, String address) {
		super();
		this.mq = new MQTTWrapper("broker.hivemq.com", false);
		this.myName = name+"-"+mq.getMyUUID().toString();
	}
	
	public String[] getNext(Queue<String> q) throws InterruptedException {
		String[] msg;
		if (q instanceof BlockingQueue<?>) {
			BlockingQueue<String> b = (BlockingQueue<String>) q;
			msg = b.take().split(C.LIMITER, 3);
		} else {
			msg = q.poll().split(C.LIMITER, 3);
		}
		System.out.println("["+myName+"] Receiving: CMD="+msg[0]+", ID="+msg[1]+", MSG="+msg[2]);
		return msg;
	}

	
	/**
	 * @param q	BlockingQueue to poll
	 * @param i	Timeout in ms
	 * @return String[3] or null if empty
	 * @throws InterruptedException
	 */
	public String[] getNext(BlockingQueue<String> q, int i) throws InterruptedException {
		String msg = null;
		msg = q.poll(i, TimeUnit.MILLISECONDS);
		if (msg != null) {
			return msg.split(C.LIMITER);
		} else {
			return null;
		}
	}
	
	public void sendMessage(String topic, String cmd, String msg) {
		System.out.println("["+myName+"] Sending: TOPIC="+topic+", CMD="+cmd+", ID="+myName+", MSG="+msg);
		mq.publish(topic, cmd + C.LIMITER + myName + C.LIMITER + msg);
	}
	
	public MQTTWrapper getMQTT() {
		return mq;
	}
	
	public String getName() {
		return myName;
	}
}
