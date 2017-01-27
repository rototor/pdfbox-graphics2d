package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class PdfBoxGraphics2DLosslessImageEncoder implements IPdfBoxGraphics2DImageEncoder {

	@Override
	public PDImage encodeImage(PDDocument document, Image image) {
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
