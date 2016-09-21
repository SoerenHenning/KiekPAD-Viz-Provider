package kiekpad.vizprovider.util.aggreation;

import java.util.List;

import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MeanAggregator implements RowsAggregator {

	private final JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

	@Override
	public ObjectNode aggregate(final List<Row> rows) {
		long timestamp = rows.get(0).getTimestamp("time").toInstant().toEpochMilli();

		double measurement = rows.stream().mapToDouble(r -> r.getDouble("measurement")).filter(m -> Double.isFinite(m)).average().orElse(Double.NaN);
		double prediction = rows.stream().mapToDouble(r -> r.getDouble("prediction")).filter(p -> Double.isFinite(p)).average().orElse(Double.NaN);
		double anomalyscore = rows.stream().mapToDouble(r -> r.getDouble("anomalyscore")).filter(s -> Double.isFinite(s)).average().orElse(Double.NaN);
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
