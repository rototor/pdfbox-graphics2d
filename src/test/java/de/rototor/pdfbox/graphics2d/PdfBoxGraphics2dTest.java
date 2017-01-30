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
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfBoxGraphics2dTest extends PdfBoxGraphics2DTestBase {

	@Test
	public void testSimpleGraphics2d() throws IOException {

		exportGraphic("simple", "simple", new GraphicsExporter() {
			@Override
			public void draw(Graphics2D gfx) throws IOException {
				BufferedImage img1 = ImageIO.read(PdfBoxGraphics2dTest.class.getResourceAsStream("colortest.png"));
				BufferedImage img2 = ImageIO.read(PdfBoxGraphics2dTest.class.getResourceAsStream("pixeltest.png"));

				gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				gfx.drawImage(img1, 70, 50, 100, 50, null);

				gfx.setColor(Color.YELLOW);
				gfx.drawRect(20, 20, 100, 100);
				gfx.setColor(Color.GREEN);
				gfx.fillRect(10, 10, 50, 50);

				gfx.setColor(Color.RED);
				gfx.drawString("Hello World!", 30, 120);
				gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				gfx.drawImage(img2, 30, 50, 50, 50, null);
			}
		});
	}

}