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
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;

/**
 * Encode and compress an image as PDImageXObject
 */
public interface IPdfBoxGraphics2DImageEncoder
{
    /**
     * Environment for image encoding
     */
    interface IPdfBoxGraphics2DImageEncoderEnv
    {

        /*
         * What kind of image interpolations are possible?
         */
        enum ImageInterpolation
        {
            /*
             * "Pixel Art" rendering.
             */
            NearestNeigbor,  //
            /*
             * Interpolate the image. What algorithmus is used depends on the PDF viewer.
             */
            Interpolate
        }

        /**
         * @return the RenderingHints.KEY_INTERPOLATION value mapped to the Interpolation enum
         */
        ImageInterpolation getImageInterpolation();
    }

    /**
     * Encode the given image into the a PDImageXObject
     *
     * @param document      the PDF document
     * @param contentStream the content stream of the page
     * @param image         the image to encode
     * @return the encoded image
     */
    PDImageXObject encodeImage(PDDocument document, PDPageContentStream contentStream, Image image,
            IPdfBoxGraphics2DImageEncoderEnv env);
}
