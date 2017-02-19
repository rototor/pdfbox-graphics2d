package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;

/**
 * Apply the given paint on the Content Stream.
 */
public interface IPdfBoxGraphics2DPaintApplier {
	/**
	 * Apply the paint on the ContentStream
	 * 
	 * @param paint
	 *            the paint which should be applied
	 * @param contentStream
	 *            the content stream to apply the paint on
	 * @param currentTransform
	 *            the current transform of the Graphics2D relative to the
	 *            contentStream default coordinate space.
	 * @param colorMapper
	 *            the color mapper, to map the colors
	 * @return null or a PDShading which should be used to fill a shape.
	 */
	PDShading applyPaint(Paint paint, PDPageContentStream contentStream, AffineTransform currentTransform,
			IPdfBoxGraphics2DColorMapper colorMapper) throws IOException;
}
