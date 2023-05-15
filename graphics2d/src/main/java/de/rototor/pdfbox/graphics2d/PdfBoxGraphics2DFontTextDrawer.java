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

import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextAttribute;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

/**
 * Default implementation to draw fonts. You can reuse instances of this class
 * within a PDDocument for more then one {@link PdfBoxGraphics2D}.
 * <p>
 * Just ensure that you call close after you closed the PDDocument to free any
 * temporary files.
 */
public class PdfBoxGraphics2DFontTextDrawer implements IPdfBoxGraphics2DFontTextDrawer, Closeable
{

    private static final Logger LOGGER = Logger.getLogger(PdfBoxGraphics2DFontTextDrawer.class.getName());

    /**
     * Close / delete all resources associated with this drawer. This mainly means
     * deleting all temporary files. You can not use this object after a call to
     * close.
     * <p>
     * Calling close multiple times does nothing.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void close()
    {
        for (File tempFile : tempFiles)
            tempFile.delete();
        tempFiles.clear();
        fontFiles.clear();
        fontMap.clear();
    }

    private static class FontEntry
    {
        String overrideName;
        File file;
    }

    private final List<FontEntry> fontFiles = new ArrayList<FontEntry>();
    private final List<File> tempFiles = new ArrayList<File>();
    private final Map<String, PDFont> fontMap = new HashMap<String, PDFont>();

    /**
     * Register a font. If possible, try to use a font file, i.e.
     * {@link #registerFont(String, File)}. This method will lead to the creation of
     * a temporary file which stores the font data.
     *
     * @param fontName   the name of the font to use. If null, the name is taken from the
     *                   font.
     * @param fontStream the input stream of the font. This file must be a ttf/otf file!
     *                   You have to close the stream outside, this method will not close
     *                   the stream.
     * @throws IOException when something goes wrong with reading the font or writing the
     *                     font to the content stream of the PDF:
     */
    @SuppressWarnings("WeakerAccess")
    public void registerFont(String fontName, InputStream fontStream) throws IOException
    {
        File fontFile = File.createTempFile("pdfboxgfx2dfont", ".ttf");
        FileOutputStream out = new FileOutputStream(fontFile);
        try
        {
            IOUtils.copy(fontStream, out);
        }
        finally
        {
            out.close();
        }
        fontFile.deleteOnExit();
        tempFiles.add(fontFile);
        registerFont(fontName, fontFile);
    }

    /**
     * Register a font.
     *
     * @param fontName the name of the font to use. If null, the name is taken from the
     *                 font.
     * @param fontFile the font file. This file must exist for the live time of this
     *                 object, as the font data will be read lazy on demand
     */
    @SuppressWarnings("WeakerAccess")
    public void registerFont(String fontName, File fontFile)
    {
        if (!fontFile.exists())
            throw new IllegalArgumentException("Font " + fontFile + " does not exist!");
        FontEntry entry = new FontEntry();
        entry.overrideName = fontName;
        entry.file = fontFile;
        fontFiles.add(entry);
    }

    /**
     * Override for registerFont(null,fontFile)
     *
     * @param fontFile the font file
     */
    @SuppressWarnings("WeakerAccess")
    public void registerFont(File fontFile)
    {
        registerFont(null, fontFile);
    }

    /**
     * Override for registerFont(null,fontStream)
     *
     * @param fontStream the font file
     * @throws IOException when something goes wrong with reading the font or writing the
     *                     font to the content stream of the PDF:
     */
    @SuppressWarnings("WeakerAccess")
    public void registerFont(InputStream fontStream) throws IOException
    {
        registerFont(null, fontStream);
    }

    /**
     * Register a font which is already associated with the PDDocument
     *
     * @param name the name of the font as returned by
     *             {@link java.awt.Font#getFontName()}. This name is used for the
     *             mapping the java.awt.Font to this PDFont.
     * @param font the PDFont to use. This font must be loaded in the current
     *             document.
     */
    @SuppressWarnings("WeakerAccess")
    public void registerFont(String name, PDFont font)
    {
        fontMap.put(name, font);
    }

