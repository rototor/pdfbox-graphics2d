/*
 * Copyright 2017 Emmeran Seehuber

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;

/**
 * Graphics 2D Adapter for PDFBox.
 */
public class PdfBoxGraphics2D extends Graphics2D {
	private final PDFormXObject xFormObject;
	private final Graphics2D calcGfx;
	private final PDPageContentStream contentStream;
	private BufferedImage calcImage;
	private PDDocument document;
	private final int pixelWidth;
	private final int pixelHeight;
	private final AffineTransform baseTransform;
	private AffineTransform transform = new AffineTransform();
	private IPdfBoxGraphics2DImageEncoder imageEncoder = new PdfBoxGraphics2DLosslessImageEncoder();
	private IPdfBoxGraphics2DColorMapper colorMapper = new PdfBoxGraphics2DColorMapper();
	private IPdfBoxGraphics2DFontApplyer fontApplyer = new PdfBoxGraphics2DFontApplyer();
	private Paint paint;
	private Stroke stroke;
	private Color xorColor;
	private Font font;
	private Composite composite;
	private Shape clipShape;
	private Color backgroundColor;
	private boolean isClone = false;
	private boolean vectoriseText = true;

	/**
	 * Determine if text should be drawn as text with embedded font in the PDF
	 * or as vector shapes.
	 * 
	 * @param vectoriseText
	 *            true if all text should be drawn as vector shapes. No fonts
	 *            will be embedded in that case.
	 */
	public void setVectoriseText(boolean vectoriseText) {
		this.vectoriseText = vectoriseText;
	}

