package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

import java.awt.*;

public class PdfBoxGraphics2DColorMapper implements IPdfBoxGraphics2DColorMapper {
	@Override
	public PDColor mapColor(PDDocument doc, Color color) {
		float[] components = new float[] { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f };
		PDColor pdColor = new PDColor(components, PDDeviceRGB.INSTANCE);
		return pdColor;
	}
}
