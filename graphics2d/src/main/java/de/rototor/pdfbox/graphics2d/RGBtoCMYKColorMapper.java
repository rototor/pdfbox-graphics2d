package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;

import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Usage:
 *
 * <pre>
 * PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(this.doc, (int) (width), (int) (height));
 * PdfBoxGraphics2DColorMapper colorMapper = new RGBtoCMYKColorMapper(icc_profile);
 * pdfBoxGraphics2D.setColorMapper(colorMapper);
 * </pre>
 * <p>
 * Where ICC_Profile is an instance of java.awt.color.ICC_Profile that supports
 * a CMYK colorspace. For testing purposes, we're using ISOcoated_v2_300_bas.icc
 * which ships with PDFBox.
 */
public class RGBtoCMYKColorMapper extends PdfBoxGraphics2DColorMapper
{
    private final ICC_ColorSpace icc_colorspace;
    private final PDICCBased pdProfile;

    public RGBtoCMYKColorMapper(ICC_Profile icc_profile, PDDocument document) throws IOException
    {
        this.icc_colorspace = new ICC_ColorSpace(icc_profile);
        this.pdProfile = new PDICCBased(document);
        OutputStream outputStream = pdProfile.getPDStream()
                .createOutputStream(COSName.FLATE_DECODE);
        outputStream.write(icc_profile.getData());
        outputStream.close();
        pdProfile.getPDStream().getCOSObject().setInt(COSName.N, 4);
        pdProfile.getPDStream().getCOSObject().setItem(COSName.ALTERNATE, COSName.DEVICECMYK);
    }

    @Override
    public PDColor mapColor(Color rgbColor, IColorMapperEnv env)
    {
        int r = rgbColor.getRed();
        int g = rgbColor.getGreen();
        int b = rgbColor.getBlue();
        int[] rgbInts = { r, g, b };
        float[] rgbFoats = rgbIntToFloat(rgbInts);
        float[] cmykFloats = icc_colorspace.fromRGB(rgbFoats);
        return new PDColor(cmykFloats, pdProfile);
    }

    private static float[] rgbIntToFloat(int[] rgbInts)
    {
        // the input ints are in the range 0 to 255
        // the output floats need to be in the range 0.0 to 1.0
        float red = (float) rgbInts[0] / 255.0F;
        float green = (float) rgbInts[1] / 255.0F;
        float blue = (float) rgbInts[2] / 255.0F;
        return new float[] { red, green, blue };
    }

}
