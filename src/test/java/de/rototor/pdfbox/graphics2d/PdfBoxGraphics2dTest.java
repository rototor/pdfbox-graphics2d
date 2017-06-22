/*
 * Copyright 2017 Emmeran Seehuber

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.rototor.pdfbox.graphics2d;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.AttributedString;

public class PdfBoxGraphics2dTest extends PdfBoxGraphics2DTestBase {

	@Test
	public void testNegativeShapesAndComposite() {
		exportGraphic("simple", "negativeWithComposite", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				RoundRectangle2D.Float rect = new RoundRectangle2D.Float(10f, 10f, 20f, 20f, 5f, 6f);

				AffineTransform transformIdentity = new AffineTransform();
				AffineTransform transformMirrored = AffineTransform.getTranslateInstance(0, 100);
				transformMirrored.scale(1, -0.5);
				for (AffineTransform tf : new AffineTransform[] { transformIdentity, transformMirrored }) {
					gfx.setTransform(tf);
					gfx.setColor(Color.red);
					gfx.fill(rect);
					gfx.setStroke(new BasicStroke(2f));
					gfx.draw(rect);
					GradientPaint gp = new GradientPaint(10.0f, 25.0f, Color.blue, (float) 100, (float) 100, Color.red);
					gfx.setPaint(gp);
					gfx.fill(AffineTransform.getTranslateInstance(30f, 20f).createTransformedShape(rect));
					Composite composite = gfx.getComposite();
					gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
					gfx.setColor(Color.blue);
					gfx.fillRect(15, 0, 40, 40);
					gfx.setComposite(composite);
				}

			}
		});
	}

	@Test
	public void testSimpleGraphics2d() throws IOException {
		exportGraphic("simple", "simple", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException, FontFormatException {
				BufferedImage imgColorTest = ImageIO
						.read(PdfBoxGraphics2dTest.class.getResourceAsStream("colortest.png"));
				BufferedImage img2 = ImageIO.read(PdfBoxGraphics2dTest.class.getResourceAsStream("pixeltest.png"));
				BufferedImage img3 = ImageIO.read(PdfBoxGraphics2dTest.class.getResourceAsStream("Rose-ProPhoto.jpg"));

				gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				gfx.drawImage(imgColorTest, 70, 50, 100, 50, null);

				gfx.drawImage(img3, 30, 200, 75, 50, null);

				gfx.setColor(Color.YELLOW);
				gfx.drawRect(20, 20, 100, 100);
				gfx.setColor(Color.GREEN);
				gfx.fillRect(10, 10, 50, 50);

				gfx.setColor(Color.RED);
				gfx.drawString("Hello World!", 30, 120);
				gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				gfx.drawImage(img2, 30, 50, 50, 50, null);

				Font font = new Font("SansSerif", Font.PLAIN, 30);
				Font font2 = Font
						.createFont(Font.TRUETYPE_FONT,
								PdfBoxGraphics2dTest.class.getResourceAsStream("DejaVuSerifCondensed.ttf"))
						.deriveFont(20f);
				final String words = "Valour fate kinship darkness";

				AttributedString as1 = new AttributedString(words);
				as1.addAttribute(TextAttribute.FONT, font);

				Rectangle2D valour = font2.getStringBounds("Valour", gfx.getFontRenderContext());
				GradientPaint gp = new GradientPaint(10.0f, 25.0f, Color.blue, (float) valour.getWidth(),
						(float) valour.getHeight(), Color.red);

				gfx.setColor(Color.GREEN);
				as1.addAttribute(TextAttribute.FOREGROUND, gp, 0, 6);
				as1.addAttribute(TextAttribute.KERNING, TextAttribute.KERNING_ON, 0, 6);
				as1.addAttribute(TextAttribute.FONT, font2, 0, 6);
				as1.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 7, 11);
				as1.addAttribute(TextAttribute.BACKGROUND, Color.LIGHT_GRAY, 12, 19);
				as1.addAttribute(TextAttribute.FONT, font2, 20, 28);
				as1.addAttribute(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON, 20, 28);
				as1.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, 20, 28);

				gfx.drawString(as1.getIterator(), 15, 160);

				// Hello World - in arabic and hebrew
				Font font3 = new Font("SansSerif", Font.PLAIN, 40);
				gfx.setFont(font3);
				gfx.setColor(Color.BLACK);
				gfx.drawString("مرحبا بالعالم", 200, 100);
				gfx.setPaint(new TexturePaint(imgColorTest, new Rectangle2D.Float(5f, 7f, 100f, 20f)));
				gfx.drawString("مرحبا بالعالم", 200, 250);
				gfx.drawString("שלום עולם", 200, 200);

				gfx.setClip(new Ellipse2D.Float(360, 360, 60, 80));
				gfx.fillRect(300, 300, 100, 100);
				gfx.setClip(null);
				gfx.fillRect(360, 360, 10, 10);

			}
		});
	}

}