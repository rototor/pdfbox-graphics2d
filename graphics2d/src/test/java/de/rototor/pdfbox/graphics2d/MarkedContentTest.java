package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MarkedContentTest {

	@Test
	public void testMarkedContentPDF() throws IOException {
		File parentDir = new File("target/test/markedcontent");
		// noinspection ResultOfMethodCallIgnored
		parentDir.mkdirs();
		File targetPDF = new File(parentDir, "markedcontent.pdf");
		PDDocument document = new PDDocument();

		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);

		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 800, 400);
		pdfBoxGraphics2D.setFontTextDrawer(new PdfBoxGraphics2DFontTextDrawerDefaultFonts());

		/* Tag this as a header */
		pdfBoxGraphics2D.drawInMarkedContentSequence(COSName.H, new IPdfBoxGraphics2DMarkedContentDrawer() {
			@Override
			public void draw(PdfBoxGraphics2D gfx) {
				gfx.setColor(Color.BLUE);
				gfx.drawString("Hello World", 20, 20);
			}
		});

		/* All tag names starting with XX are for private use only. */
		pdfBoxGraphics2D.drawInMarkedContentSequence(COSName.getPDFName("XX_MarkedContentTest"),
				new IPdfBoxGraphics2DMarkedContentDrawer() {
					@Override
					public void draw(PdfBoxGraphics2D gfx) {
						gfx.setColor(Color.RED);
						gfx.drawString("This is marked", 100, 100);
					}
				});

		COSDictionary dir = new COSDictionary();
		dir.setItem(COSName.C, COSName.UE);
		dir.setItem(COSName.SEPARATION, new COSString("Test"));
		pdfBoxGraphics2D.drawInMarkedContentSequence(COSName.getPDFName("XX_MarkedContentTest_WithProps"),
				PDPropertyList.create(dir), new IPdfBoxGraphics2DMarkedContentDrawer() {
					@Override
					public void draw(PdfBoxGraphics2D gfx) {
						gfx.setColor(Color.GREEN);
						gfx.drawString("This is marked and has a property list", 50, 200);
					}
				});

		pdfBoxGraphics2D.dispose();

		PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
		Matrix matrix = new Matrix();
		matrix.translate(0, 30);

		contentStream.saveGraphicsState();
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);
		contentStream.restoreGraphicsState();

		contentStream.close();

		document.save(targetPDF);
		document.close();
	}
}
