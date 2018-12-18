package net.imagej.legacy.convert.plot;

import ij.ImagePlus;
import org.scijava.plot.Plot;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Converter plugin, that converts {@link Plot} to {@link Img}.
 *
 * @author Matthias Arzt
 * @see ConvertService
 */
@Plugin(type = Converter.class, priority = Priority.NORMAL_PRIORITY)
public class PlotToImgConverter extends AbstractConverter<Plot, Img> {

	@Parameter
	ConvertService convertService;

	@Override
	public boolean canConvert(ConversionRequest request) {
		return request.destClass().isAssignableFrom( Img.class ) &&
				Plot.class.isAssignableFrom(request.sourceClass()) &&
				convertService.supports(new ConversionRequest(
						request.sourceObject(), request.sourceType(), ImagePlus.class));
	}

	@Override
	public <T> T convert(Object o, Class<T> aClass) {
		ImagePlus imp = convertService.convert(o, ImagePlus.class);
		@SuppressWarnings("unchecked")
		T t = (T) ImageJFunctions.wrap(imp);
		return t;
	}

	@Override
	public Class<Img> getOutputType() {
		return Img.class;
	}

	@Override
	public Class<Plot> getInputType() {
		return Plot.class;
	}
}
