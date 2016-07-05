package kiekpad.vizprovider.service;

import org.springframework.stereotype.Service;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

@Service
public class CassandraService {

	private static final String IP_ADDRESS = "192.168.99.100";
	private static final int PORT = 32770;
	private static final String KEYSPACE = "demo3";

	private final Session session;

	public CassandraService() {
		final Cluster cluster = Cluster.builder().addContactPoint(IP_ADDRESS).withPort(PORT).build();
		this.session = cluster.connect(KEYSPACE);
	}

	public Session getSession() {
		return session;
	}

}
