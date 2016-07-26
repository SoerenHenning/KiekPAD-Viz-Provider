package kiekpad.vizprovider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kiekpad.vizprovider.service.CassandraService;

@RestController
public class MeasurementsController {

	private final CassandraService cassandraService;

	@Autowired
	public MeasurementsController(final CassandraService cassandraService) {
		this.cassandraService = cassandraService;
	}

	@RequestMapping("/measurements")
	public ArrayNode measurements(@RequestParam(value = "after", defaultValue = "0") final long after) {

		JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

		ArrayNode array = jsonNodeFactory.arrayNode();

		final Select statement = QueryBuilder.select("time", "measurement", "prediction", "anomalyscore")
				.from("measurements")
				.where(QueryBuilder.eq("series_id", "temp2")) // TODO insert value
				.and(QueryBuilder.gt("time", after))
				.orderBy(QueryBuilder.asc("time"));
		final ResultSet results = this.cassandraService.getSession().execute(statement);

		for (Row row : results) {

			ObjectNode node = jsonNodeFactory.objectNode();
			node.set("time", jsonNodeFactory.numberNode(row.getTimestamp("time").toInstant().toEpochMilli()));
			node.set("measurement", jsonNodeFactory.numberNode(row.getDouble("measurement")));
			node.set("prediction", jsonNodeFactory.numberNode(row.getDouble("prediction")));
			node.set("anomalyscore", jsonNodeFactory.numberNode(row.getDouble("anomalyscore")));

			array.add(node);

		}

		return array;
	}

}
