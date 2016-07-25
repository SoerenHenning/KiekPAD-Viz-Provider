package kiekpad.vizprovider.service;

import org.springframework.stereotype.Service;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

@Service
public class CassandraService {

	private static final String IP_ADDRESS = "192.168.99.100";
	private static final int PORT = 32770;
	private static final String KEYSPACE = "demo3";

	private Session session; // TODO final

	public CassandraService() {
		try {
			final Cluster cluster = Cluster.builder().addContactPoint(IP_ADDRESS).withPort(PORT).build();
			this.session = cluster.connect(KEYSPACE);
		} catch (NoHostAvailableException exception) {
			this.session = null;
		}
	}

	public Session getSession() {
		return session;
	}

}
