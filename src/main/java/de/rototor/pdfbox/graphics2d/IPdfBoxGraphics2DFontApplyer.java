package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.*;
import java.io.IOException;

public interface IPdfBoxGraphics2DFontApplyer {
	void applyFont(Font font, PDDocument document, PDPageContentStream contentStream) throws IOException;
}
