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

import ij.gui.Roi;
import ij.gui.ShapeRoi;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.imglib2.RealLocalizable;
import net.imglib2.roi.Operators;
import net.imglib2.roi.Operators.MaskOperator;
import net.imglib2.roi.composite.BinaryCompositeMaskPredicate;

import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Converts {@link BinaryCompositeMaskPredicate} to {@link ShapeRoi}. This
 * converter does not support {@link Operators#NEGATE} or
 * {@link net.imglib2.roi.Operators.RealTransformMaskOperator}. Note,
 * {@link ShapeRoi#not(ShapeRoi)} is equivalent to {@link Operators#MINUS}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class BinaryCompositeMaskPredicateToShapeRoiConverter extends
	AbstractMaskPredicateToRoiConverter<BinaryCompositeMaskPredicate<RealLocalizable>, ShapeRoi>
{

	@Parameter
	private ConvertService convertService;

	@Override
	public boolean canConvert(final Object src, final Type dest) {
		if (super.canConvert(src, dest) &&
			src instanceof BinaryCompositeMaskPredicate)
		{
			final BinaryCompositeMaskPredicate<?> mor =
				(BinaryCompositeMaskPredicate<?>) src;
			final List<?> o = mor.operands();
			for (int i = 0; i < o.size(); i++) {
				if (!convertService.supports(o.get(i), Roi.class)) return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (super.canConvert(src, dest) &&
			src instanceof BinaryCompositeMaskPredicate)
		{
			final BinaryCompositeMaskPredicate<?> mor =
				(BinaryCompositeMaskPredicate<?>) src;
			final List<?> o = mor.operands();
			for (int i = 0; i < o.size(); i++) {
				if (!convertService.supports(o.get(i), Roi.class)) return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public Class<ShapeRoi> getOutputType() {
		return ShapeRoi.class;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<BinaryCompositeMaskPredicate<RealLocalizable>> getInputType() {
		return (Class) BinaryCompositeMaskPredicate.class;
	}

	@Override
	public ShapeRoi convert(
		final BinaryCompositeMaskPredicate<RealLocalizable> mask)
	{
		final List<Predicate<?>> o = mask.operands();
		final List<ShapeRoi> sr = new ArrayList<>();
		final MaskOperator op = mask.operator();

		for (final Predicate<?> es : o) {
			final Roi result = convertService.convert(es, Roi.class);
			if (result == null) throw new IllegalArgumentException("Cannot convert " +
				es.getClass() + " to Roi");
			if (result instanceof ShapeRoi) sr.add((ShapeRoi) result);
			else sr.add(new ShapeRoi(result));
		}

		if (sr.isEmpty()) throw new IllegalArgumentException(
			"Cannot convert operands to Rois");

		ShapeRoi base = sr.get(0);
		for (int i = 1; i < sr.size(); i++) {
			base = combineRois(base, sr.get(i), op);
		}

		return base;
	}

	@Override
	public boolean isLossy() {
		return true;
	}

	// -- Helper methods --

	private ShapeRoi combineRois(final ShapeRoi base, final ShapeRoi sr,
		final MaskOperator op)
	{
		if (op == Operators.AND) return base.and(sr);
		else if (op == Operators.OR) return base.or(sr);
		else if (op == Operators.MINUS) return base.not(sr);
		else if (op == Operators.XOR) return base.xor(sr);
		else throw new IllegalArgumentException("Unsupported Operation");
	}

}
