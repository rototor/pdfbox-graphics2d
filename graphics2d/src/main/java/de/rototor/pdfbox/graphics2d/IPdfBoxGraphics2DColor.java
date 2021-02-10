package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;

/**
 * Common interface for all our PDColor carrying classes.
 */
public interface IPdfBoxGraphics2DColor {
	/**
	 * @return the PDColor represented by this color object
	 */
	PDColor toPDColor();

	/**
	 * @return true if this color should be applied with overprint
	 */
	boolean isOverprint();
}
