package cpsLib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Optional;
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
		String[] msg = null;
		if (q instanceof BlockingQueue<?>) {
			BlockingQueue<String> b = (BlockingQueue<String>) q;
			msg = b.take().split(C.LIMITER, 3);
		} else {
			String tmp = q.poll();
			if (tmp != null)	msg = tmp.split(C.LIMITER, 3);
		}
		//if (msg != null)	System.out.println("["+myName+"] Receiving: CMD="+msg[0]+", ID="+msg[1]+", MSG="+msg[2]);
		return msg;
	}

	
	/**
	 * @param q	BlockingQueue to poll
	 * @param i	Timeout in ms
	 * @return String[3] or null if empty
	 * @throws InterruptedException
	 */
	public String[] getNext(BlockingQueue<String> q, int i) throws InterruptedException {
		String[] msg = null;
		String tmp = q.poll(i, TimeUnit.MILLISECONDS);
		if (tmp != null) {
			msg = tmp.split(C.LIMITER, 3);
		} else {
			return null;
		}
		//if (msg != null)	System.out.println("[" + myName + "] Receiving: CMD=" + msg[0] + ", ID=" + msg[1] + ", MSG=" + msg[2]);
		return msg;
	}
	
	public void sendMessage(String topic, String cmd, String msg) {
		//System.out.println("["+myName+"] Sending: TOPIC="+topic+", CMD="+cmd+", ID="+myName+", MSG="+msg);
		mq.publish(topic, cmd + C.LIMITER + myName + C.LIMITER + msg);
	}
	
//	// TODO: byte methode?
//	public void sendMessage(String topic, String cmd, byte[] msg) {
//		System.out.println("["+myName+"] Sending: TOPIC="+topic+", CMD="+cmd+", ID="+myName+", MSG="+msg);
//		mq.publish(topic, msg);
//	}

	public static Optional<String> convertToString(final Serializable object) {
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(object);
			return Optional
					.of(Base64.getEncoder().encodeToString(baos.toByteArray()));
		} catch (final IOException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Serializable> Optional<T> convertFrom(
			final String objectAsString) {
		final byte[] data = Base64.getDecoder().decode(objectAsString);
		try (final ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(data))) {
			return Optional.of((T) ois.readObject());
		} catch (final IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

	public MQTTWrapper getMQTT() {
		return mq;
	}
	
	public String getName() {
		return myName;
	}
}
