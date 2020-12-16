package de.rototor.pdfbox.graphics2d;

import java.text.AttributedCharacterIterator;

/**
 * Always draw using text, even if we know that we can not map the text correct
 */
public class PdfBoxGraphics2DFontTextForcedDrawer extends PdfBoxGraphics2DFontTextDrawerDefaultFonts {
	@Override
	public boolean canDrawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env) {
		return true;
	}
}
