package kiekpad.vizprovider.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kiekpad.vizprovider.service.CassandraService;

@RestController
public class MeasurementsController {

	private final CassandraService cassandraService;
	private final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

	@Autowired
	public MeasurementsController(final CassandraService cassandraService) {
		this.cassandraService = cassandraService;
	}

	@RequestMapping("/measurements")
	public ArrayNode measurements(@RequestParam(value = "series") final String series, @RequestParam(value = "after", defaultValue = "0") final long after) {

		ArrayNode measurements = jsonNodeFactory.arrayNode();

		final Select statement = QueryBuilder.select("time", "measurement", "prediction", "anomalyscore")
				.from("measurements")
				.where(QueryBuilder.eq("series_id", series))
				.and(QueryBuilder.gt("time", after))
				.orderBy(QueryBuilder.asc("time"));

		try {
			final ResultSet results = this.cassandraService.getSession().execute(statement);

			for (ObjectNode node : new AggregatedResultSet(results, new TakeFirstAggregator())) {
				measurements.add(node);
			}

		} catch (NoHostAvailableException exception) {
			// The database is currently not available
			throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
		}

		return measurements;
	}

	private static interface RowsAggregator {
		public ObjectNode aggregate(List<Row> rows);
	}

	private static class TakeFirstAggregator implements RowsAggregator {

		private final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

		@Override
		public ObjectNode aggregate(final List<Row> rows) {
			long timestamp = rows.get(0).getTimestamp("time").toInstant().toEpochMilli();
			double measurement = rows.get(0).getDouble("measurement");
			double prediction = rows.get(0).getDouble("prediction");
			double anomalyscore = rows.get(0).getDouble("anomalyscore");
			boolean isAggregated = rows.size() > 1;

			ObjectNode node = this.jsonNodeFactory.objectNode();
			node.set("time", this.jsonNodeFactory.numberNode(timestamp));
			node.set("measurement", this.jsonNodeFactory.numberNode(measurement));
			node.set("prediction", this.jsonNodeFactory.numberNode(prediction));
			node.set("anomalyscore", this.jsonNodeFactory.numberNode(anomalyscore));
			node.set("isAggregated", this.jsonNodeFactory.booleanNode(isAggregated));

			return node;
		}

	}

	private static class AggregatedResultSet implements Iterable<ObjectNode> {

		private final ResultSet resultSet;
		private final RowsAggregator aggregator;

		public AggregatedResultSet(final ResultSet resultSet, final RowsAggregator aggregator) {
			this.resultSet = resultSet;
			this.aggregator = aggregator;
		}

		@Override
		public Iterator<ObjectNode> iterator() {
			return new Iterator<ObjectNode>() {

				private final Iterator<Row> rowsIterator = resultSet.iterator();
				private Row previous = null;

				@Override
				public boolean hasNext() {
					return this.rowsIterator.hasNext();
				}

				@Override
				public ObjectNode next() {

					List<Row> rows = new ArrayList<>();
					if (this.previous == null) {
						rows.add(this.rowsIterator.next());
					} else {
						rows.add(this.previous);
					}

					while (this.rowsIterator.hasNext()) {
						Row row = this.rowsIterator.next();
						if (row.getTimestamp("time").toInstant().toEpochMilli() == rows.get(0).getTimestamp("time").toInstant().toEpochMilli()) {
							rows.add(row);
						} else {
							this.previous = row;
							break;
						}
					}

					return AggregatedResultSet.this.aggregator.aggregate(rows);
				}
			};
		}

	}

}
