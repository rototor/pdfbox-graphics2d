package de.rototor.pdfbox.graphics2d.extendedtests;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class TestGraphicsFillBug
{
    @Test
    public void testGraphicsFillBug() throws IOException
    {
        InputStream resourceAsStream = TestGraphicsFillBug.class.getResourceAsStream(
                "PDFBOX-5322_reduced.pdf");
        PDDocument doc = PDDocument.load(resourceAsStream);
        PDFRenderer pdfRenderer = new PDFRenderer(doc);
        DebugCodeGeneratingGraphics2d gfx = new DebugCodeGeneratingGraphics2d();
        gfx.setBackground(Color.WHITE);
        gfx.clearRect(0, 0, 1000, 1000);
        pdfRenderer.renderPageToGraphics(0, gfx);
        ImageIO.write(gfx.getDebugImage(), "PNG", new File("target/test/graphisfillbug_debug.png"));
        new File("target/test/").mkdirs();
        ImageIO.write(pdfRenderer.renderImage(0, 1f), "PNG",
                new File("target/test/graphisfillbug.png"));

    }

    @Test
    public void testAntialiasingPathDrawBug() throws IOException
    {
        BufferedImage image = new BufferedImage(500, 300, TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setBackground(new Color(0xFFFFFFFF));
        g.clearRect(0, 0, 1000, 1000);

        Map<Object, Object> hints = new HashMap<Object, Object>();
        // When not setting Antialiasing the bug is gone.
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.addRenderingHints(hints);
        g.translate(0, 311.0);
        g.scale(1.0, -1.0);

        g.setPaint(new Color(0xFF000000));
        g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null,
                0.0f));

        GeneralPath p = new GeneralPath(Path2D.WIND_NON_ZERO);
        p.moveTo(438.531494140625, 261.2290954589844);
        p.curveTo(437.89190673828125, 265.30859375, 437.6903076171875, 265.33380126953125,
                437.6903076171875, 265.33380126953125);
        p.curveTo(437.5043029785156, 265.3570861816406, 437.3822021484375, 265.2620849609375,
                437.3822021484375, 265.2620849609375);

        g.setPaint(Color.BLACK);
        g.draw(p);

        // Just for Orientation: Where is the Path p drown?
        g.setPaint(Color.RED);
        g.draw(new Ellipse2D.Float(428, 255, 20, 20));
        g.dispose();

        new File("target/test/").mkdirs();
        ImageIO.write(image, "PNG", new File("target/test/buggy.png"));
    }

}
