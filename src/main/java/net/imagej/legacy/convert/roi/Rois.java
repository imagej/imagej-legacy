/*-
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

package net.imagej.legacy.convert.roi;

import net.imglib2.AbstractRealLocalizable;
import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

/**
 * Utility class for ROI wrappers and converters.
 *
 * @author Alison Walter
 */
public class Rois {

	/**
	 * Throws {@link UnsupportedOperationException} with the given method name.
	 *
	 * @param methodName name of unsupported method
	 */
	public static void unsupported(final String methodName) {
		throw new UnsupportedOperationException(methodName);
	}

	/**
	 * Creates a new {@link IJRoiPoint}
	 *
	 * @param x x-coordinate of the point
	 * @param y y-coordinate of the point
	 */
	public static RealLocalizableRealPositionable ijRoiPoint(final double x,
		final double y)
	{
		return new IJRoiPoint(x, y);
	}

	// -- Helper classes --

	/**
	 * A {@link RealLocalizableRealPositionable} that throws
	 * {@link UnsupportedOperationException} for all {@link RealPositionable}
	 * methods. This is used for ImageJ 1.x wrappers who are writable, but their
	 * "points" (endpoints, vertices, etc.) are not.
	 *
	 * @author Alison Walter
	 */
	private static class IJRoiPoint extends AbstractRealLocalizable implements
		RealLocalizableRealPositionable
	{

		protected IJRoiPoint(final double x, final double y) {
			super(new double[] { x, y });
		}

		@Override
		public void move(final float distance, final int d) {
			unsupported("move");
		}

		@Override
		public void move(final double distance, final int d) {
			unsupported("move");
		}

		@Override
		public void move(final RealLocalizable distance) {
			unsupported("move");
		}

		@Override
		public void move(final float[] distance) {
			unsupported("move");

		}

		@Override
		public void move(final double[] distance) {
			unsupported("move");
		}

		@Override
		public void setPosition(final RealLocalizable position) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final float[] position) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final double[] position) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final float position, final int d) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final double position, final int d) {
			unsupported("setPosition");
		}

		@Override
		public void fwd(final int d) {
			unsupported("fwd");
		}

		@Override
		public void bck(final int d) {
			unsupported("bck");
		}

		@Override
		public void move(final int distance, final int d) {
			unsupported("move");
		}

		@Override
		public void move(final long distance, final int d) {
			unsupported("move");
		}

		@Override
		public void move(final Localizable localizable) {
			unsupported("move");
		}

		@Override
		public void move(final int[] distance) {
			unsupported("move");
		}

		@Override
		public void move(final long[] distance) {
			unsupported("move");
		}

		@Override
		public void setPosition(final Localizable localizable) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final int[] position) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final long[] position) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final int position, final int d) {
			unsupported("setPosition");
		}

		@Override
		public void setPosition(final long position, final int d) {
			unsupported("setPosition");
		}

	}
}