    /**
     * @return true if the font mapping is populated on demand. This is usually only
     * the case if this class has been derived. The default implementation
     * just checks for this.
     */
    @SuppressWarnings("WeakerAccess")
    protected boolean hasDynamicFontMapping()
    {
        return getClass() != PdfBoxGraphics2DFontTextDrawer.class;
    }

    @Override
    public boolean canDrawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
            throws IOException, FontFormatException
    {
        /*
         * When no font is registered we can not display the text using a font...
         */
        if (fontMap.size() == 0 && fontFiles.size() == 0 && !hasDynamicFontMapping())
            return false;

        boolean run = true;
        StringBuilder sb = new StringBuilder();
        while (run)
        {

            Font attributeFont = (Font) iterator.getAttribute(TextAttribute.FONT);
            if (attributeFont == null)
                attributeFont = env.getFont();
            if (mapFont(attributeFont, env) == null)
                return false;

            /*
             * We can not do a Background on the text currently.
             */
            if (iterator.getAttribute(TextAttribute.BACKGROUND) != null)
                return false;

            boolean isLigatures = TextAttribute.LIGATURES_ON.equals(
                    iterator.getAttribute(TextAttribute.LIGATURES));
            if (isLigatures)
                return false;

            run = iterateRun(iterator, sb);
            String s = sb.toString();
            int l = s.length();
            for (int i = 0; i < l; )
            {
                int codePoint = s.codePointAt(i);
                switch (Character.getDirectionality(codePoint))
                {
                /*
                 * We can handle normal LTR.
                 */
                case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
                case Character.DIRECTIONALITY_EUROPEAN_NUMBER:
                case Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR:
                case Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR:
                case Character.DIRECTIONALITY_WHITESPACE:
                case Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR:
                case Character.DIRECTIONALITY_NONSPACING_MARK:
                case Character.DIRECTIONALITY_BOUNDARY_NEUTRAL:
                case Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR:
                case Character.DIRECTIONALITY_SEGMENT_SEPARATOR:
                case Character.DIRECTIONALITY_OTHER_NEUTRALS:
                case Character.DIRECTIONALITY_ARABIC_NUMBER:
                    break;
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING:
                case Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE:
                case Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT:
                    /*
                     * We can not handle this
                     */
                    return false;
                default:
                    /*
                     * Default: We can not handle this
                     */
                    return false;
                }

                if (!attributeFont.canDisplay(codePoint))
                    return false;

                i += Character.charCount(codePoint);
            }
        }
        return true;
    }

    private interface ITextDecorationDrawer
    {
        void draw(PDPageContentStream stream) throws IOException;
    }

    private static class DrawTextDecorationState
    {
        final List<ITextDecorationDrawer> drawers = new ArrayList<ITextDecorationDrawer>();
        public Matrix currentMatrix;
        private final PDPageContentStream contentStream;
        public Point2D currentPoint;
        public Point2D deltaPoint = new Point2D.Float(0, 0);

        public DrawTextDecorationState(PDPageContentStream contentStream)
        {
            this.contentStream = contentStream;
        }

        private void setCurrentTextMatrix(AffineTransform newAT) throws IOException
        {
            Matrix newMatrix = new Matrix(newAT);
            if (!newMatrix.equals(currentMatrix))
            {
                currentMatrix = newMatrix;
                contentStream.setTextMatrix(currentMatrix);
            }
        }
    }

