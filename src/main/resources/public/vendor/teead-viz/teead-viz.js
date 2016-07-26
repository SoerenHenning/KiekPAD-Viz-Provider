// Copyright 2016 Sören Henning
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

function TeeAdViz(domContainer, config) {
	config = config || {};

  this.width = config.width || 600; // in px
  this.measurementsHeight = config.measurementsHeight || 300; // in px
  this.anomalyscoresHeight = config.anomalyscoresHeight || 100; // in px
	this.thresholds = config.thresholds || [null, null]; // usually lower is <= 0, upper is >= 0; null for no threshold
	this.predictionVisibility = config.predictionVisibility || false;
	this.anomalyscoresVisibility = config.anomalyscoresVisibility || false;
	this.measurementsAxisLabel = config.measurementsAxisLabel || "Measurement";
	this.anomalyscoresAxisLabel = config.anomalyscoresAxisLabel || "Anomaly Score";
	this.measurementsColor = config.measurementsColor || "orange";
	this.predictionsColor = config.predictionsColor || "steelblue";
	this.anomalyscoresColor = config.anomalyscoresColor || "red";
	this.indicatorColor = config.indicatorColor || "red";
	this.measurementsClass = config.measurementsClass || "measurements";
	this.anomalyscoresClass = config.anomalyscoresClass || "anomalyscores";
	this.measurementsPlotStartWithZero = config.measurementsPlotStartWithZero || true;
	this.indicatorOffset = config.indicatorOffset || 65; // in px
	this.defaultTimeSpan = config.defaultTimeSpan || 60*1000; // one minute
	this.defaultStartTime = config.defaultStartTime || new Date();
	this.defaultMeasurementsYDomain = config.defaultMeasurementsYDomain || [0,1];
	this.defaultAnomalyscoresYDomain = config.defaultAnomalyscoresYDomain || [0,1];

	this.values = {measurements: [], predictions: [], anomalyscores: []};
	this.measurementsPlotContainer = domContainer.append("div").attr("class", this.measurementsClass);
	this.anomalyscoresPlotContainer = domContainer.append("div").attr("class", this.anomalyscoresClass);

	this.anomalyscoresPlotContainer.attr("hidden",this.anomalyscoresVisibility ? null : true);

  this.measurementsPlot = new CanvasTimeSeriesIndicatorPlot(this.measurementsPlotContainer, [this.width, this.measurementsHeight], {
    yAxisLabel: this.measurementsAxisLabel,
		disableLegend: true,
    updateViewCallback: (this.setViews).bind(this),
		indicatorColor: this.indicatorColor
  });
  this.anomalyscoresPlot = new CanvasTimeSeriesPlot(this.anomalyscoresPlotContainer, [this.width, this.anomalyscoresHeight], {
    yAxisLabel: this.anomalyscoresAxisLabel,
		disableLegend: true,
    updateViewCallback: (this.setViews).bind(this)
  });

  this.measurementsPlot.setZoomYAxis(false);
  this.anomalyscoresPlot.setZoomYAxis(false);
	this.measurementsPlot.updateDomains([this.defaultStartTime - this.defaultTimeSpan, this.defaultStartTime], this.defaultMeasurementsYDomain, false);
  this.anomalyscoresPlot.updateDomains(this.measurementsPlot.getXDomain(), this.defaultAnomalyscoresYDomain, false);
}

// public interface

TeeAdViz.prototype.setMeasurements = function(measurementsSet) {

	var anomalystates = [];
	measurementsSet.forEach(function(value) {
		this.values.measurements.push([value.time, value.measurement]);
		this.values.predictions.push([value.time, value.prediction]);
		this.values.anomalyscores.push([value.time, value.anomalyscore]);
		if ((this.thresholds[0] != null && value.anomalyscore <= this.thresholds[0]) || (this.thresholds[1] != null && value.anomalyscore >= this.thresholds[1])) {
			anomalystates.push(value.time); // Push time to list of anomaly states
		}
	}, this);

	this.measurementsPlot.removeDataSet("measurements");
	this.measurementsPlot.removeDataSet("predictions");
	this.anomalyscoresPlot.removeDataSet("anomalyscores");

	this.measurementsPlot.addDataSet("measurements", "", this.values.measurements, this.measurementsColor, false, false);
	if (this.predictionVisibility) {
		this.measurementsPlot.addDataSet("predictions", "", this.values.predictions, this.predictionsColor, false, false);
	}
	this.measurementsPlot.setIndicatorDataSet(anomalystates, false, false);
	this.anomalyscoresPlot.addDataSet("anomalyscores", "", this.values.anomalyscores, this.anomalyscoresColor, false, false);

	if (this.values.measurements.length != 0 || this.values.predictions.length != 0 || this.values.anomalyscores.length != 0) {
		this.measurementsPlot.updateDomains(this.measurementsPlot.calculateXDomain(), this.measurementsPlot.getYDomain(), true);
		this.updateDomains();
	}
};

