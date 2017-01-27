package de.rototor.pdfbox.graphics2d;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.shape.Polyline;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;

/**
 * Graphics 2D Adapter for PDFBox.
 */
public class PdfBoxGraphics2D extends Graphics2D {
	private final PDAppearanceStream stream;
	private final Graphics2D calcGfx;
	private BufferedImage calcImage;
	private PDDocument document;
	private final int pixelWidth;
	private final int pixelHeight;
	private AffineTransform transform = new AffineTransform();
	private IPdfBoxGraphics2DImageEncoder imageEncoder = new PdfBoxGraphics2DLosslessImageEncoder();
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
	public PdfBoxGraphics2D(PDDocument document, int pixelWidth, int pixelHeight) {
		stream = new PDAppearanceStream(document);
		this.document = document;
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;
		calcImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		calcGfx = calcImage.createGraphics()
	}

	/**
	 * 
	 * @return the PDAppearanceStream which resulted in this graphics
	 */
	public PDAppearanceStream getAppearanceStream() {
		if (document != null)
			throw new IllegalStateException("You can only get the stream after you disposed the Graphics2D!");
		return stream;
	}

	public void dispose() {
		document = null;
		calcGfx.dispose();
		calcImage.flush();
		calcImage = null;
	}

	public void draw(Shape s) {

	}

	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		AffineTransform tf = (AffineTransform) xform.clone();
		tf.concatenate(transform);
		imageEncoder.encodeImage(document, img);
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
		return false;
	}

	public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
		return false;
	}

	public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
		return false;
	}

	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
			ImageObserver observer) {
		return false;
	}

	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
			Color bgcolor, ImageObserver observer) {
		return false;
	}

	public void drawString(AttributedCharacterIterator iterator, float x, float y) {

	}

	public void drawGlyphVector(GlyphVector g, float x, float y) {

	}

	public void fill(Shape s) {

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
		return this;
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
		clipShape = new Rectangle(x, y, width, height);
	}

	public Shape getClip() {
		return clipShape;
	}

	public void setClip(Shape clip) {
		clipShape = clip;
	}

	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		/*
		 * Sorry, cant do that :(
		 */
	}

	public void drawLine(int x1, int y1, int x2, int y2) {

	}

	public void fillRect(int x, int y, int width, int height) {
		fill(new Rectangle(x, y, width, height));
	}

	public void clearRect(int x, int y, int width, int height) {

	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

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
		/* TODO */
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
		transform = Tx;
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
			clipShape = shape;
		else {
			Area area = new Area(clipShape);
			area.intersect(new Area(shape));
		}
	}

	public FontRenderContext getFontRenderContext() {
		calcGfx.addRenderingHints(renderingHints);
		return calcGfx.getFontRenderContext();
	}
}
