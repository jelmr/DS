package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.Event;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.PriorityQueue;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public class Logger implements Runnable{

	private static final int MAXIMUM_QUEUE_TIME = 500;
	private static final int LOGGING_FREQUENCY = 100;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private class QueuedEvent implements Comparable<QueuedEvent> {
		Event e;
		long timestamp;


		QueuedEvent(Event e) {
			this.e = e;
			timestamp = System.currentTimeMillis();
		}


		@Override
		public int compareTo(QueuedEvent o) {
			return e.getTimestamp().compareTo(o.e.getTimestamp());
		}
	}


	private boolean running;
	private PrintStream out;
	private PriorityQueue<QueuedEvent> queue;
	private Thread printThread;


	public Logger(PrintStream out) {
		this.out = out;
		queue = new PriorityQueue<>();
		running = false;
		this.printThread = new Thread(this);
		this.printThread.start();
	}

	public void log(Event e) {
		this.queue.add(new QueuedEvent(e));
	}

	@Override
	public void run() {

		this.running = true;

		while (running) {
			// poll the nodes

			QueuedEvent event = this.queue.peek();
			// If the event has been in the queue for the maximum duration, just log it.

			while ((event = this.queue.peek()) != null
					&& (event.timestamp + MAXIMUM_QUEUE_TIME) < (System.currentTimeMillis())) {
				queue.poll();
				printEvent(event.e);
			}
			// sleep
			try {
				Thread.sleep(LOGGING_FREQUENCY);
			} catch (InterruptedException ex) {
				assert(false) : "Logger print thread was interrupted";
			}

		}

	}

	public void close() {
		running = false;
		try {
			this.printThread.join();
		} catch (InterruptedException ex) {
			assert(false) : "Logger stopPrintingThread was interrupted";
		}

	}

	private void printEvent(Event e) {
		String timestamp = DATE_FORMAT.format(new Date());
		out.printf("[%s] %s\n", timestamp, e.getLogString());
	}
}
