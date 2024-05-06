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

package net.imagej.legacy.convert.roi.line;

import net.imagej.legacy.convert.roi.IJRealRoiWrapper;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.GeomMaths;
import net.imglib2.roi.geom.real.Line;

/**
 * Wraps an ImageJ 1.x {@link ij.gui.Line Line} as an ImgLib2 {@link Line}.
 * <p>
 * This implementation does not support lines with widths.
 * </p>
 *
 * @author Alison Walter
 */
public class IJLineWrapper implements IJRealRoiWrapper<ij.gui.Line>, Line {

	private final ij.gui.Line line;

	/**
	 * Creates a new ImageJ 1.x {@link ij.gui.Line Line} with endpoints at the
	 * specified integer coordinates, and then wraps this as an ImgLib2
	 * {@link Line}.
	 *
	 * @param x1 x coordinate of the first endpoint.
	 * @param y1 y coordinate of the first endpoint.
	 * @param x2 x coordinate of the second endpoint.
	 * @param y2 y coordinate of the second endpoint.
	 */
	public IJLineWrapper(final int x1, final int y1, final int x2, final int y2) {
		line = new ij.gui.Line(x1, y1, x2, y2);
	}

	/**
	 * Creates a new ImageJ 1.x {@link ij.gui.Line Line} with endpoints at the
	 * specified real coordinates, and then wraps this as an ImgLib2 {@link Line}.
	 *
	 * @param x1 x coordinate of the first endpoint.
	 * @param y1 y coordinate of the first endpoint.
	 * @param x2 x coordinate of the second endpoint.
	 * @param y2 y coordinate of the second endpoint.
	 */
	public IJLineWrapper(final double x1, final double y1, final double x2,
		final double y2)
	{
		line = new ij.gui.Line(x1, y1, x2, y2);
	}

	/**
	 * Wraps the given ImageJ 1.x {@link ij.gui.Line Line} as an ImgLib2
	 * {@link Line}.
	 *
	 * @param line imageJ 1.x line to be wrapped
	 */
	public IJLineWrapper(final ij.gui.Line line) {
		this.line = line;
	}

	@Override
	public boolean test(final RealLocalizable t) {
		// NB: ImageJ 1.x contains(...) is not used due to the limitations of
		// integer coordinates. Due to this, ImageJ 1.x contains(...) always
		// returns false for lines with width = 1.
		return GeomMaths.lineContains(new double[] { line.x1d, line.y1d },
			new double[] { line.x2d, line.y2d }, t, 2);
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? Math.min(line.x1d, line.x2d) : Math.min(line.y1d, line.y2d);
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? Math.max(line.x1d, line.x2d) : Math.max(line.y1d, line.y2d);
	}

	@Override
	public RealLocalizable endpointOne() {
		return new AbstractRealLocalizable(new double[] { line.x1d, line.y1d }) {};
	}

	@Override
	public RealLocalizable endpointTwo() {
		return new AbstractRealLocalizable(new double[] { line.x2d, line.y2d }) {};
	}

	@Override
	public ij.gui.Line getRoi() {
		return line;
	}

	@Override
	public int hashCode() {
		return Line.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Line && Line.equals(this, (Line) obj);
	}

}
