package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
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

		assertEquals(PDType1Font.COURIER, PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFont));
		assertEquals(PDType1Font.COURIER_BOLD,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFontBold));
		assertEquals(PDType1Font.COURIER_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFontItalic));
		assertEquals(PDType1Font.COURIER_BOLD_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingCourier(anyFontBoldItalic));

		assertEquals(PDType1Font.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFont));
		assertEquals(PDType1Font.HELVETICA_BOLD,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFontBold));
		assertEquals(PDType1Font.HELVETICA_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFontItalic));
		assertEquals(PDType1Font.HELVETICA_BOLD_OBLIQUE,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingHelvetica(anyFontBoldItalic));

		assertEquals(PDType1Font.TIMES_ROMAN, PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFont));
		assertEquals(PDType1Font.TIMES_BOLD,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFontBold));
		assertEquals(PDType1Font.TIMES_ITALIC,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFontItalic));
		assertEquals(PDType1Font.TIMES_BOLD_ITALIC,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.chooseMatchingTimes(anyFontBoldItalic));
	}

	@Test
	public void testDefaultFontMapping() {
		assertEquals(PDType1Font.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.DIALOG)));
		assertEquals(PDType1Font.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.DIALOG_INPUT)));
		assertEquals(PDType1Font.HELVETICA,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Arial")));

		assertEquals(PDType1Font.COURIER,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.MONOSPACED)));

		assertEquals(PDType1Font.TIMES_ROMAN,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode(Font.SERIF)));

		assertEquals(PDType1Font.ZAPF_DINGBATS,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Dingbats")));

		assertEquals(PDType1Font.SYMBOL,
				PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Symbol")));

		assertNull(PdfBoxGraphics2DFontTextDrawerDefaultFonts.mapDefaultFonts(Font.decode("Georgia")));
	}

}