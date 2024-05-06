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

package net.imagej.legacy.convert.roi.polygon2d;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import net.imagej.legacy.convert.roi.MaskPredicateWrapper;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.WritablePolygon2D;

/**
 * A {@link PolygonRoi} with an associated {@link WritablePolygon2D}.
 *
 * @author Alison Walter
 */
public final class Polygon2DWrapper extends PolygonRoi implements
	MaskPredicateWrapper<WritablePolygon2D>
{

	private final WritablePolygon2D polygon;

	public Polygon2DWrapper(final WritablePolygon2D p) {
		super(getCoordinates(p, 0), getCoordinates(p, 1), Roi.POLYGON);
		polygon = p;
	}

	// -- MaskPredicateWrapper methods --

	@Override
	public WritablePolygon2D getSource() {
		return polygon;
	}

	@Override
	public void synchronize() {
		final FloatPolygon fp = getFloatPolygon();

		if (polygon.numVertices() > nPoints) {
			while (polygon.numVertices() != nPoints)
				polygon.removeVertex(polygon.numVertices() - 1);
		}
		if (polygon.numVertices() < nPoints) {
			while (polygon.numVertices() != nPoints)
				polygon.addVertex(polygon.numVertices(), //
					RealPoint.wrap(new double[] { 0, 0 }));
		}

		for (int i = 0; i < polygon.numVertices(); i++) {
			polygon.vertex(i).setPosition(fp.xpoints[i], 0);
			polygon.vertex(i).setPosition(fp.ypoints[i], 1);
		}
	}

	// -- Helper methods --

	private static float[] getCoordinates(final WritablePolygon2D p,
		final int dim)
	{
		final float[] coor = new float[p.numVertices()];
		for (int i = 0; i < p.numVertices(); i++)
			coor[i] = p.vertex(i).getFloatPosition(dim);
		return coor;
	}

}
