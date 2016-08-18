function CanvasTimeSeriesIndicatorPlot(parentElement, canvasDimensions, config) {
	config = config || {};

	this.indicatorData = [];
	this.indicatorPath = config.indicatorPath || new Path2D("M1024 1375v-190q0-14-9.5-23.5t-22.5-9.5h-192q-13 0-22.5 9.5t-9.5 23.5v190q0 14 9.5 23.5t22.5 9.5h192q13 0 22.5-9.5t9.5-23.5zm-2-374l18-459q0-12-10-19-13-11-24-11h-220q-11 0-24 11-10 7-10 21l17 457q0 10 10 16.5t24 6.5h185q14 0 23.5-6.5t10.5-16.5zm-14-934l768 1408q35 63-2 126-17 29-46.5 46t-63.5 17h-1536q-34 0-63.5-17t-46.5-46q-37-63-2-126l768-1408q17-31 47-49t65-18 65 18 47 49z");
	this.indicatorBackgroundPath = config.indicatorBackgroundPath || new Path2D("M1010 60l768 1408q35 63-2 126-17 29-46.5 46t-63.5 17h-1536q-34 0-63.5-17t-46.5-46q-37-63-2-126l768-1408q17-31 47-49t65-18 65 18 47 49z");
	this.indicatorColor = config.indicatorColor || 'red';
	this.indicatorBackgroundColor = config.indicatorBackgroundColor || 'white';

	CanvasTimeSeriesPlot.call(this, parentElement, canvasDimensions, config);
}
CanvasTimeSeriesIndicatorPlot.prototype = Object.create(CanvasTimeSeriesPlot.prototype);

CanvasTimeSeriesIndicatorPlot.prototype.setIndicatorDataSet = function(dataSet, updateDomains, copyData) {
	dataSet = dataSet || [];
	if(copyData) {
		this.indicatorData = [];
		for(var i=0; i<dataSet.length; ++i) {
			this.indicatorData.push(dataSet[i]);
		}
	} else {
		this.indicatorData = dataSet;
	}

	this.updateLegend();

	if(updateDomains) {
		this.updateDomains(this.calculateXDomain(), this.calculateYDomain(), true);
	} else {
		this.updateDisplayIndices();
		this.drawCanvas();
	}
};

CanvasTimeSeriesIndicatorPlot.prototype.addIndicatorDataPoint = function(dataPoint, updateDomains, copyData) {
	if (this.indicatorData.length > 0 && this.indicatorData[this.indicatorData.length-1][0] > dataPoint[0]) {
		return;
	}
	this.indicatorData.push(copyData ? dataPoint.slice(0) : dataPoint);

	if(updateDomains) {
		this.updateDomains(this.calculateXDomain(), this.calculateYDomain(), true);
	} else {
		this.updateDisplayIndices();
		this.drawCanvas();
	}
};

CanvasTimeSeriesIndicatorPlot.prototype.drawCanvas = function() {

	CanvasTimeSeriesPlot.prototype.drawCanvas.call(this);

	var d = this.indicatorData;

	if(d.length < 1) {
		return;
	}

  this.canvas.lineWidth = 1;

	for(var i = 0; i < d.length; i++) {
		this.canvas.save();
		this.canvas.translate(this.xScale(d[i]) - 17, 5);
		this.canvas.scale(0.02, 0.02);
		this.canvas.strokeStyle = this.indicatorBackgroundColor;
		this.canvas.stroke(this.indicatorBackgroundPath);
		this.canvas.fillStyle = this.indicatorBackgroundColor;
		this.canvas.fill(this.indicatorBackgroundPath);
		this.canvas.strokeStyle = this.indicatorColor;
		this.canvas.stroke(this.indicatorPath);
		this.canvas.fillStyle = this.indicatorColor;
		this.canvas.fill(this.indicatorPath);
		this.canvas.restore();
	}

};
