package de.rototor.pdfbox.graphics2d;

import java.io.IOException;

/**
 * Functional Interface to allow draw within a marked content sequence
 */
public interface IPdfBoxGraphics2DMarkedContentDrawer {
	/**
	 * Draw within a marked content. All state changes on the given gfx will not have any effect
	 * after returning from this call
	 * @param gfx  the graphics to draw on. Do NOT dispose this.
	 */
	void draw(PdfBoxGraphics2D gfx) throws IOException;
}
