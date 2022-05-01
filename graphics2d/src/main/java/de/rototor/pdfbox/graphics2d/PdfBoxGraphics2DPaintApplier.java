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
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDShadingPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType3;
import org.apache.pdfbox.pdmodel.graphics.shading.ShadingPaint;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;

/**
 * Default paint mapper.
 * <p>
 * NOTE: Objects of this class are stateful and *not* thread safe!
 */
public class PdfBoxGraphics2DPaintApplier implements IPdfBoxGraphics2DPaintApplier
{

    interface ShadingMaskModifier
    {
        PDShading applyMasking(PaintApplierState state, PDShading pdShading) throws IOException;
    }

    static class IdentityShadingMaskModifier implements ShadingMaskModifier
    {

        @Override
        public PDShading applyMasking(PaintApplierState state, PDShading pdShading)
        {
            return pdShading;
        }

        final static IdentityShadingMaskModifier INSTANCE = new IdentityShadingMaskModifier();
    }

    @SuppressWarnings("WeakerAccess")
    protected static class PaintApplierState
    {
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
        private IPdfBoxGraphics2DColorMapper.IColorMapperEnv colorMapperEnv;
        public AffineTransform tf;
        /**
         * This transform is only set, when we apply a nested
         * paint (e.g. a TilingPattern's paint)
         */
        protected AffineTransform nestedTransform;

        private ShadingMaskModifier shadingMaskModifier = IdentityShadingMaskModifier.INSTANCE;

        private void ensureExtendedState()
        {
            if (pdExtendedGraphicsState == null)
            {
                this.dictExtendedState = new COSDictionary();
                this.dictExtendedState.setItem(COSName.TYPE, COSName.EXT_G_STATE);
                pdExtendedGraphicsState = new PDExtendedGraphicsState(this.dictExtendedState);
            }
            assert pdExtendedGraphicsState != null;
        }
    }

    private final ExtGStateCache extGStateCache = new ExtGStateCache();
    private final PDShadingCache shadingCache = new PDShadingCache();

    @Override
    public PDShading applyPaint(Paint paint, PDPageContentStream contentStream, AffineTransform tf,
            IPaintEnv env) throws IOException
    {
        PaintApplierState state = new PaintApplierState();
        state.document = env.getDocument();
        state.resources = env.getResources();
        state.contentStream = contentStream;
        state.colorMapper = env.getColorMapper();
        state.imageEncoder = env.getImageEncoder();
        state.composite = env.getComposite();
        state.pdExtendedGraphicsState = null;
        state.env = env;
        state.colorMapperEnv = env.getGraphics2D().colorMapperEnv;
        state.tf = tf;
        state.nestedTransform = null;
        PDShading shading = applyPaint(paint, state);
        if (state.pdExtendedGraphicsState != null)
            contentStream.setGraphicsStateParameters(
                    extGStateCache.makeUnqiue(state.pdExtendedGraphicsState));
        return shading;
    }

    @SuppressWarnings("WeakerAccess")
    protected void applyAsStrokingColor(Color color, PaintApplierState state) throws IOException
    {
        PDPageContentStream contentStream = state.contentStream;
        IPdfBoxGraphics2DColorMapper colorMapper = state.colorMapper;
        contentStream.setStrokingColor(colorMapper.mapColor(color, state.colorMapperEnv));
        contentStream.setNonStrokingColor(colorMapper.mapColor(color, state.colorMapperEnv));

        int alpha = color.getAlpha();
        if (alpha < 255)
        {
            /*
             * This is semitransparent
             */
            state.ensureExtendedState();
            Float strokingAlphaConstant = state.pdExtendedGraphicsState.getStrokingAlphaConstant();
            if (strokingAlphaConstant == null)
                strokingAlphaConstant = 1f;
            state.pdExtendedGraphicsState.setStrokingAlphaConstant(
                    strokingAlphaConstant * (alpha / 255f));
            Float nonStrokingAlphaConstant = state.pdExtendedGraphicsState.getNonStrokingAlphaConstant();
            if (nonStrokingAlphaConstant == null)
                nonStrokingAlphaConstant = 1f;
            state.pdExtendedGraphicsState.setNonStrokingAlphaConstant(
                    nonStrokingAlphaConstant * (alpha / 255f));
        }

        if (color instanceof IPdfBoxGraphics2DColor)
        {
            if (((IPdfBoxGraphics2DColor) color).isOverprint())
            {
                state.ensureExtendedState();
                state.pdExtendedGraphicsState.setOverprintMode(1.0f);
                /*
                 * Till a fixed version of PDFBOX for PDFBOX-5361 is available,
                 * we do this workaround
                 */
                state.dictExtendedState.setItem(COSName.OPM, COSInteger.get(1));
                state.pdExtendedGraphicsState.setNonStrokingOverprintControl(true);
                state.pdExtendedGraphicsState.setStrokingOverprintControl(true);
            }
        }
    }

