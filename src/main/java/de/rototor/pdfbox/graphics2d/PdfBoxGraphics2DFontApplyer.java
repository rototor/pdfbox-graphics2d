package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFontFactory;

import java.awt.*;
import java.io.IOException;

public class PdfBoxGraphics2DFontApplyer implements IPdfBoxGraphics2DFontApplyer {

	@Override
	public void applyFont(Font font, PDDocument document, PDPageContentStream contentStream) throws IOException {
		contentStream.setFont(PDFontFactory.createDefaultFont(), font.getSize2D());
	}
}
