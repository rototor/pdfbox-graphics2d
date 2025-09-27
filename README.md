[![Java CI with Maven](https://github.com/rototor/pdfbox-graphics2d/actions/workflows/maven.yml/badge.svg)](https://github.com/rototor/pdfbox-graphics2d/actions/workflows/maven.yml)
[![CodeQL](https://github.com/rototor/pdfbox-graphics2d/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/rototor/pdfbox-graphics2d/actions/workflows/codeql-analysis.yml)

# pdfbox-graphics2d

Graphics2D Bridge for Apache PDFBox

## Intro

Using this library you can use any Graphics2D API based SVG / graph / chart library to embed those
graphics as vector
drawing in a PDF. In combination with PDFBox PDFRenderer/PageDrawer you can also "rerender" PDF
pages and change certain aspects. E.g.
[change the color mapping and perform an overfill](graphics2d/src/test/java/de/rototor/pdfbox/graphics2d/PdfRerenderTest.java).
Now also it's possible to setup masking of all draw()/fill() calls. Think of this like an
additional clipping, just that this supports bitmap images and complete complex drawings (XForm's)
as alpha masks. See
[here](https://github.com/rototor/pdfbox-graphics2d/blob/master/graphics2d/src/test/java/de/rototor/pdfbox/graphics2d/TestMaskedDraws.java)
how that works.

The following features are supported:

- Drawing any shape using ```draw...()``` and ```fill...()``` methods from Graphics2D.
- Drawing images. The default is to always lossless compress them. You could plugin your
  own ```Image```
  -> ```PDImageXObject``` conversion if you want to encode the images as jpeg.
- All ```BasicStroke``` attributes.
- ```Paint```:
    - ```Color```. You can specify your own color mapping implementation to special map the (RGB)
      colors to ```PDColor```. Beside using CMYK colors you can also use spot colors.
    - ```GradientPaint```, ```LinearGradientPaint``` and ```RadialGradientPaint```. There are some
      restrictions:
        - ```GradientPaint``` always generates acyclic gradients.
    - ```TexturePaint```.
- Drawing text. By default, all text is drawn as vector shapes, so no fonts are embedded. RTL
  languages are supported.
  It's possible to use fonts, but this loses some features (especially RTL support)
  and you must provide the TTF files of the fonts if the default PDF fonts are not enough.

The following features are not supported (yet):

- ```(Alpha-)Composite``` with a rule different then ```AlphaComposite.SRC_OVER```.
- ```copyArea()```. This is not possible to implement.
- ```hit()```. Why would you want to use that?
- ```setXORMode()```. Their is no blend mode in PDF which would allow to emulate this, so this is
  not possible to be implemeted.

## Download

This library is available through Maven:

For PDFBox 2.0.x:

```xml

<dependency>
	<groupId>de.rototor.pdfbox</groupId>
	<artifactId>graphics2d</artifactId>
	<version>0.46</version>
</dependency>
```

This library targets Java 1.6 and should work with Java 1.6. But at the moment it is only tested
with Java 8, Java 11 and Java 17.

For PDFBox 3.0.x:

```xml

<dependency>
	<groupId>de.rototor.pdfbox</groupId>
	<artifactId>graphics2d</artifactId>
	<version>3.0.4</version>
</dependency>
```

This version targets Java 8. It should be identical to the 2.0.x version. If not, than thats a bug. The 3.0.x
version is maintained in the pdfbox-3.0.0 branch. For now, maintance is done in the 2.0.x branch and the merged into then
3.0.x branch.


## Example Usage

```java
public class PDFGraphics2DSample
{
    public static main(String[] argv)
    {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        /*
         * Creates the Graphics and sets a size in pixel. This size is used for the BBox of the XForm.
         * So everything drawn outside (0x0)-(width,height) will be clipped.
         */
        PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);

        /*
         * Now do your drawing. By default all texts are rendered as vector shapes
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

See
also [manual drawing](graphics2d/src/test/java/de/rototor/pdfbox/graphics2d/PdfBoxGraphics2dTest.java)
and [drawing SVGs](graphics2d/src/test/java/de/rototor/pdfbox/graphics2d/RenderSVGsTest.java). The
testdrivers are only
smoke tests, i.e. they don't explicit test the result, they just run and test if the their are
crashes. You have to
manually compare the PDF result of the testdriver with the also generated PNG compare image.

## Rendering text using fonts vs vectors

When rendering a text in a PDF file you can choose two methods:

- Render the text using a font as text.
- Render the text using TextLayout as vector graphics.

Rendering a text using a font is the normal and preferred way to display a text:

- The text can be copied and is searchable.
- Usually it takes less space then when using vector shapes.
- When printing in PrePress (Digital / Offset Print) the RIP usually handles text special to ensure
  the best possible
  reading experience. E.g. RGB Black is usually mapped to a black with some cyan. This gives a "
  deeper" black,
  especially if you have a large black area. But if you use a RGB black to render text it is usually
  mapped to pure
  black to avoid any printing registration mismatches, which would be very bad for reading the text.
- Note: When rendering a text using a font you should always embed the needed subset of the font
  into the PDF. Otherwise
  not every (=most) PDF viewers will be able to display the text correctly, if they don't have the
  font or have a
  different version of the font, which can happen across different OS and OS versions.
- Note: Not all PDF viewer can handle all fonts correctly. E.g. PDFBox 1.8 was not able to handle
  fonts right. But
  nowadays all PDF viewers should be able to handle fonts fine.
- Note: ```TextAttribute.LIGATURES``` is currently not supported.
- Note: ```TextAttribute.BACKGROUND``` is currently not supported.
- Note: There is no Bidi support at the moment. See
  the [problems](https://issues.apache.org/jira/browse/PDFBOX-3550)
  PDFBox has with rendering RTL languages at the moment.

On the other site rendering a text using vector shapes has the following properties:

- The text is always displayed the same. They will be no differences between the PDF viewers.
- The text is not searchable and can not be copied.
- Note: Vector shapes take more space than a embedded font.
- Note: You may want to manually alter the color mapping to e.g. ensure a black text is printed
  using pure CMYK black.
  If you do not plan to print the PDF in offset or digital print you can ignore that. This will make
  no difference for
  your normal desktop printer.
- Note: When using Apache Batik to draw SVGs the text will always be drawn as vector shape. Batik
  always converts texts
  to vector shapes first and then applies the transforms on it (if there are any). So
  PdfBoxGraphics2D never even gets a
  chance to draw the text using a PDF font. In theory, this could be solved by installing an
  appropriate text painter on
  the Batik bridge context. But no one has created such a text painter yet.

If you want to get a 1:1 mapping of your ```Graphics2D``` drawing in the PDF you should use the
vector mode. If you want
to have the text searchable and only use LTR languanges (i.e. latin-based)
you may try the text mode. For this mode to work you need the font files (.ttf / .ttc) of the fonts
you want to use and
must register it with this library. Using the normal Java font API it is not possible to access the
underlying font
file. So a manual mapping of Font to PDFont is needed.

### Example how to use the font mapping

The font mapping is done using the ```PdfBoxGraphics2DFontTextDrawer``` class. There you register
the fonts you have. By
default the mapping tries to only use fonts when all features used by the drawn text are supported.
If your text uses a
features which is not supported (e.g. RTL text) then it falls back to using vectorized text.

If you always want to force the use of fonts you can use the
class ```PdfBoxGraphics2DFontTextForcedDrawer```. But this
is unsafe and not recommend, because if some text can not be rendered using the given fonts it will
not be drawn at all
(e.g. if a font misses a needed glyph).

If you want to use the default PDF fonts as much as possible to have no embedded fonts you can use
the class
```PdfBoxGraphics2DFontTextDrawerDefaultFonts```. This class will always use a default PDF font, but
you can also
register additional fonts.

```java
public class PDFGraphics2DSample
{
    public static main(String[] argv)
    {
        /*
         * Document creation and init as in the example above
         */

        // ...

        /*
         * Register your fonts
         */
        PdfBoxGraphics2DFontTextDrawer fontTextDrawer = new PdfBoxGraphics2DFontTextDrawer();
        try
        {
            /*
             * Register the font using a file
             */
            fontTextDrawer.registerFont(new File("..path..to../DejaVuSerifCondensed.ttf"));

            /*
             * Or register the font using a stream
             */
            fontTextDrawer.registerFont(
                    PDFGraphics2DSample.class.getResourceAsStream("DejaVuSerifCondensed.ttf"));

            /*
             * You already have a PDFont in the document? Then make it known to the library.
             */
            fontTextDrawer.registerFont("My Custom Font", pdMyCustomFont);


            /*
             * Create the graphics
             */
            PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);

            /*
             * Set the fontTextDrawer on the Graphics2D. Note:
             * You can and should reuse the PdfBoxGraphics2DFontTextDrawer
             * within the same PDDocument if you use multiple PdfBoxGraphics2D.
             */
            pdfBoxGraphics2D.setFontTextDrawer(fontTextDrawer);

            /* Do you're drawing */

            /*
             * Dispose when finished
             */
            pdfBoxGraphics2D.dispose();

            /*
             * Use the result as above
             */
            // ...
        }
        finally
        {
            /*
             * If you register a font using a stream then a tempfile
             * will be created in the background.
             * Close the PdfBoxGraphics2DFontTextDrawer to free any
             * tempfiles created for the fonts.
             */
            fontTextDrawer.close();
        }

    }
}
```

You can also complete customize the font mapping if you derive
from ```PdfBoxGraphics2DFontTextDrawer```:

```java
class MyPdfBoxGraphics2DFontTextDrawer extends PdfBoxGraphics2DFontTextDrawer
{
    @Override
    protected PDFont mapFont(Font font, IFontTextDrawerEnv env)
            throws IOException, FontFormatException
    {
        // Using the font, especially the font.getFontName() or font.getFamily() to determine which
        // font to use... return null if the font can not be mapped. You can also call registerFont() here.

        // Default lookup in the registered fonts
        return super.mapFont(font, env);
    }
}
```

This allows you to load the fonts on demand.

## Compression

By default the content stream data is compressed using the zlib default level 6. If you want to get
the maximum
compression out of PDFBox you should set a system property before generating your PDF:

```java
    System.setProperty(Filter.SYSPROP_DEFLATELEVEL,"9");
```

## Creating PDF reports

If you want to create complex PDF reports with text and graphs mixed it is recommend to not use
PDFBox and this library
directly, as both are very low level. Instead you should use
[OpenHtmlToPdf](https://github.com/danfickle/openhtmltopdf). OpenHtmlToPdf allows you to build your
reports using HTML (
which you can generate with any template engine you like, e.g. Apache FreeMarker) and place custom
graphs
(which are draw using Graphics2D using this library) with &lt;object&gt; HTML tags.

## Changes

### PDFBox 3.x based version 
Version 3.0.5:
- Update to PDFBox 3.0.5
- Fix for out of bounds exception on transparent gradient when keyframes are missing on both ends.  
  Thanks to @ganomi. See [#64](https://github.com/rototor/pdfbox-graphics2d/pull/64)

Version 3.0.3:
- Update to PDFBox 3.0.4
- Add osgi metadata. Thanks to @zspitzer. [#61](https://github.com/rototor/pdfbox-graphics2d/pull/61)

Version 3.0.2:
- Update to PDFBox 3.0.2

Version 3.0.1:
- Update to PDFBox 3.0.1
- Use now protected constructor to access the PDCloneUtility.  [#56](https://github.com/rototor/pdfbox-graphics2d/issues/56)

Version 3.0.0:
- Initial release based on PDFBox 3.0
- Same as PDFBox 2.x version, just ported over to the changed PAI.

---
### PDFBox 2.x based version

Version 0.46:
- Upgraded PDFBox to 2.0.34
- Fix for out of bounds exception on transparent gradient when keyframes are missing on both ends.  
  Thanks to @ganomi. See [#64](https://github.com/rototor/pdfbox-graphics2d/pull/64)

Version 0.45:
- Upgraded PDFBox to 2.0.33
- Partial fix for font text rendering. Thanks to @fransbouwmans. See [#53](https://github.com/rototor/pdfbox-graphics2d/pull/53)

Version 0.44:
- Upgraded PDFBox to 2.0.31
- Added additional font test driver by @fransbouwmans

Version 0.43:

- Upgraded PDFBox to 2.0.28
- [#50](https://github.com/rototor/pdfbox-graphics2d/issues/50): Use the Java logger API instead of System.err.
Thanks @pmds-martins for the PR.

Version 0.42:

- Upgraded PDFBox to 2.0.27
- [#46](https://github.com/rototor/pdfbox-graphics2d/issues/46): Also override drawRect() and use a
  Rectangle with drawShape(). Thanks @fransbouwmans for the report.
- [#40](https://github.com/rototor/pdfbox-graphics2d/issues/40): Correctly implement image
  interpolation and respect the chosen interpolation when caching an image. NOTE: This is a 
  API breaking change on the IPdfBoxGraphics2DImageEncoder. So if you have implemented this interface
  you need to adapt to the new signature (env parameter).

Version 0.41:

- #45 Copy & paste error in drawImage() call forwarding. sy1 should be passed for sy1, not sy2...
  Thanks @fransbouwmans for pointing this out. This affected one
  specifc drawImage() overload.

Version 0.40:

- Messed up the access permissions
  for `PdfBoxGraphics2DPaintApplier.PaintApplierState::setupLuminosityMasking`. They are
  now accessible.

Version 0.39:

- Extended the `PdfBoxGraphics2DPaintApplier` to allow overriding
  `PdfBoxGraphics2DPaintApplier::applyPaint(Paint, PdfBoxGraphics2DPaintApplier.PaintApplierState)`.
- Made a `PdfBoxGraphics2DPaintApplier.PaintApplierState::setupLuminosityMasking`, which allows to
  setup a masking for the next fill / draw operations. You can mask all fill / draw operations using
  a bitmap or XForm based mask.
- Initial basic support for opacity in gradients. At the moment this does not correctly work for
  every case. More work is needed here. Thanks @iocoker for bringing this
  up. ([#37](https://github.com/rototor/pdfbox-graphics2d/issues/37))

Version 0.38:

- Fix for line width of stroks in the case there is a rotation transform on the Graphics2D. Thanks
  @jonmccracken-wf for
  reporting and fixing this bug ([#36](https://github.com/rototor/pdfbox-graphics2d/pull/36))
- Upgrade to PDFBox 2.0.26

Version 0.37:

- Make PdfBoxGraphics2DFontTextDrawer.getFontMetrics::stringWidth() behave like the
  JDK. Thanks @Lorgod ([#35](https://github.com/rototor/pdfbox-graphics2d/issues/35))

Version 0.36:

- API breakage: Changed the `IPdfBoxGraphics2DColorMapper::mapColor()` signatur to get a
  IColorMapperEnv instead of a
  PDPageContentStream. The env provides access to the `PDPageContentStream` and the `PDResources`
- It is now possible to get the associated PDResources by using `PdfBoxGraphics2D::getResources()`.

Version 0.35:

- Temporary workaround for [PDFBOX-5361](https://issues.apache.org/jira/browse/PDFBOX-5361). PDFBOX
  currently sets the OverprintMode (/OPM) in the
  Extended Graphics State as float - which is a spec violation.
- Fixed some dependency bugs in the extended tests, thanks
  @pgrt ([#33](https://github.com/rototor/pdfbox-graphics2d/issues/33),
  [#34](https://github.com/rototor/pdfbox-graphics2d/issues/34)).

Version 0.34:

- Per default specify the rendering
  hint ```RenderingHints.KEY_FRACTIONALMETRICS=RenderingHints.VALUE_FRACTIONALMETRICS_ON```.
  We always render into a vector space, so we don't have to round to ints while drawing text. Thanks
  to @paatero for bringing this up ([#32](https://github.com/rototor/pdfbox-graphics2d/issues/32)).
- Upgrade to PDFBox 2.0.25
- It is not allowed to make state changes after defining a path and before clipping / drawing, at
  least acording to the PDF specification
  [PDFBOX-5322](https://issues.apache.org/jira/browse/PDFBOX-5322). Fixed the ordering here.

Version 0.33:

- Don't crash when drawString() is called with an empty
  string [#31](https://github.com/rototor/pdfbox-graphics2d/issues/31).
  Thanks @Kischloren for the report.

Version 0.32:

- It is now possible to draw within a marked content sequence. PdfBoxGraphics2D got a new  
  drawInMarkedContentSequence() method for this. This is usefull if you want to mark some parts of a
  drawing so that you
  can later do some special processing on it. Or if you simply want to provide  
  accesibility information for the content you draw. See also
  e.g. https://www.w3.org/TR/WCAG20-TECHS/PDF21.html or the
  section 14.8.4.3.3 in the PDF specification.
- Upgrade to PDFBox 2.0.24

Version 0.31:

- Support for colors with overprint.
- New PdfBoxGraphics2DColor class to allow using any kind of color. E.g. PDSeperation based colors.

Version 0.30:

- Clip invalid miter limit values [#29](https://github.com/rototor/pdfbox-graphics2d/issues/29).
  Thanks to @kiwiwings
  for reporting this.
- Added a new module for extended-tests. This module will contain tests with 3rdparty library which
  by themself depend
  on pdfbox-graphics2d. It also now contains a new class DebugCodeGeneratingGraphics2d
  (by @kiwiwings) which helps to create isolated testcases.
- Upgrade to PDFBox 2.0.22

Version 0.29:

- Fix a bug where the AlphaComposite alpha value would be mixed with a color alpha value when
  drawing images. When
  setting a transparent color this had resulted in a invisible image. Thanks to @kiwiwings for
  reporting this.
- Initial support for ```TextAttribute.UNDERLINE``` and ```Textattribute.STRIKETHROUGH``` when using
  a font to render a
  text.

Version 0.28:

- Fix handling of AttributedString (off-by-one error). Thanks for @kiwiwings for pointing out the
  error and providing a
  fix [#27](https://github.com/rototor/pdfbox-graphics2d/issues/27).
- Upgrade to PDFBox 2.0.21
- Respect that default fonts may not allow to be embedded. (PDFBox 2.0.21 now respects the flags
  within a TTF font, so
  we also must do this)
- When painting an image with an AlphaComposite the alpha is now respected correctly.

Version 0.27:

- Internal API breakage to implement getFontMetrics().stringWidth() correctly in the case a PDFont
  is used to draw the
  text [#16](https://github.com/rototor/pdfbox-graphics2d/issues/16). Thanks to @megri for reporting
  this problem.
- Reverted back to PDFBox 2.0.19 because of rendering
  issues [PDFBOX-4886](https://issues.apache.org/jira/browse/PDFBOX-4886).

Version 0.26:

- Added
  a [CMYK color mapper](graphics2d/src/main/java/de/rototor/pdfbox/graphics2d/RGBtoCMYKColorMapper.java)
  , which
  converts the paint colors to CMYK using an ICC Profile. Thanks to @larrylynn-wf for providing this
  feature [#22](https://github.com/rototor/pdfbox-graphics2d/issues/22).
- Upgrade to PDFBox 2.0.20
- Initial support
  for [Apache PDFBox TilingPaint](https://github.com/rototor/pdfbox-graphics2d/pull/25). Thanks to
  @p1xel. Currently this is not clean and also not correct in many cases.

Version 0.25:

- Upgrade to PDFBox 2.0.17
- Correctly handle GradientPaint fractions.
- Correctly handle SVG LinearGradientPaint's in ObjectBoundingBox
  mode [#19](https://github.com/rototor/pdfbox-graphics2d/issues/19). Thanks to @larrylynn-wf for
  the report and the
  idea how to fix it.
- Internal API breakage to support non quadratic SVG gradients
  correctly [#19](https://github.com/rototor/pdfbox-graphics2d/issues/19).

Version 0.24:

- Upgrade to PDFBox 2.0.16

Version 0.23:

- Correctly handle even odd winding rules when clipping and filling shapes.

Version 0.22:

- Upgrade the PDFBox version to 2.0.15

Version 0.21:

- Provide the current XORMode color in the IPaintEnv. And document that XORMode is not working as
  it's not possible to
  emulate. Thanks @gredler for pointing this
  out [#14](https://github.com/rototor/pdfbox-graphics2d/issues/14). But you
  can do whatever you want with that information in your IPdfBoxGraphics2DPaintAplier subclass.
- Upgrade the PDFBox version to 2.0.14
- Handle PDFBox ShadingPaint's.

Version 0.20:

- Handle null transforms in drawImage() correctly. I.e. dont throw a NullPointerException, just
  ignore the not existing
  transform.
- Cache the different environments for the mapper/drawer/applier. This is a minor memory saving.

Version 0.19:

- You can now influence the shape fill/draw operations by setting a custom
  IPdfBoxGraphics2DDrawControl. This allows to
  do different things like e.g. draw an overfill for shapes (i.e. make shapes have a additional
  border). This can be
  useful if you need to preprocess a PDF for pre-press.

Version 0.18:

- setPaint(null) will cause the following fillXXX() and drawXXX() operations to be ignored. This
  allows in combination
  with PDFRenderer/PageDrawer to extract parts of a PDF page. E.g. you can draw only certain
  seperation colors into the
  resulting PDF if you filter the paints in PageDrawer.getPaint() and extract a seperation color
  from a PDF in that way.
- New class PdfBoxGraphics2DCMYKColor() which derives from java.awt.Color to be able to specify a
  CMYK color when
  painting.
- The default PdfBoxGraphics2DColorMapper now also supports mapping of "legacy" old iText 2
  CMYKColor's.

Version 0.17:

- Upgrade the PDFBox version to 2.0.12

Versoin 0.16:

- Added new method ```disposeDanglingChildGraphics()``` to cleanup all dangling child graphics. This
  allows to use this
  graphics adapter with old legacy code which does not correctly call ```dispose()``` on the
  graphics it used.

Version 0.15:

- Upgrade the PDFBox version to 2.0.11

Version 0.14:

- Don't write invalid path commands into the stream, as this will break rendering in Acrobat Reader.
  Thanks
  @FabioVassallo [#12](https://github.com/rototor/pdfbox-graphics2d/pull/12)

Version 0.13:

- Ugraded the PDFBox version to 2.0.9

Version 0.12:

- Don't share resources between XForm's, as Acrobat Reader does not like that.

Version 0.11:

- Support Batik SVG PatternPaint. Thanks @vipcxj
  for [pointing this out and providing a testfile](https://github.com/danfickle/openhtmltopdf/issues/170#issuecomment-362294830)
  .
- Compress embedded image ICC Profile Data

Version 0.10:

- Don't export the same extended graphics state over and over again. Same for
  shadings. [#8](https://github.com/rototor/pdfbox-graphics2d/issues/8)

Version 0.9:

- Compress the content stream generated for the XForm.
- When drawing the same image multiple times, it is only encoded once now.

Version 0.8:

- Implemented ```PdfBoxGraphics2DFontTextDrawerDefaultFonts``` to allow preferring default PDF fonts
  over vectorized
  text [#5](https://github.com/rototor/pdfbox-graphics2d/issues/5).

Version 0.7:

- Bugfixes on the font based text support. Now also gradients can be used to paint text.

Version 0.6:

- Implemented basic support for using fonts to render texts.

Version 0.5:

- Fixed ```getClip()``` and ```clip(Shape)``` handling. Both did not correctly handle transforms.
  This bug was exposed
  by Batik 1.9 and found by @ketanmpandya. Thanks
  @ketanmpandya [#2](https://github.com/rototor/pdfbox-graphics2d/pull/2),
  OpenHtmlToPdf [#99](https://github.com/danfickle/openhtmltopdf/issues/99)

Version 0.4:

- Initial support for basic ```AlphaComposite```. Thanks
  @FabioVassallo [#1](https://github.com/rototor/pdfbox-graphics2d/pull/1)
- When drawing a shape with a zero or negative size don't use ```PDShadings```, as they won't
  work. Thanks
  @FabioVassallo [#1](https://github.com/rototor/pdfbox-graphics2d/pull/1)

Version 0.3:

- Fix for a NPE when calling ```setClip()``` with null.
- Upgrade to PDFBox 2.0.5, replacing the usage of ```appendRawCommands()```
  with ```setMiterLimit()```.

Version 0.2:

- The paint applier (Mapping of ```java.awt.Paint``` to PDF) can be customized, so you can map
  special paints if needed.
- Support for ```TexturePaint```

## Licence

Licenced using the Apache Licence 2.0.
