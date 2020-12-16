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

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.text.AttributedCharacterIterator;

/**
 * Draw text using Fonts
 */
public interface IPdfBoxGraphics2DFontTextDrawer
{

    /**
     * Enviroment for font based drawing of text
     */
    interface IFontTextDrawerEnv
    {
        /**
         * @return the document we are writing to
         */
        PDDocument getDocument();

        /**
         * @return the content stream
         */
        PDPageContentStream getContentStream();

        /**
         * @return the current font set on the graphics. This is the "default" font to
         * use when no other font is set on the
         * {@link AttributedCharacterIterator}.
         */
        Font getFont();

        /**
         * @return the current paint set on the graphics. This is the "default" paint
         * when no other paint is set on on the
         * {@link AttributedCharacterIterator}.
         */
        Paint getPaint();

        /**
         * Apply the given paint on the current content stream
         *
         * @param paint       Paint to apply
         * @param shapeToDraw the shape to draw of the text, if known. This is needed to
         *                    calculate correct gradients.
         * @throws IOException if an IO error occurs when writing the paint to the content
         *                     stream.
         */
        void applyPaint(Paint paint, Shape shapeToDraw) throws IOException;

        /**
         * @return the {@link Graphics2D} {@link FontRenderContext}
         */
        FontRenderContext getFontRenderContext();

        /**
         * @return the bbox of the {@link PdfBoxGraphics2D}
         */
        PDRectangle getGraphicsBBox();

        /**
         * @return the resource of the content stream
         */
        PDResources getResources();

        /**
         * @return the default calcuation BufferedImage based graphics.
         */
        Graphics2D getCalculationGraphics();

        /**
         * Apply this stroke on the current content stream
         *
         * @param stroke the stroke to apply
         */
        void applyStroke(Stroke stroke) throws IOException;

        /**
         * @return the current transform active to draw on this stream
         */
        AffineTransform getCurrentEffectiveTransform();
    }

    /**
     * @param iterator Has the text and all its properties
     * @param env      Environment
     * @return true when the given text can be fully drawn using fonts. return false
     * to have the text drawn as vector shapes
     * @throws IOException         when a font can not be loaded or a paint can't be applied.
     * @throws FontFormatException when the font file can not be loaded
     */
    boolean canDrawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
            throws IOException, FontFormatException;

    /**
     * @param iterator The text with all properties
     * @param env      Environment
     * @throws IOException         when a font can not be loaded or a paint can't be applied.
     * @throws FontFormatException when the font file can not be loaded
     */
    void drawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
            throws IOException, FontFormatException;

    /**
     * @param font the Font
     * @param env  the Environment
     * @return the resulting font metrics
     */
    FontMetrics getFontMetrics(Font font, IFontTextDrawerEnv env)
            throws IOException, FontFormatException;
}
