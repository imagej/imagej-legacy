/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2024 ImageJ2 developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.legacy.convert.roi.point;

import ij.gui.PointRoi;

import java.util.Iterator;

import net.imagej.legacy.convert.roi.MaskPredicateWrapper;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.WritableRealPointCollection;
import net.imglib2.roi.util.RealLocalizableRealPositionable;
import net.imglib2.roi.util.RealLocalizableRealPositionableWrapper;

import org.scijava.util.FloatArray;

/**
 * A {@link PointRoi} with an associated {@link WritableRealPointCollection}.
 *
 * @author Alison Walter
 */
public final class RealPointCollectionWrapper extends PointRoi implements
	MaskPredicateWrapper<WritableRealPointCollection<RealLocalizableRealPositionable>>
{

	private final WritableRealPointCollection<RealLocalizableRealPositionable> rpc;
	private int numPoints;

	public RealPointCollectionWrapper(
		final WritableRealPointCollection<RealLocalizableRealPositionable> p)
	{
		super(getCoors(p, 0), getCoors(p, 1), countPoints(p));
		numPoints = getFloatPolygon().npoints;
		rpc = p;
	}

	// -- MaskPredicateWrapper methods --

	@Override
	public WritableRealPointCollection<RealLocalizableRealPositionable>
		getSource()
	{
		return rpc;
	}

	@Override
	public void synchronize() {
		// Check if points were added
		if (getNCoordinates() > numPoints) {
			while (getNCoordinates() != numPoints) {
				rpc.addPoint(new RealLocalizableRealPositionableWrapper<>(new RealPoint(
					2)));
				numPoints++;
			}
		}
		// Check if points were removed
		if (getNCoordinates() < numPoints) {
			while (getNCoordinates() != numPoints) {
				rpc.removePoint(rpc.points().iterator().next());
				numPoints--;
			}
		}

		// Update point locations
		final Iterator<RealLocalizableRealPositionable> itr = rpc.points()
			.iterator();
		final float[] xCoor = getContainedFloatPoints().xpoints;
		final float[] yCoor = getContainedFloatPoints().ypoints;
		for (int i = 0; i < numPoints; i++)
			itr.next().setPosition(new float[] { xCoor[i], yCoor[i] });
	}

	// -- Helper methods --

	private static <L extends RealLocalizable> float[] getCoors(
		final WritableRealPointCollection<RealLocalizableRealPositionable> rpc,
		final int d)
	{
		final FloatArray coor = new FloatArray();
		final Iterator<RealLocalizableRealPositionable> itr = rpc.points()
			.iterator();
		while (itr.hasNext())
			coor.addValue(itr.next().getFloatPosition(d));

		return coor.getArray();
	}

	private static <L extends RealLocalizable> int countPoints(
		final WritableRealPointCollection<RealLocalizableRealPositionable> rpc)
	{
		final Iterator<RealLocalizableRealPositionable> itr = rpc.points()
			.iterator();
		int count = 0;
		while (itr.hasNext()) {
			itr.next();
			count++;
		}

		return count;
	}

}
