package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.*;
import java.io.IOException;

/**
 * Like {@link PdfBoxGraphics2DFontTextDrawer}, but tries to use default fonts
 * whenever possible. Default fonts are not embedded. You can register
 * additional font files. If no font mapping is found, Helvetica is used.
 * 
 * This will fallback to vectorized text if any kind of RTL text is rendered
 * and/or any other not supported feature is used.
 */
public class PdfBoxGraphics2DFontTextDrawerDefaultFonts extends PdfBoxGraphics2DFontTextDrawer {
	@Override
	protected PDFont mapFont(Font font, IFontTextDrawerEnv env) throws IOException, FontFormatException {
		Standard14Fonts.FontName standardFontName= mapDefaultFonts(font);
		if (standardFontName != null)
			return new PDType1Font(standardFontName);

		/*
		 * Do we have a manual registered mapping with a font file?
		 */
		PDFont pdFont = super.mapFont(font, env);
		if (pdFont != null)
			return pdFont;
		return new PDType1Font(chooseMatchingHelvetica(font));
	}

	/**
	 * Find a PDFont for the given font object, which does not need to be embedded.
	 * 
	 * @param font
	 *            font for which to find a suitable default font
	 * @return null if no default font is found or a default font which does not
	 *         need to be embedded.
	 */
	public static Standard14Fonts.FontName mapDefaultFonts(Font font) {
		/*
		 * Map default font names to the matching families.
		 */
		if (fontNameEqualsAnyOf(font, Font.SANS_SERIF, Font.DIALOG, Font.DIALOG_INPUT, "Arial", "Helvetica"))
			return chooseMatchingHelvetica(font);
		if (fontNameEqualsAnyOf(font, Font.MONOSPACED, "courier", "courier new"))
			return chooseMatchingCourier(font);
		if (fontNameEqualsAnyOf(font, Font.SERIF, "Times", "Times New Roman", "Times Roman"))
			return chooseMatchingTimes(font);
		if (fontNameEqualsAnyOf(font, "Symbol"))
			return (Standard14Fonts.FontName.SYMBOL);
		if (fontNameEqualsAnyOf(font, "ZapfDingbats", "Dingbats"))
			return (Standard14Fonts.FontName.ZAPF_DINGBATS);
		return null;
	}

	private static boolean fontNameEqualsAnyOf(Font font, String... names) {
		String name = font.getName();
		for (String fontName : names) {
			if (fontName.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	/**
	 * Get a PDType1Font.TIMES-variant, which matches the given font
	 * 
	 * @param font
	 *            Font to get the styles from
	 * @return a PDFont Times variant which matches the style in the given Font
	 *         object.
	 */
	public static Standard14Fonts.FontName chooseMatchingTimes(Font font) {
		if ((font.getStyle() & (Font.ITALIC | Font.BOLD)) == (Font.ITALIC | Font.BOLD))
			return Standard14Fonts.FontName.TIMES_BOLD_ITALIC;
		if ((font.getStyle() & Font.ITALIC) == Font.ITALIC)
			return Standard14Fonts.FontName.TIMES_ITALIC;
		if ((font.getStyle() & Font.BOLD) == Font.BOLD)
			return Standard14Fonts.FontName.TIMES_BOLD;
		return Standard14Fonts.FontName.TIMES_ROMAN;
	}

	/**
	 * Get a PDType1Font.COURIER-variant, which matches the given font
	 * 
	 * @param font
	 *            Font to get the styles from
	 * @return a PDFont Courier variant which matches the style in the given Font
	 *         object.
	 */
	public static Standard14Fonts.FontName chooseMatchingCourier(Font font) {
		if ((font.getStyle() & (Font.ITALIC | Font.BOLD)) == (Font.ITALIC | Font.BOLD))
			return Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE;
		if ((font.getStyle() & Font.ITALIC) == Font.ITALIC)
			return Standard14Fonts.FontName.COURIER_OBLIQUE;
		if ((font.getStyle() & Font.BOLD) == Font.BOLD)
			return Standard14Fonts.FontName.COURIER_BOLD;
		return Standard14Fonts.FontName.COURIER;
	}

	/**
	 * Get a PDType1Font.HELVETICA-variant, which matches the given font
	 * 
	 * @param font
	 *            Font to get the styles from
	 * @return a PDFont Helvetica variant which matches the style in the given Font
	 *         object.
	 */
	public static Standard14Fonts.FontName chooseMatchingHelvetica(Font font) {
		if ((font.getStyle() & (Font.ITALIC | Font.BOLD)) == (Font.ITALIC | Font.BOLD))
			return Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE;
		if ((font.getStyle() & Font.ITALIC) == Font.ITALIC)
			return Standard14Fonts.FontName.HELVETICA_OBLIQUE;
		if ((font.getStyle() & Font.BOLD) == Font.BOLD)
			return Standard14Fonts.FontName.HELVETICA_BOLD;
		return Standard14Fonts.FontName.HELVETICA;
	}
}
