package kiekpad.vizprovider.util.aggreation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AggregatedResultSet implements Iterable<ObjectNode> {

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
