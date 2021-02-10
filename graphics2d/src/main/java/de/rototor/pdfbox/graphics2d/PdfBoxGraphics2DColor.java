package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;

import java.awt.*;
import java.io.IOException;

/**
 * Universal PDColor Carrying class. It does not assume any specific colorspace.
 * Useful for e.g. speration colors
 */
public class PdfBoxGraphics2DColor extends Color implements IPdfBoxGraphics2DColor {
	private final PDColor color;
	private final int alpha;
	private final boolean overprint;

	/**
	 * @param color the PDColor
	 */
	public PdfBoxGraphics2DColor(PDColor color) {
		this(color, 255);
	}

	/**
	 * @param color the PDColor
	 * @param alpha the alpha to use
	 */
	public PdfBoxGraphics2DColor(PDColor color, int alpha) {
		this(color, alpha, false);
	}

	/**
	 *
	 * @param color the PDColor
	 * @param alpha the alpha to use
	 * @param overprint determine if overprint should be used
	 */
	public PdfBoxGraphics2DColor(PDColor color, int alpha, boolean overprint)  {
		super(toRGBValue(color, alpha));
		this.color = color;
		this.alpha = alpha;
		this.overprint = overprint;
	}

	private static int toRGBValue(PDColor color, int alpha) {
		try {
			return (color.toRGB() & 0xFFFFFF) | alpha << 24;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getAlpha() {
		return alpha;
	}

	public boolean isOverprint() {
		return overprint;
	}

	public PDColor toPDColor(){
		return color;
	}
}
