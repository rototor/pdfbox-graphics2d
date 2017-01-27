package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;

import java.awt.*;

public interface IPdfBoxGraphics2DColorMapper {
	PDColor mapColor(PDDocument doc, Color color);
}
