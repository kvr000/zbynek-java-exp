package com.github.kvr000.exp.java.chart.jfreechart.svg;

import lombok.val;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;


public class Application
{
	public static void main(String[] args) throws Exception
	{
		var application = new Application();
		application.createPieChart();
		application.createXyLineChart();
	}

	public void createPieChart() throws IOException
	{
		DefaultPieDataset data = new DefaultPieDataset();
		data.setValue("JFreeChart", 77);
		data.setValue("Batik", 80);
		data.setValue("Chart", 55);
		data.setValue("Apache", 67);
		data.setValue("Java", 80);

		JFreeChart chart = ChartFactory.createPieChart("JFreeChart - SVG Pie Chart Example",data,true,true,false);
		Color trans = new Color(0xFF, 0xFF, 0xFF, 0);
		chart.setBackgroundPaint(trans);

		// Create generator:
		DOMImplementation dom = GenericDOMImplementation.getDOMImplementation();
		Document document = dom.createDocument(null, "svg", null);
		SVGGraphics2D generator = new SVGGraphics2D(document);
		generator.setBackground(trans);

		// Render:
		chart.draw(generator, new Rectangle2D.Double(0, 0, 640, 480), null);

		// Save
		generator.stream("target/output_pie_chart.svg");
	}

	public void createXyLineChart() throws IOException
	{
		XYSeries gzip_data = new XYSeries("Gzip");
		gzip_data.add(1, 20);
		gzip_data.add(5, 30);
		gzip_data.add(9, 50);

		XYSeries xz_data = new XYSeries("Xz");
		xz_data.add(1, 25);
		xz_data.add(5, 37);
		xz_data.add(9, 60);

		XYSeries zstd_data = new XYSeries("Zstd");
		zstd_data.add(1, 23);
		zstd_data.add(5, 36);
		zstd_data.add(9, 59);

		XYSeriesCollection svgXyDataSeries= new XYSeriesCollection();
		// add series:
		svgXyDataSeries.addSeries(gzip_data);
		svgXyDataSeries.addSeries(xz_data);
		svgXyDataSeries.addSeries(zstd_data);


		// Use createXYLineChart to create the chart
		JFreeChart xyLineChart = ChartFactory.createXYLineChart("Compressor - compression","Level","Compression",svgXyDataSeries, PlotOrientation.VERTICAL,true,true,false);

		// Create generator:
		DOMImplementation dom = GenericDOMImplementation.getDOMImplementation();
		Document document = dom.createDocument(null, "svg", null);
		SVGGraphics2D generator = new SVGGraphics2D(document);
		Color trans = new Color(0xFF, 0xFF, 0xFF, 0);
		xyLineChart.setBackgroundPaint(trans);
		generator.setBackground(trans);

		// Render:
		xyLineChart.draw(generator, new Rectangle2D.Double(0, 0, 640, 480), null);

		// Save:
		generator.stream("target/output_xy_line_chart.svg");
	}
}
