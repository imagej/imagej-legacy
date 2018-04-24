package net.imagej.legacy.translate;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.array.ArrayImgs;
import org.scijava.Context;
import org.scijava.ui.UIService;

public class SixDimesional
{
	public static void main(String... args) {
		UIService ui = new Context().service( UIService.class );
		AxisType[] axes = {Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME, Axes.unknown()};
		ui.show( new ImgPlus<>( ArrayImgs.unsignedBytes(2,2,2,2,2,2), "title", axes) );
	}
}
