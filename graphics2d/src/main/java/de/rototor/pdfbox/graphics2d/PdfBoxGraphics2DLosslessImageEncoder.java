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
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDICCBased;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Encodes all images using lossless compression. Tries to reuse images as much
 * as possible. You can share an instance of this class with multiple
 * PdfBoxGraphics2D objects.
 */
public class PdfBoxGraphics2DLosslessImageEncoder implements IPdfBoxGraphics2DImageEncoder
{
    private Map<ImageSoftReference, SoftReference<PDImageXObject>> imageMap = new HashMap<ImageSoftReference, SoftReference<PDImageXObject>>();
    private Map<ProfileSoftReference, SoftReference<PDColorSpace>> profileMap = new HashMap<ProfileSoftReference, SoftReference<PDColorSpace>>();
    private SoftReference<PDDocument> doc;

    @Override
    public PDImageXObject encodeImage(PDDocument document, PDPageContentStream contentStream,
            Image image, IPdfBoxGraphics2DImageEncoderEnv env)
    {
        final BufferedImage bi;

        if (image instanceof BufferedImage)
        {
            bi = (BufferedImage) image;
        }
        else
        {
            int width = image.getWidth(null);
            int height = image.getHeight(null);
            bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics graphics = bi.getGraphics();
            if (!graphics.drawImage(image, 0, 0, null, null))
                throw new IllegalStateException("Not fully loaded images are not supported.");
            graphics.dispose();
        }

        try
        {
            if (doc == null || doc.get() != document)
            {
                imageMap = new HashMap<ImageSoftReference, SoftReference<PDImageXObject>>();
                profileMap = new HashMap<ProfileSoftReference, SoftReference<PDColorSpace>>();
                doc = new SoftReference<PDDocument>(document);
            }
            SoftReference<PDImageXObject> pdImageXObjectSoftReference = imageMap.get(
                    new ImageSoftReference(image, env.getImageInterpolation()));
            PDImageXObject imageXObject =
                    pdImageXObjectSoftReference == null ? null : pdImageXObjectSoftReference.get();
            if (imageXObject == null)
            {
                imageXObject = LosslessFactory.createFromImage(document, bi);

                /*
                 * Do we have a color profile we need to embed?
                 */
                if (bi.getColorModel().getColorSpace() instanceof ICC_ColorSpace)
                {
                    ICC_Profile profile = ((ICC_ColorSpace) bi.getColorModel()
                            .getColorSpace()).getProfile();
                    /*
                     * Only tag a profile if it is not the default sRGB profile.
                     */
                    if (((ICC_ColorSpace) bi.getColorModel().getColorSpace()).getProfile()
                            != ICC_Profile.getInstance(ColorSpace.CS_sRGB))
                    {

                        SoftReference<PDColorSpace> pdProfileRef = profileMap.get(
                                new ProfileSoftReference(profile));

                        /*
                         * We try to reduce the copies of the same ICC profile in the PDF file. If the
                         * image already has a profile, it will be the right one. Otherwise we must
                         * assume that the image is now in sRGB.
                         */
                        PDColorSpace pdProfile = pdProfileRef == null ? null : pdProfileRef.get();
                        if (pdProfile == null)
                        {
                            pdProfile = imageXObject.getColorSpace();
                            if (pdProfile instanceof PDICCBased)
                            {
                                profileMap.put(new ProfileSoftReference(profile),
                                        new SoftReference<PDColorSpace>(pdProfile));
                            }
                        }
                        imageXObject.setColorSpace(pdProfile);
                    }
                }
                imageMap.put(new ImageSoftReference(image, env.getImageInterpolation()),
                        new SoftReference<PDImageXObject>(imageXObject));
            }

            imageXObject.setInterpolate(env.getImageInterpolation()
                    == IPdfBoxGraphics2DImageEncoderEnv.ImageInterpolation.Interpolate);

            return imageXObject;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not encode Image", e);
        }
    }

    private static class ImageSoftReference extends SoftReference<Image>
    {
        private final IPdfBoxGraphics2DImageEncoderEnv.ImageInterpolation interpolation;

        ImageSoftReference(Image referent,
                IPdfBoxGraphics2DImageEncoderEnv.ImageInterpolation interpolation)
        {
            super(referent);
            this.interpolation = interpolation;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            assert obj instanceof ImageSoftReference;
            return ((ImageSoftReference) obj).get() == get()
                    && ((ImageSoftReference) obj).interpolation == interpolation;
        }

        @Override
        public int hashCode()
        {
            Image image = get();
            if (image == null)
                return 0;
            return image.hashCode() ^ interpolation.hashCode();
        }
    }

    private static class ProfileSoftReference extends SoftReference<ICC_Profile>
    {
        ProfileSoftReference(ICC_Profile referent)
        {
            super(referent);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            assert obj instanceof ProfileSoftReference;
            return ((ProfileSoftReference) obj).get() == get();
        }

        @Override
        public int hashCode()
        {
            ICC_Profile image = get();
            if (image == null)
                return 0;
            return image.hashCode();
        }
    }
}
