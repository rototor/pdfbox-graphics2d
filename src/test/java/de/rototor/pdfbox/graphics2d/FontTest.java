package de.rototor.pdfbox.graphics2d;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.text.AttributedString;

import org.junit.Test;

public class FontTest extends PdfBoxGraphics2DTestBase
{
    @Test
    public void testAntonioFont() throws IOException, FontFormatException
    {
        final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
                PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf"))
                .deriveFont(15f);
        exportGraphic("fonts", "antonio", new GraphicsExporter()
        {
            @Override
            public void draw(Graphics2D gfx) throws IOException, FontFormatException
            {
                gfx.setColor(Color.BLACK);
                gfx.setFont(antonioRegular);
                gfx.drawString("Für älter österlich, Umlauts are not always fun.", 10, 50);
            }
        });
    }

    @Test
    public void testStyledAttributeIterator() throws IOException, FontFormatException
    {
        final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
                PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf"))
                .deriveFont(15f);
        exportGraphic("fonts", "attributed_text", new GraphicsExporter()
        {
            @Override
            public void draw(Graphics2D gfx) throws IOException, FontFormatException
            {
                gfx.setColor(Color.BLACK);
                gfx.setFont(antonioRegular);
                AttributedString str = new AttributedString(
                        "This is some funny text with some attributes.");
                str.addAttribute(TextAttribute.SIZE, 20f, 0, 4);

                str.addAttribute(TextAttribute.FOREGROUND, Color.RED, 0, 4);
                str.addAttribute(TextAttribute.FOREGROUND, Color.green, 13, 23);
                str.addAttribute(TextAttribute.SIZE, 18f, 13, 23);
                str.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 13, 23);

                gfx.drawString(str.getIterator(), 10, 50);
            }
        });
    }
}
