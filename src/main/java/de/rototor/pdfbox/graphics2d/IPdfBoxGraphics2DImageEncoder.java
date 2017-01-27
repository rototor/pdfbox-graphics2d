package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

import java.awt.*;

public interface IPdfBoxGraphics2DImageEncoder {
	PDImage encodeImage(PDDocument document, Image image) ;
}
