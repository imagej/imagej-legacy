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

package net.imagej.legacy.convert.roi;

import net.imglib2.roi.MaskPredicate;
import net.imglib2.roi.geom.real.WritableBox;
import net.imglib2.roi.geom.real.WritableEllipsoid;
import net.imglib2.roi.geom.real.WritableLine;
import net.imglib2.roi.geom.real.WritablePointMask;
import net.imglib2.roi.geom.real.WritablePolygon2D;
import net.imglib2.roi.geom.real.WritablePolyline;
import net.imglib2.roi.geom.real.WritableRealPointCollection;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converters which unwrap wrapped {@link MaskPredicate}s.
 *
 * @author Alison Walter
 */
public final class MaskPredicateUnwrappers {

	private MaskPredicateUnwrappers() {
		// NB: prevent instantiation of base class
	}

	/** Unwraps wrapped {@link WritableBox}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToWritableBoxConverter extends
		AbstractMaskPredicateUnwrapConverter<WritableBox>
	{

		@Override
		public Class<WritableBox> getOutputType() {
			return WritableBox.class;
		}
	}

	/** Unwraps wrapped {@link WritableEllipsoid}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToWritableEllipsoid extends
		AbstractMaskPredicateUnwrapConverter<WritableEllipsoid>
	{

		@Override
		public Class<WritableEllipsoid> getOutputType() {
			return WritableEllipsoid.class;
		}
	}

	/** Unwraps wrapped {@link WritableLine}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToWritableLine extends
		AbstractMaskPredicateUnwrapConverter<WritableLine>
	{

		@Override
		public Class<WritableLine> getOutputType() {
			return WritableLine.class;
		}
	}

	/** Unwraps wrapped {@link WritablePointMask}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToWritablePointMask extends
		AbstractMaskPredicateUnwrapConverter<WritablePointMask>
	{

		@Override
		public Class<WritablePointMask> getOutputType() {
			return WritablePointMask.class;
		}
	}

	/** Unwraps wrapped {@link WritableRealPointCollection}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToWritableRealPointCollection extends
		AbstractMaskPredicateUnwrapConverter<WritableRealPointCollection<RealLocalizableRealPositionable>>
	{

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Class<WritableRealPointCollection<RealLocalizableRealPositionable>>
			getOutputType()
		{
			return (Class) WritableRealPointCollection.class;
		}
	}

	/** Unwraps wrapped {@link WritablePolygon2D}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToWritablePolygon2D extends
		AbstractMaskPredicateUnwrapConverter<WritablePolygon2D>
	{

		@Override
		public Class<WritablePolygon2D> getOutputType() {
			return WritablePolygon2D.class;
		}
	}

	/** Unwraps wrapped {@link WritablePolyline}s. */
	@Plugin(type = Converter.class, priority = Priority.HIGH)
	public static class WrapperToWritablePolyline extends
		AbstractMaskPredicateUnwrapConverter<WritablePolyline>
	{

		@Override
		public Class<WritablePolyline> getOutputType() {
			return WritablePolyline.class;
		}
	}

}
