package net.imagej.legacy.convert;

import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

@Plugin(type = Converter.class, priority = Priority.LOW)
public class StringToButtonConverter extends AbstractConverter<String, Button> {
	@Override
	public <T> T convert(Object src, Class<T> dest) {
		return null;
	}

	@Override
	public Class<Button> getOutputType() {
		return Button.class;
	}

	@Override
	public Class<String> getInputType() {
		return String.class;
	}

	@Override
	public boolean supports(ConversionRequest request) {
		return super.supports(request);
	}
}
