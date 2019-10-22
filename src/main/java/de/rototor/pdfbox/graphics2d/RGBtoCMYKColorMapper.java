package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;

import java.awt.*;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;

/*
  Usage:

  PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(this.doc, (int)(width), (int)(height));
  PdfBoxGraphics2DColorMapper colorMapper = new RGBtoCMYKColorMapper();
  pdfBoxGraphics2D.setColorMapper(colorMapper);

 */


public class RGBtoCMYKColorMapper extends PdfBoxGraphics2DColorMapper {
    ICC_ColorSpace icc_colorspace;

    public RGBtoCMYKColorMapper(ICC_Profile icc_profile){
        resetColorSpace(icc_profile);
    }

    public void resetColorSpace(ICC_Profile icc_profile){
        icc_colorspace = new ICC_ColorSpace(icc_profile);
    }

    public PDColor mapColor(PDPageContentStream contentStream, Color rgbColor) {
        int r = rgbColor.getRed();
        int g = rgbColor.getGreen();
        int b = rgbColor.getBlue();
        int[] rgbInts = {r, g, b};
        float[] rgbFoats = rgbIntToFloat(rgbInts);
        float[] cmykFloats = icc_colorspace.fromRGB(rgbFoats);

        PDColor cmykColor = new PDColor(cmykFloats, PDDeviceCMYK.INSTANCE);
        return cmykColor;
    }

    public static float[] rgbIntToFloat(int [] rgbInts){
        // the input ints are in the range 0 to 255
        // the output floats need to be in the range 0.0 to 1.0
        float red = (float) rgbInts[0] /  255.0F;
        float green = (float) rgbInts[1] / 255.0F;
        float blue = (float) rgbInts[2] / 255.0F;
        float [] rgbFloats = new float[] {red, green, blue};
        return rgbFloats;
    }

}

