package org.scijava.plugin;

import org.scijava.Typed;

/**
 * TODO
 *
 * @author Curtis Rueden
 * @param <D> Source data type.
 * @param <O> Data type of objects created by the factory.
 */
public interface FactoryPlugin<D, O> extends SciJavaPlugin, Typed<D> {
	O create(D data);
}
