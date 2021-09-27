package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PdfBoxGraphics2DFontTextDrawerDefaultFontsTest {

	@Test
	public void testFontStyleMatching() {
		Font anyFont = Font.decode("Dialog");
		Font anyFontBold = anyFont.deriveFont(Font.BOLD);
		Font anyFontItalic = anyFont.deriveFont(Font.ITALIC);
		Font anyFontBoldItalic = anyFont.deriveFont(Font.BOLD | Font.ITALIC);

		assertEquals( Standard14Fonts.FontName.COURIER, PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFont));
		assertEquals( Standard14Fonts.FontName.COURIER_BOLD,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFontBold));
		assertEquals( Standard14Fonts.FontName.COURIER_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFontItalic));
		assertEquals( Standard14Fonts.FontName.COURIER_BOLD_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFontBoldItalic));

		assertEquals( Standard14Fonts.FontName.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFont));
		assertEquals( Standard14Fonts.FontName.HELVETICA_BOLD,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFontBold));
		assertEquals( Standard14Fonts.FontName.HELVETICA_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFontItalic));
		assertEquals( Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFontBoldItalic));

		assertEquals( Standard14Fonts.FontName.TIMES_ROMAN, PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFont));
		assertEquals( Standard14Fonts.FontName.TIMES_BOLD,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFontBold));
		assertEquals( Standard14Fonts.FontName.TIMES_ITALIC,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFontItalic));
		assertEquals( Standard14Fonts.FontName.TIMES_BOLD_ITALIC,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFontBoldItalic));
	}

	@Test
	public void testDefaultFontMapping() {
		assertEquals( Standard14Fonts.FontName.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.DIALOG)));
		assertEquals( Standard14Fonts.FontName.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.DIALOG_INPUT)));
		assertEquals( Standard14Fonts.FontName.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Arial")));

		assertEquals( Standard14Fonts.FontName.COURIER,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.MONOSPACED)));

		assertEquals( Standard14Fonts.FontName.TIMES_ROMAN,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.SERIF)));

		assertEquals( Standard14Fonts.FontName.ZAPF_DINGBATS,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Dingbats")));

		assertEquals( Standard14Fonts.FontName.SYMBOL,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Symbol")));

		assertNull(PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Georgia")));
	}

}