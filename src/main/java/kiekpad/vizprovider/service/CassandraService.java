package kiekpad.vizprovider.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

@Service
public class CassandraService {

	private final static int WAITING_SLEEP_MILLIS = 1000;

	private Session session;

	@Autowired
	public CassandraService(@Value("${cassandra.address}") final String host, @Value("${cassandra.port}") final int port,
			@Value("${cassandra.keyspace}") final String keyspace, @Value("${cassandra.timeout}") final int timeoutInMillis) {
		createSession(host, port, keyspace, timeoutInMillis);
	}

	public Session getSession() {
		return session;
	}

	private void createSession(final String host, final int port, final String keyspace, final int timeoutInMillis) {
		final Instant start = Instant.now();

		System.out.println("Use host: " + host); // TODO
		System.out.println("Use port: " + port); // TODO

		Cluster cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
		while (true) {
			try {
				this.session = cluster.connect(keyspace);
				break;
			} catch (NoHostAvailableException exception) {
				// Host not unavailable
				System.out.println("Waiting for host..."); // TODO
				if (Duration.between(start, Instant.now()).toMillis() < timeoutInMillis) {
					cluster.close();
					cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
					try {
						Thread.sleep(WAITING_SLEEP_MILLIS);
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				} else {
					throw exception;
				}
			} catch (InvalidQueryException exception) {
				// Keyspace does not exist
				System.out.println("Create Keyspace..."); // TODO
				createKeyspaceIfNotExists(cluster, keyspace);
			}
		}
	}

	private void createKeyspaceIfNotExists(final Cluster cluster, final String keyspace) {
		Session session = cluster.connect();
		session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace + " WITH replication " +
				"= {'class':'SimpleStrategy', 'replication_factor':1};");
		session.close();
	}

}
