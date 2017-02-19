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

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;

import java.awt.*;

/**
 * Map Color to PDColor
 */
public interface IPdfBoxGraphics2DColorMapper {
	/**
	 * Map the given Color to a PDColor
	 * 
	 * @param contentStream
	 *            the content stream
	 * @param color
	 *            the color to map
	 * @return the mapped color
	 */
	PDColor mapColor(PDPageContentStream contentStream, Color color);
}
