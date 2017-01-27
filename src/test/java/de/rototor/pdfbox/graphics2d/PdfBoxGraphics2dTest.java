package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.util.Matrix;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class PdfBoxGraphics2dTest {

	@Test
	public void testSimpleGraphics2d() throws IOException {
		PDDocument document = new PDDocument();

		// Create a new blank page and add it to the document
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);
		File parentDir = new File("target/test/simple");
		parentDir.mkdirs();

		PDPageContentStream contentStream = new PDPageContentStream(document, page);

		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 200, 200);
		pdfBoxGraphics2D.setColor(Color.YELLOW);
		pdfBoxGraphics2D.drawRect(20, 20, 100, 100);
		pdfBoxGraphics2D.setColor(Color.GREEN);
		pdfBoxGraphics2D.fillRect(10, 10, 50, 50);

		pdfBoxGraphics2D.setColor(Color.RED);
		pdfBoxGraphics2D.drawString("Hello World!", 30, 120);
		pdfBoxGraphics2D.dispose();

		PDAppearanceStream appearanceStream = pdfBoxGraphics2D.getAppearanceStream();
		Matrix matrix = new Matrix();
		matrix.translate(0, 20);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);

		matrix.scale(1.5f, 1.5f);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);
		contentStream.close();

		// Save the newly created document
		document.save(new File(parentDir, "BlankPage.pdf"));

		// finally make sure that the document is properly
		// closed.
		document.close();
	}
}