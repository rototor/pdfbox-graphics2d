package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType3;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Default paint mapper.
 * 
 * NOTE: Objects of this class are stateful and *not* thread safe!
 */
public class PdfBoxGraphics2DPaintApplier implements IPdfBoxGraphics2DPaintApplier {
	private PDPageContentStream contentStream;
	private IPdfBoxGraphics2DColorMapper colorMapper;

	@Override
	public PDShading applyPaint(Paint paint, PDPageContentStream contentStream, AffineTransform tf,
			IPdfBoxGraphics2DColorMapper colorMapper) throws IOException {
		this.contentStream = contentStream;
		this.colorMapper = colorMapper;
		return applyPaint(paint, tf);
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyAsStrokingColor(Color color) throws IOException {
		contentStream.setStrokingColor(colorMapper.mapColor(contentStream, color));
		contentStream.setNonStrokingColor(colorMapper.mapColor(contentStream, color));

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

	private PDShading applyPaint(Paint paint, AffineTransform tf) throws IOException {

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
			PDColor firstColorMapped = colorMapper.mapColor(contentStream, firstColor);
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
			PDColor firstColorMapped = colorMapper.mapColor(contentStream, firstColor);
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
			PDColor firstColorMapped = colorMapper.mapColor(contentStream, firstColor);

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

	@SuppressWarnings("WeakerAccess")
	protected PDFunctionType3 buildType3Function(Color[] colors, @SuppressWarnings("unused") float[] fractions) {
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

	@SuppressWarnings("WeakerAccess")
	protected COSArray buildType2Functions(Color[] colors, COSArray domain, COSArray encode) {
		Color prevColor = colors[0];

		COSArray functions = new COSArray();
		for (int i = 1; i < colors.length; i++) {
			Color color = colors[i];
			PDColor prevPdColor = colorMapper.mapColor(contentStream, prevColor);
			PDColor pdColor = colorMapper.mapColor(contentStream, color);
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
	 */
	@SuppressWarnings({ "unchecked", "WeakerAccess" })
	protected <T> T getPropertyValue(Object obj, String propertyGetter) {
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

}
