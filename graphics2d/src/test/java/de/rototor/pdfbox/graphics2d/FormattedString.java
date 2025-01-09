package de.rototor.pdfbox.graphics2d;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

/*
 * The FormattedString class takes a markedup string and turns it into an AttributedString
 * Markup is in the format of tags in the form <tag>...</tag>
 * The format tags supported are:
 * b,strong: bold
 * i,em: italic
 * s,del: strikethrough
 * u,ins: underline
 * sup: superscript
 * sub: subscript
 * rot
 */
public class FormattedString
{
	private final static char ESCAPE_CHAR = '\\';

    private class Markup
    {
		final int start;
		final String mark;
		final Attribute attribute;
		final Object value;
		int end = -1;

        Markup(String mark, int start)
        {
			this.mark = removeValue(mark);
			this.start = start;
            if ("b".equals(mark) || "strong".equals(mark))
            {
				attribute = TextAttribute.WEIGHT;
				value = Float.valueOf(2.0F);
            }
            else if ("i".equals(mark) || "em".equals(mark))
            {
				attribute = TextAttribute.POSTURE;
				value = TextAttribute.POSTURE_OBLIQUE;
            }
            else if ("s".equals(mark) || "del".equals(mark))
            {
				attribute = TextAttribute.STRIKETHROUGH;
				value = TextAttribute.STRIKETHROUGH_ON;
            }
            else if ("u".equals(mark) || "ins".equals(mark))
            {
				attribute = TextAttribute.UNDERLINE;
				value = TextAttribute.UNDERLINE_ON;
            }
            else if ("sup".equals(mark))
            {
				attribute = TextAttribute.SUPERSCRIPT;
				value = TextAttribute.SUPERSCRIPT_SUPER;
            }
            else if ("sub".equals(mark))
            {
				attribute = TextAttribute.SUPERSCRIPT;
				value = TextAttribute.SUPERSCRIPT_SUB;
            }
            else if (mark.startsWith("size="))
            {
				float size = getFloatValueFromMark(mark, 10.0F);
				attribute = TextAttribute.SIZE;
				value = Float.valueOf(size);
            }
            else if (mark.startsWith("color="))
            {
				attribute = TextAttribute.FOREGROUND;
				value = getColorValueFromMark(mark);
            }
            else if (mark.startsWith("bg-color="))
            {
				attribute = TextAttribute.BACKGROUND;
				value = getColorValueFromMark(mark);
            }
            else if (mark.startsWith("rot="))
            {
                attribute = TextAttribute.TRANSFORM;
                float angle = getFloatValueFromMark(mark, 0.0F);
                value = new TransformAttribute(
                        AffineTransform.getRotateInstance(Math.toRadians(angle)));
            }
            else
            {
				attribute = null;
				value = null;
			}
		}

        private Color getColorValueFromMark(String tag)
        {
			Color color = Color.black;
			int p = tag.indexOf("=");
            if (p > 0)
            {
				String value = tag.substring(p + 1);
                if (value.startsWith("#"))
                {
                    try
                    {
						int iColor = Integer.parseUnsignedInt(value.substring(1), 16);
						color = new Color(iColor);
                    }
                    catch (Exception e)
                    {
						// LOGGER
					}
                }
                else if ("red".equals(value))
                {
					color = Color.red;
                }
                else if ("green".equals(value))
                {
					color = Color.green;
                }
                else if ("blue".equals(value))
                {
					color = Color.blue;
				}
			}
			return color;
		}

        private String removeValue(String mark)
        {
			int p = mark.indexOf("=");
            if (p > 0)
            {
				mark = mark.substring(0, p);
			}
			return mark;
		}

        public float getFloatValueFromMark(String mark, float defaultValue)
        {
			float value = defaultValue;
			int p = mark.indexOf("=");
            if (p > 0)
            {
				String sValue = mark.substring(p + 1);
                try
                {
					value = Float.parseFloat(sValue);
                }
                catch (Exception e)
                {
					// LOGGER?
				}
			}
			return value;
		}

        boolean isClosed()
        {
			return end >= start;
		}
	}

	private String formattedString;
	private List<Markup> markups;
	private int i;
	private StringBuilder str;

    public FormattedString(String formattedString)
    {
        markups = new ArrayList<Markup>();
		this.formattedString = formattedString;
		str = new StringBuilder(formattedString.length());
		i = 0;
		boolean escape = false;
        while (i < formattedString.length())
        {
			char c = formattedString.charAt(i);
			i++;
            if (escape)
            {
				str.append(c);
				escape = false;
            }
            else if (c == ESCAPE_CHAR)
            {
				escape = true;
            }
            else
            {
                if (c == '<')
                {
					findMarkup();
                }
                else
                {
					// no escape and no start of tag
					str.append(c);
				}
			}
		}
		finalizeMarkups();
	}

    private void findMarkup()
    {
        if (formattedString.length() > i + 1)
        {
            if (formattedString.charAt(i) == '/')
            {
				i++;
				String mark = parseTag();
				closeMark(mark);
            }
            else
            {
				String mark = parseTag();
				openMark(mark);
			}
		}
	}

    private void openMark(String mark)
    {
        if (mark != null && !mark.isEmpty())
        {
			markups.add(new Markup(mark, str.length()));
		}
	}

    private void closeMark(String mark)
    {
        for (int j = markups.size() - 1; j >= 0; j--)
        {
			Markup markup = markups.get(j);
            if (!markup.isClosed())
            {
                if (markup.mark.equals(mark))
                {
					markup.end = str.length();
					return;
				}
			}
		}
	}

    private String parseTag()
    {
		String tag = "";
        while (i < formattedString.length())
        {
            if (formattedString.charAt(i) == '>')
            {
				i++;
				return tag;
            }
            else
            {
				tag += formattedString.charAt(i);
				i++;
			}
		}
		str.append(tag);
		return null;
	}

    public AttributedString getAttributedString()
    {
		AttributedString attributedString = new AttributedString(str.toString());
        for (Markup markup : markups)
        {
            if (markup.attribute != null)
            {
                attributedString.addAttribute(markup.attribute, markup.value, markup.start,
                        markup.end);
			}
		}
		return attributedString;
	}

    private void finalizeMarkups()
    {
        for (Markup markup : markups)
        {
            if (markup.end < markup.start)
            {
				markup.end = str.length();
			}
		}
	}

    public String getText()
    {
		return str.toString();
	}
}
