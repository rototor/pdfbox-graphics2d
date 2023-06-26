package de.rototor.pdfbox.graphics2d;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.text.AttributedString;

import org.junit.Test;

public class FontTest extends PdfBoxGraphics2DTestBase {
	@Test
	public void testAntonioFont() throws IOException, FontFormatException {
		final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
				PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf")).deriveFont(15f);
		exportGraphic("fonts", "antonio", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				gfx.setColor(Color.BLACK);
				gfx.setFont(antonioRegular);
				gfx.drawString("Für älter österlich, Umlauts are not always fun.", 10, 50);
			}
		});
	}

	@Test
	public void testStyledAttributeIterator() throws IOException, FontFormatException {
		final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
				PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf")).deriveFont(15f);
		exportGraphic("fonts", "attributed_text", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				gfx.setColor(Color.BLACK);
				gfx.setFont(antonioRegular);
				AttributedString str = new AttributedString("This is some funny text with some attributes.");
				str.addAttribute(TextAttribute.SIZE, 20f, 0, 4);

				str.addAttribute(TextAttribute.FOREGROUND, Color.RED, 0, 4);

				str.addAttribute(TextAttribute.FOREGROUND, Color.green, 13, 23);
				str.addAttribute(TextAttribute.SIZE, 18f, 13, 23);
				str.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 13, 23);

				str.addAttribute(TextAttribute.FOREGROUND, Color.MAGENTA, 34, 44);
				str.addAttribute(TextAttribute.SIZE, 22f, 34, 44);
				str.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, 34, 44);

				gfx.drawString(str.getIterator(), 10, 50);

				Font font = new Font("SansSerif", Font.PLAIN, 12);
				Font font2 = Font
						.createFont(Font.TRUETYPE_FONT,
								PdfBoxGraphics2dTest.class.getResourceAsStream("DejaVuSerifCondensed.ttf"))
						.deriveFont(13f);
				str.addAttribute(TextAttribute.FONT, font);
				gfx.drawString(str.getIterator(), 10, 100);
				str.addAttribute(TextAttribute.FONT, font2);
				gfx.drawString(str.getIterator(), 10, 150);
			}
		});
	}

	@Test
	public void testTransformedFont() throws IOException, FontFormatException {
		final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
				PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf")).deriveFont(15f);
		exportGraphic("fonts", "transformed", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				AffineTransform affineTransform = antonioRegular.getTransform();
				affineTransform.rotate(Math.toRadians(-90), 0, 0);
				Font rotatedFont = antonioRegular.deriveFont(affineTransform);
				gfx.setColor(Color.BLACK);
				gfx.setFont(rotatedFont);
				gfx.drawString("Some sample text", 50, 150);

				AffineTransform saveTF = gfx.getTransform();
				AffineTransform at = AffineTransform.getTranslateInstance(100, 150);
				at.rotate(Math.toRadians(-90), 0, 0);
				gfx.setColor(Color.RED);
				gfx.setFont(antonioRegular);
				gfx.setTransform(at);
				gfx.drawString("Some sample text", 0, 0);
				gfx.setTransform(saveTF);
			}
		});
	}

	@Test
	public void testFancyTransformedFont() throws IOException, FontFormatException {
		final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
				PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf")).deriveFont(15f);
		exportGraphic("fonts", "fancyTransformed", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				AffineTransform affineTransform = antonioRegular.getTransform();
				affineTransform.shear(Math.toRadians(15), Math.toRadians(-35));
				Font rotatedFont = antonioRegular.deriveFont(affineTransform);
				gfx.setColor(Color.BLACK);
				gfx.setFont(rotatedFont);
				gfx.drawString("Sheared Text", 50, 150);

				affineTransform = antonioRegular.getTransform();
				affineTransform.rotate(Math.toRadians(45), Math.toRadians(-45));
				rotatedFont = antonioRegular.deriveFont(affineTransform);
				gfx.setColor(Color.BLUE);
				gfx.setFont(rotatedFont);
				gfx.drawString("Rotated Text", 150, 150);

				affineTransform = antonioRegular.getTransform();
				affineTransform.rotate(Math.toRadians(45), Math.toRadians(-45));
				affineTransform.shear(Math.toRadians(45), Math.toRadians(-45));
				rotatedFont = antonioRegular.deriveFont(affineTransform);
				gfx.setColor(Color.GREEN);
				gfx.setFont(rotatedFont);
				gfx.drawString("Shear & Rotated Text", 50, 250);
			}
		});
	}

	@Test
	public void testStyledAttributeIteratorTransformed() throws IOException, FontFormatException {
		final Font antonioRegular = Font.createFont(Font.TRUETYPE_FONT,
				PdfBoxGraphics2dTest.class.getResourceAsStream("antonio/Antonio-Regular.ttf")).deriveFont(15f);
		exportGraphic("fonts", "attributed_transformed_text", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				gfx.setColor(Color.BLACK);
				gfx.setFont(antonioRegular);
				AttributedString str = new AttributedString("This is some funny text with some attributes.");
				str.addAttribute(TextAttribute.SIZE, 20f, 0, 4);

				str.addAttribute(TextAttribute.FOREGROUND, Color.RED, 0, 4);

				str.addAttribute(TextAttribute.FOREGROUND, Color.green, 13, 23);
				str.addAttribute(TextAttribute.SIZE, 18f, 13, 23);
				str.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 13, 23);
				str.addAttribute(TextAttribute.TRANSFORM,
						new TransformAttribute(AffineTransform.getRotateInstance(Math.toRadians(10))), 13, 23);

				str.addAttribute(TextAttribute.FOREGROUND, Color.MAGENTA, 34, 44);
				str.addAttribute(TextAttribute.SIZE, 22f, 34, 44);
				str.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, 34, 44);
				str.addAttribute(TextAttribute.TRANSFORM,
						new TransformAttribute(AffineTransform.getRotateInstance(Math.toRadians(-10))), 34, 44);

				gfx.drawString(str.getIterator(), 10, 50);

				Font font = new Font("SansSerif", Font.PLAIN, 12);
				Font font2 = Font
						.createFont(Font.TRUETYPE_FONT,
								PdfBoxGraphics2dTest.class.getResourceAsStream("DejaVuSerifCondensed.ttf"))
						.deriveFont(13f).deriveFont(AffineTransform.getRotateInstance(Math.toRadians(15)));

				str.addAttribute(TextAttribute.FONT, font);
				gfx.drawString(str.getIterator(), 10, 100);
				str.addAttribute(TextAttribute.FONT, font2);
				gfx.drawString(str.getIterator(), 10, 150);
			}
		});
	}

	@Test
	public void testFontFamily() throws IOException, FontFormatException {
		exportGraphic("fonts", "formatted_attributes", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				String[] lines = new String[] {// 
						"Straight text with no tags",
						"Bold:<b>text</b>, and no bold",
						"Italic:<i>text in italic</i>, and continue without",
						"Strikethrough:<s>this is strikethrough text</s> followed by <u>underlined text</u>.",
						"Superscript: operating temp: 35<sup>celcius</sup>.",
						"Subscript: note<sub>see footmark 1</sub> and continue",
						"Now change color to <color=red>red</color> and back to black",
						"Use size <size=18>large</size> and <size=6>small</size> to normal"
						};
				gfx.setColor(Color.BLACK);
				int y = 20;
				for (String line : lines) {
					FormattedString formattedString = new FormattedString(line);
					gfx.drawString(formattedString.getAttributedString().getIterator(), 10, y);
					y += 20;

				}
			}
		});
	}

}
