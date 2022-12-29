package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PageNofMExample
{
    @Test
    public void makePageNOfM() throws IOException
    {
        PDDocument document = new PDDocument();

        createPages(document);  //render all pages

        int totalNumberOfPages = document.getNumberOfPages(); //now we know the total number of pages and can draw the strings
        for (int i = 0; i < totalNumberOfPages; i++)
        {
            PDPage page = document.getPage(i);
            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true);
            PdfBoxGraphics2D pageNumberG = new PdfBoxGraphics2D(document, PDRectangle.A4.getWidth(),
                    PDRectangle.A4.getHeight());
            pageNumberG.setColor(java.awt.Color.BLACK);
            pageNumberG.drawString("This is page " + (i + 1) + " of " + totalNumberOfPages, 100,
                    120);
            pageNumberG.dispose();
            contentStream.drawForm(pageNumberG.getXFormObject());
            contentStream.close();
        }
        File parentDir = new File("target/test/");
        // noinspection ResultOfMethodCallIgnored
        parentDir.mkdirs();
        File targetPDF = new File(parentDir, "PageNofM.pdf");
        document.save(targetPDF);
        document.close();
    }

    static void createPages(PDDocument document) throws IOException
    {
        for (int i = 0; i < 100; i++)
        {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PdfBoxGraphics2D pageG = new PdfBoxGraphics2D(document, PDRectangle.A4.getWidth(),
                    PDRectangle.A4.getHeight());
            pageG.setColor(java.awt.Color.BLACK);
            pageG.drawString("Some text", 100, 100);
            pageG.drawString("Some more text", 100, 140);
            pageG.dispose();
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.drawForm(pageG.getXFormObject());
            contentStream.close();
        }
    }
}