    private PDShading applyPaint(Paint paint, PaintApplierState state) throws IOException
    {
        applyComposite(state);

        /*
         * We can not apply not existing paints
         */
        if (paint == null)
            return null;

        String simpleName = paint.getClass().getSimpleName();
        if (paint instanceof Color)
        {
            applyAsStrokingColor((Color) paint, state);
        }
        else if (simpleName.equals("LinearGradientPaint"))
        {
            return shadingCache.makeUnqiue(buildLinearGradientShading(paint, state));
        }
        else if (simpleName.equals("RadialGradientPaint"))
        {
            return shadingCache.makeUnqiue(buildRadialGradientShading(paint, state));
        }
        else if (simpleName.equals("PatternPaint"))
        {
            applyPatternPaint(paint, state);
        }
        else if (simpleName.equals("TilingPaint"))
        {
            applyPdfBoxTilingPaint(paint, state);
        }
        else if (paint instanceof GradientPaint)
        {
            return shadingCache.makeUnqiue(buildGradientShading((GradientPaint) paint, state));
        }
        else if (paint instanceof TexturePaint)
        {
            applyTexturePaint((TexturePaint) paint, state);
        }
        else if (paint instanceof ShadingPaint)
        {
            // PDFBox paint, we can import the shading directly
            return shadingCache.makeUnqiue(
                    importPDFBoxShadingPaint((ShadingPaint<?>) paint, state));
        }
        else
        {
            System.err.printf("Don't know paint %s", paint.getClass().getName());
        }

        return null;
    }

    private PDShading importPDFBoxShadingPaint(ShadingPaint<?> paint, PaintApplierState state)
            throws IOException
    {
        /*
         * Before cloning the shading paint we must ensure that we have already walked the shape
         */
        state.env.ensureShapeIsWalked();

        PDFCloneUtility pdfCloneUtility = new PDFCloneUtility(state.document);

        Matrix matrix = paint.getMatrix();
        PDShading shading = paint.getShading();

        state.contentStream.transform(matrix);
        return PDShading.create(
                (COSDictionary) pdfCloneUtility.cloneForNewDocument(shading.getCOSObject()));
    }

