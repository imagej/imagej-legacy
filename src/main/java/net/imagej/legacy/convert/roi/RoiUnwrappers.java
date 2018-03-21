/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

import ij.gui.EllipseRoi;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RotatedRectRoi;
import ij.gui.ShapeRoi;

/**
 * Converters which unwrap wrapped ImageJ 1.x Rois.
 *
 * @author Alison Walter
 */
public final class RoiUnwrappers {

	private RoiUnwrappers() {
		// NB: prevent instantiation of base class
	}

	/** Unwraps wrapped {@link EllipseRoi}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToEllipseRoiConverter extends
		AbstractRoiUnwrapConverter<EllipseRoi>
	{

		@Override
		public Class<EllipseRoi> getOutputType() {
			return EllipseRoi.class;
		}
	}

	/** Unwraps wrapped {@link ij.gui.Line}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToLineConverter extends
		AbstractRoiUnwrapConverter<ij.gui.Line>
	{

		@Override
		public Class<Line> getOutputType() {
			return ij.gui.Line.class;
		}
	}

	/** Unwraps wrapped {@link OvalRoi}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToOvalRoiConverter extends
		AbstractRoiUnwrapConverter<OvalRoi>
	{

		@Override
		public Class<OvalRoi> getOutputType() {
			return OvalRoi.class;
		}
	}

	/** Unwraps wrapped {@link PointRoi}. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToPointRoiConverter extends
		AbstractRoiUnwrapConverter<PointRoi>
	{

		@Override
		public Class<PointRoi> getOutputType() {
			return PointRoi.class;
		}
	}

	/** Unwraps wrapped {@link PolygonRoi}. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToPolygonRoiConverter extends
		AbstractRoiUnwrapConverter<PolygonRoi>
	{

		@Override
		public Class<PolygonRoi> getOutputType() {
			return PolygonRoi.class;
		}
	}

	/** Unwraps wrapped {@link Roi}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH - 1)
	public static class WrapperToRoiConverter extends
		AbstractRoiUnwrapConverter<Roi>
	{

		@Override
		public Class<Roi> getOutputType() {
			return Roi.class;
		}
	}

	/** Unwraps wrapped {@link RotatedRectRoi}. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToRotatedRectRoiConverter extends
		AbstractRoiUnwrapConverter<RotatedRectRoi>
	{

		@Override
		public Class<RotatedRectRoi> getOutputType() {
			return RotatedRectRoi.class;
		}
	}

	/** Unwraps wrapped {@link ShapeRoi}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToShapeRoiConverter extends
		AbstractRoiUnwrapConverter<ShapeRoi>
	{

		@Override
		public Class<ShapeRoi> getOutputType() {
			return ShapeRoi.class;
		}
	}
}