	/**
	 * Create a PDfBox Graphics2D. When this is disposed
	 */
	public PdfBoxGraphics2D(PDDocument document, int pixelWidth, int pixelHeight) throws IOException {
		this.document = document;
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;

		PDAppearanceStream appearance = new PDAppearanceStream(document);
		xFormObject = appearance;
		xFormObject.setResources(new PDResources());
		xFormObject.setBBox(new PDRectangle(pixelWidth, pixelHeight));
		contentStream = new PDPageContentStream(document, appearance, xFormObject.getStream().createOutputStream());
		contentStream.saveGraphicsState();

		baseTransform = new AffineTransform();
		baseTransform.translate(0, pixelHeight);
		baseTransform.scale(1, -1);

		calcImage = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR);
		calcGfx = calcImage.createGraphics();
		font = calcGfx.getFont();
	}

	private PdfBoxGraphics2D(PdfBoxGraphics2D gfx) throws IOException {
		this.document = gfx.document;
		this.pixelWidth = gfx.pixelWidth;
		this.pixelHeight = gfx.pixelHeight;
		this.xFormObject = gfx.xFormObject;
		this.contentStream = gfx.contentStream;
		this.baseTransform = gfx.baseTransform;
		this.transform = (AffineTransform) gfx.transform.clone();
		this.calcGfx = gfx.calcGfx;
		this.calcImage = gfx.calcImage;
		this.font = gfx.font;
		this.stroke = gfx.stroke;
		this.paint = gfx.paint;
		this.clipShape = gfx.clipShape;
		this.backgroundColor = gfx.backgroundColor;
		this.colorMapper = gfx.colorMapper;
		this.fontApplyer = gfx.fontApplyer;
		this.imageEncoder = gfx.imageEncoder;
		this.xorColor = gfx.xorColor;

		this.isClone = true;
		this.contentStream.saveGraphicsState();
	}

	/**
	 * 
	 * @return the PDAppearanceStream which resulted in this graphics
	 */
	public PDFormXObject getXFormObject() {
		if (document != null)
			throw new IllegalStateException("You can only get the xFormObject after you disposed the Graphics2D!");
		if (isClone)
			throw new IllegalStateException("You can not get the xform stream from the clone");
		return xFormObject;
	}

	public void dispose() {
		if (isClone) {
			try {
				this.contentStream.restoreGraphicsState();
			} catch (IOException e) {
				throwIOException(e);
			}
			return;
		}
		try {
			contentStream.restoreGraphicsState();
			contentStream.close();
		} catch (IOException e) {
			throwIOException(e);
		}
		document = null;
		calcGfx.dispose();
		calcImage.flush();
		calcImage = null;
	}

	public void draw(Shape s) {
		try {
			contentStream.saveGraphicsState();
			applyPaint();
			if (stroke instanceof BasicStroke) {
				BasicStroke basicStroke = (BasicStroke) this.stroke;

				// Cap Style maps 1:1 between Java and PDF Spec
				contentStream.setLineCapStyle(basicStroke.getEndCap());
				// Line Join Style maps 1:1 between Java and PDF Spec
				contentStream.setLineJoinStyle(basicStroke.getLineJoin());

				// Pending PDFBOX-3669 - to be replaced
				// noinspection deprecation
				contentStream.appendRawCommands(basicStroke.getMiterLimit() + " M ");

				AffineTransform tf = new AffineTransform();
				tf.concatenate(baseTransform);
				tf.concatenate(transform);

				double scaleX = tf.getScaleX();
				contentStream.setLineWidth((float) Math.abs(basicStroke.getLineWidth() * scaleX));
				float[] dashArray = basicStroke.getDashArray();
				if (dashArray != null) {
					for (int i = 0; i < dashArray.length; i++)
						dashArray[i] = (float) Math.abs(dashArray[i] * scaleX);
					contentStream.setLineDashPattern(dashArray, (float) Math.abs(basicStroke.getDashPhase() * scaleX));
				}
			}
			walkShape(s);
			contentStream.stroke();
			contentStream.restoreGraphicsState();
		} catch (IOException e) {
			throwIOException(e);
		}
	}

	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		AffineTransform tf = new AffineTransform();
		tf.concatenate(baseTransform);
		tf.concatenate(transform);
		tf.concatenate((AffineTransform) xform.clone());

		PDImageXObject pdImage = imageEncoder.encodeImage(document, img);
		try {
			contentStream.saveGraphicsState();
			int imgHeight = img.getHeight(obs);
			tf.translate(0, imgHeight);
			tf.scale(1, -1);
			contentStream.transform(new Matrix(tf));

			if (RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
					.equals(renderingHints.get(RenderingHints.KEY_INTERPOLATION)))
				pdImage.setInterpolate(false);
			contentStream.drawImage(pdImage, 0, 0, img.getWidth(obs), imgHeight);
			contentStream.restoreGraphicsState();
		} catch (IOException e) {
			throwIOException(e);
		}
		return true;
	}

	public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
		BufferedImage img1 = op.filter(img, null);
		drawImage(img1, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
	}

	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		WritableRaster data = img.copyData(null);
		drawImage(new BufferedImage(img.getColorModel(), data, false, null), xform, null);
	}

	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		drawRenderedImage(img.createDefaultRendering(), xform);
	}

	public void drawString(String str, int x, int y) {
		drawString(str, (float) x, (float) y);
	}

	public void drawString(String str, float x, float y) {
		drawString(new AttributedString(str).getIterator(), x, y);
	}

	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		drawString(iterator, (float) x, (float) y);
	}

	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), observer);
	}

	public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
		AffineTransform tf = new AffineTransform();
		tf.translate(x, y);
		tf.scale((float) width / img.getWidth(null), (float) height / img.getHeight(null));
		return drawImage(img, tf, observer);
	}

	public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
		return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), bgcolor, observer);
	}

	public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
		try {
			if (bgcolor != null) {
				contentStream.setNonStrokingColor(colorMapper.mapColor(document, bgcolor));
				walkShape(new Rectangle(x, y, width, height));
				contentStream.fill();
			}
			return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), observer);
		} catch (IOException e) {
			throwIOException(e);
			return false;
		}
	}

	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
			ImageObserver observer) {
		return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy2, sx2, sy2, null, observer);
	}

	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
			Color bgcolor, ImageObserver observer) {
		try {
			contentStream.saveGraphicsState();
			int width = dx2 - dx1;
			int height = dy2 - dy1;

			/*
			 * Set the clipping
			 */
			walkShape(new Rectangle2D.Double(dx1, dy1, width, height));
			contentStream.clip();

			/*
			 * Maybe fill the background color
			 */
			if (bgcolor != null) {
				contentStream.setNonStrokingColor(colorMapper.mapColor(document, bgcolor));
				walkShape(new Rectangle(dx1, dy1, width, height));
				contentStream.fill();
			}

			/*
			 * Build the transform for the image
			 */
			AffineTransform tf = new AffineTransform();
			tf.translate(dx1, dy1);
			float imgWidth = img.getWidth(observer);
			float imgHeight = img.getHeight(observer);
			tf.scale((float) width / imgWidth, (float) height / imgHeight);
			tf.translate(-sx1, -sy1);
			tf.scale((sx2 - sx1) / imgWidth, (sy2 - sy1) / imgHeight);

			drawImage(img, tf, observer);
			contentStream.restoreGraphicsState();
			return true;
		} catch (IOException e) {
			throwIOException(e);
			return false;
		}
	}

	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		try {
			if (vectoriseText)
				drawStringUsingShapes(iterator, x, y);
			else
				drawStringUsingText(iterator, x, y);
		} catch (IOException e) {
			throwIOException(e);
		}
	}

	private void drawStringUsingShapes(AttributedCharacterIterator iterator, float x, float y) throws IOException {
		float xOffset = 0;
		boolean run = true;
		Stroke originalStroke = stroke;
		Paint originalPaint = paint;
		while (run) {
			StringBuilder sb = new StringBuilder();
			Font attributeFont = (Font) iterator.getAttribute(TextAttribute.FONT);
			if (attributeFont == null)
				attributeFont = font;
			Number fontSize = ((Number) iterator.getAttribute(TextAttribute.SIZE));
			if (fontSize != null)
				attributeFont = attributeFont.deriveFont(fontSize.floatValue());
			Paint foreground = (Paint) iterator.getAttribute(TextAttribute.FOREGROUND);
			if (foreground == null)
				foreground = originalPaint;
			Paint background = (Paint) iterator.getAttribute(TextAttribute.BACKGROUND);
			boolean underline = TextAttribute.UNDERLINE_ON.equals(iterator.getAttribute(TextAttribute.UNDERLINE));
			boolean strikethrough = TextAttribute.STRIKETHROUGH_ON
					.equals(iterator.getAttribute(TextAttribute.STRIKETHROUGH));
			boolean kerning = TextAttribute.KERNING_ON.equals(iterator.getAttribute(TextAttribute.KERNING));
			boolean ligatures = TextAttribute.LIGATURES_ON.equals(iterator.getAttribute(TextAttribute.LIGATURES));

			if (kerning || ligatures) {
				Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
				if (kerning)
					attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
				if (ligatures)
					attributes.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
				attributeFont = attributeFont.deriveFont(attributes);
			}

			run = iterateRun(iterator, sb);
			assert attributeFont != null;
			GlyphVector glyphVector = attributeFont.createGlyphVector(getFontRenderContext(), sb.toString());

			Rectangle2D visualBounds = glyphVector.getVisualBounds();
			AffineTransform af = new AffineTransform();
			af.translate(x + xOffset, y);

			if (background != null) {
				paint = background;
				fill(af.createTransformedShape(visualBounds));
			}

			paint = foreground;
			stroke = originalStroke;
			drawGlyphVector(glyphVector, x + xOffset, y);

			if (underline || strikethrough) {
				double x1 = visualBounds.getMinX();
				double x2 = visualBounds.getMaxX();
				LineMetrics lineMetrics = attributeFont.getLineMetrics(sb.toString(), getFontRenderContext());
				if (underline) {
					double yPos = lineMetrics.getUnderlineOffset() + 1;
					stroke = new BasicStroke(lineMetrics.getUnderlineThickness());
					draw(af.createTransformedShape(new Line2D.Double(x1, yPos, x2, yPos)));
				}
				if (strikethrough) {
					double yPos = lineMetrics.getStrikethroughOffset();
					stroke = new BasicStroke(lineMetrics.getStrikethroughThickness());
					draw(af.createTransformedShape(new Line2D.Double(x1, yPos, x2, yPos)));
				}
			}

			xOffset += glyphVector.getLogicalBounds().getWidth();

		}
		paint = originalPaint;
		stroke = originalStroke;
	}

	private boolean iterateRun(AttributedCharacterIterator iterator, StringBuilder sb) {
		sb.setLength(0);
		int charCount = iterator.getRunLimit() - iterator.getRunStart();
		while (charCount-- >= 0) {
			char c = iterator.current();
			iterator.next();
			if (c == AttributedCharacterIterator.DONE) {
				return false;
			} else {
				sb.append(c);
			}
		}
		return true;
	}

	private void drawStringUsingText(AttributedCharacterIterator iterator, float x, float y) throws IOException {
		contentStream.saveGraphicsState();
		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);
		tf.translate(x, y);
		contentStream.transform(new Matrix(tf));

		Matrix textMatrix = new Matrix();
		textMatrix.scale(1, -1);
		contentStream.beginText();
		fontApplyer.applyFont(font, document, contentStream);
		applyPaint();
		contentStream.setTextMatrix(textMatrix);

		calcGfx.setFont(font);
		boolean run = true;
		while (run) {
			StringBuilder sb = new StringBuilder();
			Font attributeFont = (Font) iterator.getAttribute(TextAttribute.FONT);
			Number fontSize = ((Number) iterator.getAttribute(TextAttribute.SIZE));
			if (attributeFont != null) {
				if (fontSize != null)
					attributeFont = attributeFont.deriveFont(fontSize.floatValue());
				fontApplyer.applyFont(attributeFont, document, contentStream);
			}

			run = iterateRun(iterator, sb);
			contentStream.showText(sb.toString());
		}
		contentStream.endText();
		contentStream.restoreGraphicsState();
	}

	public void drawGlyphVector(GlyphVector g, float x, float y) {
		AffineTransform transformOrig = (AffineTransform) transform.clone();
		transform.translate(x, y);
		fill(g.getOutline());
		transform = transformOrig;
	}

	@SuppressWarnings("unchecked")
	private <T> T getPropertyValue(Object obj, String propertyGetter) {
		try {
			Class c = obj.getClass();
			while (c != null) {
				try {
					Method m = c.getMethod(propertyGetter, (Class<?>[]) null);
					return (T) m.invoke(obj);
				} catch (NoSuchMethodException ignored) {
				}
				c = c.getSuperclass();
			}
			throw new NullPointerException("Method " + propertyGetter + " not found!");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private PDShading applyPaint() throws IOException {
		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);

		if (paint instanceof Color) {
			Color color = (Color) paint;
			applyAsStrokingColor(color);
		} else if (paint.getClass().getSimpleName().equals("LinearGradientPaint")) {
			/*
			 * Batik has a copy of RadialGradientPaint, but it has the same
			 * structure as the AWT RadialGradientPaint. So we use Reflection to
			 * access the fields of both these classes.
			 */
			Color[] colors = getPropertyValue(paint, "getColors");
			Color firstColor = colors[0];
			PDColor firstColorMapped = colorMapper.mapColor(document, firstColor);
			applyAsStrokingColor(firstColor);

			PDShadingType3 shading = new PDShadingType3(new COSDictionary());
			shading.setShadingType(PDShading.SHADING_TYPE2);
			shading.setColorSpace(firstColorMapped.getColorSpace());
			float[] fractions = getPropertyValue(paint, "getFractions");
			Point2D startPoint = getPropertyValue(paint, "getStartPoint");
			Point2D endPoint = getPropertyValue(paint, "getEndPoint");
			AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
			tf.concatenate(gradientTransform);

			tf.transform(startPoint, startPoint);
			tf.transform(endPoint, endPoint);

			COSArray coords = new COSArray();
			coords.add(new COSFloat((float) startPoint.getX()));
			coords.add(new COSFloat((float) startPoint.getY()));
			coords.add(new COSFloat((float) endPoint.getX()));
			coords.add(new COSFloat((float) endPoint.getY()));
			shading.setCoords(coords);

			PDFunctionType3 type3 = buildType3Function(colors, fractions);

			COSArray extend = new COSArray();
			extend.add(COSBoolean.TRUE);
			extend.add(COSBoolean.TRUE);
			shading.setFunction(type3);
			shading.setExtend(extend);
			return shading;
		} else if (paint.getClass().getSimpleName().equals("RadialGradientPaint")) {
			/*
			 * Batik has a copy of RadialGradientPaint, but it has the same
			 * structure as the AWT RadialGradientPaint. So we use Reflection to
			 * access the fields of both these classes.
			 */
			Color[] colors = getPropertyValue(paint, "getColors");
			Color firstColor = colors[0];
			PDColor firstColorMapped = colorMapper.mapColor(document, firstColor);
			applyAsStrokingColor(firstColor);

			PDShadingType3 shading = new PDShadingType3(new COSDictionary());
			shading.setShadingType(PDShading.SHADING_TYPE3);
			shading.setColorSpace(firstColorMapped.getColorSpace());
			float[] fractions = getPropertyValue(paint, "getFractions");
			Point2D centerPoint = getPropertyValue(paint, "getCenterPoint");
			Point2D focusPoint = getPropertyValue(paint, "getFocusPoint");
			AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
			tf.concatenate(gradientTransform);
			tf.transform(centerPoint, centerPoint);
			tf.transform(focusPoint, focusPoint);

			@SuppressWarnings("ConstantConditions")
			float radius = getPropertyValue(paint, "getRadius");
			radius = (float) Math.abs(radius * tf.getScaleX());

			COSArray coords = new COSArray();

			coords.add(new COSFloat((float) centerPoint.getX()));
			coords.add(new COSFloat((float) centerPoint.getY()));
			coords.add(new COSFloat(0));
			coords.add(new COSFloat((float) focusPoint.getX()));
			coords.add(new COSFloat((float) focusPoint.getY()));
			coords.add(new COSFloat(radius));
			shading.setCoords(coords);

			PDFunctionType3 type3 = buildType3Function(colors, fractions);

			COSArray extend = new COSArray();
			extend.add(COSBoolean.TRUE);
			extend.add(COSBoolean.TRUE);
			shading.setFunction(type3);
			shading.setExtend(extend);
			return shading;
		} else if (paint instanceof GradientPaint) {
			GradientPaint gradientPaint = (GradientPaint) paint;
			Color[] colors = new Color[] { gradientPaint.getColor1(), gradientPaint.getColor2() };
			Color firstColor = colors[0];
			PDColor firstColorMapped = colorMapper.mapColor(document, firstColor);

			applyAsStrokingColor(firstColor);

			PDShadingType3 shading = new PDShadingType3(new COSDictionary());
			shading.setShadingType(PDShading.SHADING_TYPE2);
			shading.setColorSpace(firstColorMapped.getColorSpace());
			float[] fractions = new float[] { 0, 1 };
			Point2D startPoint = gradientPaint.getPoint1();
			Point2D endPoint = gradientPaint.getPoint2();

			tf.transform(startPoint, startPoint);
			tf.transform(endPoint, endPoint);

			COSArray coords = new COSArray();
			coords.add(new COSFloat((float) startPoint.getX()));
			coords.add(new COSFloat((float) startPoint.getY()));
			coords.add(new COSFloat((float) endPoint.getX()));
			coords.add(new COSFloat((float) endPoint.getY()));
			shading.setCoords(coords);

			PDFunctionType3 type3 = buildType3Function(colors, fractions);

			COSArray extend = new COSArray();
			extend.add(COSBoolean.TRUE);
			extend.add(COSBoolean.TRUE);

			shading.setFunction(type3);
			shading.setExtend(extend);
			return shading;
		} else {
			System.err.println("Don't know paint " + paint.getClass().getName());
		}
		return null;
	}

	private PDFunctionType3 buildType3Function(Color[] colors, float[] fractions) {
		COSDictionary function = new COSDictionary();
		function.setInt(COSName.FUNCTION_TYPE, 3);

		COSArray domain = new COSArray();
		domain.add(new COSFloat(0));
		domain.add(new COSFloat(1));

		COSArray encode = new COSArray();

		COSArray range = new COSArray();
		range.add(new COSFloat(0));
		range.add(new COSFloat(1));
		COSArray bounds = new COSArray();
		for (int i = 2; i < colors.length; i++)
			bounds.add(new COSFloat((1.0f / colors.length) * (i - 1)));

		COSArray functions = buildType2Functions(colors, domain, encode);

		function.setItem(COSName.FUNCTIONS, functions);
		function.setItem(COSName.BOUNDS, bounds);
		function.setItem(COSName.ENCODE, encode);

		PDFunctionType3 type3 = new PDFunctionType3(function);
		type3.setDomainValues(domain);
		return type3;
	}

	private COSArray buildType2Functions(Color[] colors, COSArray domain, COSArray encode) {
		Color prevColor = colors[0];

		COSArray functions = new COSArray();
		for (int i = 1; i < colors.length; i++) {
			Color color = colors[i];
			PDColor prevPdColor = colorMapper.mapColor(document, prevColor);
			PDColor pdColor = colorMapper.mapColor(document, color);
			COSArray c0 = new COSArray();
			COSArray c1 = new COSArray();
			for (float component : prevPdColor.getComponents())
				c0.add(new COSFloat(component));
			for (float component : pdColor.getComponents())
				c1.add(new COSFloat(component));

			COSDictionary type2Function = new COSDictionary();
			type2Function.setInt(COSName.FUNCTION_TYPE, 2);
			type2Function.setItem(COSName.C0, c0);
			type2Function.setItem(COSName.C1, c1);
			type2Function.setInt(COSName.N, 1);
			type2Function.setItem(COSName.DOMAIN, domain);
			functions.add(type2Function);

			encode.add(new COSFloat(0));
			encode.add(new COSFloat(1));
			prevColor = color;
		}
		return functions;
	}

	private void applyAsStrokingColor(Color color) throws IOException {
		contentStream.setStrokingColor(colorMapper.mapColor(document, color));
		contentStream.setNonStrokingColor(colorMapper.mapColor(document, color));

		int alpha = color.getAlpha();
		if (alpha < 255) {
			/*
			 * This is semitransparent
			 */
			PDExtendedGraphicsState pdExtendedGraphicsState = new PDExtendedGraphicsState();
			pdExtendedGraphicsState.setStrokingAlphaConstant((alpha / 255f));
			pdExtendedGraphicsState.setNonStrokingAlphaConstant((alpha / 255f));
			contentStream.setGraphicsStateParameters(pdExtendedGraphicsState);
		}
	}

	public void fill(Shape s) {
		try {
			contentStream.saveGraphicsState();
			PDShading shading = applyPaint();
			if (shading != null) {
				walkShape(s);
				contentStream.clip();
				contentStream.shadingFill(shading);
			} else {
				walkShape(s);
				contentStream.fill();
			}
			contentStream.restoreGraphicsState();
		} catch (IOException e) {
			throwIOException(e);
		}
	}

	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		return false;
	}

	public GraphicsConfiguration getDeviceConfiguration() {
		return null;
	}

	public void setComposite(Composite comp) {
		composite = comp;
	}

	public void setPaint(Paint paint) {
		this.paint = paint;
	}

	public void setStroke(Stroke stroke) {
		this.stroke = stroke;
	}

	private Map<RenderingHints.Key, Object> renderingHints = new HashMap<RenderingHints.Key, Object>();

	public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
		renderingHints.put(hintKey, hintValue);
	}

	public Object getRenderingHint(RenderingHints.Key hintKey) {
		return renderingHints.get(hintKey);
	}

	public void setRenderingHints(Map<?, ?> hints) {
		hints.clear();
		addRenderingHints(hints);
	}

	@SuppressWarnings("unchecked")
	public void addRenderingHints(Map<?, ?> hints) {
		renderingHints.putAll((Map<? extends RenderingHints.Key, ?>) hints);

	}

	public RenderingHints getRenderingHints() {
		return new RenderingHints(renderingHints);
	}

	public Graphics create() {
		try {
			return new PdfBoxGraphics2D(this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void translate(int x, int y) {
		transform.translate(x, y);
	}

	public Color getColor() {
		if (paint instanceof Color)
			return (Color) paint;
		return null;
	}

	public void setColor(Color color) {
		this.paint = color;
	}

	public void setPaintMode() {
		xorColor = null;
	}

	public void setXORMode(Color c1) {
		xorColor = c1;
	}

	public Font getFont() {
		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public FontMetrics getFontMetrics(Font f) {
		return calcGfx.getFontMetrics(f);
	}

	public Rectangle getClipBounds() {
		if (clipShape != null)
			return clipShape.getBounds();
		return null;
	}

	public void clipRect(int x, int y, int width, int height) {
		Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
		clip(rect);
	}

	public void setClip(int x, int y, int width, int height) {
		setClip(new Rectangle(x, y, width, height));
	}

	public Shape getClip() {
		return clipShape;
	}

	public void setClip(Shape clip) {
		clipShape = clip;
		/*
		 * Clip on the content stream
		 */
		try {
			contentStream.restoreGraphicsState();
			contentStream.saveGraphicsState();
			walkShape(clip);
			contentStream.clip();
		} catch (IOException e) {
			throwIOException(e);
		}
	}

	private void walkShape(Shape clip) throws IOException {
		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);
		PathIterator pi = clip.getPathIterator(tf);
		float[] coords = new float[6];
		while (!pi.isDone()) {
			switch (pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
				contentStream.moveTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_LINETO:
				contentStream.lineTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_QUADTO:
				contentStream.curveTo1(coords[0], coords[1], coords[2], coords[3]);
				break;
			case PathIterator.SEG_CUBICTO:
				contentStream.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
				break;
			case PathIterator.SEG_CLOSE:
				contentStream.closePath();
				break;
			}
			pi.next();
		}
	}

	private void throwIOException(IOException e) {
		throw new RuntimeException(e);
	}

	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		/*
		 * Sorry, cant do that :(
		 */
		throw new IllegalStateException("copyArea() not possible!");
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
		draw(new Line2D.Double(x1, y1, x2, y2));
	}

	public void fillRect(int x, int y, int width, int height) {
		fill(new Rectangle(x, y, width, height));
	}

	public void clearRect(int x, int y, int width, int height) {
		Paint p = paint;
		paint = backgroundColor;
		fillRect(x, y, width, height);
		paint = p;
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		draw(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight));
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		fill(new RoundRectangle2D.Double(x, y, width, height, arcWidth, arcHeight));
	}

	public void drawOval(int x, int y, int width, int height) {
		draw(new Ellipse2D.Double(x, y, width, height));
	}

	public void fillOval(int x, int y, int width, int height) {
		fill(new Ellipse2D.Double(x, y, width, height));
	}

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		draw(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN));
	}

	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		fill(new Arc2D.Double(x, y, width, height, startAngle, arcAngle, Arc2D.PIE));
	}

	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
		Path2D.Double path = new Path2D.Double();
		path.moveTo(xPoints[0], yPoints[0]);
		for (int i = 1; i < nPoints; i++)
			path.lineTo(xPoints[i], yPoints[i]);
		draw(path);
	}

	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		draw(new Polygon(xPoints, yPoints, nPoints));
	}

	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		fill(new Polygon(xPoints, yPoints, nPoints));
	}

	public void translate(double tx, double ty) {
		transform.translate(tx, ty);
	}

	public void rotate(double theta) {
		transform.rotate(theta);
	}

	public void rotate(double theta, double x, double y) {
		transform.rotate(theta, x, y);
	}

	public void scale(double sx, double sy) {
		transform.scale(sx, sy);
	}

	public void shear(double shx, double shy) {
		transform.shear(shx, shy);
	}

	public void transform(AffineTransform Tx) {
		transform.concatenate(Tx);
	}

	public void setTransform(AffineTransform Tx) {
		transform = new AffineTransform();
		transform.concatenate(Tx);
	}

	public AffineTransform getTransform() {
		return (AffineTransform) transform.clone();
	}

	public Paint getPaint() {
		return paint;
	}

	public Composite getComposite() {
		return composite;
	}

	public void setBackground(Color color) {
		backgroundColor = color;
	}

	public Color getBackground() {
		return backgroundColor;
	}

	public Stroke getStroke() {
		return stroke;
	}

	public void clip(Shape shape) {
		if (clipShape == null)
			setClip(shape);
		else {
			Area area = new Area(clipShape);
			area.intersect(new Area(shape));
			setClip(area);
		}
	}

	public FontRenderContext getFontRenderContext() {
		calcGfx.addRenderingHints(renderingHints);
		return calcGfx.getFontRenderContext();
	}
}