    /*
     * Batik SVG Pattern Paint
     */
    private void applyPatternPaint(Paint paint, PaintApplierState state) throws IOException
    {
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
        if (paintPatternTransform != null)
        {
            paintPatternTransform = new AffineTransform(paintPatternTransform);
            paintPatternTransform.preConcatenate(state.tf);
            patternTransform.concatenate(paintPatternTransform);
        }
        else
            patternTransform.concatenate(state.tf);
        patternTransform.scale(1f, -1f);
        pattern.setMatrix(patternTransform);

        PDAppearanceStream appearance = new PDAppearanceStream(state.document);
        appearance.setResources(pattern.getResources());
        appearance.setBBox(pattern.getBBox());

        Object graphicsNode = getPropertyValue(paint, "getGraphicsNode");
        PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(state.document, pattern.getBBox(),
                state.env.getGraphics2D());
        try
        {
            Method paintMethod = graphicsNode.getClass().getMethod("paint", Graphics2D.class);
            paintMethod.invoke(graphicsNode, pdfBoxGraphics2D);
        }
        catch (Exception e)
        {
            System.err.printf(
                    "PdfBoxGraphics2DPaintApplier error while drawing Batik PatternPaint %s",
                    e.getMessage());
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

    /*
     * Apache PDFBox Tiling Paint
     */
    private void applyPdfBoxTilingPaint(Paint paint, PaintApplierState state)
    {
        try
        {
            Paint tilingPaint = PrivateFieldAccessor.getPrivateField(paint, "paint");
            Matrix patternMatrix = PrivateFieldAccessor.getPrivateField(paint, "patternMatrix");
            state.nestedTransform = patternMatrix.createAffineTransform();
            applyPaint(tilingPaint, state);
        }
        catch (Exception e)
        {
            System.err.printf("PdfBoxGraphics2DPaintApplier error while drawing Tiling Paint %s",
                    e.getMessage());
        }
    }

    private void applyComposite(PaintApplierState state)
    {
        /*
         * If we don't have a composite we don't need to do any mapping
         */
        if (state.composite == null)
            return;

        // Possibly set the alpha constant
        float alpha = 1;
        COSName blendMode = COSName.COMPATIBLE;
        int rule = AlphaComposite.SRC;

        if (state.composite instanceof AlphaComposite)
        {
            AlphaComposite composite = (AlphaComposite) state.composite;
            alpha = composite.getAlpha();
            rule = composite.getRule();
        }
        else if (state.composite.getClass().getSimpleName().equals("SVGComposite"))
        {
            /*
             * Batik Composite
             */
            alpha = getPropertyValue(state.composite, "alpha");
            rule = getPropertyValue(state.composite, "rule");
        }
        else
        {
            System.err.printf("Unknown composite %s", state.composite.getClass().getSimpleName());
        }

        state.ensureExtendedState();
        if (alpha < 1)
        {
            assert state.pdExtendedGraphicsState != null;
            state.pdExtendedGraphicsState.setStrokingAlphaConstant(alpha);
            state.pdExtendedGraphicsState.setNonStrokingAlphaConstant(alpha);
        }
        /*
         * Try to map the alpha rule into blend modes
         */
        switch (rule)
        {
        case AlphaComposite.CLEAR:
            break;
        case AlphaComposite.SRC:
            blendMode = COSName.NORMAL;
            break;
        case AlphaComposite.SRC_OVER:
            //noinspection ConstantConditions
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
            //noinspection ConstantConditions
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

    private Point2D clonePoint(Point2D point2D)
    {
        return new Point2D.Double(point2D.getX(), point2D.getY());
    }

    /**
     * Very small number, everything smaller than this is zero for us.
     */
    private static final double EPSILON = 0.00001;

    static boolean haveColorsTransparency(Color[] colors)
    {
        for (Color c : colors)
        {
            if (c.getAlpha() != 255)
                return true;
        }
        return false;
    }

    private PDShading buildLinearGradientShading(Paint paint, PaintApplierState state)
            throws IOException
    {
        /*
         * Batik has a copy of RadialGradientPaint, but it has the same structure as the AWT RadialGradientPaint. So we use
         * Reflection to access the fields of both these classes.
         */
        boolean isBatikGradient = paint.getClass().getPackage().getName()
                .equals("org.apache.batik.ext.awt");
        boolean isObjectBoundingBox = false;
        if (isBatikGradient)
        {
            AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
            if (!gradientTransform.isIdentity())
            {
                /*
                 * If the scale is not square, we need to use the object bounding box logic
                 */
                if (Math.abs(gradientTransform.getScaleX() - gradientTransform.getScaleY())
                        > EPSILON)
                    isObjectBoundingBox = true;
            }
        }

        /*
         * When doing a shading paint, we need to always walk the shape first.
         */
        state.env.ensureShapeIsWalked();

        final PDShading shading;
        if (isObjectBoundingBox)
        {
            shading = linearGradientObjectBoundingBoxShading(paint, state);
        }
        else
        {
            shading = linearGradientUserSpaceOnUseShading(paint, state);
        }
        return state.shadingMaskModifier.applyMasking(state, shading);
    }

    private PDShading linearGradientObjectBoundingBoxShading(Paint paint, PaintApplierState state)
            throws IOException
    {
        /*
         * I found this Stack Overflow question to be useful: https://stackoverflow.com/questions/50617275/svg-linear-gradients-
         * objectboundingbox-vs-userspaceonuse SVG has 2 different gradient display modes objectBoundingBox & userSpaceOnUse The
         * default is objectBoundingBox. PDF Axial gradients seem to be capable of displaying in any manner, but the default is
         * the normal rendered at a 90 degree angle from the gradient vector. This looks like an SVG in userSpaceOnUse mode. So
         * the task becomes how can we map the default of one format to a non-default mode in another so that the PDF an axial
         * gradient looks like an SVG with a linear gradient.
         *
         * The approach I've used is as follows: Draw the axial gradient on a 1x1 box. A perfect square is a special case where
         * the PDF defaults display matches the SVG default display. Then, use the gradient transform attached to the paint to
         * warp the space containing the box & distort it to a larger rectangle (which may, or may not, still be a square). This
         * makes the gradient in the PDF look like the gradient in an SVG if the SVG is using the objectBoundingBox mode.
         *
         * Note: there is some trickery with shape inversion because SVGs lay out from the top down & PDFs lay out from the
         * bottom up.
         */
        PDShadingType3 shading = setupBasicLinearShading(paint, state);

        Point2D startPoint = clonePoint((Point2D.Double) getPropertyValue(paint, "getStartPoint"));
        Point2D endPoint = clonePoint((Point2D.Double) getPropertyValue(paint, "getEndPoint"));
        AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
        state.tf.concatenate(gradientTransform);

        // noinspection unused
        MultipleGradientPaint.CycleMethod cycleMethod = getCycleMethod(paint);
        // noinspection unused
        MultipleGradientPaint.ColorSpaceType colorSpaceType = getColorSpaceType(paint);

        // Note: all of the start and end points I've seen for linear gradients
        // that use the objectBoundingBox mode define a 1x1 box. I don't know if
        // this can be guaranteed.
        setupShadingCoords(shading, startPoint, endPoint);

        // We need the rectangle here so that the call to clip(useEvenOdd)
        // in PdfBoxGraphics2D.java clips to the right frame of reference
        //
        // Note: tricky stuff follows . . .
        // We're deliberately creating a bounding box with a negative height.
        // Why? Because that contentsStream.transform() is going to invert it
        // so that it has a positive height. It will always invert because
        // SVGs & PDFs have opposite layout directions.
        // If we started with a positive height, then inverted to a negative height
        // we end up with a negative height clipping box in the output PDF
        // and some PDF viewers cannot handle that.
        // e.g. Adobe acrobat will display the PDF one way & Mac Preview
        // will display it another.
        float calculatedX = (float) Math.min(startPoint.getX(), endPoint.getX());
        float calculatedY = (float) Math.max(1.0f, Math.max(startPoint.getY(), endPoint.getY()));
        float calculatedWidth = Math.max(1.0f,
                Math.abs((float) (endPoint.getX() - startPoint.getX())));
        float negativeHeight =
                -1.0f * Math.max(1.0f, Math.abs((float) (endPoint.getY() - startPoint.getY())));

        state.contentStream.addRect(calculatedX, calculatedY, calculatedWidth, negativeHeight);

        state.env.getGraphics2D().markPathIsOnStream();
        state.env.getGraphics2D().internalClip(false);

        // Warp the 1x1 box containing the gradient to fill a larger rectangular space
        state.contentStream.transform(new Matrix(state.tf));

        return shading;
    }

    private void setupShadingCoords(PDShadingType3 shading, Point2D startPoint, Point2D endPoint)
    {
        COSArray coords = new COSArray();
        coords.add(new COSFloat((float) startPoint.getX()));
        coords.add(new COSFloat((float) startPoint.getY()));
        coords.add(new COSFloat((float) endPoint.getX()));
        coords.add(new COSFloat((float) endPoint.getY()));
        shading.setCoords(coords);
    }

    /**
     * This is the default gradient mode for both SVG and java.awt gradients.
     */
    private PDShading linearGradientUserSpaceOnUseShading(Paint paint, PaintApplierState state)
            throws IOException
    {

        PDShadingType3 shading = setupBasicLinearShading(paint, state);

        Point2D startPoint = clonePoint((Point2D.Double) getPropertyValue(paint, "getStartPoint"));
        Point2D endPoint = clonePoint((Point2D.Double) getPropertyValue(paint, "getEndPoint"));
        AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
        state.tf.concatenate(gradientTransform);

        // noinspection unused
        MultipleGradientPaint.CycleMethod cycleMethod = getCycleMethod(paint);
        // noinspection unused
        MultipleGradientPaint.ColorSpaceType colorSpaceType = getColorSpaceType(paint);

        state.tf.transform(startPoint, startPoint);
        state.tf.transform(endPoint, endPoint);

        setupShadingCoords(shading, startPoint, endPoint);

        return shading;
    }

    private PDShadingType3 setupBasicLinearShading(Paint paint, PaintApplierState state)
            throws IOException
    {
        PDShadingType3 shading = new PDShadingType3(new COSDictionary());
        Color[] colors = getPropertyValue(paint, "getColors");
        float[] fractions = getPropertyValue(paint, "getFractions");
        PDColor firstColorMapped = mapFirstColorOfGradient(state, colors);

        if (haveColorsTransparency(colors))
        {
            PdfBoxGraphics2DColor[] alphaGrayscaleColors = mapAlphaToGrayscale(colors);
            state.shadingMaskModifier = new CreateAlphaShadingMask(fractions, alphaGrayscaleColors);
        }

        PDFunctionType3 type3 = buildType3Function(colors, fractions, state);
        shading.setAntiAlias(true);
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(firstColorMapped.getColorSpace());
        shading.setFunction(type3);
        shading.setExtend(setupExtends());
        return shading;
    }

    private PDColor mapFirstColorOfGradient(PaintApplierState state, Color[] colors)
            throws IOException
    {
        Color firstColor = colors[0];
        PDColor firstColorMapped = state.colorMapper.mapColor(firstColor, state.colorMapperEnv);
        applyAsStrokingColor(firstColor, state);
        return firstColorMapped;
    }

    private COSArray setupExtends()
    {
        COSArray extend = new COSArray();
        /*
         * We need to always extend the gradient
         */
        extend.add(COSBoolean.TRUE);
        extend.add(COSBoolean.TRUE);
        return extend;
    }

    /**
     * Map the cycleMethod of the GradientPaint to the java.awt.MultipleGradientPaint.CycleMethod enum.
     *
     * @param paint the paint to get the cycleMethod from (if not in any other way possible using reflection)
     * @return the CycleMethod
     */
    private MultipleGradientPaint.CycleMethod getCycleMethod(Paint paint)
    {
        if (paint instanceof java.awt.MultipleGradientPaint)
            return ((MultipleGradientPaint) paint).getCycleMethod();
        if (paint.getClass().getPackage().getName().equals("org.apache.batik.ext.awt"))
        {
            setupBatikReflectionAccess(paint);
            Object cycleMethod = getPropertyValue(paint, "getCycleMethod");
            if (cycleMethod == BATIK_GRADIENT_NO_CYCLE)
                return MultipleGradientPaint.CycleMethod.NO_CYCLE;
            if (cycleMethod == BATIK_GRADIENT_REFLECT)
                return MultipleGradientPaint.CycleMethod.REFLECT;
            if (cycleMethod == BATIK_GRADIENT_REPEAT)
                return MultipleGradientPaint.CycleMethod.REPEAT;
        }
        return MultipleGradientPaint.CycleMethod.NO_CYCLE;
    }

    private MultipleGradientPaint.ColorSpaceType getColorSpaceType(Paint paint)
    {
        if (paint instanceof java.awt.MultipleGradientPaint)
            return ((MultipleGradientPaint) paint).getColorSpace();
        if (paint.getClass().getPackage().getName().equals("org.apache.batik.ext.awt"))
        {
            setupBatikReflectionAccess(paint);
            Object cycleMethod = getPropertyValue(paint, "getColorSpace");
            if (cycleMethod == BATIK_COLORSPACE_SRGB)
                return MultipleGradientPaint.ColorSpaceType.SRGB;
            if (cycleMethod == BATIK_COLORSPACE_LINEAR_RGB)
                return MultipleGradientPaint.ColorSpaceType.LINEAR_RGB;
        }
        return MultipleGradientPaint.ColorSpaceType.SRGB;
    }

    private Object BATIK_GRADIENT_NO_CYCLE;
    private Object BATIK_GRADIENT_REFLECT;
    private Object BATIK_GRADIENT_REPEAT;
    private Object BATIK_COLORSPACE_SRGB;
    private Object BATIK_COLORSPACE_LINEAR_RGB;

    private void setupBatikReflectionAccess(Paint paint)
    {
        /*
         * As we don't have Batik on our class path we need to access it by reflection if the user application is using Batik
         */
        if (BATIK_GRADIENT_NO_CYCLE != null)
            return;

        try
        {
            Class<?> cls = paint.getClass();
            if (cls.getSimpleName().equals("MultipleGradientPaint"))
            {
                BATIK_GRADIENT_NO_CYCLE = cls.getDeclaredField("NO_CYCLE");
                BATIK_GRADIENT_REFLECT = cls.getDeclaredField("REFLECT");
                BATIK_GRADIENT_REPEAT = cls.getDeclaredField("REPEAT");
                BATIK_COLORSPACE_SRGB = cls.getDeclaredField("SRGB");
                BATIK_COLORSPACE_LINEAR_RGB = cls.getDeclaredField("LINEAR_RGB");
            }
        }
        catch (NoSuchFieldException ignored)
        {
            /*
             * Can not detect Batik CycleMethods :(
             */
        }
    }

    private PDShading buildRadialGradientShading(Paint paint, PaintApplierState state)
            throws IOException
    {
        /*
         * Batik has a copy of RadialGradientPaint, but it has the same structure as the AWT RadialGradientPaint. So we use
         * Reflection to access the fields of both these classes.
         */
        Color[] colors = getPropertyValue(paint, "getColors");
        PDColor firstColorMapped = mapFirstColorOfGradient(state, colors);
        float[] fractions = getPropertyValue(paint, "getFractions");
        Point2D centerPoint = clonePoint((Point2D) getPropertyValue(paint, "getCenterPoint"));
        Point2D focusPoint = clonePoint((Point2D) getPropertyValue(paint, "getFocusPoint"));
        float radius = getPropertyValue(paint, "getRadius");

        /*
         * When doing a shading paint, we need to always walk the shape first.
         */
        state.env.ensureShapeIsWalked();

        PDShadingType3 shading = new PDShadingType3(new COSDictionary());
        shading.setAntiAlias(true);
        shading.setShadingType(PDShading.SHADING_TYPE3);
        shading.setColorSpace(firstColorMapped.getColorSpace());
        AffineTransform gradientTransform = getPropertyValue(paint, "getTransform");
        state.tf.concatenate(gradientTransform);
        state.tf.transform(centerPoint, centerPoint);
        state.tf.transform(focusPoint, focusPoint);

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

        shading.setFunction(type3);
        shading.setExtend(setupExtends());

        if (haveColorsTransparency(colors))
        {
            PdfBoxGraphics2DColor[] alphaGrayscaleColors = mapAlphaToGrayscale(colors);
            state.shadingMaskModifier = new CreateAlphaShadingMask(fractions, alphaGrayscaleColors);
        }

        return state.shadingMaskModifier.applyMasking(state, shading);
    }

    static PdfBoxGraphics2DColor mapAlphaToGrayscale(Color c)
    {
        return new PdfBoxGraphics2DColor(
                new PDColor(new float[] { (c.getAlpha() / 255f) }, PDDeviceGray.INSTANCE));
    }

    static PdfBoxGraphics2DColor[] mapAlphaToGrayscale(Color[] colors)
    {
        PdfBoxGraphics2DColor[] ret = new PdfBoxGraphics2DColor[colors.length];
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = mapAlphaToGrayscale(colors[i]);
        }
        return ret;
    }

    private PDShading buildGradientShading(GradientPaint gradientPaint, PaintApplierState state)
            throws IOException
    {
        Point2D startPoint = gradientPaint.getPoint1();
        Point2D endPoint = gradientPaint.getPoint2();

        Color[] colors = new Color[] { gradientPaint.getColor1(), gradientPaint.getColor2() };
        PDColor firstColorMapped = mapFirstColorOfGradient(state, colors);

        if (haveColorsTransparency(colors))
        {
            PdfBoxGraphics2DColor[] alphaGrayscaleColors = mapAlphaToGrayscale(colors);
            state.shadingMaskModifier = new CreateAlphaShadingMask(null, alphaGrayscaleColors);
        }

        /*
         * When doing a shading paint, we need to always walk the shape first.
         */
        state.env.ensureShapeIsWalked();

        PDShadingType3 shading = new PDShadingType3(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(firstColorMapped.getColorSpace());
        float[] fractions = new float[] { 0, 1 };
        PDFunctionType3 type3 = buildType3Function(colors, fractions, state);

        state.tf.transform(startPoint, startPoint);
        state.tf.transform(endPoint, endPoint);

        setupShadingCoords(shading, startPoint, endPoint);

        shading.setFunction(type3);
        shading.setExtend(setupExtends());
        return state.shadingMaskModifier.applyMasking(state, shading);
    }

    private void createAndApplyGradientTransparencyMask(Paint alphaPaint, PaintApplierState state)
            throws IOException
    {
        PDRectangle bbox = state.env.getGraphics2D().bbox;
        double width = bbox.getWidth();
        double height = bbox.getHeight();

        /*
         * Paint the mask into a XForm
         */
        PdfBoxGraphics2D alphaMaskGfx = new PdfBoxGraphics2D(state.document, (float) width,
                (float) height);
        alphaMaskGfx.transform(state.tf);
        alphaMaskGfx.setPaint(alphaPaint);
        alphaMaskGfx.fillRect(0, 0, (int) (width + 1), (int) (height + 1));
        alphaMaskGfx.dispose();

        COSName firstShadingName = alphaMaskGfx.getXFormObject().getResources().getShadingNames()
                .iterator().next();
        PDShading translatedShading = alphaMaskGfx.getXFormObject().getResources()
                .getShading(firstShadingName);
        COSArray domain = new COSArray();
        domain.add(COSInteger.ZERO);
        domain.add(COSInteger.ONE);
        translatedShading.getCOSObject().setItem(COSName.DOMAIN, domain);

        PDAppearanceStream appearance = new PDAppearanceStream(state.document);
        PDFormXObject xFormObject;
        xFormObject = appearance;
        xFormObject.setResources(new PDResources());
        xFormObject.setBBox(bbox);
        xFormObject.setFormType(1);

        PDShadingPattern pattern = new PDShadingPattern();
        pattern.setShading(translatedShading);
        COSName tilingPatternName = xFormObject.getResources().add(pattern);
        PDColor patternColor = new PDColor(tilingPatternName, PDDeviceGray.INSTANCE);

        PDPageContentStream contentStream = new PDPageContentStream(state.document, appearance,
                xFormObject.getStream().createOutputStream(COSName.FLATE_DECODE));
        contentStream.saveGraphicsState();
        PDExtendedGraphicsState gfxState = new PDExtendedGraphicsState();
        gfxState.setNonStrokingAlphaConstant(1f);
        gfxState.setStrokingAlphaConstant(1f);
        contentStream.setGraphicsStateParameters(gfxState);
        contentStream.setNonStrokingColor(patternColor);
        contentStream.addRect(0, 0, bbox.getWidth(), bbox.getHeight());
        contentStream.fill();
        contentStream.restoreGraphicsState();
        contentStream.close();

        /*
         * And now apply it as mask
         */
        COSDictionary group = new COSDictionary();
        group.setItem(COSName.S, COSName.TRANSPARENCY);
        group.setItem(COSName.CS, COSName.DEVICEGRAY);
        group.setItem(COSName.TYPE, COSName.GROUP);
        xFormObject.getCOSObject().setItem(COSName.GROUP, group);
        state.resources.add(xFormObject);

        state.ensureExtendedState();
        state.pdExtendedGraphicsState.setAlphaSourceFlag(false);
        state.pdExtendedGraphicsState.setNonStrokingAlphaConstant(null);
        state.pdExtendedGraphicsState.setStrokingAlphaConstant(null);

        COSDictionary mask = new COSDictionary();
        mask.setItem(COSName.G, xFormObject);
        mask.setItem(COSName.S, COSName.LUMINOSITY);
        mask.setItem(COSName.TYPE, COSName.MASK);

        state.dictExtendedState.setItem(COSName.SMASK, mask);
    }

    private void applyTexturePaint(TexturePaint texturePaint, PaintApplierState state)
            throws IOException
    {
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
        PDImageXObject imageXObject = state.imageEncoder.encodeImage(state.document,
                imageContentStream, texturePaintImage);

        float ratioW = (float) ((anchorRect.getWidth()) / texturePaintImage.getWidth());
        float ratioH = (float) ((anchorRect.getHeight()) / texturePaintImage.getHeight());
        float paintHeight = (texturePaintImage.getHeight()) * ratioH;
        if (state.nestedTransform != null)
        {
            imageContentStream.transform(new Matrix(state.nestedTransform));
        }
        imageContentStream.drawImage(imageXObject, (float) anchorRect.getX(),
                (float) (paintHeight + anchorRect.getY()), texturePaintImage.getWidth() * ratioW,
                -paintHeight);
        imageContentStream.close();

        PDColorSpace patternCS1 = new PDPattern(null, imageXObject.getColorSpace());
        COSName tilingPatternName = state.resources.add(pattern);
        PDColor patternColor = new PDColor(tilingPatternName, patternCS1);

        state.contentStream.setNonStrokingColor(patternColor);
        state.contentStream.setStrokingColor(patternColor);
    }

    /**
     * Encode a color gradient as a type3 function
     *
     * @param colors    The colors to encode
     * @param fractions the fractions for encoding
     * @param state     our state, this is needed for color mapping
     * @return the type3 function
     */
    private PDFunctionType3 buildType3Function(Color[] colors, float[] fractions,
            PaintApplierState state)
    {
        COSDictionary function = new COSDictionary();
        function.setInt(COSName.FUNCTION_TYPE, 3);

        COSArray domain = new COSArray();
        domain.add(new COSFloat(0));
        domain.add(new COSFloat(1));

        COSArray encode = new COSArray();

        COSArray range = new COSArray();
        range.add(new COSFloat(0));
        range.add(new COSFloat(1));

        List<Color> colorList = new ArrayList<Color>(Arrays.asList(colors));
        COSArray bounds = new COSArray();
        if (needBoundsKeyFrameEntry(fractions))
        {
            /*
             * We need to insert a "keyframe" for fraction 0. See also java.awt.LinearGradientPaint for future information
             */
            colorList.add(0, colors[0]);
            bounds.add(new COSFloat(fractions[0]));
        }

        /*
         * We always add the inner fractions
         */
        for (int i = 1; i < fractions.length - 1; i++)
        {
            float fraction = fractions[i];
            bounds.add(new COSFloat(fraction));
        }
        if (Math.abs(fractions[fractions.length - 1] - 1f) > EPSILON)
        {
            /*
             * We also need to insert a "keyframe" at the end for fraction 1
             */
            colorList.add(colors[colors.length - 1]);
            bounds.add(new COSFloat(fractions[fractions.length - 1]));
        }

        COSArray type2Functions = buildType2Functions(colorList, domain, encode, state);

        function.setItem(COSName.FUNCTIONS, type2Functions);
        function.setItem(COSName.BOUNDS, bounds);
        function.setItem(COSName.ENCODE, encode);

        PDFunctionType3 type3 = new PDFunctionType3(function);
        type3.setDomainValues(domain);
        return type3;
    }

    private boolean needBoundsKeyFrameEntry(float[] fractions)
    {
        return Math.abs(fractions[0]) > EPSILON;
    }

    /**
     * Build a type2 function to interpolate between the given colors.
     *
     * @param colors the color to encode
     * @param domain the domain which should already been setuped. It will be used for the Type2 function
     * @param encode will get the domain information per color channel, i.e. colors.length x [0, 1]
     * @param state  our internal state, this is needed for color mapping
     * @return the Type2 function COSArray
     */
    private COSArray buildType2Functions(List<Color> colors, COSArray domain, COSArray encode,
            PaintApplierState state)
    {
        Color prevColor = colors.get(0);

        COSArray functions = new COSArray();
        for (int i = 1; i < colors.size(); i++)
        {
            Color color = colors.get(i);
            PDColor prevPdColor = state.colorMapper.mapColor(prevColor, state.colorMapperEnv);
            PDColor pdColor = state.colorMapper.mapColor(color, state.colorMapperEnv);
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
     * @param obj            The object to get a property from.
     * @param propertyGetter method name of the getter, i.e. "getXY".
     * @param <T>            the type of the property you want to get.
     * @return the value read from the object
     */
    @SuppressWarnings({ "unchecked", "WeakerAccess" })
    protected static <T> T getPropertyValue(Object obj, String propertyGetter)
    {
        try
        {
            Class<?> c = obj.getClass();
            while (c != null)
            {
                try
                {
                    Method m = c.getMethod(propertyGetter, (Class<?>[]) null);
                    return (T) m.invoke(obj);
                }
                catch (NoSuchMethodException ignored)
                {
                }
                c = c.getSuperclass();
            }
            throw new NullPointerException("Method " + propertyGetter + " not found!");
        }
        catch (Exception e)
        {
            return PdfBoxGraphics2D.throwException(e);
        }
    }

    private static abstract class COSResourceCacheBase<TObject extends COSObjectable>
    {
        private final Map<Integer, List<TObject>> states = new HashMap<Integer, List<TObject>>();

        private static boolean equalsCOSDictionary(COSDictionary cosDictionary,
                COSDictionary cosDictionary1)
        {
            if (cosDictionary.size() != cosDictionary1.size())
                return false;
            for (COSName name : cosDictionary.keySet())
            {
                COSBase item = cosDictionary.getItem(name);
                COSBase item2 = cosDictionary1.getItem(name);
                if (!equalsCOSBase(item, item2))
                    return false;
            }
            return true;
        }

        private static boolean equalsCOSBase(COSBase item, COSBase item2)
        {
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

        private static boolean equalsCOSArray(COSArray item, COSArray item2)
        {
            if (item.size() != item2.size())
                return false;
            for (int i = 0; i < item.size(); i++)
            {
                COSBase i1 = item.getObject(i);
                COSBase i2 = item2.getObject(i);
                if (!equalsCOSBase(i1, i2))
                    return false;
            }
            return true;
        }

        protected abstract int getKey(TObject obj);

        TObject makeUnqiue(TObject state)
        {
            int key = getKey(state);
            List<TObject> pdExtendedGraphicsStates = states.get(key);
            if (pdExtendedGraphicsStates == null)
            {
                pdExtendedGraphicsStates = new ArrayList<TObject>();
                states.put(key, pdExtendedGraphicsStates);
            }
            for (TObject s : pdExtendedGraphicsStates)
            {
                if (stateEquals(s, state))
                    return s;
            }
            pdExtendedGraphicsStates.add(state);
            return state;
        }

        private boolean stateEquals(TObject s, TObject state)
        {
            COSBase base1 = s.getCOSObject();
            COSBase base2 = state.getCOSObject();
            return equalsCOSBase(base1, base2);
        }
    }

    private static class ExtGStateCache extends COSResourceCacheBase<PDExtendedGraphicsState>
    {
        @Override
        protected int getKey(PDExtendedGraphicsState obj)
        {
            return obj.getCOSObject().size();
        }
    }

    private static class PDShadingCache extends COSResourceCacheBase<PDShading>
    {
        @Override
        protected int getKey(PDShading obj)
        {
            return obj.getCOSObject().size();
        }
    }

    private final class CreateAlphaShadingMask implements ShadingMaskModifier
    {
        private final float[] fractions;
        private final PdfBoxGraphics2DColor[] alphaGrayscaleColors;

        public CreateAlphaShadingMask(float[] fractions,
                PdfBoxGraphics2DColor[] alphaGrayscaleColors)
        {
            this.fractions = fractions;
            this.alphaGrayscaleColors = alphaGrayscaleColors;
        }

        @Override
        public PDShading applyMasking(PaintApplierState state, PDShading shading) throws IOException
        {
            PDShading translatedShading = createMaskShading(state, shading);

            PDRectangle bbox = state.env.getGraphics2D().bbox;
            PDAppearanceStream appearance = new PDAppearanceStream(state.document);
            PDFormXObject xFormObject;
            xFormObject = appearance;
            xFormObject.setResources(new PDResources());
            xFormObject.setBBox(bbox);
            xFormObject.setFormType(1);

            PDPageContentStream contentStream = new PDPageContentStream(state.document, appearance,
                    xFormObject.getStream().createOutputStream(COSName.FLATE_DECODE));
            contentStream.saveGraphicsState();
            PDExtendedGraphicsState gfxState = new PDExtendedGraphicsState();
            gfxState.setNonStrokingAlphaConstant(1f);
            gfxState.setStrokingAlphaConstant(1f);
            contentStream.setGraphicsStateParameters(gfxState);
            contentStream.addRect(0, 0, bbox.getWidth(), bbox.getHeight());
            contentStream.shadingFill(translatedShading);
            contentStream.restoreGraphicsState();
            contentStream.close();

            /*
             * And now apply it as mask
             */
            COSDictionary group = new COSDictionary();
            group.setItem(COSName.S, COSName.TRANSPARENCY);
            group.setItem(COSName.CS, COSName.DEVICEGRAY);
            group.setItem(COSName.TYPE, COSName.GROUP);
            xFormObject.getCOSObject().setItem(COSName.GROUP, group);
            state.resources.add(xFormObject);

            state.ensureExtendedState();
            state.pdExtendedGraphicsState.setAlphaSourceFlag(false);
            state.pdExtendedGraphicsState.setNonStrokingAlphaConstant(null);
            state.pdExtendedGraphicsState.setStrokingAlphaConstant(null);

            COSDictionary mask = new COSDictionary();
            mask.setItem(COSName.G, xFormObject);
            mask.setItem(COSName.S, COSName.LUMINOSITY);
            mask.setItem(COSName.TYPE, COSName.MASK);

            state.dictExtendedState.setItem(COSName.SMASK, mask);

            return shading;
        }

        private PDShading createMaskShading(PaintApplierState state, PDShading shading)
                throws IOException
        {
            PDFCloneUtility pdfCloneUtility = new PDFCloneUtility(state.document);
            COSDictionary shadingDictionary = (COSDictionary) pdfCloneUtility.cloneForNewDocument(
                    shading.getCOSObject());
            COSArray functions = (COSArray) shadingDictionary.getItem(COSName.FUNCTIONS);
            if (functions != null)
            {
                int colorIdx = 0;
                for (int i = 0; i < functions.size(); i++)
                {
                    colorIdx = patchFunction(colorIdx, (COSDictionary) functions.get(i));
                }
            }
            else
            {
                COSDictionary function = (COSDictionary) shadingDictionary.getItem(
                        COSName.FUNCTION);
                patchFunction(0, function);
            }
            PDShading translatedShading = PDShading.create(shadingDictionary);
            translatedShading.setColorSpace(PDDeviceGray.INSTANCE);
            return translatedShading;
        }

        private int patchFunction(int colorIdx, COSDictionary cosBase)
        {
            int functionType = cosBase.getInt(COSName.FUNCTION_TYPE);
            switch (functionType)
            {
            case 3:
                /*
                 * Combined Function
                 */
                COSArray functions = (COSArray) cosBase.getItem(COSName.FUNCTIONS);
                for (int i = 0; i < functions.size(); i++)
                {
                    colorIdx = patchFunction(colorIdx, (COSDictionary) functions.get(i));
                }
                break;
            case 2:
                /*
                 * Linear interpolation
                 */
                final float alpha0;
                final float alpha1;
                if (needBoundsKeyFrameEntry(fractions))
                {
                    if (0 == colorIdx)
                    {
                        alpha0 = 1f;
                    }
                    else
                    {
                        PdfBoxGraphics2DColor clr = alphaGrayscaleColors[colorIdx - 1];
                        alpha0 = clr.toPDColor().getComponents()[0];
                    }

                    PdfBoxGraphics2DColor clr = alphaGrayscaleColors[colorIdx];
                    alpha1 = clr.toPDColor().getComponents()[0];
                    colorIdx++;
                }
                else
                {

                    PdfBoxGraphics2DColor clr = alphaGrayscaleColors[colorIdx];
                    alpha0 = clr.toPDColor().getComponents()[0];
                    if (colorIdx + 1 < alphaGrayscaleColors.length)
                        clr = alphaGrayscaleColors[colorIdx + 1];
                    alpha1 = clr.toPDColor().getComponents()[0];
                    colorIdx++;
                }

                COSArray c0Array = new COSArray();
                COSArray c1Array = new COSArray();
                c0Array.add(new COSFloat(alpha0));
                c1Array.add(new COSFloat(alpha1));
                cosBase.setItem(COSName.C0, c0Array);
                cosBase.setItem(COSName.C1, c1Array);
                break;
            }
            return colorIdx;
        }
    }
}
