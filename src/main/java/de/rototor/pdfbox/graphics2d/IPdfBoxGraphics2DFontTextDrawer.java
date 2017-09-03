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
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.text.AttributedCharacterIterator;

/**
 * Draw text using Fonts
 */
public interface IPdfBoxGraphics2DFontTextDrawer {

	/**
	 * Enviroment for font based drawing of text
	 */
	interface IFontTextDrawerEnv {
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
		 *         use when no other font is set on the
		 *         {@link AttributedCharacterIterator}.
		 */
		Font getFont();

		/**
		 * 
		 * @return the current paint set on the graphics. This is the "default" paint
		 *         when no other paint is set on on the
		 *         {@link AttributedCharacterIterator}.
		 */
		Paint getPaint();

		/**
		 * Apply the given paint on the current content stream
		 * 
		 * @param paint
		 *            Paint to apply
		 * @return the shading for this paint or null if this paint has no special
		 *         shading. At the moment I don't know of a way to use the shading with
		 *         text.
		 * @throws IOException
		 *             if an IO error occurs when writing the paint to the content
		 *             stream.
		 */
		PDShading applyPaint(Paint paint) throws IOException;

		/**
		 * @return the {@link Graphics2D} {@link FontRenderContext}
		 */
		FontRenderContext getFontRenderContext();
	}

	/**
	 * @param iterator
	 *            Has the text and all its properties
	 * @param env
	 *            Environment
	 * @return true when the given text can be fully drawn using fonts. return false
	 *         to have the text drawn as vector shapes
	 * @throws IOException
	 *             when a font can not be loaded or a paint can't be applied.
	 */
	boolean canDrawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
			throws IOException, FontFormatException;

	/**
	 * @param iterator
	 *            The text with all properties
	 * @param env
	 *            Environment
	 */
	void drawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env) throws IOException, FontFormatException;
}
