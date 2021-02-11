package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.*;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PdfRerenderTest
{

    @Test
    public void testPDFRerender() throws IOException
    {
        rerenderPDF("heart.pdf");
        rerenderPDF("barChart.pdf");
        rerenderPDF("compuserver_msn_Ford_Focus.pdf");
        rerenderPDF("patternfill.pdf");
    }

    @Test
    public void testSimpleRerender() throws IOException
    {
        simplePDFRerender("antonio_sample.pdf");
    }

    @Test
    public void testSimpleRerenderAsBitmap() throws IOException
    {
        simplePDFRerenderAsBitmap("antonio_sample.pdf", false);
        simplePDFRerenderAsBitmap("antonio_sample.pdf", true);
    }

    public void simplePDFRerenderAsBitmap(String name, boolean lossless) throws IOException
    {
        File parentDir = new File("target/test");
        // noinspection ResultOfMethodCallIgnored
        parentDir.mkdirs();

        PDDocument document = new PDDocument();
        PDDocument sourceDoc = PDDocument.load(PdfRerenderTest.class.getResourceAsStream(name));

        for (PDPage sourcePage : sourceDoc.getPages())
        {
            PDRectangle mediaBox = sourcePage.getMediaBox();
            PDPage rerenderedPage = new PDPage(mediaBox);
            document.addPage(rerenderedPage);
            PDPageContentStream cb = new PDPageContentStream(document, rerenderedPage);
            try
            {

                PDFRenderer pdfRenderer = new PDFRenderer(sourceDoc);
                float targetDPI = 300;
                BufferedImage bufferedImage = pdfRenderer
                        .renderImage(sourceDoc.getPages().indexOf(sourcePage), targetDPI / 72.0f);

                PDImageXObject image;
                if (lossless)
                    image = LosslessFactory.createFromImage(document, bufferedImage);
                else
                    image = JPEGFactory.createFromImage(document, bufferedImage, 0.7f);

                cb.drawImage(image, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
            }
            finally
            {
                cb.close();
            }
        }
        document.save(new File(parentDir, "simple_bitmap_" + (lossless ? "" : "_jpeg_") + name));
        document.close();
        sourceDoc.close();
    }

    public void simplePDFRerender(String name) throws IOException
    {
        File parentDir = new File("target/test");
        // noinspection ResultOfMethodCallIgnored
        parentDir.mkdirs();

        PDDocument document = new PDDocument();
        PDDocument sourceDoc = PDDocument.load(PdfRerenderTest.class.getResourceAsStream(name));

        for (PDPage sourcePage : sourceDoc.getPages())
        {
            PDPage rerenderedPage = new PDPage(sourcePage.getMediaBox());
            document.addPage(rerenderedPage);
            PDPageContentStream cb = new PDPageContentStream(document, rerenderedPage);
            try
            {
                PdfBoxGraphics2D gfx = new PdfBoxGraphics2D(document, sourcePage.getMediaBox());
                PDFRenderer pdfRenderer = new PDFRenderer(sourceDoc);
                pdfRenderer.renderPageToGraphics(sourceDoc.getPages().indexOf(sourcePage), gfx);
                gfx.dispose();

                PDFormXObject xFormObject = gfx.getXFormObject();
                cb.drawForm(xFormObject);
            }
            finally
            {
                cb.close();
            }
        }
        document.save(new File(parentDir, "simple_rerender" + name));
        document.close();
        sourceDoc.close();
    }

    private void rerenderPDF(String name) throws IOException
    {
        File parentDir = new File("target/test");
        // noinspection ResultOfMethodCallIgnored
        parentDir.mkdirs();

        PDDocument document = new PDDocument();
        PDDocument sourceDoc = PDDocument.load(PdfRerenderTest.class.getResourceAsStream(name));

        for (PDPage sourcePage : sourceDoc.getPages())
        {
            PDPage rerenderedPage = new PDPage(sourcePage.getMediaBox());
            document.addPage(rerenderedPage);
            PDPageContentStream cb = new PDPageContentStream(document, rerenderedPage);
            try
            {
                PdfBoxGraphics2D gfx = new PdfBoxGraphics2D(document, sourcePage.getMediaBox());

                // Do overfill for red with a transparent green
                gfx.setDrawControl(new PdfBoxGraphics2DDrawControlDefault()
                {
                    boolean insideOwnDraw = false;

                    @Override
                    public void afterShapeFill(Shape shape, IDrawControlEnv env)
                    {
                        afterShapeDraw(shape, env);
                    }

                    @Override
                    public void afterShapeDraw(Shape shape, IDrawControlEnv env)
                    {
                        if (insideOwnDraw)
                            return;
                        insideOwnDraw = true;
                        Paint paint = env.getPaint();
                        if (paint instanceof Color)
                        {
                            if (paint.equals(Color.RED))
                            {
                                // We overfill with black a little bit
                                PdfBoxGraphics2D graphics = env.getGraphics();
                                Stroke prevStroke = graphics.getStroke();
                                float additinalStrokeWidth = 1f;
                                if (prevStroke instanceof BasicStroke)
                                {
                                    BasicStroke basicStroke = ((BasicStroke) prevStroke);
                                    graphics.setStroke(new BasicStroke(
                                            basicStroke.getLineWidth() + additinalStrokeWidth,
                                            basicStroke.getEndCap(), basicStroke.getLineJoin(),
                                            basicStroke.getMiterLimit(), basicStroke.getDashArray(),
                                            basicStroke.getDashPhase()));
                                }
                                else
                                {
                                    graphics.setStroke(new BasicStroke(additinalStrokeWidth));
                                }

                                graphics.setPaint(new PdfBoxGraphics2DColor(
                                        new PDColor(new float[] { 0.5f }, new PDCalGray())));
                                graphics.draw(shape);

                                graphics.setPaint(paint);
                                graphics.setStroke(prevStroke);
                            }
                        }
                        insideOwnDraw = false;
                    }
                });

                PDFRenderer pdfRenderer = new PDFRenderer(sourceDoc)
                {
                    @Override
                    protected PageDrawer createPageDrawer(PageDrawerParameters parameters)
                            throws IOException
                    {
                        return new PageDrawer(parameters)
                        {
                            @Override
                            protected Paint getPaint(PDColor color) throws IOException
                            {
                                PDColorSpace colorSpace = color.getColorSpace();

                                // We always must handle patterns recursive
                                if (colorSpace instanceof PDPattern)
                                    return super.getPaint(color);

                                // Now our special logic
                                if (colorSpace instanceof PDDeviceRGB)
                                {
                                    float[] components = color.getComponents();
                                    boolean allBlack = true;
                                    for (float f : components)
                                    {
                                        if (f > 0.0)
                                            allBlack = false;
                                    }
                                    if (allBlack)
                                        return new PdfBoxGraphics2DCMYKColor(1f, 0.0f, 0.2f, 0.1f,
                                                128);
                                }

                                // All other colors just stay the same...
                                return super.getPaint(color);
                            }

                            @Override
                            public void drawImage(PDImage pdImage)
                            {
                                // We dont like images, just skip them all
                            }
                        };
                    }
                };
                pdfRenderer.renderPageToGraphics(sourceDoc.getPages().indexOf(sourcePage), gfx);
                gfx.dispose();

                PDFormXObject xFormObject = gfx.getXFormObject();
                cb.drawForm(xFormObject);
            }
            finally
            {
                cb.close();
            }
        }

        document.save(new File(parentDir, "rerendered_" + name));
        document.close();
        sourceDoc.close();
    }
}