    @Override
    public void drawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
            throws IOException, FontFormatException
    {
        PDPageContentStream contentStream = env.getContentStream();
        DrawTextDecorationState drawState = new DrawTextDecorationState(contentStream);

        contentStream.saveGraphicsState();

        contentStream.beginText();

        AffineTransform identityTextMatrix = new AffineTransform();
        identityTextMatrix.scale(1, -1);
        boolean needMatrixResetToIdentity = true;
        drawState.currentPoint = new Point2D.Float(0, 0);

        StringBuilder sb = new StringBuilder();
        drawState.currentMatrix = null;
        boolean run = true;
        while (run)
        {

            Font attributeFont = (Font) iterator.getAttribute(TextAttribute.FONT);
            boolean wasAttributeFont = attributeFont != null;
            if (attributeFont == null)
                attributeFont = env.getFont();

            Number fontSize = ((Number) iterator.getAttribute(TextAttribute.SIZE));
            if (fontSize != null)
                attributeFont = attributeFont.deriveFont(fontSize.floatValue());
            PDFont font = applyFont(attributeFont, env);
            Object transform = iterator.getAttribute(TextAttribute.TRANSFORM);
            AffineTransform attributedTransform = null;
            if (transform instanceof AffineTransform)
                attributedTransform = (AffineTransform) transform;
            if (transform instanceof TransformAttribute)
                attributedTransform = ((TransformAttribute) transform).getTransform();

            boolean attributeFontTransformed = attributeFont.isTransformed();
            /*
             * The JDK does not respect transforms on the attribute when we have
             * a font set.
             */
            if ((attributedTransform != null && !wasAttributeFont) || attributeFontTransformed)
            {
                AffineTransform tf;
                if (attributeFontTransformed)
                    tf = attributeFont.getTransform();
                else
                    tf = attributedTransform;

                AffineTransform newAT = AffineTransform.getTranslateInstance(
                        drawState.currentPoint.getX(), drawState.currentPoint.getY());
                newAT.concatenate(tf);
                newAT.scale(1, -1);

                drawState.setCurrentTextMatrix(newAT);
                needMatrixResetToIdentity = true;
            }
            else if (needMatrixResetToIdentity)
            {
                AffineTransform at = AffineTransform.getTranslateInstance(
                        drawState.currentPoint.getX(), drawState.currentPoint.getY());
                at.scale(1, -1);
                drawState.setCurrentTextMatrix(at);
                needMatrixResetToIdentity = false;
            }

            Paint paint = (Paint) iterator.getAttribute(TextAttribute.FOREGROUND);
            if (paint == null)
                paint = env.getPaint();

            boolean isStrikeThrough = TextAttribute.STRIKETHROUGH_ON.equals(
                    iterator.getAttribute(TextAttribute.STRIKETHROUGH));
            boolean isUnderline = TextAttribute.UNDERLINE_ON.equals(
                    iterator.getAttribute(TextAttribute.UNDERLINE));
            boolean isLigatures = TextAttribute.LIGATURES_ON.equals(
                    iterator.getAttribute(TextAttribute.LIGATURES));

            run = iterateRun(iterator, sb);
            String text = sb.toString();

            /*
             * Apply the paint
             */
            env.applyPaint(paint, null);

            /*
             * If we force the text write we may encounter situations where the font can not
             * display the characters. PDFBox will throw an exception in this case. We will
             * just silently ignore the text and not display it instead.
             */
            try
            {
                showTextOnStream(env, attributeFont, font, isStrikeThrough, isUnderline,
                        isLigatures, drawState, paint, text);
            }
            catch (IllegalArgumentException e)
            {
                IllegalArgumentException iae = e;
                if (font instanceof PDType1Font && !font.isEmbedded())
                {
                    /*
                     * We tried to use a builtin default font, but it does not have the needed
                     * characters. So we use a embedded font as fallback.
                     */
                    try
                    {
                        if (fallbackFontUnknownEncodings == null)
                            fallbackFontUnknownEncodings = findFallbackFont(env);
                        if (fallbackFontUnknownEncodings != null)
                        {
                            env.getContentStream().setFont(fallbackFontUnknownEncodings,
                                    attributeFont.getSize2D());
                            showTextOnStream(env, attributeFont, fallbackFontUnknownEncodings,
                                    isStrikeThrough, isUnderline, isLigatures, drawState, paint,
                                    text);
                            iae = null;
                        }
                    }
                    catch (IllegalArgumentException e1)
                    {
                        iae = e1;
                    }
                }

                if (iae != null)
                    LOGGER.log(Level.SEVERE, "PDFBoxGraphics: Can not map text " + text + 
                            " with font " + attributeFont.getFontName() + ": " + iae.getMessage(), iae);
            }
        }
        contentStream.endText();

        contentStream.restoreGraphicsState();

        if (!drawState.drawers.isEmpty())
        {
            for (ITextDecorationDrawer drawer : drawState.drawers)
            {
                contentStream.saveGraphicsState();
                drawer.draw(contentStream);
                contentStream.restoreGraphicsState();
            }
        }
    }

