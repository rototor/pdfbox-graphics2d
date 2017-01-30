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
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfBoxGraphics2DLosslessImageEncoder implements IPdfBoxGraphics2DImageEncoder {

	@Override
	public PDImageXObject encodeImage(PDDocument document, Image image) {
		final BufferedImage bi;
		if (image instanceof BufferedImage) {
			bi = (BufferedImage) image;
		} else {
			int width = image.getWidth(null);
			int height = image.getHeight(null);
			bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics graphics = bi.getGraphics();
			if (!graphics.drawImage(image, 0, 0, null, null))
				throw new IllegalStateException("Not fully loaded images are not supported.");
		}
		try {
			return LosslessFactory.createFromImage(document, bi);
		} catch (IOException e) {
			throw new RuntimeException("Could not encode Image", e);
		}
	}
}
