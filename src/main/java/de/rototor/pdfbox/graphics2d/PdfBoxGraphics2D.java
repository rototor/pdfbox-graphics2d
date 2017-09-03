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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private final AffineTransform baseTransform;
	private AffineTransform transform = new AffineTransform();
	private IPdfBoxGraphics2DImageEncoder imageEncoder = new PdfBoxGraphics2DLosslessImageEncoder();
	private IPdfBoxGraphics2DColorMapper colorMapper = new PdfBoxGraphics2DColorMapper();
	private IPdfBoxGraphics2DPaintApplier paintApplier = new PdfBoxGraphics2DPaintApplier();
	private IPdfBoxGraphics2DFontTextDrawer fontTextDrawer = new PdfBoxGraphics2DFontTextDrawer();
	private Paint paint;
	private Stroke stroke;
	private Color xorColor;
	private Font font;
	private Composite composite;
	private Shape clipShape;
	private Color backgroundColor;
	private final CloneInfo cloneInfo;
	@SuppressWarnings("SpellCheckingInspection")
	private final PDRectangle bbox;

	/**
	 * Set a new color mapper.
	 * 
	 * @param colorMapper
	 *            the color mapper which maps Color to PDColor.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void setColorMapper(IPdfBoxGraphics2DColorMapper colorMapper) {
		this.colorMapper = colorMapper;
	}

	/**
	 * Set a new image encoder
	 * 
	 * @param imageEncoder
	 *            the image encoder, which encodes a image as PDImageXForm.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void setImageEncoder(IPdfBoxGraphics2DImageEncoder imageEncoder) {
		this.imageEncoder = imageEncoder;
	}

	/**
	 * Set a new paint applier. You should always derive your custom paint applier
	 * from the {@link PdfBoxGraphics2DPaintApplier} and just extend the paint
	 * mapping for custom paint.
	 * 
	 * If the paint you map is a paint from a standard library and you can implement
	 * the mapping using reflection please feel free to send a pull request to
	 * extend the default paint mapper.
	 * 
	 * @param paintApplier
	 *            the paint applier responsible for mapping the paint correctly
	 */
	@SuppressWarnings("unused")
	public void setPaintApplier(IPdfBoxGraphics2DPaintApplier paintApplier) {
		this.paintApplier = paintApplier;
	}

	/**
	 * Create a PDfBox Graphics2D. This size is used for the BBox of the XForm. So
	 * everything drawn outside the rectangle (0x0)-(pixelWidth,pixelHeight) will be
	 * clipped.
	 * 
	 * Note: pixelWidth and pixelHeight only define the size of the coordinate space
	 * within this Graphics2D. They do not affect how big the XForm is finally
	 * displayed in the PDF.
	 * 
	 * @param document
	 *            The document the graphics should be used to create a XForm in.
	 * @param pixelWidth
	 *            the width in pixel of the drawing area.
	 * @param pixelHeight
	 *            the height in pixel of the drawing area.
	 */
	@SuppressWarnings("WeakerAccess")
	public PdfBoxGraphics2D(PDDocument document, int pixelWidth, int pixelHeight) throws IOException {
		this(document, new PDRectangle(pixelWidth, pixelHeight));
	}

	/**
	 * Create a PDfBox Graphics2D. This size is used for the BBox of the XForm. So
	 * everything drawn outside the rectangle (0x0)-(pixelWidth,pixelHeight) will be
	 * clipped.
	 *
	 * Note: pixelWidth and pixelHeight only define the size of the coordinate space
	 * within this Graphics2D. They do not affect how big the XForm is finally
	 * displayed in the PDF.
	 *
	 * @param document
	 *            The document the graphics should be used to create a XForm in.
	 * @param pixelWidth
	 *            the width in pixel of the drawing area.
	 * @param pixelHeight
	 *            the height in pixel of the drawing area.
	 * @throws IOException
	 *             if something goes wrong with writing into the content stream of
	 *             the {@link PDDocument}.
	 */
	@SuppressWarnings("WeakerAccess")
	public PdfBoxGraphics2D(PDDocument document, float pixelWidth, float pixelHeight) throws IOException {
		this(document, new PDRectangle(pixelWidth, pixelHeight));
	}

	/**
	 * Set an optional text drawer. By default, all text is vectorized and drawn
	 * using vector shapes. To embed fonts into a PDF file it is necessary to have
	 * the underlying TTF file. The java.awt.Font class does not provide that. The
	 * FontTextDrawer must perform the java.awt.Font &lt;=&gt; PDFont mapping and
	 * also must perform the text layout. If it can not map the text or font
	 * correctly, the font drawing falls back to vectoring the text.
	 * 
	 * @param fontTextDrawer
	 *            The text drawer, which can draw text using fonts
	 */
	@SuppressWarnings("WeakerAccess")
	public void setFontTextDrawer(IPdfBoxGraphics2DFontTextDrawer fontTextDrawer) {
		this.fontTextDrawer = fontTextDrawer;
	}

	/**
	 * @param document
	 *            The document the graphics should be used to create a XForm in.
	 * @param bbox
	 *            Bounding Box of the graphics
	 * @throws IOException
	 *             when something goes wrong with writing into the content stream of
	 *             the {@link PDDocument}.
	 */
	public PdfBoxGraphics2D(PDDocument document, PDRectangle bbox) throws IOException {
		this.document = document;
		this.bbox = bbox;

		PDAppearanceStream appearance = new PDAppearanceStream(document);
		xFormObject = appearance;
		xFormObject.setResources(new PDResources());
		xFormObject.setBBox(bbox);
		contentStream = new PDPageContentStream(document, appearance, xFormObject.getStream().createOutputStream());
		contentStream.saveGraphicsState();

		baseTransform = new AffineTransform();
		baseTransform.translate(0, bbox.getHeight());
		baseTransform.scale(1, -1);

		calcImage = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR);
		calcGfx = calcImage.createGraphics();
		font = calcGfx.getFont();
		cloneInfo = null;
	}

	private final List<CloneInfo> cloneList = new ArrayList<CloneInfo>();

	private static class CloneInfo {
		PdfBoxGraphics2D sourceGfx;
		PdfBoxGraphics2D clone;

	}

	private PdfBoxGraphics2D(PdfBoxGraphics2D gfx) throws IOException {
		CloneInfo info = new CloneInfo();
		info.clone = this;
		info.sourceGfx = gfx;
		gfx.cloneList.add(info);
		this.cloneInfo = info;

		this.document = gfx.document;
		this.bbox = gfx.bbox;
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
		this.fontTextDrawer = gfx.fontTextDrawer;
		this.imageEncoder = gfx.imageEncoder;
		this.xorColor = gfx.xorColor;

		this.contentStream.saveGraphicsState();
	}

	/**
	 * 
	 * @return the PDAppearanceStream which resulted in this graphics
	 */
	@SuppressWarnings("WeakerAccess")
	public PDFormXObject getXFormObject() {
		if (document != null)
			throw new IllegalStateException("You can only get the XformObject after you disposed the Graphics2D!");
		if (cloneInfo != null)
			throw new IllegalStateException("You can not get the Xform stream from the clone");
		return xFormObject;
	}

	public void dispose() {
		if (cloneInfo != null) {
			cloneInfo.sourceGfx.cloneList.remove(cloneInfo);
			try {
				this.contentStream.restoreGraphicsState();
			} catch (IOException e) {
				throwException(e);
			}
			return;
		}
		if (cloneList.size() > 0)
			throw new RuntimeException("Not all PdfGraphics2D clones where destroyed!!!");
		try {
			contentStream.restoreGraphicsState();
			contentStream.close();
		} catch (IOException e) {
			throwException(e);
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
				if (basicStroke.getMiterLimit() > 0) {
					// Also Miter maps 1:1 between Java and PDF Spec
					// (NB: set the miter-limit only if value is > 0)
					contentStream.setMiterLimit(basicStroke.getMiterLimit());
				}

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
			throwException(e);
		}
	}

	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		AffineTransform tf = new AffineTransform();
		tf.concatenate(baseTransform);
		tf.concatenate(transform);
		tf.concatenate((AffineTransform) xform.clone());

		PDImageXObject pdImage = imageEncoder.encodeImage(document, contentStream, img);
		try {
			contentStream.saveGraphicsState();
			int imgHeight = img.getHeight(obs);
			tf.translate(0, imgHeight);
			tf.scale(1, -1);
			contentStream.transform(new Matrix(tf));

			Object keyInterpolation = renderingHints.get(RenderingHints.KEY_INTERPOLATION);
			if (RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR.equals(keyInterpolation))
				pdImage.setInterpolate(false);
			contentStream.drawImage(pdImage, 0, 0, img.getWidth(obs), imgHeight);
			contentStream.restoreGraphicsState();
		} catch (IOException e) {
			throwException(e);
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
		AttributedString attributedString = new AttributedString(str);
		attributedString.addAttribute(TextAttribute.FONT, font);
		drawString(attributedString.getIterator(), x, y);
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
				contentStream.setNonStrokingColor(colorMapper.mapColor(contentStream, bgcolor));
				walkShape(new Rectangle(x, y, width, height));
				contentStream.fill();
			}
			return drawImage(img, x, y, img.getWidth(observer), img.getHeight(observer), observer);
		} catch (IOException e) {
			throwException(e);
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
				contentStream.setNonStrokingColor(colorMapper.mapColor(contentStream, bgcolor));
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
			throwException(e);
			return false;
		}
	}

	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		try {
			/*
			 * If we can draw the text using fonts, we do this
			 */
			if (fontTextDrawer.canDrawText((AttributedCharacterIterator) iterator.clone(), fontDrawerEnv())) {
				drawStringUsingText(iterator, x, y);
			} else {
				/*
				 * Otherwise we fall back to draw using shapes. This works always
				 */
				drawStringUsingShapes(iterator, x, y);
			}
		} catch (IOException e) {
			throwException(e);
		} catch (FontFormatException e) {
			throwException(e);
		}
	}

	private void drawStringUsingShapes(AttributedCharacterIterator iterator, float x, float y) throws IOException {
		Stroke originalStroke = stroke;
		Paint originalPaint = paint;
		TextLayout textLayout = new TextLayout(iterator, getFontRenderContext());
		textLayout.draw(this, x, y);
		paint = originalPaint;
		stroke = originalStroke;
	}

	private void drawStringUsingText(AttributedCharacterIterator iterator, float x, float y)
			throws IOException, FontFormatException {
		contentStream.saveGraphicsState();
		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);
		tf.translate(x, y);
		contentStream.transform(new Matrix(tf));

		fontTextDrawer.drawText(iterator, fontDrawerEnv());

		contentStream.restoreGraphicsState();
	}

	private IPdfBoxGraphics2DFontTextDrawer.IFontTextDrawerEnv fontDrawerEnv() {
		return new IPdfBoxGraphics2DFontTextDrawer.IFontTextDrawerEnv() {
			@Override
			public PDDocument getDocument() {
				return document;
			}

			@Override
			public PDPageContentStream getContentStream() {
				return contentStream;
			}

			@Override
			public Font getFont() {
				return font;
			}

			@Override
			public Paint getPaint() {
				return paint;
			}

			@Override
			public PDShading applyPaint(Paint paint) throws IOException {
				return PdfBoxGraphics2D.this.applyPaint(paint);
			}

			@Override
			public FontRenderContext getFontRenderContext() {
				return PdfBoxGraphics2D.this.getFontRenderContext();
			}
		};
	}

	public void drawGlyphVector(GlyphVector g, float x, float y) {
		AffineTransform transformOrig = (AffineTransform) transform.clone();
		transform.translate(x, y);
		fill(g.getOutline());
		transform = transformOrig;
	}

	public void fill(Shape s) {
		try {
			contentStream.saveGraphicsState();

			PDShading shading = applyPaint();
			walkShape(s);
			if (shading != null) {
				// NB: the shading fill doesn't work with shapes with zero or
				// negative dimensions
				// (width and/or height): in these cases a normal fill is used
				Rectangle2D r2d = s.getBounds2D();
				if ((r2d.getWidth() <= 0) || (r2d.getHeight() <= 0)) {
					contentStream.fill();
				} else {
					contentStream.clip();
					contentStream.shadingFill(shading);
				}
			} else {
				contentStream.fill();
			}
			contentStream.restoreGraphicsState();
		} catch (IOException e) {
			throwException(e);
		}
	}

	private PDShading applyPaint() throws IOException {
		return applyPaint(paint);
	}

	private PDShading applyPaint(Paint paintToApply) throws IOException {
		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);
		return paintApplier.applyPaint(paintToApply, contentStream, tf, new IPdfBoxGraphics2DPaintApplier.IPaintEnv() {
			@Override
			public IPdfBoxGraphics2DColorMapper getColorMapper() {
				return colorMapper;
			}

			@Override
			public IPdfBoxGraphics2DImageEncoder getImageEncoder() {
				return imageEncoder;
			}

			@Override
			public PDDocument getDocument() {
				return document;
			}

			@Override
			public PDResources getResources() {
				return xFormObject.getResources();
			}

			@Override
			public Composite getComposite() {
				return PdfBoxGraphics2D.this.getComposite();
			}
		});
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
		Shape clip = getClip();
		if (clip != null)
			return clip.getBounds();
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
		try {
			return transform.createInverse().createTransformedShape(clipShape);
		} catch (NoninvertibleTransformException e) {
			return null;
		}
	}

	public void setClip(Shape clip) {
		checkNoCloneActive();
		this.clipShape = transform.createTransformedShape(clip);
		/*
		 * Clip on the content stream
		 */
		try {
			contentStream.restoreGraphicsState();
			contentStream.saveGraphicsState();
			/*
			 * clip can be null, only set a clipping if not null
			 */
			if (clip != null) {
				walkShape(clip);
				contentStream.clip();
			}
		} catch (IOException e) {
			throwException(e);
		}
	}

	private void walkShape(Shape clip) throws IOException {
		checkNoCloneActive();

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

	private void checkNoCloneActive() {
		/*
		 * As long as a clone is in use you are not allowed to do anything here
		 */
		if (cloneList.size() > 0)
			throw new IllegalStateException("Don't use the main context as long as a clone is active!");
	}

	private void throwException(Exception e) {
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
		checkNoCloneActive();
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
		Shape clip = getClip();
		if (clip == null)
			setClip(shape);
		else {
			Area area = new Area(clip);
			area.intersect(new Area(shape));
			setClip(area);
		}
	}

	public FontRenderContext getFontRenderContext() {
		calcGfx.addRenderingHints(renderingHints);
		return calcGfx.getFontRenderContext();
	}

}