TeeAdViz.prototype.addMeasurements = function(measurementsSet) {
	var beforeCalculatedXDomain = this.measurementsPlot.calculateXDomain();
	var beforeActualXDomain = this.measurementsPlot.getXDomain();
	var beforeEmpty = this.values.measurements.length == 0 && this.values.predictions.length == 0 && this.values.anomalyscores.length == 0;

	measurementsSet.forEach(function(value) {
		// This updated also this.values
		this.measurementsPlot.addDataPoint("measurements", [value.time, value.measurement], false, false);
		if (this.predictionVisibility) {
			this.measurementsPlot.addDataPoint("predictions", [value.time, value.prediction], false, false);
		} else {
			this.values.predictions.push([value.time, value.prediction]);
		}
		if ((this.thresholds[0] != null && value.anomalyscore <= this.thresholds[0]) || (this.thresholds[1] != null && value.anomalyscore >= this.thresholds[1])) {
			this.measurementsPlot.addIndicatorDataPoint(value.time, false, false);
		}
		this.anomalyscoresPlot.addDataPoint("anomalyscores", [value.time, value.anomalyscore], false, false);
	}, this);

	var afterCalculatedXDomain = this.measurementsPlot.calculateXDomain();
	var afterActualXDomain = this.measurementsPlot.getXDomain();

	if (beforeEmpty) {
		var xDomain;
		if (afterCalculatedXDomain[1] - afterCalculatedXDomain[0] < this.defaultTimeSpan) {
			xDomain = [afterCalculatedXDomain[0], afterCalculatedXDomain[0] + this.defaultTimeSpan];
		} else {
			xDomain = [afterCalculatedXDomain[1] - this.defaultTimeSpan, afterCalculatedXDomain[1]];
		}
		this.measurementsPlot.updateDomains(xDomain, this.measurementsPlot.getYDomain(), false);
	} else {
		if (beforeCalculatedXDomain[1] <= beforeActualXDomain[1] && afterCalculatedXDomain[1] > afterActualXDomain[1]) {
			var shifting = afterCalculatedXDomain[1] - beforeCalculatedXDomain[1];
			var xDomain = [this.measurementsPlot.getXDomain()[0]*1 + shifting  , this.measurementsPlot.getXDomain()[1]*1 + shifting];
			this.measurementsPlot.updateDomains(xDomain, this.measurementsPlot.getYDomain(), false);
		}
	}
	this.updateDomains();
};

TeeAdViz.prototype.setThresholds = function() {
	if (arguments.length >= 2) {
		this.thresholds = [(arguments[0] == null) ? null : - Math.abs(arguments[0]), arguments[1]];
	} else if (arguments.length == 1) {
		this.thresholds = [arguments[0], arguments[0]];
	}

	var anomalystates = [];
	this.values.anomalyscores.forEach(function(value) {
		if ((this.thresholds[0] != null && value[1] <= this.thresholds[0]) || (this.thresholds[1] != null && value[1] >= this.thresholds[1])) {
			anomalystates.push(value[0]); // Push time to list of anomaly states
		}
	}, this);
	this.measurementsPlot.setIndicatorDataSet(anomalystates, false, false);
};

TeeAdViz.prototype.setPredictionVisibility = function(visibility) {
	this.predictionVisibility = visibility;
	if (this.predictionVisibility) {
		this.measurementsPlot.removeDataSet("predictions");
		this.measurementsPlot.addDataSet("predictions", "", this.values.predictions, this.predictionsColor, false, false);
		this.updateDomains();
	} else {
		this.measurementsPlot.removeDataSet("predictions");
	}
};

TeeAdViz.prototype.setAnomalyScoreVisibility = function(visibility) {
	this.anomalyscoresVisibility = visibility;
	this.anomalyscoresPlotContainer.attr("hidden", visibility ? null : true);
};

// private methods

TeeAdViz.prototype.setViews = function(except, xDomain, yDomain) {
	var plots = [this.measurementsPlot, this.anomalyscoresPlot];

	plots.forEach(function(plot) {
		if (plot != except) {
			plot.updateDomains(xDomain, plot.getYDomain(), false);
		}
	});
};

TeeAdViz.prototype.updateDomains = function() {
	var measurementsYDomain = this.measurementsPlot.calculateYDomain();
	if (this.measurementsPlotStartWithZero) {
		measurementsYDomain[0] = 0;
	}
	measurementsYDomain[1] = measurementsYDomain[1] + ((this.indicatorOffset / this.measurementsHeight) * (measurementsYDomain[1] - measurementsYDomain[0]));
	this.measurementsPlot.updateDomains(this.measurementsPlot.getXDomain(), measurementsYDomain, false);
	this.anomalyscoresPlot.updateDomains(this.measurementsPlot.getXDomain(), this.anomalyscoresPlot.calculateYDomain(), false);
};
