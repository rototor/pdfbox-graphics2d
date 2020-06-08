package de.rototor.pdfbox.graphics2d;

import org.junit.Test;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.*;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FontWidthDiscrepancyTest extends PdfBoxGraphics2DTestBase {
        @Test
        public void testAnonioFontWidth() throws IOException, FontFormatException
        {
                final PDDocument doc = new PDDocument();
                final String testString = "MMMMMMMMMMMMMMMMMMMMMM";
                final float fontSize = 20f;
                final Font antonioRegular = Font
                        .createFont(Font.TRUETYPE_FONT,
                                        PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf"))
                        .deriveFont(fontSize);
                final PDFont pdFont =  PDType0Font
                        .load(doc, PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf"));

                final Graphics2D gfx = new PdfBoxGraphics2D(doc, 400, 400);

                float pdfWidth = pdFont.getStringWidth(testString) / 1000 * fontSize;
                float gfxWidth = gfx.getFontMetrics(antonioRegular).stringWidth(testString);
                
                gfx.dispose();

                assertEquals(pdfWidth, gfxWidth, 1.0);
               
        }

}
