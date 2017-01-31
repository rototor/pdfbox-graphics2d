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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PdfBoxGraphics2DTestBase {
	protected void exportGraphic(String dir, String name, GraphicsExporter exporter) {
		try {
			PDDocument document = new PDDocument();

			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			File parentDir = new File("target/test/" + dir);
			parentDir.mkdirs();

			PDPageContentStream contentStream = new PDPageContentStream(document, page);

			PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);
			exporter.draw(pdfBoxGraphics2D);
			pdfBoxGraphics2D.dispose();

			BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D imageGraphics = image.createGraphics();
			exporter.draw(imageGraphics);
			imageGraphics.dispose();
			ImageIO.write(image, "PNG", new File(parentDir, name + ".png"));

			PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
			Matrix matrix = new Matrix();
			matrix.translate(0, 20);
			contentStream.transform(matrix);
			contentStream.drawForm(appearanceStream);

			matrix.scale(1.5f, 1.5f);
			matrix.translate(0, 100);
			contentStream.transform(matrix);
			contentStream.drawForm(appearanceStream);
			contentStream.close();

			document.save(new File(parentDir, name + ".pdf"));
			document.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	interface GraphicsExporter {
		void draw(Graphics2D gfx) throws IOException, FontFormatException;
	}
}
