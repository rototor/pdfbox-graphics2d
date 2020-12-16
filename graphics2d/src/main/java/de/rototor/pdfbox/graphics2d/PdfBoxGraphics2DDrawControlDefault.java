package de.rototor.pdfbox.graphics2d;

import java.awt.*;

/**
 * Default implementation which does nothing. You can derive from it to only
 * override the needed methods
 */
public class PdfBoxGraphics2DDrawControlDefault implements IPdfBoxGraphics2DDrawControl {
	public static final PdfBoxGraphics2DDrawControlDefault INSTANCE = new PdfBoxGraphics2DDrawControlDefault();

	protected PdfBoxGraphics2DDrawControlDefault() {
	}

	@Override
	public Shape transformShapeBeforeFill(Shape shape, IDrawControlEnv env) {
		return shape;
	}

	@Override
	public Shape transformShapeBeforeDraw(Shape shape, IDrawControlEnv env) {
		return shape;
	}

	@Override
	public void afterShapeFill(Shape shape, IDrawControlEnv env) {
	}

	@Override
	public void afterShapeDraw(Shape shape, IDrawControlEnv env) {
	}
}