    @Override
    public FontMetrics getFontMetrics(final Font f, IFontTextDrawerEnv env)
            throws IOException, FontFormatException
    {
        final FontMetrics defaultMetrics = env.getCalculationGraphics().getFontMetrics(f);
        final PDFont pdFont = mapFont(f, env);
        /*
         * By default we delegate to the buffered image based calculation. This is wrong
         * as soon as we use the native PDF Box font, as those have sometimes different widths.
         *
         * But it is correct and fine as long as we use vector shapes.
         */
        if (pdFont == null)
            return defaultMetrics;
        return new FontMetrics(f)
        {
            public int getDescent()
            {
                return defaultMetrics.getDescent();
            }

            public int getHeight()
            {
                return defaultMetrics.getHeight();
            }

            public int getMaxAscent()
            {
                return defaultMetrics.getMaxAscent();
            }

            public int getMaxDescent()
            {
                return defaultMetrics.getMaxDescent();
            }

            public boolean hasUniformLineMetrics()
            {
                return defaultMetrics.hasUniformLineMetrics();
            }

            public LineMetrics getLineMetrics(String str, Graphics context)
            {
                return defaultMetrics.getLineMetrics(str, context);
            }

            public LineMetrics getLineMetrics(String str, int beginIndex, int limit,
                    Graphics context)
            {
                return defaultMetrics.getLineMetrics(str, beginIndex, limit, context);
            }

            public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit,
                    Graphics context)
            {
                return defaultMetrics.getLineMetrics(chars, beginIndex, limit, context);
            }

            public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit,
                    Graphics context)
            {
                return defaultMetrics.getLineMetrics(ci, beginIndex, limit, context);
            }

            public Rectangle2D getStringBounds(String str, Graphics context)
            {
                return defaultMetrics.getStringBounds(str, context);
            }

            public Rectangle2D getStringBounds(String str, int beginIndex, int limit,
                    Graphics context)
            {
                return defaultMetrics.getStringBounds(str, beginIndex, limit, context);
            }

