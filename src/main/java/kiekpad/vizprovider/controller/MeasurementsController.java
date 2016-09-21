package kiekpad.vizprovider.controller;

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
import kiekpad.vizprovider.util.aggreation.AggregatedResultSet;
import kiekpad.vizprovider.util.aggreation.MeanAggregator;

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

			for (ObjectNode node : new AggregatedResultSet(results, new MeanAggregator())) {
				measurements.add(node);
			}

		} catch (NoHostAvailableException exception) {
			// The database is currently not available
			throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
		}

		return measurements;
	}

	@RequestMapping("/series")
	public ArrayNode series() {
		ArrayNode series = jsonNodeFactory.arrayNode();

		final Select statement = QueryBuilder.select("series_id").distinct().from("measurements");
		try {
			final ResultSet results = this.cassandraService.getSession().execute(statement);
			for (Row row : results) {
				String name = row.getString("series_id");
				series.add(jsonNodeFactory.textNode(name));
			}
		} catch (NoHostAvailableException exception) {
			// The database is currently not available
			throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
		}

		return series;
	}
}
