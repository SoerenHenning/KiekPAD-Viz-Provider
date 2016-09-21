package kiekpad.vizprovider.util.aggreation;

import java.util.List;

import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.node.ObjectNode;

interface RowsAggregator {
	public ObjectNode aggregate(List<Row> rows);
}
