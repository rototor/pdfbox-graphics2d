/*
 * Copyright 2017 Emmeran Seehuber

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class PdfBoxGraphics2DTestBase
{

    enum Mode
    {
        DefaultVectorized, FontTextIfPossible, ForceFontText, DefaultFontText
    }

    void exportGraphic(String dir, String name, GraphicsExporter exporter)
    {
        try
        {
            PDDocument document = new PDDocument();

            PDFont pdArial = PDType1Font.HELVETICA;

            File parentDir = new File("target/test/" + dir);
            // noinspection ResultOfMethodCallIgnored
            parentDir.mkdirs();

            int scale = 2;
            BufferedImage image = new BufferedImage(400 * scale, 400 * scale,
                    BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D imageGraphics = image.createGraphics();
            imageGraphics.scale(scale, scale);
            exporter.draw(imageGraphics);
            imageGraphics.dispose();
            ImageIO.write(image, "PNG", new File(parentDir, name + ".png"));

            for (Mode m : Mode.values())
            {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);
                PdfBoxGraphics2DFontTextDrawer fontTextDrawer = null;
                contentStream.beginText();
                contentStream.setStrokingColor(0f, 0f, 0f);
                contentStream.setNonStrokingColor(0f, 0f, 0f);
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 15);
                contentStream.setTextMatrix(Matrix.getTranslateInstance(10, 800));
                contentStream.showText("Mode " + m);
                contentStream.endText();
                switch (m)
                {
                case FontTextIfPossible:
                    fontTextDrawer = new PdfBoxGraphics2DFontTextDrawer();
                    registerFots(fontTextDrawer);
                    break;
                case DefaultFontText:
                {
                    fontTextDrawer = new PdfBoxGraphics2DFontTextDrawerDefaultFonts();
                    registerFots(fontTextDrawer);
                    break;
                }
                case ForceFontText:
                    fontTextDrawer = new PdfBoxGraphics2DFontTextForcedDrawer();
                    registerFots(fontTextDrawer);
                    fontTextDrawer.registerFont("Arial", pdArial);
                    break;
                case DefaultVectorized:
                default:
                    break;
                }

                if (fontTextDrawer != null)
                {
                    pdfBoxGraphics2D.setFontTextDrawer(fontTextDrawer);
                }

                exporter.draw(pdfBoxGraphics2D);
                pdfBoxGraphics2D.dispose();

                PDFormXObject appearanceStream = pdfBoxGraphics2D.getXFormObject();
                Matrix matrix = new Matrix();
                matrix.translate(0, 20);
                contentStream.transform(matrix);
                contentStream.drawForm(appearanceStream);

                matrix.scale(1.5f, 1.5f);
                matrix.translate(0, 100);
                contentStream.transform(matrix);
                contentStream.drawForm(appearanceStream);
                contentStream.close();
            }

            document.save(new File(parentDir, name + ".pdf"));
            document.close();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void registerFots(PdfBoxGraphics2DFontTextDrawer fontTextDrawer)
    {
        fontTextDrawer.registerFont(new File(
                "src/test/resources/de/rototor/pdfbox/graphics2d/DejaVuSerifCondensed.ttf"));
        fontTextDrawer.registerFont(new File(
                "src/test/resources/de/rototor/pdfbox/graphics2d/antonio/Antonio-Regular.ttf"));
    }

    interface GraphicsExporter
    {
        void draw(Graphics2D gfx) throws IOException, FontFormatException;
    }

}
