package de.rototor.pdfbox.graphics2d;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;
import org.apache.pdfbox.pdmodel.graphics.color.PDSeparation;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.junit.Test;

/**
 * Generates some PDFs with a special color scodix. In one PDF the painted
 * rectangle is not visible, because its complete clipped out. What is this good
 * for -> technical marker in a context in which multible such PDFs are
 * composed/combined to build the final PDF, so a rather very special case.
 */
public class SpecialColorsTest {

	@Test
	public void writeEmptyScodixPdf() throws IOException {

		PDDocument document = new PDDocument();
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);

		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);

		final PDSeparation scodix = getScodixSeperationColor();
		pdfBoxGraphics2D.setColor(new PdfBoxGraphics2DColor(new PDColor(new float[] { 1 }, scodix), 255, true));
		pdfBoxGraphics2D.fillRoundRect(10, 10, 100, 100, 20, 20);
		pdfBoxGraphics2D.dispose();

		PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
		Matrix matrix = new Matrix();
		matrix.translate(0, 20);
		contentStream.transform(matrix);
		/*
		 * We set an invalid clipping, i.e. a clipping in which the resulting region is
		 * completly empty
		 */
		contentStream.moveTo(1, 1);
		contentStream.lineTo(20, 20);
		contentStream.lineTo(10, 10);
		contentStream.clip();
		contentStream.moveTo(50, 10);
		contentStream.lineTo(200, 200);
		contentStream.lineTo(100, 100);
		contentStream.clip();
		contentStream.drawForm(appearanceStream);
		contentStream.close();

		parent.mkdirs();
		document.save(new File(parent, "empty_scodix.pdf"));
	}

	static final File parent = new File("target/test/specialcolor");

	@Test
	public void writeScodixPdf() throws IOException {
		PDDocument document = new PDDocument();
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);

		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);

		final PDSeparation scodix = getScodixSeperationColor();
		pdfBoxGraphics2D.setColor(new PdfBoxGraphics2DColor(new PDColor(new float[] { 1 }, scodix), 255, true));
		pdfBoxGraphics2D.fillRoundRect(10, 10, 300, 300, 20, 20);

		pdfBoxGraphics2D.dispose();

		PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
		Matrix matrix = new Matrix();
		matrix.translate(0, 210);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);
		matrix = new Matrix();
		matrix.translate(150, 210);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);
		matrix = new Matrix();
		matrix.translate(180, -220);
		matrix.scale(0.5f, 0.5f);
		contentStream.transform(matrix);
		contentStream.drawForm(appearanceStream);
		contentStream.close();

		parent.mkdirs();
		document.save(new File(parent, "scodix.pdf"));
	}

	private PDSeparation getScodixSeperationColor() throws IOException {
		PDSeparation scodix = new PDSeparation();
		scodix.setAlternateColorSpace(PDDeviceCMYK.INSTANCE);
		scodix.setColorantName("Scodix");

		COSDictionary tintFunctionArray = new COSDictionary();
		COSArray c0Array = new COSArray();
		COSArray c1Array = new COSArray();
		for (int i = 0; i < 4; i++) {
			c0Array.add(COSNumber.get("0.0"));
			c1Array.add(COSNumber.get("0.0"));
		}
		c1Array.set(1, COSNumber.get("1.0"));

		tintFunctionArray.setItem(COSName.C0, c0Array);
		tintFunctionArray.setItem(COSName.C1, c1Array);
		COSArray domain = new COSArray();
		domain.add(COSInteger.get(0));
		domain.add(COSInteger.get(1));
		tintFunctionArray.setItem(COSName.DOMAIN, domain);
		tintFunctionArray.setItem(COSName.N, COSNumber.get("1.0"));
		tintFunctionArray.setItem(COSName.FUNCTION_TYPE, COSInteger.get(2));

		COSArray range = new COSArray();
		for (int i = 0; i < 4; i++) {
			range.add(COSNumber.get("0.0"));
			range.add(COSNumber.get("1.0"));
		}
		tintFunctionArray.setItem(COSName.RANGE, range);

		scodix.setTintTransform(new PDFunctionType2(tintFunctionArray));
		return scodix;
	}
}
