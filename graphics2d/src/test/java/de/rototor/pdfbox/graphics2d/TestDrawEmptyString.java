package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class TestDrawEmptyString {

	@Test
	public void testDrawEmptyString() throws IOException {
		PDDocument document = new PDDocument();
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);

		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);

		pdfBoxGraphics2D.setColor(Color.GREEN);
		pdfBoxGraphics2D.drawString("This is visible", 10, 10);
		pdfBoxGraphics2D.drawString("", 50, 50);
		pdfBoxGraphics2D.drawString(" ", 100, 50);

		pdfBoxGraphics2D.dispose();

		PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
		Matrix matrix = new Matrix();
		matrix.translate(0, 20);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);
		contentStream.close();

		File file = new File("target/test/draw_empty_test.pdf");
		// noinspection ResultOfMethodCallIgnored
		file.getParentFile().mkdirs();
		document.save(file);
		document.close();

	}
}
