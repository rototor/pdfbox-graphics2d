package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;

import java.awt.*;
import java.io.IOException;

/**
 * This color class represents a CMYK Color. You can use this class if you want
 * to paint with DeviceCMYK Colors
 */
public class PdfBoxGraphics2DCMYKColor extends Color {
	private final float c, m, y, k;
	private final PDColorSpace colorSpace;

	public PdfBoxGraphics2DCMYKColor(int c, int m, int y, int k, int alpha) {
		this(c / 255f, m / 255f, y / 255f, k / 255f, alpha);
	}

	public PdfBoxGraphics2DCMYKColor(int c, int m, int y, int k) {
		this(c / 255f, m / 255f, y / 255f, k / 255f);
	}

	public PdfBoxGraphics2DCMYKColor(float c, float m, float y, float k) {
		this(c, m, y, k, 255);
	}

	private static int toRGBValue(float c, float m, float y, float k, int alpha, PDColorSpace colorSpace) {
		float[] rgb;
		try {
			rgb = colorSpace.toRGB(new float[] { c, m, y, k });
			int r = ((int) (rgb[0] * 0xFF)) & 0xFF;
			int g = ((int) (rgb[1] * 0xFF)) & 0xFF;
			int b = ((int) (rgb[2] * 0xFF)) & 0xFF;
			return alpha << 24 | r << 16 | g << 8 | b;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public PdfBoxGraphics2DCMYKColor(float c, float m, float y, float k, int alpha) {
		this(c, m, y, k, alpha, PDDeviceCMYK.INSTANCE);
	}

	public PdfBoxGraphics2DCMYKColor(float c, float m, float y, float k, int alpha, PDColorSpace colorSpace) {
		super(toRGBValue(c, m, y, k, alpha, colorSpace), true);
		this.c = c;
		this.m = m;
		this.y = y;
		this.k = k;
		this.colorSpace = colorSpace;
	}

	public float getC() {
		return c;
	}

	public float getM() {
		return m;
	}

	public float getY() {
		return y;
	}

	public float getK() {
		return k;
	}

	/**
	 * @return the PDColor represented by this color object
	 */
	public PDColor toPDColor() {
		return new PDColor(new float[] { getC(), getM(), getY(), getK() }, colorSpace);
	}
}
