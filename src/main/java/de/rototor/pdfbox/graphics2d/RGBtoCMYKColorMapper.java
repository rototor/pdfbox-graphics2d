package de.rototor.pdfbox.graphics2d;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceCMYK;

import java.awt.*;

/*
  Usage:

  PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(this.doc, (int)(width), (int)(height));
  PdfBoxGraphics2DColorMapper colorMapper = new RGBtoCMYKColorMapper();
  pdfBoxGraphics2D.setColorMapper(colorMapper);

 */


public class RGBtoCMYKColorMapper extends PdfBoxGraphics2DColorMapper {
    public PDColor mapColor(PDPageContentStream contentStream, Color rgbColor) {
        int r = rgbColor.getRed();
        int g = rgbColor.getGreen();
        int b = rgbColor.getBlue();
        int[] rgbInts = {r, g, b};
        float[] rgbFoats = rgbIntToFloat(rgbInts);
        float rf = rgbFoats[0];
        float gf = rgbFoats[1];
        float bf = rgbFoats[2];
        float[] cmykFloats = convertRGBToCMYK(rf, gf, bf);

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

    protected static float[] convertRGBToCMYK( float red, float green, float blue )
    {
        //
        // RGB->CMYK from From
        // http://en.wikipedia.org/wiki/Talk:CMYK_color_model
        //
        float c = 1.0f - red;
        float m = 1.0f - green;
        float y = 1.0f - blue;
        float k = 1.0f;

        k = Math.min( Math.min( Math.min( c,k ), m), y );
        if (k == 1.0){
            // avoid div by zero errors for pure black
            c = m = y = 0.0F;
        }
        else {
            c = ( c - k ) / ( 1 - k );
            m = ( m - k ) / ( 1 - k );
            y = ( y - k ) / ( 1 - k );
        }

        return new float[] { c,m,y,k };
    }
}

