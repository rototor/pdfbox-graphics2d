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

import de.rototor.pdfbox.graphics2d.IPdfBoxGraphics2DDrawControl.IDrawControlEnv;
import de.rototor.pdfbox.graphics2d.IPdfBoxGraphics2DFontTextDrawer.IFontTextDrawerEnv;
import de.rototor.pdfbox.graphics2d.IPdfBoxGraphics2DPaintApplier.IPaintEnv;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.RenderingHints.Key;
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
	private IPdfBoxGraphics2DDrawControl drawControl = PdfBoxGraphics2DDrawControlDefault.INSTANCE;
	private Paint paint;
	private Stroke stroke;
	private Color xorColor;
	private Font font;
	private Composite composite;
	private Shape clipShape;
	private Color backgroundColor;
	private final CopyInfo copyInfo;
	private final PDRectangle bbox;

	/**
	 * Set a new color mapper.
	 *
	 * @param colorMapper
	 *            the color mapper which maps Color to PDColor.
	 */
	@SuppressWarnings({ "unused" })
	public void setColorMapper(IPdfBoxGraphics2DColorMapper colorMapper) {
		this.colorMapper = colorMapper;
	}

	/**
	 * Set a new image encoder
	 *
	 * @param imageEncoder
	 *            the image encoder, which encodes a image as PDImageXForm.
	 */
	@SuppressWarnings({ "unused" })
	public void setImageEncoder(IPdfBoxGraphics2DImageEncoder imageEncoder) {
		this.imageEncoder = imageEncoder;
	}

	/**
	 * Set a new paint applier. You should always derive your custom paint applier
	 * from the {@link IPdfBoxGraphics2DPaintApplier} and just extend the paint
	 * mapping for custom paint.
	 * <p>
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
	 * Set a new draw control. This allows you to influence fill() and draw()
	 * operations. drawString() is only influence if the text is drawn as vector
	 * shape.
	 *
	 * @param drawControl
	 *            the draw control
	 */
	@SuppressWarnings({ "unused", "WeakerAccess" })
	public void setDrawControl(IPdfBoxGraphics2DDrawControl drawControl) {
		this.drawControl = drawControl;
	}

	/**
	 * Create a PDfBox Graphics2D. This size is used for the BBox of the XForm. So
	 * everything drawn outside the rectangle (0x0)-(pixelWidth,pixelHeight) will be
	 * clipped.
	 * <p>
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
	public PdfBoxGraphics2D(PDDocument document, int pixelWidth, int pixelHeight) throws IOException {
		this(document, new PDRectangle(pixelWidth, pixelHeight));
	}

	/**
	 * Create a PDfBox Graphics2D. This size is used for the BBox of the XForm. So
	 * everything drawn outside the rectangle (0x0)-(pixelWidth,pixelHeight) will be
	 * clipped.
	 * <p>
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

	private int saveCounter = 0;

	private final List<CopyInfo> copyList = new ArrayList<CopyInfo>();

	private static class CopyInfo {
		PdfBoxGraphics2D sourceGfx;
		PdfBoxGraphics2D copy;

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
		this(document, bbox, null);
	}

	/*
	 * @internal
	 */
	PdfBoxGraphics2D(PDDocument document, PDRectangle bbox, PdfBoxGraphics2D parentGfx) throws IOException {
		this.document = document;
		this.bbox = bbox;

		PDAppearanceStream appearance = new PDAppearanceStream(document);
		xFormObject = appearance;
		xFormObject.setResources(new PDResources());
		xFormObject.setBBox(bbox);
		contentStream = new PDPageContentStream(document, appearance,
				xFormObject.getStream().createOutputStream(COSName.FLATE_DECODE));
		contentStreamSaveState();

		if (parentGfx != null) {
			this.colorMapper = parentGfx.colorMapper;
			this.fontTextDrawer = parentGfx.fontTextDrawer;
			this.imageEncoder = parentGfx.imageEncoder;
			this.paintApplier = parentGfx.paintApplier;
		}

		baseTransform = new AffineTransform();
		baseTransform.translate(0, bbox.getHeight());
		baseTransform.scale(1, -1);

		calcImage = new BufferedImage(100, 100, BufferedImage.TYPE_4BYTE_ABGR);
		calcGfx = calcImage.createGraphics();
		font = calcGfx.getFont();
		copyInfo = null;

	}

	/**
	 * @return the PDAppearanceStream which resulted in this graphics
	 */
	@SuppressWarnings("WeakerAccess")
	public PDFormXObject getXFormObject() {
		if (document != null)
			throw new IllegalStateException("You can only get the XformObject after you disposed the Graphics2D!");
		if (copyInfo != null)
			throw new IllegalStateException("You can not get the Xform stream from the copy");
		return xFormObject;
	}

	private PdfBoxGraphics2D(PdfBoxGraphics2D gfx) throws IOException {
		CopyInfo info = new CopyInfo();
		info.copy = this;
		info.sourceGfx = gfx;
		gfx.copyList.add(info);
		this.copyInfo = info;

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
		this.paintApplier = gfx.paintApplier;
		this.drawControl = gfx.drawControl;
		this.composite = gfx.composite;
		this.renderingHints = new HashMap<Key, Object>(gfx.renderingHints);
		this.xorColor = gfx.xorColor;
		this.saveCounter = 0;

		contentStreamSaveState();
	}

	/**
	 * Sometimes the users of {@link #create()} don't correctly {@link #dispose()}
	 * the child graphics they create. And you may not always be able to fix this
	 * uses, as it may be in some 3rdparty library. In this case this method can
	 * help you. It will cleanup all dangling child graphics. The child graphics can
	 * not be used after that. This method is a workaround for a buggy old code. You
	 * should only use it if you have to. <br>
	 * <p>
	 * Note: You can only call this method on the "main" graphics, not on a child
	 * created with {@link #create()}
	 */
	@SuppressWarnings("WeakerAccess")
	public void disposeDanglingChildGraphics() {
		if (copyInfo != null)
			throw new IllegalStateException("Don't call disposeDanglingChildGraphics() on a child!");
		disposeCopies(copyList);
	}

	private static void disposeCopies(List<CopyInfo> cl) {
		while (cl.size() > 0) {
			CopyInfo copyInfo = cl.get(0);
			disposeCopies(copyInfo.copy.copyList);
			copyInfo.copy.dispose();
		}
	}

	public void dispose() {
		if (copyInfo != null) {
			copyInfo.sourceGfx.copyList.remove(copyInfo);
			try {
				contentStreamRestoreState();
			} catch (IOException e) {
				throwException(e);
			}
			if (this.saveCounter != 0)
				throw new IllegalStateException("Copy - SaveCounter should be 0, but is " + this.saveCounter);
			return;
		}
		if (copyList.size() > 0)
			/*
			 * When not all copies created by create() are disposed(), the resulting PDF
			 * content stream will be invalid, as the save/restore context commands (q/Q)
			 * are not balanced. You should always dispose() a graphics context when you are
			 * done with it.
			 */
			throw new IllegalStateException(
					"Not all PdfGraphics2D copies were destroyed! Please ensure that all create() calls get a matching dispose() on the returned copies. Also consider using disposeDanglingChildGraphics()");
		try {
			contentStreamRestoreState();
			contentStream.close();
		} catch (IOException e) {
			throwException(e);
		}
		if (this.saveCounter != 0)
			throw new IllegalStateException("SaveCounter should be 0, but is " + this.saveCounter);

		document = null;
		calcGfx.dispose();
		calcImage.flush();
		calcImage = null;
	}

	private final IDrawControlEnv drawControlEnv = new IDrawControlEnv() {
		@Override
		public Paint getPaint() {
			return paint;
		}

		@Override
		public PdfBoxGraphics2D getGraphics() {
			return PdfBoxGraphics2D.this;
		}
	};

	public void draw(Shape s) {
		checkNoCopyActive();
		/*
		 * Don't try to draw with no paint, just ignore that.
		 */
		if (paint == null)
			return;
		try {
			contentStreamSaveState();

			Shape shapeToDraw = drawControl.transformShapeBeforeDraw(s, drawControlEnv);

			if (shapeToDraw != null) {
				walkShape(shapeToDraw);
				PDShading pdShading = applyPaint(shapeToDraw);
				if (pdShading != null)
					applyShadingAsColor(pdShading);

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
						contentStream.setLineDashPattern(dashArray,
								(float) Math.abs(basicStroke.getDashPhase() * scaleX));
					}
				}

				contentStream.stroke();
			}

			drawControl.afterShapeDraw(s, drawControlEnv);

			contentStreamRestoreState();
		} catch (IOException e) {
			throwException(e);
		}
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

	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		checkNoCopyActive();
		AffineTransform tf = new AffineTransform();
		tf.concatenate(baseTransform);
		tf.concatenate(transform);

		// Sometimes the xform can be null
		if (xform != null)
			tf.concatenate((AffineTransform) xform.clone());

		PDImageXObject pdImage = imageEncoder.encodeImage(document, contentStream, img);
		try {
			contentStreamSaveState();
			int imgHeight = img.getHeight(obs);
			tf.translate(0, imgHeight);
			tf.scale(1, -1);
			contentStream.transform(new Matrix(tf));

			Object keyInterpolation = renderingHints.get(RenderingHints.KEY_INTERPOLATION);
			if (RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR.equals(keyInterpolation))
				pdImage.setInterpolate(false);
			contentStream.drawImage(pdImage, 0, 0, img.getWidth(obs), imgHeight);
			contentStreamRestoreState();
		} catch (IOException e) {
			throwException(e);
		}
		return true;
	}

	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
			Color bgcolor, ImageObserver observer) {
		try {
			contentStreamSaveState();
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
			contentStreamRestoreState();
			return true;
		} catch (IOException e) {
			throwException(e);
			return false;
		}
	}

	private void drawStringUsingShapes(AttributedCharacterIterator iterator, float x, float y) {
		Stroke originalStroke = stroke;
		Paint originalPaint = paint;
		TextLayout textLayout = new TextLayout(iterator, getFontRenderContext());
		textLayout.draw(this, x, y);
		paint = originalPaint;
		stroke = originalStroke;
	}

	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		/*
		 * Don't try to draw with no paint, just ignore that.
		 */
		if (paint == null)
			return;

		try {
			contentStreamSaveState();
			/*
			 * If we can draw the text using fonts, we do this
			 */
			if (fontTextDrawer.canDrawText((AttributedCharacterIterator) iterator.clone(), fontDrawerEnv)) {
				drawStringUsingText(iterator, x, y);
			} else {
				/*
				 * Otherwise we fall back to draw using shapes. This works always
				 */
				drawStringUsingShapes(iterator, x, y);
			}
			contentStreamRestoreState();
		} catch (IOException e) {
			throwException(e);
		} catch (FontFormatException e) {
			throwException(e);
		}
	}

	private void drawStringUsingText(AttributedCharacterIterator iterator, float x, float y)
			throws IOException, FontFormatException {
		contentStreamSaveState();

		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);
		tf.translate(x, y);
		contentStream.transform(new Matrix(tf));

		fontTextDrawer.drawText(iterator, fontDrawerEnv);

		contentStreamRestoreState();
	}

	private void contentStreamSaveState() throws IOException {
		saveCounter++;
		contentStream.saveGraphicsState();
	}

	private void contentStreamRestoreState() throws IOException {
		if (saveCounter == 0)
			throw new IllegalStateException("Internal save/restore state error. Should never happen.");
		saveCounter--;
		contentStream.restoreGraphicsState();
	}

	private final IFontTextDrawerEnv fontDrawerEnv = new IPdfBoxGraphics2DFontTextDrawer.IFontTextDrawerEnv() {
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
		public void applyPaint(Paint paint, Shape shapeToDraw) throws IOException {
			PDShading pdShading = PdfBoxGraphics2D.this.applyPaint(paint, shapeToDraw);
			if (pdShading != null)
				applyShadingAsColor(pdShading);
		}

		@Override
		public FontRenderContext getFontRenderContext() {
			return PdfBoxGraphics2D.this.getFontRenderContext();
		}

		@Override
		public PDRectangle getGraphicsBBox() {
			return bbox;
		}

		@Override
		public PDResources getResources() {
			return xFormObject.getResources();
		}
	};

	public void drawGlyphVector(GlyphVector g, float x, float y) {
		checkNoCopyActive();
		AffineTransform transformOrig = (AffineTransform) transform.clone();
		transform.translate(x, y);
		fill(g.getOutline());
		transform = transformOrig;
	}

	public void fill(Shape s) {
		checkNoCopyActive();

		/*
		 * Don't try to draw with no paint, just ignore that.
		 */
		if (paint == null)
			return;

		try {
			contentStreamSaveState();

			Shape shapeToFill = drawControl.transformShapeBeforeFill(s, drawControlEnv);

			if (shapeToFill != null) {
				boolean useEvenOdd = walkShape(shapeToFill);
				PDShading shading = applyPaint(shapeToFill);
				if (shading != null) {
					/*
					 * NB: the shading fill doesn't work with shapes with zero or negative
					 * dimensions (width and/or height): in these cases a normal fill is used
					 */
					Rectangle2D r2d = s.getBounds2D();
					if ((r2d.getWidth() <= 0) || (r2d.getHeight() <= 0)) {
						/*
						 * But we apply the shading as color, we usually want to avoid that because it
						 * creates another nested XForm for that ...
						 */
						applyShadingAsColor(shading);
						fill(useEvenOdd);
					} else {
						clip(useEvenOdd);
						contentStream.shadingFill(shading);
					}
				} else {
					fill(useEvenOdd);
				}
			}

			drawControl.afterShapeFill(s, drawControlEnv);

			contentStreamRestoreState();
		} catch (IOException e) {
			throwException(e);
		}
	}

	private void fill(boolean useEvenOdd) throws IOException {
		if (useEvenOdd)
			contentStream.fillEvenOdd();
		else
			contentStream.fill();
	}

	private void applyShadingAsColor(PDShading shading) throws IOException {
		/*
		 * If the paint has a shading we must create a tiling pattern and set that as
		 * stroke color...
		 */
		PDTilingPattern pattern = new PDTilingPattern();
		pattern.setPaintType(PDTilingPattern.PAINT_COLORED);
		pattern.setTilingType(PDTilingPattern.TILING_CONSTANT_SPACING_FASTER_TILING);
		PDRectangle anchorRect = bbox;
		pattern.setBBox(anchorRect);
		pattern.setXStep(anchorRect.getWidth());
		pattern.setYStep(anchorRect.getHeight());

		PDAppearanceStream appearance = new PDAppearanceStream(this.document);
		appearance.setResources(pattern.getResources());
		appearance.setBBox(pattern.getBBox());

		PDPageContentStream imageContentStream = new PDPageContentStream(document, appearance,
				((COSStream) pattern.getCOSObject()).createOutputStream());
		imageContentStream.addRect(0, 0, anchorRect.getWidth(), anchorRect.getHeight());
		imageContentStream.clip();
		imageContentStream.shadingFill(shading);
		imageContentStream.close();

		PDColorSpace patternCS1 = new PDPattern(null);
		COSName tilingPatternName = xFormObject.getResources().add(pattern);
		PDColor patternColor = new PDColor(tilingPatternName, patternCS1);

		contentStream.setNonStrokingColor(patternColor);
		contentStream.setStrokingColor(patternColor);
	}

	private PDShading applyPaint(Shape shapeToDraw) throws IOException {
		return applyPaint(paint, shapeToDraw);
	}

	private final PaintEnvImpl paintEnv = new PaintEnvImpl();

	private PDShading applyPaint(Paint paintToApply, Shape shapeToDraw) throws IOException {
		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);
		paintEnv.shapeToDraw = shapeToDraw;
		return paintApplier.applyPaint(paintToApply, contentStream, tf, paintEnv);
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

	/**
	 * Creates a copy of this graphics object. Please call {@link #dispose()} always
	 * on the copy after you have finished drawing with it. <br>
	 * <br>
	 * Never draw both in this copy and its parent graphics at the same time, as
	 * they all write to the same content stream. This will create a broken PDF
	 * content stream. You should get an {@link java.lang.IllegalStateException} if
	 * you do so, but better just don't try. <br>
	 * <br>
	 * The copy allows you to have different transforms, paints, etc. than the
	 * parent graphics context without affecting the parent. You may also call
	 * create() on a copy, but always remember to call {@link #dispose()} in reverse
	 * order.
	 *
	 * @return a copy of this Graphics.
	 */
	public PdfBoxGraphics2D create() {
		try {
			return new PdfBoxGraphics2D(this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public PdfBoxGraphics2D create(int x, int y, int width, int height) {
		return (PdfBoxGraphics2D) super.create(x, y, width, height);
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

	/**
	 * XOR Mode is currently not implemented as it's not possible in PDF. This mode
	 * is ignored.
	 * 
	 * @param c1
	 *            the XORMode Color
	 */
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
		checkNoCopyActive();
		this.clipShape = transform.createTransformedShape(clip);
		/*
		 * Clip on the content stream
		 */
		try {
			contentStreamRestoreState();
			contentStreamSaveState();
			/*
			 * clip can be null, only set a clipping if not null
			 */
			if (clip != null) {
				clip(walkShape(clip));
			}
		} catch (IOException e) {
			throwException(e);
		}
	}

	private void clip(boolean useEvenOdd) throws IOException {
		if (useEvenOdd)
			contentStream.clipEvenOdd();
		else
			contentStream.clip();
	}

	/**
	 * Float#isFinite() is JDK 8+. We just copied the trivial implementation here.
	 * When we require JDK 8+ we can just drop this method and replace it bei
	 * Float#isFinite()
	 */
	private static boolean isFinite(float f) {
		return Math.abs(f) <= Float.MAX_VALUE;
	}

	/**
	 * @return true when all required values are finite
	 */
	private static boolean isFinite(float[] coords, int count) {
		for (int i = 0; i < count; i++)
			if (!isFinite(coords[i]))
				return false;
		return true;
	}

	/**
	 * Walk the path and return true if we need to use the even odd winding rule.
	 * 
	 * @return true if we need to use the even odd winding rule
	 */
	private boolean walkShape(Shape clip) throws IOException {
		checkNoCopyActive();

		AffineTransform tf = new AffineTransform(baseTransform);
		tf.concatenate(transform);
		PathIterator pi = clip.getPathIterator(tf);
		float[] coords = new float[6];
		while (!pi.isDone()) {
			int segment = pi.currentSegment(coords);
			switch (segment) {
			case PathIterator.SEG_MOVETO:
				if (isFinite(coords, 2))
					contentStream.moveTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_LINETO:
				if (isFinite(coords, 2))
					contentStream.lineTo(coords[0], coords[1]);
				break;
			case PathIterator.SEG_QUADTO:
				if (isFinite(coords, 4))
					contentStream.curveTo1(coords[0], coords[1], coords[2], coords[3]);
				break;
			case PathIterator.SEG_CUBICTO:
				if (isFinite(coords, 6))
					contentStream.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
				break;
			case PathIterator.SEG_CLOSE:
				contentStream.closePath();
				break;
			}
			pi.next();
		}
		return pi.getWindingRule() == PathIterator.WIND_EVEN_ODD;
	}

	private void checkNoCopyActive() {
		/*
		 * As long as a copy is in use you are not allowed to do anything here
		 */
		if (copyList.size() > 0)
			throw new IllegalStateException("Don't use the main context as long as a copy is active!");
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
		checkNoCopyActive();
		transform.translate(tx, ty);
	}

	public void rotate(double theta) {
		checkNoCopyActive();
		transform.rotate(theta);
	}

	public void rotate(double theta, double x, double y) {
		checkNoCopyActive();
		transform.rotate(theta, x, y);
	}

	public void scale(double sx, double sy) {
		checkNoCopyActive();
		transform.scale(sx, sy);
	}

	public void shear(double shx, double shy) {
		checkNoCopyActive();
		transform.shear(shx, shy);
	}

	public void transform(AffineTransform Tx) {
		checkNoCopyActive();
		transform.concatenate(Tx);
	}

	public void setTransform(AffineTransform Tx) {
		checkNoCopyActive();
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

	private class PaintEnvImpl implements IPaintEnv {
		public Shape shapeToDraw;

		@Override
		public Shape getShapeToDraw() {
			return shapeToDraw;
		}

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

		@Override
		public PdfBoxGraphics2D getGraphics2D() {
			return PdfBoxGraphics2D.this;
		}

		@Override
		public Color getXORMode() {
			return xorColor;
		}
	}
}
