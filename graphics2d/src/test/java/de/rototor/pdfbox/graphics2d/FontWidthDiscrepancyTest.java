package de.rototor.pdfbox.graphics2d;

import org.junit.Test;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.*;
import java.awt.font.TextLayout;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class FontWidthDiscrepancyTest extends PdfBoxGraphics2DTestBase
{

    @Test
    public void testAntonioFontWidth() throws IOException, FontFormatException
    {

        final String testString = "MMMMMMMMMMMMMMMMMMMMMM";
        final float fontSize = 20f;
        final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
                PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf"))
                .deriveFont(fontSize);

        final PDDocument doc = new PDDocument();
        final PDFont pdFont = PDType0Font.load(doc,
                PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf"));

        final Graphics2D gfx = new PdfBoxGraphics2D(doc, 400, 400);

        final float pdfWidth = pdFont.getStringWidth(testString) / 1000 * fontSize;
        final int gfxWidth = gfx.getFontMetrics(antonioRegular).stringWidth(testString);
        gfx.dispose();
        doc.close();

        exportGraphic("fontWidthDiscrepancy", "antonio-m", new GraphicsExporter()
        {
            @Override
            public void draw(Graphics2D gfx) throws IOException, FontFormatException
            {
                gfx.setFont(antonioRegular);
                gfx.setColor(Color.GREEN);
                gfx.drawString(testString, 10, 10);
                gfx.setColor(Color.RED);
                gfx.drawLine(10, 1, (int) (10 + pdfWidth), 1);
                gfx.setColor(Color.BLUE);
                gfx.drawLine(10, 15, (int) (10 + gfxWidth), 15);

                gfx.setColor(Color.magenta);
                FontMetrics fontMetrics = gfx.getFontMetrics();
                int currentMeasurement = fontMetrics.stringWidth(testString);
                gfx.drawLine(10, 25, (int) (10 + currentMeasurement), 25);

                gfx.drawLine(10, 5, 10 + fontMetrics.charWidth('M'), 5);

                assertNotNull(fontMetrics.getWidths());
            }
        });

    }

}
