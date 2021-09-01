////////////////////////////////////////////////////////////////////////////////
//
//  Location - An Android location app.
//
//  Copyright (C) 2015	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.buses;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

// CopyrightOverlay
public class CopyrightOverlay extends Overlay
{
    private Paint paint;
    private String copyright;
    private final DisplayMetrics dm;

    protected int xOffset = 10;
    protected int yOffset = 10;

    protected boolean alignBottom = false;
    protected boolean alignRight  = false;
    
    // Constructor
    public CopyrightOverlay(Context context, int id)
    {
	super();

	// Get the string
	Resources resources = context.getResources();
	copyright = resources.getString(id);

	// Get the display metrics
	dm = resources.getDisplayMetrics();

	// Get paint
	paint = new Paint();
	paint.setAntiAlias(true);
 	paint.setTextSize(dm.density * 12);
   }

    // Set text size
    public void setTextSize(int fontSize)
    {
        paint.setTextSize(dm.density * fontSize);
    }

    // Set text colour
    public void setTextColor(int color)
    {
        paint.setColor(color);
    }

    // Set alignBottom
    public void setAlignBottom(boolean alignBottom)
    {
	this.alignBottom = alignBottom;
    }

    // Set alignRight
    public void setAlignRight(boolean alignRight)
    {
	this.alignRight = alignRight;
    }

    public void setOffset(final int x, final int y)
    {
        xOffset = x;
        yOffset = y;
    }

    // Draw
    @Override
    public void draw(Canvas canvas, MapView map, boolean shadow)
    {
	int width = canvas.getWidth();
	int height = canvas.getHeight();

	float x = 0;
	float y = 0;

	if (shadow)
	    return;

	if (map.isAnimating())
            return;

	if (alignRight)
	{
	    x = width - xOffset;
	    paint.setTextAlign(Paint.Align.RIGHT);
	}

	else
	{
	    x = xOffset;
	    paint.setTextAlign(Paint.Align.LEFT);
	}

	if (alignBottom)
	    y = height - yOffset;

	else
	    y = paint.getTextSize() + yOffset;

	// Draw the text
	canvas.drawText(copyright, x, y, paint);
    }
}
