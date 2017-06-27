package de.rototor.pdfbox.graphics2d;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.GradientXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;

public class ChartFileBuilder
{
    private PDDocument  document = null;

    private String filePath = null;

    private PDPage              currentPage   = null;
    private PDPageContentStream contentStream = null;

    public ChartFileBuilder(String filePath)
    {
        this.filePath = filePath;

        document = new PDDocument();

        addPage();
    }

    public void addPage()
    {
        currentPage = new PDPage();
        currentPage.setMediaBox(PDRectangle.A4);
        document.addPage(currentPage);
    }

    public void go()
    {
        try
        {
            printStuff();

            // TODO - If saveState is true ==> saveGraphicsState()
            boolean saveState = true;
            printChart(10, 10, 550, 550, buildDummyChart(1), saveState);
            addPage();
            printChart(10, 10, 550, 550, buildDummyChart(2), saveState);

            document.save(new File(filePath));
            document.close();
            System.out.println(filePath + " - DONE");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    // --------------------------------------------------------------------

    private void printStuff() throws IOException
    {
        openContentStream();

        contentStream.setFont(PDType1Font.HELVETICA, 24);
        contentStream.beginText();
        contentStream.newLineAtOffset(220, 740);
        contentStream.showText("TEST");
        contentStream.endText();

        contentStream.setLineWidth(0.7f);
        contentStream.moveTo( 50, 700);
        contentStream.lineTo(450, 700);
        contentStream.stroke();

        closeContentStream();
    }

    private JFreeChart buildDummyChart(int n)
    {
        // ------------------------------------------
        // NB: big memory allocation!
        Map<Integer, String> m = new HashMap<>();
        for(int i=0; i<1_000_000; i++)
        {
            m.put(i, Long.toHexString(Double.doubleToLongBits(Math.random())));
        }
        m = null;
        // ------------------------------------------

        String[] names = new String[10];
        String s = "" + n;
        XYIntervalSeriesCollection dataSet = new XYIntervalSeriesCollection();
        for(int y=0; y<10; y++)
        {
            names[y] = s + "." + y;
            XYIntervalSeries intervalSeries = new XYIntervalSeries("a" + y);
            for(int x=0; x<50; x+=10)
            {
                intervalSeries.add(x, x, x + 8, y, y - 0.4, y + 0.4);
            }
            dataSet.addSeries(intervalSeries);
        }

        JFreeChart chart = ChartFactory.createXYBarChart("", "", true, "", dataSet, PlotOrientation.VERTICAL, false, false, false);

        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setRangeAxis(new SymbolAxis("", names));
        XYBarRenderer renderer = (XYBarRenderer)plot.getRenderer();
        renderer.setUseYInterval(true);
        renderer.setBarPainter(new GradientXYBarPainter(1, 0, 0));

        for(int i=0; i<10; i++)
        {
            renderer.setSeriesPaint(i, new Color(Color.RED.getRed(), Color.RED.getGreen(),  Color.RED.getBlue(), 64));
        }

        return chart;
    }

    public void printChart(float x, float y, int width, int height, JFreeChart chart, boolean saveState) throws Exception
    {
        openContentStream();

        PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, width, height);
        pdfBoxGraphics2D.setVectoringText(false);
        Rectangle2D rectangle = new Rectangle2D.Double(0, 0, width, height);
        chart.draw(pdfBoxGraphics2D, rectangle);

        pdfBoxGraphics2D.dispose();

        PDFormXObject xform = pdfBoxGraphics2D.getXFormObject();

        Matrix matrix = new Matrix();
        matrix.translate(x, y);
        contentStream.transform(matrix);
        contentStream.drawForm(xform);

        // TODO - This is the hack
        if(saveState)
        {
            pdfBoxGraphics2D.saveGraphicsState();
        }

        closeContentStream();
    }

    // --------------------------------------------------------------------

    private void openContentStream() throws IOException
    {
        contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true, true);
    }

    public void closeContentStream()
    {
        if(contentStream != null)
        {
            try
            {
                contentStream.close();
            }
            catch(Exception e)
            {
            }
        }

        contentStream = null;
    }
}