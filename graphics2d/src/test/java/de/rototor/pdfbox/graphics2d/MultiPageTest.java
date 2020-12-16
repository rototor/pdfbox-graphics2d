package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.TableOrder;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class MultiPageTest {
	@Test
	public void testMultiPageJFreeChart() throws IOException {
		File parentDir = new File("target/test/multipage");
		// noinspection ResultOfMethodCallIgnored
		parentDir.mkdirs();
		File targetPDF = new File(parentDir, "multipage.pdf");
		PDDocument document = new PDDocument();
		for (int i = 0; i < 6; i++) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			PDPageContentStream contentStream = new PDPageContentStream(document, page);
			PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 800, 400);
			drawOnGraphics(pdfBoxGraphics2D, i);
			pdfBoxGraphics2D.dispose();

			PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
			Matrix matrix = new Matrix();
			matrix.translate(0, 30);
			matrix.scale(0.7f, 1f);

			contentStream.saveGraphicsState();
			contentStream.transform(matrix);
			contentStream.drawForm(appearanceStream);
			contentStream.restoreGraphicsState();

			contentStream.close();
		}
		document.save(targetPDF);
		document.close();
	}

	private void drawOnGraphics(PdfBoxGraphics2D gfx, int i) {
		Rectangle rectangle = new Rectangle(800, 400);
		switch (i) {
		case 0: {
			final XYDataset dataset = createDatasetXY();
			final JFreeChart chart = createChartXY(dataset);
			chart.draw(gfx, rectangle);
			break;
		}
		case 1: {
			final IntervalCategoryDataset dataset = createDatasetGantt();
			final JFreeChart chart = createChartGantt(dataset);
			chart.draw(gfx, rectangle);
			break;
		}
		case 2: {
			final CategoryDataset dataset = createDatasetCategory();
			final JFreeChart chart = createChartCategory(dataset);
			chart.draw(gfx, rectangle);
			break;
		}
		case 3: {
			final XYDataset dataset = createDatasetXY();
			final JFreeChart chart = createChartXY(dataset);
			chart.draw(gfx, rectangle);
			break;
		}
		case 4: {
			final CategoryDataset dataset = createDatasetCategory();
			final JFreeChart chart = createSpiderChart(dataset);
			chart.draw(gfx, rectangle);
			break;
		}
		case 5: {
			final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			dataset.addValue(0.0, "Row 0", "Column 0");
			dataset.addValue(0.0, "Row 0", "Column 1");
			dataset.addValue(0.0, "Row 0", "Column 2");
			dataset.addValue(0.0, "Row 0", "Column 3");
			dataset.addValue(0.0, "Row 0", "Column 4");
			final JFreeChart chart = createSpiderChart(dataset);
			chart.setTitle("Invalid Spider Chart");
			chart.draw(gfx, rectangle);
			break;
		}
		}
	}

	/**
	 * Creates a sample dataset.
	 *
	 * @return a sample dataset.
	 */
	private XYDataset createDatasetXY() {

		final XYSeries series1 = new XYSeries("First");
		series1.add(1.0, 1.0);
		series1.add(2.0, 4.0);
		series1.add(3.0, 3.0);
		series1.add(4.0, 5.0);
		series1.add(5.0, 5.0);
		series1.add(6.0, 7.0);
		series1.add(7.0, 7.0);
		series1.add(8.0, 8.0);

		final XYSeries series2 = new XYSeries("Second");
		series2.add(1.0, 5.0);
		series2.add(2.0, 7.0);
		series2.add(3.0, 6.0);
		series2.add(4.0, 8.0);
		series2.add(5.0, 4.0);
		series2.add(6.0, 4.0);
		series2.add(7.0, 2.0);
		series2.add(8.0, 1.0);

		final XYSeries series3 = new XYSeries("Third");
		series3.add(3.0, 4.0);
		series3.add(4.0, 3.0);
		series3.add(5.0, 2.0);
		series3.add(6.0, 3.0);
		series3.add(7.0, 6.0);
		series3.add(8.0, 3.0);
		series3.add(9.0, 4.0);
		series3.add(10.0, 3.0);

		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series1);
		dataset.addSeries(series2);
		dataset.addSeries(series3);

		return dataset;

	}

	/**
	 * Creates a chart.
	 *
	 * @param dataset
	 *            the data for the chart.
	 *
	 * @return a chart.
	 */
	private JFreeChart createChartXY(final XYDataset dataset) {

		// create the chart...
		final JFreeChart chart = ChartFactory.createXYLineChart("Line Chart Demo 6", // chart
																						// title
				"X", // x axis label
				"Y", // y axis label
				dataset, // data
				PlotOrientation.VERTICAL, true, // include legend
				true, // tooltips
				false // urls
		);

		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
		chart.setBackgroundPaint(Color.white);

		// final StandardLegend legend = (StandardLegend) chart.getLegend();
		// legend.setDisplaySeriesShapes(true);

		// get a reference to the plot for further customisation...
		final XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.lightGray);
		// plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);

		final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible(0, false);
		renderer.setSeriesShapesVisible(1, false);
		plot.setRenderer(renderer);

		// change the auto tick unit selection to integer units only...
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		// OPTIONAL CUSTOMISATION COMPLETED.

		return chart;

	}

	/**
	 * Creates a sample dataset for a Gantt chart.
	 *
	 * @return The dataset.
	 */
	private static IntervalCategoryDataset createDatasetGantt() {

		final TaskSeries s1 = new TaskSeries("Scheduled");
		s1.add(new Task("Write Proposal",
				new SimpleTimePeriod(date(1, Calendar.APRIL, 2001), date(5, Calendar.APRIL, 2001))));
		s1.add(new Task("Obtain Approval",
				new SimpleTimePeriod(date(9, Calendar.APRIL, 2001), date(9, Calendar.APRIL, 2001))));
		s1.add(new Task("Requirements Analysis",
				new SimpleTimePeriod(date(10, Calendar.APRIL, 2001), date(5, Calendar.MAY, 2001))));
		s1.add(new Task("Design Phase",
				new SimpleTimePeriod(date(6, Calendar.MAY, 2001), date(30, Calendar.MAY, 2001))));
		s1.add(new Task("Design Signoff",
				new SimpleTimePeriod(date(2, Calendar.JUNE, 2001), date(2, Calendar.JUNE, 2001))));
		s1.add(new Task("Alpha Implementation",
				new SimpleTimePeriod(date(3, Calendar.JUNE, 2001), date(31, Calendar.JULY, 2001))));
		s1.add(new Task("Design Review",
				new SimpleTimePeriod(date(1, Calendar.AUGUST, 2001), date(8, Calendar.AUGUST, 2001))));
		s1.add(new Task("Revised Design Signoff",
				new SimpleTimePeriod(date(10, Calendar.AUGUST, 2001), date(10, Calendar.AUGUST, 2001))));
		s1.add(new Task("Beta Implementation",
				new SimpleTimePeriod(date(12, Calendar.AUGUST, 2001), date(12, Calendar.SEPTEMBER, 2001))));
		s1.add(new Task("Testing",
				new SimpleTimePeriod(date(13, Calendar.SEPTEMBER, 2001), date(31, Calendar.OCTOBER, 2001))));
		s1.add(new Task("Final Implementation",
				new SimpleTimePeriod(date(1, Calendar.NOVEMBER, 2001), date(15, Calendar.NOVEMBER, 2001))));
		s1.add(new Task("Signoff",
				new SimpleTimePeriod(date(28, Calendar.NOVEMBER, 2001), date(30, Calendar.NOVEMBER, 2001))));

		final TaskSeries s2 = new TaskSeries("Actual");
		s2.add(new Task("Write Proposal",
				new SimpleTimePeriod(date(1, Calendar.APRIL, 2001), date(5, Calendar.APRIL, 2001))));
		s2.add(new Task("Obtain Approval",
				new SimpleTimePeriod(date(9, Calendar.APRIL, 2001), date(9, Calendar.APRIL, 2001))));
		s2.add(new Task("Requirements Analysis",
				new SimpleTimePeriod(date(10, Calendar.APRIL, 2001), date(15, Calendar.MAY, 2001))));
		s2.add(new Task("Design Phase",
				new SimpleTimePeriod(date(15, Calendar.MAY, 2001), date(17, Calendar.JUNE, 2001))));
		s2.add(new Task("Design Signoff",
				new SimpleTimePeriod(date(30, Calendar.JUNE, 2001), date(30, Calendar.JUNE, 2001))));
		s2.add(new Task("Alpha Implementation",
				new SimpleTimePeriod(date(1, Calendar.JULY, 2001), date(12, Calendar.SEPTEMBER, 2001))));
		s2.add(new Task("Design Review",
				new SimpleTimePeriod(date(12, Calendar.SEPTEMBER, 2001), date(22, Calendar.SEPTEMBER, 2001))));
		s2.add(new Task("Revised Design Signoff",
				new SimpleTimePeriod(date(25, Calendar.SEPTEMBER, 2001), date(27, Calendar.SEPTEMBER, 2001))));
		s2.add(new Task("Beta Implementation",
				new SimpleTimePeriod(date(27, Calendar.SEPTEMBER, 2001), date(30, Calendar.OCTOBER, 2001))));
		s2.add(new Task("Testing",
				new SimpleTimePeriod(date(31, Calendar.OCTOBER, 2001), date(17, Calendar.NOVEMBER, 2001))));
		s2.add(new Task("Final Implementation",
				new SimpleTimePeriod(date(18, Calendar.NOVEMBER, 2001), date(5, Calendar.DECEMBER, 2001))));
		s2.add(new Task("Signoff",
				new SimpleTimePeriod(date(10, Calendar.DECEMBER, 2001), date(11, Calendar.DECEMBER, 2001))));

		final TaskSeriesCollection collection = new TaskSeriesCollection();
		collection.add(s1);
		collection.add(s2);

		return collection;
	}

	/**
	 * Utility method for creating <code>Date</code> objects.
	 *
	 * @param day
	 *            the date.
	 * @param month
	 *            the month.
	 * @param year
	 *            the year.
	 *
	 * @return a date.
	 */
	private static Date date(final int day, final int month, @SuppressWarnings("SameParameterValue") final int year) {
		final Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day);
		return calendar.getTime();

	}

	/**
	 * Creates a chart.
	 *
	 * @param dataset
	 *            the dataset.
	 *
	 * @return The chart.
	 */
	private JFreeChart createChartGantt(final IntervalCategoryDataset dataset) {
		return ChartFactory.createGanttChart("Gantt Chart Demo", // chart
																	// title
				"Task", // domain axis label
				"Date", // range axis label
				dataset, // data
				true, // include legend
				true, // tooltips
				false // urls
		);
	}

	/**
	 * Creates a sample dataset.
	 *
	 * @return A sample dataset.
	 */
	private CategoryDataset createDatasetCategory() {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		dataset.addValue(5.6, "Row 0", "Column 0");
		dataset.addValue(3.2, "Row 0", "Column 1");
		dataset.addValue(1.8, "Row 0", "Column 2");
		dataset.addValue(0.2, "Row 0", "Column 3");
		dataset.addValue(4.1, "Row 0", "Column 4");

		dataset.addValue(9.8, "Row 1", "Column 0");
		dataset.addValue(6.3, "Row 1", "Column 1");
		dataset.addValue(0.1, "Row 1", "Column 2");
		dataset.addValue(1.9, "Row 1", "Column 3");
		dataset.addValue(9.6, "Row 1", "Column 4");

		dataset.addValue(7.0, "Row 2", "Column 0");
		dataset.addValue(5.2, "Row 2", "Column 1");
		dataset.addValue(2.8, "Row 2", "Column 2");
		dataset.addValue(8.8, "Row 2", "Column 3");
		dataset.addValue(7.2, "Row 2", "Column 4");

		dataset.addValue(9.5, "Row 3", "Column 0");
		dataset.addValue(1.2, "Row 3", "Column 1");
		dataset.addValue(4.5, "Row 3", "Column 2");
		dataset.addValue(4.4, "Row 3", "Column 3");
		dataset.addValue(0.2, "Row 3", "Column 4");

		dataset.addValue(3.5, "Row 4", "Column 0");
		dataset.addValue(6.7, "Row 4", "Column 1");
		dataset.addValue(9.0, "Row 4", "Column 2");
		dataset.addValue(1.0, "Row 4", "Column 3");
		dataset.addValue(5.2, "Row 4", "Column 4");

		dataset.addValue(5.1, "Row 5", "Column 0");
		dataset.addValue(6.7, "Row 5", "Column 1");
		dataset.addValue(0.9, "Row 5", "Column 2");
		dataset.addValue(3.3, "Row 5", "Column 3");
		dataset.addValue(3.9, "Row 5", "Column 4");

		dataset.addValue(5.6, "Row 6", "Column 0");
		dataset.addValue(5.6, "Row 6", "Column 1");
		dataset.addValue(5.6, "Row 6", "Column 2");
		dataset.addValue(5.6, "Row 6", "Column 3");
		dataset.addValue(5.6, "Row 6", "Column 4");

		dataset.addValue(7.5, "Row 7", "Column 0");
		dataset.addValue(9.0, "Row 7", "Column 1");
		dataset.addValue(3.4, "Row 7", "Column 2");
		dataset.addValue(4.1, "Row 7", "Column 3");
		dataset.addValue(0.5, "Row 7", "Column 4");

		return dataset;
	}

	/**
	 * Creates a sample chart for the given dataset.
	 *
	 * @param dataset
	 *            the dataset.
	 *
	 * @return A sample chart.
	 */
	private JFreeChart createChartCategory(final CategoryDataset dataset) {
		final JFreeChart chart = ChartFactory.createMultiplePieChart3D("Multiple Pie Chart Demo 4", dataset,
				TableOrder.BY_COLUMN, false, true, false);
		chart.setBackgroundPaint(new Color(216, 255, 216));
		final MultiplePiePlot plot = (MultiplePiePlot) chart.getPlot();
		final JFreeChart subchart = plot.getPieChart();
		// final StandardLegend legend = new StandardLegend();
		// legend.setItemFont(new Font("SansSerif", Font.PLAIN, 8));
		// legend.setAnchor(Legend.SOUTH);
		// subchart.setLegend(legend);
		plot.setLimit(0.10);
		final PiePlot p = (PiePlot) subchart.getPlot();
		// p.setLabelGenerator(new StandardPieItemLabelGenerator("{0}"));
		p.setLabelFont(new Font("SansSerif", Font.PLAIN, 8));
		p.setInteriorGap(0.30);

		return chart;
	}

	private static JFreeChart createSpiderChart(CategoryDataset dataset) {
		SpiderWebPlot plot = new SpiderWebPlot(dataset);
		plot.setStartAngle(54);
		plot.setInteriorGap(0.40);
		plot.setToolTipGenerator(new StandardCategoryToolTipGenerator());
		JFreeChart chart = new JFreeChart("Spider Web Chart Demo 1", TextTitle.DEFAULT_FONT, plot, false);
		LegendTitle legend = new LegendTitle(plot);
		legend.setPosition(RectangleEdge.BOTTOM);
		chart.addSubtitle(legend);
		ChartUtilities.applyCurrentTheme(chart);
		return chart;

	}

}
