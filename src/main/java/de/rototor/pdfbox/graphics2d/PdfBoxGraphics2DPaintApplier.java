package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.multipdf.PDFCloneUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.shading.ShadingPaint;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default paint mapper.
 *
 * NOTE: Objects of this class are stateful and *not* thread safe!
 */
public class PdfBoxGraphics2DPaintApplier implements IPdfBoxGraphics2DPaintApplier {
	@SuppressWarnings("WeakerAccess")
	protected class PaintApplierState {
		protected PDDocument document;
		protected PDPageContentStream contentStream;
		@SuppressWarnings("WeakerAccess")
		protected IPdfBoxGraphics2DColorMapper colorMapper;
		@SuppressWarnings("WeakerAccess")
		protected IPdfBoxGraphics2DImageEncoder imageEncoder;
		@SuppressWarnings("WeakerAccess")
		protected PDResources resources;
		@SuppressWarnings("WeakerAccess")
		protected PDExtendedGraphicsState pdExtendedGraphicsState;
		@SuppressWarnings("WeakerAccess")
		protected Composite composite;
		private COSDictionary dictExtendedState;
		private IPaintEnv env;
		public AffineTransform tf;

		private void ensureExtendedState() {
			if (pdExtendedGraphicsState == null) {
				this.dictExtendedState = new COSDictionary();
				this.dictExtendedState.setItem(COSName.TYPE, COSName.EXT_G_STATE);
				pdExtendedGraphicsState = new PDExtendedGraphicsState(this.dictExtendedState);
			}
		}
	}

	private ExtGStateCache extGStateCache = new ExtGStateCache();
	private PDShadingCache shadingCache = new PDShadingCache();

