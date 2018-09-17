package de.rototor.pdfbox.graphics2d;

import java.awt.*;
import java.io.IOException;

import org.junit.Test;

public class FontTest extends PdfBoxGraphics2DTestBase {
	@Test
	public void testAntonioFont() throws IOException, FontFormatException
	{
		final Font antonioRegular = Font
				.createFont(Font.TRUETYPE_FONT,
						PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf"))
				.deriveFont(15f);
		exportGraphic("fonts", "antonio", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				gfx.setColor(Color.BLACK);
				gfx.setFont(antonioRegular);
				gfx.drawString("Für älter österlich, Umlauts are not always fun.", 10, 50);
			}
		});
	}
}