            public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit,
                    Graphics context)
            {
                return defaultMetrics.getStringBounds(chars, beginIndex, limit, context);
            }

            public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit,
                    Graphics context)
            {
                return defaultMetrics.getStringBounds(ci, beginIndex, limit, context);
            }

            public Rectangle2D getMaxCharBounds(Graphics context)
            {
                return defaultMetrics.getMaxCharBounds(context);
            }

            @Override
            public int getAscent()
            {
                return defaultMetrics.getAscent();
            }

            @Override
            public int getMaxAdvance()
            {
                return defaultMetrics.getMaxAdvance();
            }

            @Override
            public int getLeading()
            {
                return defaultMetrics.getLeading();
            }

            @Override
            public FontRenderContext getFontRenderContext()
            {
                return defaultMetrics.getFontRenderContext();
            }

            @Override
            public int charWidth(char ch)
            {
                char[] chars = { ch };
                return charsWidth(chars, 0, chars.length);
            }

            @Override
            public int charWidth(int codePoint)
            {
                char[] data = Character.toChars(codePoint);
                return charsWidth(data, 0, data.length);
            }

            @Override
            public int charsWidth(char[] data, int off, int len)
            {
                return stringWidth(new String(data, off, len));
            }

            @Override
            public int stringWidth(String str)
            {
                try
                {
                    float width = pdFont.getStringWidth(str) / 1000 * f.getSize2D();
                    return (int) (width + .5f);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                catch (IllegalArgumentException e)
                {
                    /*
                     * We let unknown chars be handled with
                     */
                    return defaultMetrics.stringWidth(str);
                }
            }

            @Override
            public int[] getWidths()
            {
                try
                {
                    int[] first256Widths = new int[256];
                    for (int i = 0; i < first256Widths.length; i++)
                        first256Widths[i] = (int) (pdFont.getWidth(i) / 1000 * f.getSize());
                    return first256Widths;
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }

        };
    }

    private PDFont fallbackFontUnknownEncodings;

    private PDFont findFallbackFont(IFontTextDrawerEnv env) throws IOException
    {
        /*
         * We search for the right font in the system folders... We try to use
         * LucidaSansRegular and if not found Arial, because this fonts often exists. We
         * use the Java default font as fallback.
         *
         * Normally this method is only used and called if a default font misses some
         * special characters, e.g. Hebrew or Arabic characters.
         */
        String javaHome = System.getProperty("java.home", ".");
        String javaFontDir = javaHome + "/lib/fonts";
        String windir = System.getenv("WINDIR");
        if (windir == null)
            windir = javaFontDir;
        File[] paths = new File[] { new File(new File(windir), "fonts"),
                new File(System.getProperty("user.dir", ".")),
                // Mac Fonts
                new File("/Library/Fonts"), new File("/System/Library/Fonts/Supplemental/"),
                // Unix Fonts
                new File("/usr/share/fonts/truetype"), new File("/usr/share/fonts/truetype/dejavu"),
                new File("/usr/share/fonts/truetype/liberation"),
                new File("/usr/share/fonts/truetype/noto"), new File(javaFontDir) };
        for (String fontFileName : new String[] { "LucidaSansRegular.ttf", "arial.ttf", "Arial.ttf",
                "DejaVuSans.ttf", "LiberationMono-Regular.ttf", "NotoSerif-Regular.ttf",
                "Arial Unicode.ttf", "Tahoma.ttf" })
        {
            for (File path : paths)
            {
                File arialFile = new File(path, fontFileName);
                if (arialFile.exists())
                {
                    // We try to use the first font we can find and use.
                    PDType0Font pdType0Font = tryToLoadFont(env, arialFile);
                    if (pdType0Font != null)
                        return pdType0Font;
                }
            }
        }
        return null;
    }

    private PDType0Font tryToLoadFont(IFontTextDrawerEnv env, File foundFontFile)
    {
        try
        {
            return PDType0Font.load(env.getDocument(), foundFontFile);
        }
        catch (IOException e)
        {
            // The font maybe have an embed restriction.
            return null;
        }
    }

    private static final boolean DEBUG_BOX = false;

    private void showTextOnStream(final IFontTextDrawerEnv env, final Font attributeFont,
            final PDFont font, final boolean isStrikeThrough, final boolean isUnderline,
            boolean isLigatures, DrawTextDecorationState drawState, final Paint paint, String text)
            throws IOException
    {
        // noinspection StatementWithEmptyBody
        if (isLigatures)
        {
            /*
             * No idea how to map this ...
             */

        }
        final PDPageContentStream contentStream = drawState.contentStream;

        contentStream.showText(text);

        final float stringWidth = (font.getStringWidth(text) / 1000f) * attributeFont.getSize2D();
        if ((isStrikeThrough || isUnderline))
        {
            final LineMetrics lineMetrics = attributeFont.getLineMetrics(text,
                    env.getFontRenderContext());

            final Matrix currentMatrix = drawState.currentMatrix;
            final float ourX =
                    (float) drawState.currentPoint.getX() - currentMatrix.getTranslateX();
            final float ourY =
                    (float) drawState.currentPoint.getY() - currentMatrix.getTranslateY();
            //final float ourX = ;
            //final float ourY = ;

            drawState.drawers.add(new ITextDecorationDrawer()
            {
                @Override
                public void draw(PDPageContentStream stream) throws IOException
                {

                    float height = lineMetrics.getHeight();
                    float pdFontHeight =
                            font.getBoundingBox().getHeight() / 1000 * attributeFont.getSize2D();
                    float scale = pdFontHeight / height;
                    float decent = lineMetrics.getDescent();
                    contentStream.transform(currentMatrix);
                    if (DEBUG_BOX)
                    {
                        env.applyStroke(new BasicStroke(1));
                        env.applyPaint(new Color(0x5F2F13F2),
                                new Rectangle.Float(ourX, ourY - decent * scale, stringWidth,
                                        height * scale));

                        contentStream.addRect(ourX, ourY - decent * scale, stringWidth,
                                height / scale);
                        contentStream.stroke();
                    }

                    env.applyPaint(paint,
                            new Rectangle.Float(ourX, ourY - decent * scale, stringWidth,
                                    height * scale));
                    float baseline = lineMetrics.getBaselineOffsets()[lineMetrics.getBaselineIndex()];
                    if (isStrikeThrough)
                    {
                        env.applyStroke(new BasicStroke(
                                getSensibleThickness(lineMetrics.getStrikethroughThickness(),
                                        attributeFont)));
                        float strikethroughOffset =
                                scale * (baseline + lineMetrics.getStrikethroughOffset());
                        contentStream.moveTo(ourX, ourY - strikethroughOffset);
                        contentStream.lineTo(ourX + stringWidth, ourY - strikethroughOffset);
                        contentStream.stroke();
                    }
                    if (isUnderline)
                    {
                        env.applyStroke(new BasicStroke(
                                getSensibleThickness(lineMetrics.getUnderlineThickness(),
                                        attributeFont)));
                        float underlineOffset =
                                scale * (baseline + lineMetrics.getUnderlineOffset());
                        contentStream.moveTo(ourX, ourY - underlineOffset);
                        contentStream.lineTo(ourX + stringWidth, ourY - underlineOffset);
                        contentStream.stroke();
                    }
                }
            });
        }

        Point2D deltaPoint = new Point2D.Double(stringWidth, 0);
        assert drawState.currentMatrix != null;
        drawState.currentMatrix.transform(deltaPoint);
        drawState.deltaPoint = deltaPoint;
        drawState.currentPoint = new Point2D.Double(
                drawState.currentPoint.getX() + deltaPoint.getX(),
                drawState.currentPoint.getY() + deltaPoint.getY());
    }

    private float getSensibleThickness(float thickness, Font font)
    {
        if (thickness < 0.00001f)
            return .04f * font.getSize2D();
        return thickness;
    }

    private PDFont applyFont(Font font, IFontTextDrawerEnv env)
            throws IOException, FontFormatException
    {
        PDFont fontToUse = mapFont(font, env);
        if (fontToUse == null)
        {
            /*
             * If we have no font but are forced to apply a font, we just use the default
             * builtin PDF font...
             */
            fontToUse = PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(font);
        }
        env.getContentStream().setFont(fontToUse, font.getSize2D());
        return fontToUse;
    }

    /**
     * Try to map the java.awt.Font to a PDFont.
     *
     * @param font the java.awt.Font for which a mapping should be found
     * @param env  environment of the font mapper
     * @return the PDFont or null if none can be found.
     * @throws IOException         when the font can not be loaded
     * @throws FontFormatException when the font file can not be loaded
     */
    @SuppressWarnings("WeakerAccess")
    protected PDFont mapFont(final Font font, final IFontTextDrawerEnv env)
            throws IOException, FontFormatException
    {
        /*
         * If we have any font registering's, we must perform them now
         */
        for (final FontEntry fontEntry : fontFiles)
        {
            if (fontEntry.overrideName == null)
            {
                Font javaFont = Font.createFont(Font.TRUETYPE_FONT, fontEntry.file);
                fontEntry.overrideName = javaFont.getFontName();
            }
            if (fontEntry.file.getName().toLowerCase(Locale.US).endsWith(".ttc"))
            {
                TrueTypeCollection collection = new TrueTypeCollection(fontEntry.file);
                collection.processAllFonts(new TrueTypeCollection.TrueTypeFontProcessor()
                {
                    @Override
                    public void process(TrueTypeFont ttf) throws IOException
                    {
                        PDFont pdFont = PDType0Font.load(env.getDocument(), ttf, true);
                        fontMap.put(fontEntry.overrideName, pdFont);
                        fontMap.put(pdFont.getName(), pdFont);
                    }
                });
            }
            else
            {
                /*
                 * We load the font using the file.
                 */
                PDFont pdFont = PDType0Font.load(env.getDocument(), fontEntry.file);
                fontMap.put(fontEntry.overrideName, pdFont);
            }
        }
        fontFiles.clear();

        return fontMap.get(font.getFontName());
    }

    private boolean iterateRun(AttributedCharacterIterator iterator, StringBuilder sb)
    {
        sb.setLength(0);
        int charCount = iterator.getRunLimit() - iterator.getRunStart();
        while (charCount-- > 0)
        {
            char c = iterator.current();
            iterator.next();
            if (c == AttributedCharacterIterator.DONE)
            {
                return false;
            }
            else
            {
                sb.append(c);
            }
        }
        return (iterator.getIndex() < iterator.getRunLimit());
    }

}
