package net.imagej.legacy.display;

import org.scijava.display.Display;

import ij.gui.Overlay;

/**
 * Marker interface for {@link Display} implementations that will be displaying
 * {@link Overlay}s.
 *
 * @author Gabriel Selzer
 */
public interface OverlayDisplay extends Display<Overlay> {
	// This interface intentionally left blank.
}