	@Override
	public PDShading applyPaint(Paint paint, PDPageContentStream contentStream, AffineTransform tf, IPaintEnv env)
			throws IOException {
		PaintApplierState state = new PaintApplierState();
		state.document = env.getDocument();
		state.resources = env.getResources();
		state.contentStream = contentStream;
		state.colorMapper = env.getColorMapper();
		state.imageEncoder = env.getImageEncoder();
		state.composite = env.getComposite();
		state.pdExtendedGraphicsState = null;
		state.env = env;
		state.tf = tf;
		PDShading shading = applyPaint(paint, state);
		if (state.pdExtendedGraphicsState != null)
			contentStream.setGraphicsStateParameters(extGStateCache.makeUnqiue(state.pdExtendedGraphicsState));
		return shading;
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyAsStrokingColor(Color color, PaintApplierState state) throws IOException {
		PDPageContentStream contentStream = state.contentStream;
		IPdfBoxGraphics2DColorMapper colorMapper = state.colorMapper;
		contentStream.setStrokingColor(colorMapper.mapColor(contentStream, color));
		contentStream.setNonStrokingColor(colorMapper.mapColor(contentStream, color));

		int alpha = color.getAlpha();
		if (alpha < 255) {
			/*
			 * This is semitransparent
			 */
			state.ensureExtendedState();
			Float strokingAlphaConstant = state.pdExtendedGraphicsState.getStrokingAlphaConstant();
			if (strokingAlphaConstant == null)
				strokingAlphaConstant = 1f;
			state.pdExtendedGraphicsState.setStrokingAlphaConstant(strokingAlphaConstant * (alpha / 255f));
			Float nonStrokingAlphaConstant = state.pdExtendedGraphicsState.getNonStrokingAlphaConstant();
			if (nonStrokingAlphaConstant == null)
				nonStrokingAlphaConstant = 1f;
			state.pdExtendedGraphicsState.setNonStrokingAlphaConstant(nonStrokingAlphaConstant * (alpha / 255f));
		}
	}

	private PDShading applyPaint(Paint paint, PaintApplierState state) throws IOException {
		applyComposite(state);

		/*
		 * We can not apply not existing paints
		 */
		if (paint == null)
			return null;

		String simpleName = paint.getClass().getSimpleName();
		if (paint instanceof Color) {
			applyAsStrokingColor((Color) paint, state);
		} else if (simpleName.equals("LinearGradientPaint")) {
			return shadingCache.makeUnqiue(buildLinearGradientShading(paint, state));
		} else if (simpleName.equals("RadialGradientPaint")) {
			return shadingCache.makeUnqiue(buildRadialGradientShading(paint, state));
		} else if (simpleName.equals("PatternPaint")) {
			applyPatternPaint(paint, state);
		} else if (paint instanceof GradientPaint) {
			return shadingCache.makeUnqiue(buildGradientShading((GradientPaint) paint, state));
		} else if (paint instanceof TexturePaint) {
			applyTexturePaint((TexturePaint) paint, state);
		} else if (paint instanceof ShadingPaint) {
			// PDFBox paint, we can import the shading directly
			return shadingCache.makeUnqiue(importPDFBoxShadingPaint((ShadingPaint) paint, state));
		} else {
			System.err.println("Don't know paint " + paint.getClass().getName());
		}

		return null;
	}

	private PDShading importPDFBoxShadingPaint(ShadingPaint paint, PaintApplierState state) throws IOException {
		PDFCloneUtility pdfCloneUtility = new PDFCloneUtility(state.document);

		Matrix matrix = paint.getMatrix();
		PDShading shading = paint.getShading();

		state.contentStream.transform(matrix);
		return PDShading.create((COSDictionary) pdfCloneUtility.cloneForNewDocument(shading.getCOSObject()));
	}

	/*
	 * Batik SVG Pattern Paint
	 */
	private void applyPatternPaint(Paint paint, PaintApplierState state) throws IOException {
		Rectangle2D anchorRect = getPropertyValue(paint, "getPatternRect");

		AffineTransform paintPatternTransform = getPropertyValue(paint, "getPatternTransform");
		PDTilingPattern pattern = new PDTilingPattern();
		pattern.setPaintType(PDTilingPattern.PAINT_COLORED);
		pattern.setTilingType(PDTilingPattern.TILING_CONSTANT_SPACING_FASTER_TILING);

		pattern.setBBox(new PDRectangle((float) anchorRect.getX(), (float) anchorRect.getY(),
				(float) anchorRect.getWidth(), (float) anchorRect.getHeight()));
		pattern.setXStep((float) anchorRect.getWidth());
		pattern.setYStep((float) anchorRect.getHeight());

		AffineTransform patternTransform = new AffineTransform();
		if (paintPatternTransform != null) {
			paintPatternTransform = new AffineTransform(paintPatternTransform);
			paintPatternTransform.preConcatenate(state.tf);
			patternTransform.concatenate(paintPatternTransform);
		} else
			patternTransform.concatenate(state.tf);
		patternTransform.scale(1f, -1f);
		pattern.setMatrix(patternTransform);

		PDAppearanceStream appearance = new PDAppearanceStream(state.document);
		appearance.setResources(pattern.getResources());
		appearance.setBBox(pattern.getBBox());

		Object graphicsNode = getPropertyValue(paint, "getGraphicsNode");
		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(state.document, pattern.getBBox(),
				state.env.getGraphics2D());
		try {
			Method paintMethod = graphicsNode.getClass().getMethod("paint", Graphics2D.class);
			paintMethod.invoke(graphicsNode, pdfBoxGraphics2D);
		} catch (Exception e) {
			System.err.println("PdfBoxGraphics2DPaintApplier error while drawing Batik PatternPaint");
			e.printStackTrace();
			return;
		}
		pdfBoxGraphics2D.dispose();
		PDFormXObject xFormObject = pdfBoxGraphics2D.getXFormObject();

		PDPageContentStream imageContentStream = new PDPageContentStream(state.document, appearance,
				((COSStream) pattern.getCOSObject()).createOutputStream());
		imageContentStream.drawForm(xFormObject);
		imageContentStream.close();

		PDColorSpace patternCS1 = new PDPattern(null);
		COSName tilingPatternName = state.resources.add(pattern);
		PDColor patternColor = new PDColor(tilingPatternName, patternCS1);

		state.contentStream.setNonStrokingColor(patternColor);
		state.contentStream.setStrokingColor(patternColor);
	}

	private void applyComposite(PaintApplierState state) {
		/*
		 * If we don't have a composite we don't need to do any mapping
		 */
		if (state.composite == null)
			return;

		// Possibly set the alpha constant
		float alpha = 1;
		COSName blendMode = COSName.COMPATIBLE;
		int rule = AlphaComposite.SRC;

		if (state.composite instanceof AlphaComposite) {
			AlphaComposite composite = (AlphaComposite) state.composite;
			alpha = composite.getAlpha();
			rule = composite.getRule();
		} else if (state.composite.getClass().getSimpleName().equals("SVGComposite")) {
			/*
			 * Batik Composite
			 */
			alpha = getPropertyValue(state.composite, "alpha");
			rule = getPropertyValue(state.composite, "rule");
		} else {
			System.err.println("Unknown composite " + state.composite.getClass().getSimpleName());
		}

		state.ensureExtendedState();
		if (alpha < 1) {
			state.pdExtendedGraphicsState.setStrokingAlphaConstant(alpha);
			state.pdExtendedGraphicsState.setNonStrokingAlphaConstant(alpha);
		}
		/*
		 * Try to map the alpha rule into blend modes
		 */
		switch (rule) {
		case AlphaComposite.CLEAR:
			break;
		case AlphaComposite.SRC:
			blendMode = COSName.NORMAL;
			break;
		case AlphaComposite.SRC_OVER:
			blendMode = COSName.COMPATIBLE;
			break;
		case AlphaComposite.XOR:
			blendMode = COSName.EXCLUSION;
			break;
		case AlphaComposite.DST:
			break;
		case AlphaComposite.DST_ATOP:
			break;
		case AlphaComposite.SRC_ATOP:
			blendMode = COSName.COMPATIBLE;
			break;
		case AlphaComposite.DST_IN:
			break;
		case AlphaComposite.DST_OUT:
			break;
		case AlphaComposite.SRC_IN:
			break;
		case AlphaComposite.SRC_OUT:
			break;
		case AlphaComposite.DST_OVER:
			break;
		}
		state.dictExtendedState.setItem(COSName.BM, blendMode);
	}

	private Point2D clonePoint(Point2D point2D) {
		return new Point2D.Double(point2D.getX(), point2D.getY());
	}

	private PDShading buildLinearGradientShading(Paint paint, PaintApplierState state) throws IOException {
		/*
		 * Batik has a copy of RadialGradientPaint, but it has the same structure as the
		 * AWT RadialGradientPaint. So we use Reflection to access the fields of both
		 * these classes.
		 */
		boolean isAWTGradient = paint.getClass().getPackage().getName().equals("java.awt");
		boolean isBatikGradient = paint.getClass().getPackage().getName().equals("org.apache.batik.ext.awt");

		Color[] colors = getPropertyValue(paint, "getColors");
		Color firstColor = colors[0];
		PDColor firstColorMapped = state.colorMapper.mapColor(state.contentStream, firstColor);
		applyAsStrokingColor(firstColor, state);

		PDShadingType3 shading = new PDShadingType3(new COSDictionary());
		shading.setShadingType(PDShading.SHADING_TYPE2);
		shading.setColorSpace(firstColorMapped.getColorSpace());
		float[] fractions = getPropertyValue(paint, "getFractions");
		Point2D startPoint = clonePoint((Point2D.Double) getPropertyValue(paint, "getStartPoint"));
		Point2D endPoint = clonePoint((Point2D.Double) getPropertyValue(paint, "getEndPoint"));
		AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
		state.tf.concatenate(gradientTransform);

		Shape shapeToDraw = state.env.getShapeToDraw();
		if (shapeToDraw != null) {
			Rectangle2D bounds2D = shapeToDraw.getBounds2D();
			Area area = new Area(bounds2D);
			area.transform(state.tf);
			bounds2D = area.getBounds2D();
			double height = bounds2D.getHeight();
			double width = bounds2D.getWidth();
			double min = Math.min(width, height);
			double ratioH = min / height;
			double ratioW = min / width;

			AffineTransform pointTransform = new AffineTransform();

			// pointTransform.scale(ratioW, ratioH);
			state.tf.concatenate(pointTransform);

		}
		state.tf.transform(startPoint, startPoint);
		state.tf.transform(endPoint, endPoint);

		COSArray coords = new COSArray();
		coords.add(new COSFloat((float) startPoint.getX()));
		coords.add(new COSFloat((float) startPoint.getY()));
		coords.add(new COSFloat((float) endPoint.getX()));
		coords.add(new COSFloat((float) endPoint.getY()));
		shading.setCoords(coords);

		PDFunctionType3 type3 = buildType3Function(colors, fractions, state);

		COSArray extend = new COSArray();
		extend.add(COSBoolean.TRUE);
		extend.add(COSBoolean.TRUE);
		shading.setFunction(type3);
		shading.setExtend(extend);
		return shading;
	}

	private PDShading buildRadialGradientShading(Paint paint, PaintApplierState state) throws IOException {
		/*
		 * Batik has a copy of RadialGradientPaint, but it has the same structure as the
		 * AWT RadialGradientPaint. So we use Reflection to access the fields of both
		 * these classes.
		 */
		Color[] colors = getPropertyValue(paint, "getColors");
		Color firstColor = colors[0];
		PDColor firstColorMapped = state.colorMapper.mapColor(state.contentStream, firstColor);
		applyAsStrokingColor(firstColor, state);

		PDShadingType3 shading = new PDShadingType3(new COSDictionary());
		shading.setShadingType(PDShading.SHADING_TYPE3);
		shading.setColorSpace(firstColorMapped.getColorSpace());
		float[] fractions = getPropertyValue(paint, "getFractions");
		Point2D centerPoint = clonePoint((Point2D) getPropertyValue(paint, "getCenterPoint"));
		Point2D focusPoint = clonePoint((Point2D) getPropertyValue(paint, "getFocusPoint"));
		AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
		state.tf.concatenate(gradientTransform);
		state.tf.transform(centerPoint, centerPoint);
		state.tf.transform(focusPoint, focusPoint);

		float radius = getPropertyValue(paint, "getRadius");
		radius = (float) Math.abs(radius * state.tf.getScaleX());

		COSArray coords = new COSArray();

		coords.add(new COSFloat((float) centerPoint.getX()));
		coords.add(new COSFloat((float) centerPoint.getY()));
		coords.add(new COSFloat(0));
		coords.add(new COSFloat((float) focusPoint.getX()));
		coords.add(new COSFloat((float) focusPoint.getY()));
		coords.add(new COSFloat(radius));
		shading.setCoords(coords);

		PDFunctionType3 type3 = buildType3Function(colors, fractions, state);

		COSArray extend = new COSArray();
		extend.add(COSBoolean.TRUE);
		extend.add(COSBoolean.TRUE);
		shading.setFunction(type3);
		shading.setExtend(extend);
		return shading;
	}

	private PDShading buildGradientShading(GradientPaint gradientPaint, PaintApplierState state) throws IOException {
		Color[] colors = new Color[] { gradientPaint.getColor1(), gradientPaint.getColor2() };
		Color firstColor = colors[0];
		PDColor firstColorMapped = state.colorMapper.mapColor(state.contentStream, firstColor);
		applyAsStrokingColor(firstColor, state);

		PDShadingType3 shading = new PDShadingType3(new COSDictionary());
		shading.setShadingType(PDShading.SHADING_TYPE2);
		shading.setColorSpace(firstColorMapped.getColorSpace());
		float[] fractions = new float[] { 0, 1 };
		Point2D startPoint = gradientPaint.getPoint1();
		Point2D endPoint = gradientPaint.getPoint2();

		state.tf.transform(startPoint, startPoint);
		state.tf.transform(endPoint, endPoint);

		COSArray coords = new COSArray();
		coords.add(new COSFloat((float) startPoint.getX()));
		coords.add(new COSFloat((float) startPoint.getY()));
		coords.add(new COSFloat((float) endPoint.getX()));
		coords.add(new COSFloat((float) endPoint.getY()));
		shading.setCoords(coords);

		PDFunctionType3 type3 = buildType3Function(colors, fractions, state);

		COSArray extend = new COSArray();
		extend.add(COSBoolean.TRUE);
		extend.add(COSBoolean.TRUE);

		shading.setFunction(type3);
		shading.setExtend(extend);
		return shading;
	}

	private void applyTexturePaint(TexturePaint texturePaint, PaintApplierState state) throws IOException {
		Rectangle2D anchorRect = texturePaint.getAnchorRect();
		PDTilingPattern pattern = new PDTilingPattern();
		pattern.setPaintType(PDTilingPattern.PAINT_COLORED);
		pattern.setTilingType(PDTilingPattern.TILING_CONSTANT_SPACING_FASTER_TILING);

		pattern.setBBox(new PDRectangle((float) anchorRect.getX(), (float) anchorRect.getY(),
				(float) anchorRect.getWidth(), (float) anchorRect.getHeight()));
		pattern.setXStep((float) anchorRect.getWidth());
		pattern.setYStep((float) anchorRect.getHeight());

		AffineTransform patternTransform = new AffineTransform();
		patternTransform.translate(0, anchorRect.getHeight());
		patternTransform.scale(1f, -1f);
		pattern.setMatrix(patternTransform);

		PDAppearanceStream appearance = new PDAppearanceStream(state.document);
		appearance.setResources(pattern.getResources());
		appearance.setBBox(pattern.getBBox());

		PDPageContentStream imageContentStream = new PDPageContentStream(state.document, appearance,
				((COSStream) pattern.getCOSObject()).createOutputStream());
		BufferedImage texturePaintImage = texturePaint.getImage();
		PDImageXObject imageXObject = state.imageEncoder.encodeImage(state.document, imageContentStream,
				texturePaintImage);

		float ratioW = (float) ((anchorRect.getWidth()) / texturePaintImage.getWidth());
		float ratioH = (float) ((anchorRect.getHeight()) / texturePaintImage.getHeight());
		float paintHeight = (texturePaintImage.getHeight()) * ratioH;
		imageContentStream.drawImage(imageXObject, (float) anchorRect.getX(), (float) (paintHeight + anchorRect.getY()),
				texturePaintImage.getWidth() * ratioW, -paintHeight);
		imageContentStream.close();

		PDColorSpace patternCS1 = new PDPattern(null, imageXObject.getColorSpace());
		COSName tilingPatternName = state.resources.add(pattern);
		PDColor patternColor = new PDColor(tilingPatternName, patternCS1);

		state.contentStream.setNonStrokingColor(patternColor);
		state.contentStream.setStrokingColor(patternColor);
	}

	@SuppressWarnings("WeakerAccess")
	protected PDFunctionType3 buildType3Function(Color[] colors, @SuppressWarnings("unused") float[] fractions,
			PaintApplierState state) {
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

		COSArray functions = buildType2Functions(colors, domain, encode, state);

		function.setItem(COSName.FUNCTIONS, functions);
		function.setItem(COSName.BOUNDS, bounds);
		function.setItem(COSName.ENCODE, encode);

		PDFunctionType3 type3 = new PDFunctionType3(function);
		type3.setDomainValues(domain);
		return type3;
	}

	@SuppressWarnings("WeakerAccess")
	protected COSArray buildType2Functions(Color[] colors, COSArray domain, COSArray encode, PaintApplierState state) {
		Color prevColor = colors[0];

		COSArray functions = new COSArray();
		for (int i = 1; i < colors.length; i++) {
			Color color = colors[i];
			PDColor prevPdColor = state.colorMapper.mapColor(state.contentStream, prevColor);
			PDColor pdColor = state.colorMapper.mapColor(state.contentStream, color);
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

	/**
	 * Get a property value from an object using reflection
	 *
	 * @param obj
	 *            The object to get a property from.
	 * @param propertyGetter
	 *            method name of the getter, i.e. "getXY".
	 * @param <T>
	 *            the type of the property you want to get.
	 * @return the value read from the object
	 */
	@SuppressWarnings({ "unchecked", "WeakerAccess" })
	protected static <T> T getPropertyValue(Object obj, String propertyGetter) {
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

	private static abstract class COSResourceCacheBase<TObject extends COSObjectable> {
		private Map<Integer, List<TObject>> states = new HashMap<Integer, List<TObject>>();

		private static boolean equalsCOSDictionary(COSDictionary cosDictionary, COSDictionary cosDictionary1) {
			if (cosDictionary.size() != cosDictionary1.size())
				return false;
			for (COSName name : cosDictionary.keySet()) {
				COSBase item = cosDictionary.getItem(name);
				COSBase item2 = cosDictionary1.getItem(name);
				if (!equalsCOSBase(item, item2))
					return false;
			}
			return true;
		}

		private static boolean equalsCOSBase(COSBase item, COSBase item2) {
			if (item == item2)
				return true;
			if (item == null)
				return false;
			if (item2 == null)
				return false;
			/*
			 * Can the items be compared directly?
			 */
			if (item.equals(item2))
				return true;

			if (item instanceof COSDictionary && item2 instanceof COSDictionary)
				return equalsCOSDictionary((COSDictionary) item, (COSDictionary) item2);

			// noinspection SimplifiableIfStatement
			if (item instanceof COSArray && item2 instanceof COSArray)
				return equalsCOSArray((COSArray) item, (COSArray) item2);

			return false;
		}

		private static boolean equalsCOSArray(COSArray item, COSArray item2) {
			if (item.size() != item2.size())
				return false;
			for (int i = 0; i < item.size(); i++) {
				COSBase i1 = item.getObject(i);
				COSBase i2 = item2.getObject(i);
				if (!equalsCOSBase(i1, i2))
					return false;
			}
			return true;
		}

		protected abstract int getKey(TObject obj);

		TObject makeUnqiue(TObject state) {
			int key = getKey(state);
			List<TObject> pdExtendedGraphicsStates = states.get(key);
			if (pdExtendedGraphicsStates == null) {
				pdExtendedGraphicsStates = new ArrayList<TObject>();
				states.put(key, pdExtendedGraphicsStates);
			}
			for (TObject s : pdExtendedGraphicsStates) {
				if (stateEquals(s, state))
					return s;
			}
			pdExtendedGraphicsStates.add(state);
			return state;
		}

		private boolean stateEquals(TObject s, TObject state) {
			COSBase base1 = s.getCOSObject();
			COSBase base2 = state.getCOSObject();
			return equalsCOSBase(base1, base2);
		}
	}

	private static class ExtGStateCache extends COSResourceCacheBase<PDExtendedGraphicsState> {
		@Override
		protected int getKey(PDExtendedGraphicsState obj) {
			return obj.getCOSObject().size();
		}
	}

	private static class PDShadingCache extends COSResourceCacheBase<PDShading> {
		@Override
		protected int getKey(PDShading obj) {
			return obj.getCOSObject().size();
		}
	}

}
