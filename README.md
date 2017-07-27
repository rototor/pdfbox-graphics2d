# pdfbox-graphics2d
Graphics2D Bridge for Apache PDFBox

## Intro
Using this library you can use any Graphics2D API based SVG / graph / chart library 
to embed those graphics as vector drawing in a PDF.

The following features are supported:

- Drawing any shape using draw...() and fill...() methods from Graphics2D.
- Drawing images. The default is to always lossless compress them. You could plugin 
  your own Image -> PDImageXObject conversion if you want to encode the images as jpeg. 
  **Note:** At the moment PDFBox only writes 8 bit images. So even if you draw 
  a 16 bit image it will be reduced to 8 bit. Depending on the colorspaces this may be 
  bad and cause colorshifts in the embedded image (e.g. with 16 Bit ProPhoto color profile).
- All BasicStroke attributes
- Paint:
	- Colors. You can specify your own color mapping implementation to special map the (RGB) 
	colors to PDColor. Beside using CMYK colors you can also use spot colors.
	- GradientPaint, LinearGradientPaint and RadialGradientPaint. There are some restrictions:
	  - GradientPaint always generates acyclic gradients. 
	  - LinearGradientPaint and RadialGradientPaint always assume even split fractions. 
	  The actual given fractions are ignored at the moment.
	- TexturePaint. 
- Drawing text. At the moment all text is drawn as vector shapes, so no fonts are embedded. 
RTL languages are supported.

The following features are not supported (yet):

- (Alpha-)Composite with a rule different then AlphaComposite.SRC_OVER.
- copyArea(). This is not possible to implement.
- hit(). Why would you want to use that?

## Download

This library is available through Maven:

```xml
<dependency>
	<groupId>de.rototor.pdfbox</groupId>
	<artifactId>graphics2d</artifactId>
	<version>0.5</version>
</dependency>
```

## Example Usage

```java
public class PDFGraphics2DSample {
	public static main(String[] argv) throws Exception {
		PDDocument document = new PDDocument();
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);
			
		/*
		 * Creates the Graphics and sets a size in pixel. This size is used for the BBox of the XForm.
		 * So everything drawn outside (0x0)-(width,height) will be clipped.
		 */
		PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);
		
		/*
		 * Now do your drawing
		 */ 
		
		/* ... */
		
		/* 
		 * Dispose when finished
		 */
		pdfBoxGraphics2D.dispose();
		
		/*
		 * After dispose() of the graphics object we can get the XForm.
		 */
		PDFormXObject xform = pdfBoxGraphics2D.getXFormObject();
		
		/*
		 * Build a matrix to place the form
		 */
		Matrix matrix = new Matrix();
		/*
		 *  Note: As PDF coordinates start at the bottom left corner, we move up from there.
		 */
		matrix.translate(0, 20);
		PDPageContentStream contentStream = new PDPageContentStream(document, page);
		contentStream.transform(matrix);
		
		/*
		 * Now finally draw the form. As we not do any scaling, the form drawn has a size of 5,5 x 5,5 inches, 
		 * because PDF uses 72 DPI for its lengths by default. If you want to scale, skew or rotate the form you can 
		 * of course do this. And you can also draw the form more then once. Think of the XForm as a stamper.
		 */
		contentStream.drawForm(xform);
		
		contentStream.close();
		
		document.save(new File("mysample.pdf"));
		document.close();
	}
}
```

See also [manual drawing](src/test/java/de/rototor/pdfbox/graphics2d/PdfBoxGraphics2dTest.java) 
and [drawing SVGs](src/test/java/de/rototor/pdfbox/graphics2d/RenderSVGsTest.java). The testdrivers are only
smoke tests, i.e. they don't explicit test the result, they just run and test if the their are crashes. You have 
to manually compare the PDF result of the testdriver with the also generated PNG compare image.

## Rendering text using fonts vs vectors

When rendering a text in a PDF file you can choose two methods:
- Render the text using a font as text.
- Render the text using TextLayout as vector graphics.

Rendering a text using a font is the normal and preferred way to display a text:
- The text can be copied and is searchable.
- Usually it takes less space then when using vector shapes.
- When printing in PrePress (Digital / Offset Print) the RIP usually handles text special to ensure 
the best possible reading experience. E.g. RGB Black is usually mapped to a black 
with some cyan. This gives a "deeper" black, especially if you have a large black area. 
But if you use a RGB black to render text it is usually mapped to pure black to avoid 
any printing registration mismatches, which would be very bad for reading the text.
- Note: When rendering a text using a font you should always embed the needed subset of the font into the PDF. 
  Otherwise not every (=most) PDF viewers will be able to display the text correctly, if they don't have the font or
  have a different version of the font, which can happen across different OS and OS versions.
- Note: Not all PDF viewer can handle all fonts correctly. E.g. PDFBox 1.8 was not able to handle fonts right. 
But nowadays all PDF viewers should be able to handle fonts fine.

On the other site rendering a text using vector shapes has the following properties:
- The text is always displayed the same. They will be no differences between the PDF viewers.
- The text is not searchable and can not be copied.
- Note: Vector shapes take more space than a embedded font.
- Note: You may want to manually alter the color mapping to e.g. ensure a black text is printed using pure CMYK black. 
If you do not plan to print the PDF in offset or digital print you can ignore that. This will make no difference for 
your normal desktop printer.

At the moment only rendering text as vector shapes is implemented. To embed a text using its font into a PDF direct 
access to the underling font file is required, because PDFBox needs to build the embedded font subset. Using the normal 
Java font API it is not possible to access the underlying font file. So a mapping Font -> PDFont is needed. At the 
moment there is a hook to implement that, but its not working yet. Also note that PDFBox has 
[problems](https://issues.apache.org/jira/browse/PDFBOX-3550) rendering RTL languages at the moment.

## Changes

Version 0.5:
 - Fixed getClip() and clip(Shape) handling. Both did not correctly handle transforms. This bug was
 exposed by Batik 1.9 and found by @ketanmpandya. Thanks @ketanmpandya [#2](https://github.com/rototor/pdfbox-graphics2d/pull/2), OpenHtmlToPdf [#99](https://github.com/danfickle/openhtmltopdf/issues/99)
 
Version 0.4:
 - Initial support for basic AlphaComposite. Thanks @FabioVassallo [#1](https://github.com/rototor/pdfbox-graphics2d/pull/1)
 - When drawing a shape with a zero or negative size don't use PDShadings, as they won't work.Thanks @FabioVassallo [#1](https://github.com/rototor/pdfbox-graphics2d/pull/1)

Version 0.3:
 - Fix for a NPE when calling setClip() with null.
 - Upgrade to PDFBox 2.0.5, replacing the usage of appendRawCommands() with setMiterLimit().

Version 0.2:
 - The paint applier (Mapping of java.awt.Paint to PDF) can be customized, so you can map special paints if needed.
 - Support for TexturePaint

## Licence

Licenced using the Apache Licence 2.0.
