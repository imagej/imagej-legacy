package net.imagej.legacy.display;

import ij.gui.Overlay;
import org.scijava.convert.ConvertService;
import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Display.class)
public class DefaultOverlayDisplay extends AbstractDisplay<Overlay> implements OverlayDisplay {

	@Parameter
	public ConvertService convert;

	public DefaultOverlayDisplay() {
		super(Overlay.class);
	}

	@Override
	public boolean canDisplay(Object src) {
		return convert.supports(src, Overlay.class);
	}

	@Override
	public void display(Object src) {
		super.display(convert.convert(src, Overlay.class));
	}

}
