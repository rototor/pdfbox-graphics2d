package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;

public interface IPdfBoxGraphics2DImageEncoder {
	PDImageXObject encodeImage(PDDocument document, Image image) ;
}
