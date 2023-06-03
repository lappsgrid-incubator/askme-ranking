package org.lappsgrid.askme.ranking

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass
import org.junit.Ignore;
import org.junit.Test;
import org.lappsgrid.askme.core.Configuration;
import org.lappsgrid.rabbitmq.Message;
import org.lappsgrid.rabbitmq.RabbitMQ;
import org.lappsgrid.rabbitmq.topic.MessageBox;
import org.lappsgrid.rabbitmq.topic.PostOffice;

import static org.junit.Assert.*;

/**
 *
 */
@Ignore
public class MainTest
{
	static Configuration config
	Main app
	Object lock
	final String MAILBOX = "ranking-test-mailbox"

	@BeforeClass
	static void init() {
		config = new Configuration()
	}

//	@Before
	void setup() {
		lock = new Object()
//		app = new Main()
		Thread.start {
			println "Running the app"
			app.run(lock)
		}
		println "Setup complete."
	}

	@After
	void teardown() {
//        app.stop()
		app = null
	}

	@Test
	void ping() {
		boolean passed = false
		println "Creating the return mailbox."
		MessageBox box = new MessageBox(config.EXCHANGE, MAILBOX, config.HOST) {

			@Override
			void recv(Message message) {
				passed = message.command == "PONG"
				synchronized (lock) {
					lock.notifyAll()
				}
			}
		}

		println "Opening the post office."
		PostOffice po = new PostOffice(config.EXCHANGE, config.HOST)
		println "creating the message"
		Message message = new Message()
				.command("PING")
				.route(config.RANKING_MBOX)
				.route(MAILBOX)
		println "Sending the message"
		po.send(message)
		println "Waiting for the lock"
		synchronized (lock) {
			lock.wait(1000)
		}
		println "Closing post office and mailbox"
		po.close()
		box.close()
		assert passed
	}

	@Test
	void metrics() {
		PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
//		registry.
		Counter counter = registry.counter("counts", "test", "groovy")
		Timer timer = registry.timer("timers", "test", "groovy")
//		long start = System.currentTimeMillis()
//		long duration = System.currentTimeMillis() - start
		timer.record {
			new ClassLoaderMetrics().bindTo(registry)
			new JvmMemoryMetrics().bindTo(registry)
			new JvmGcMetrics().bindTo(registry)
			new ProcessorMetrics().bindTo(registry)
			new JvmThreadMetrics().bindTo(registry)
		}
		counter.increment()
		println "Scraping"
		println registry.scrape()
	}
}