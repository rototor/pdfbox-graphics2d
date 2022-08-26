package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.util.Matrix;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TestMaskedDraws
{
    private static final float POINTS_PER_INCH = 72;

    /**
     * user space units per millimeter
     */
    private static final float POINTS_PER_MM = 1 / (10 * 2.54f) * POINTS_PER_INCH;

    @Test
    public void testMaskedDraws() throws IOException
    {
        final PDDocument document = new PDDocument();
        final PDPage page = new PDPage(new PDRectangle(302 * POINTS_PER_MM, 111 * POINTS_PER_MM));
        document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document,
                page.getMediaBox().getWidth(), page.getMediaBox().getHeight());

        pdfBoxGraphics2D.setPaintApplier(new PdfBoxGraphics2DPaintApplier()
        {
            @Override
            protected PDShading applyPaint(Paint paint, PaintApplierState state) throws IOException
            {
                PDShading ret = super.applyPaint(paint, state);
                InputStream resourceAsStream = PdfBoxGraphics2dTest.class.getResourceAsStream(
                        "maskingExample.png");
                assert resourceAsStream != null;
                BufferedImage mask = ImageIO.read(resourceAsStream);
                state.setupLuminosityMasking(mask, page.getMediaBox());
                return ret;
            }
        });

        LinearGradientPaint linearGradientPaint = new LinearGradientPaint(0, 0,
                page.getMediaBox().getWidth(), page.getMediaBox().getHeight(),
                new float[] { 0.0f, .2f, .4f, .9f, 1f },
                new Color[] { Color.YELLOW, Color.GREEN, Color.RED, Color.BLUE, Color.PINK });
        pdfBoxGraphics2D.setPaint(linearGradientPaint);

        pdfBoxGraphics2D.fill(new Rectangle2D.Double(0, 0, page.getMediaBox().getWidth(),
                page.getMediaBox().getHeight()));

        pdfBoxGraphics2D.dispose();

        PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
        Matrix matrix = new Matrix();
        matrix.translate(0, 0);
        contentStream.transform(matrix);
        contentStream.drawForm(appearanceStream);
        contentStream.close();

        File file = new File("target/test/masked_draws.pdf");
        // noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        document.save(file);
        document.close();

    }
}
