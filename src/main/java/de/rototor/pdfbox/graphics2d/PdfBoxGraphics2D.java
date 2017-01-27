package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;

/**
 * Graphics 2D Adapter for PDFBox.
 */
public class PdfBoxGraphics2D extends Graphics2D {
	private final PDAppearanceStream appearanceStream;
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
	private Color color;
	private Color xorColor;
	private Font font;
	private Composite composite;
	private Shape clipShape;
	private Color backgroundColor;

	/**
	 * Create a PDfBox Graphics2D. When this is disposed
	 */
	public PdfBoxGraphics2D(PDDocument document, int pixelWidth, int pixelHeight) throws IOException {
		this.document = document;
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;

		appearanceStream = new PDAppearanceStream(document);
		contentStream = new PDPageContentStream(document, appearanceStream);
		contentStream.saveGraphicsState();

		baseTransform = new AffineTransform();
		baseTransform.translate(0, -pixelHeight);

		calcImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		calcGfx = calcImage.createGraphics();
		font = calcGfx.getFont();
	}

	/**
	 * 
	 * @return the PDAppearanceStream which resulted in this graphics
	 */
	public PDAppearanceStream getAppearanceStream() {
		if (document != null)
			throw new IllegalStateException("You can only get the appearanceStream after you disposed the Graphics2D!");
		return appearanceStream;
	}

	public void dispose() {
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
			contentStream.setStrokingColor(colorMapper.mapColor(document, color));
			walkShape(s);
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
			contentStream.transform(new Matrix(tf));
			contentStream.drawImage(pdImage, 0, 0, img.getWidth(obs), img.getHeight(obs));
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

	}

	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {

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
			contentStream.saveGraphicsState();
			AffineTransform tf = new AffineTransform(baseTransform);
			tf.concatenate(transform);
			tf.translate(x, y);
			contentStream.transform(new Matrix(tf));
			contentStream.setStrokingColor(colorMapper.mapColor(document, color));

			contentStream.beginText();
			fontApplyer.applyFont(font, document, contentStream);
			calcGfx.setFont(font);
			boolean run = true;
			while (run) {
				StringBuilder sb = new StringBuilder();
				int charCount = iterator.getRunLimit() - iterator.getRunStart();
				Font attributeFont = (Font) iterator.getAttribute(TextAttribute.FONT);
				Number fontSize = ((Number) iterator.getAttribute(TextAttribute.SIZE));
				if (attributeFont != null) {
					if (fontSize != null)
						attributeFont = attributeFont.deriveFont(fontSize.floatValue());
					fontApplyer.applyFont(attributeFont, document, contentStream);
				}

				while (charCount-- > 0) {
					char c = iterator.next();
					if (c == AttributedCharacterIterator.DONE) {
						run = false;
						break;
					} else {
						sb.append(c);
					}
				}
				contentStream.showText(sb.toString());
				sb.setLength(0);
			}
			contentStream.endText();
			contentStream.restoreGraphicsState();
		} catch (IOException e) {
			throwIOException(e);
		}
	}

	public void drawGlyphVector(GlyphVector g, float x, float y) {
		throw new IllegalStateException("Not implemeted yet");
	}

	public void fill(Shape s) {
		try {
			contentStream.setNonStrokingColor(colorMapper.mapColor(document, color));
			walkShape(s);
			contentStream.fill();
		} catch (IOException e) {
			throwIOException(e);
		}
	}

	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		return false;
	}

	public GraphicsConfiguration getDeviceConfiguration() {
		return new GraphicsConfiguration() {
			@Override
			public GraphicsDevice getDevice() {
				return null;
			}

			@Override
			public ColorModel getColorModel() {
				return null;
			}

			@Override
			public ColorModel getColorModel(int transparency) {
				return null;
			}

			@Override
			public AffineTransform getDefaultTransform() {
				return transform;
			}

			@Override
			public AffineTransform getNormalizingTransform() {
				return transform;
			}

			@Override
			public Rectangle getBounds() {
				return new Rectangle(0, 0, pixelWidth, pixelHeight);
			}
		};
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
		throw new IllegalStateException("Not implemeted");
	}

	public void translate(int x, int y) {
		transform.translate(x, y);
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
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
		Color c = color;
		color = backgroundColor;
		fillRect(x, y, width, height);
		color = c;
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
