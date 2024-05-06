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

package net.imagej.legacy.plugin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.ScriptException;

import net.imagej.legacy.IJ1Helper;

import org.scijava.module.ModuleItem;
import org.scijava.script.AbstractScriptEngine;
import org.scijava.script.ScriptModule;
import org.scijava.util.ArrayUtils;

/**
 * A JSR-223-compliant script engine for the ImageJ 1.x macro language.
 *
 * @author Johannes Schindelin
 * @author Curtis Rueden
 */
public class IJ1MacroEngine extends AbstractScriptEngine {

	private static final String[] RESERVED_WORDS = { "Array", "Dialog", "Ext",
		"File", "Fit", "IJ", "List", "Overlay", "PI", "Plot", "Roi", "Stack",
		"String", "abs", "acos", "asin", "atan", "atan2", "autoUpdate", "beep",
		"bitDepth", "calibrate", "call", "changeValues", "charCodeAt", "close",
		"cos", "d2s", "doCommand", "doWand", "drawLine", "drawOval", "drawRect",
		"drawString", "dump", "endsWith", "eval", "exec", "exit", "exp", "fill",
		"fillOval", "fillRect", "floodFill", "floor", "fromCharCode", "getArgument",
		"getBoolean", "getBoundingRect", "getCursorLoc", "getDateAndTime",
		"getDimensions", "getDirectory", "getDisplayedArea", "getFileList",
		"getFontList", "getHeight", "getHistogram", "getImageID", "getImageInfo",
		"getInfo", "getLine", "getList", "getLocationAndSize", "getLut",
		"getMetadata", "getMinAndMax", "getNumber", "getPixel", "getPixelSize",
		"getProfile", "getRawStatistics", "getResult", "getResultLabel",
		"getResultString", "getSelectionBounds", "getSelectionCoordinates",
		"getSliceNumber", "getStatistics", "getString", "getStringWidth",
		"getThreshold", "getTime", "getTitle", "getValue", "getVersion",
		"getVoxelSize", "getWidth", "getZoom", "imageCalculator", "indexOf", "is",
		"isActive", "isKeyDown", "isNaN", "isOpen", "lastIndexOf", "lengthOf",
		"lineTo", "log", "makeArrow", "makeEllipse", "makeLine", "makeOval",
		"makePoint", "makePolygon", "makeRectangle", "makeSelection", "makeText",
		"matches", "maxOf", "minOf", "moveTo", "nImages", "nResults", "nSlices",
		"newArray", "newImage", "newMenu", "open", "parseFloat", "parseInt", "pow",
		"print", "random", "rename", "replace", "requires", "reset",
		"resetMinAndMax", "resetThreshold", "restoreSettings", "roiManager",
		"round", "run", "runMacro", "save", "saveAs", "saveSettings",
		"screenHeight", "screenWidth", "selectImage", "selectWindow",
		"selectionContains", "selectionName", "selectionType", "setAutoThreshold",
		"setBackgroundColor", "setBatchMode", "setColor", "setFont",
		"setForegroundColor", "setJustification", "setKeyDown", "setLineWidth",
		"setLocation", "setLut", "setMetadata", "setMinAndMax", "setOption",
		"setPasteMode", "setPixel", "setRGBWeights", "setResult",
		"setSelectionLocation", "setSelectionName", "setSlice", "setThreshold",
		"setTool", "setVoxelSize", "setZCoordinate", "setupUndo", "showMessage",
		"showMessageWithCancel", "showProgress", "showStatus", "showText", "sin",
		"snapshot", "split", "sqrt", "startsWith", "substring", "tan", "toBinary",
		"toHex", "toLowerCase", "toScaled", "toString", "toUnscaled", "toUpperCase",
		"toolID", "updateDisplay", "updateResults", "wait", "waitForUser" };

	private final IJ1Helper ij1Helper;
	private ScriptModule module;

	private static ThreadLocal<Object> interpreters = new ThreadLocal<>();

	/** Called by ImageJ 1.x at the beginning of each macro execution. */
	public static void saveInterpreter() {
		interpreters.set(IJ1Helper.getInterpreter());
	}

	/**
	 * Constructs an ImageJ 1.x macro engine.
	 *
	 * @param ij1Helper the helper to evaluate the macros
	 */
	public IJ1MacroEngine(final IJ1Helper ij1Helper) {
		this.ij1Helper = ij1Helper;
		engineScopeBindings = new IJ1MacroBindings();
	}

	@Override
	public Object eval(final String macro) throws ScriptException {
		// collect input variable key/value pairs from bindings + module inputs
		final LinkedHashMap<String, Object> inVars = new LinkedHashMap<>();
		inVars.putAll(engineScopeBindings);
		if (module != null) inVars.putAll(module.getInputs());

		final StringBuilder prefix = new StringBuilder();
		final StringBuilder suffix = new StringBuilder();
		
		// prefix contains var declarations and a call to initializeSciJavaParameters()
		prefix.append("var ");
		// suffix contains initializeSciJavaParameters()
		suffix.append("function initializeSciJavaParameters() {\n");

		// during macro execution, save a reference to the ij.macro.Interpreter
		final String method = "\"" + getClass().getName() + ".saveInterpreter\"";
		suffix.append("  call(" + method + ");\n");

		// prepend variable assignments to the macro
		for (final Entry<String, Object> entry : inVars.entrySet()) {
			appendDeclaration(prefix, entry.getKey());
			appendVar(suffix, entry.getKey(), entry.getValue());
		}

		prefix.append("; initializeSciJavaParameters(); ");
		suffix.append("}\n");

		// run the macro!
		final String returnValue = ij1Helper.runMacro(prefix + macro + "\n" + suffix);

		// retrieve the interpreter used
		final Object interpreter = interpreters.get();
		interpreters.remove();

		// populate bindings with the results
		for (final String var : ij1Helper.getVariables(interpreter)) {
			String name = var.substring(0, var.indexOf('\t'));
			// global variables contain a ' (g)' suffix, see:
			// https://github.com/imagej/imagej1/blob/ac613d3/ij/macro/Interpreter.java#L2042-L2044
			final String globalSuffix = " (g)";
			if (name.endsWith(globalSuffix)) {
				name = name.substring(0, name.indexOf(globalSuffix));
			}
			engineScopeBindings.put(name, ij1Helper.getVariable(interpreter, name));
		}

		if (module != null) {
			// convert ImagePlus IDs to their corresponding instances
			for (final ModuleItem<?> item : module.getInfo().outputs()) {
				if (ij1Helper.isImagePlus(item.getType())) {
					final String name = item.getName();
					final Object value = convertToImagePlus(get(name));
					if (value != null) put(name, value);
				}
			}
		}

		if ("[aborted]".equals(returnValue)) {
			// NB: Macro was canceled. Return null, to avoid displaying the output.
			return null;
		}
		return returnValue;
	}

	@Override
	public Object eval(final Reader reader) throws ScriptException {
		final StringBuilder builder = new StringBuilder();
		final char[] buffer = new char[16384];
		try {
			for (;;) {
				final int count = reader.read(buffer);
				if (count < 0) {
					break;
				}
				builder.append(buffer, 0, count);
			}
			reader.close();
		}
		catch (final IOException e) {
			throw new ScriptException(e);
		}
		return eval(builder.toString());
	}

	@Override
	public void put(final String key, final Object value) {
		if (ScriptModule.class.getName().equals(key)) {
			module = (ScriptModule) value;
			return;
		}
		engineScopeBindings.put(key, value);
	}

	// -- Helper methods --

	private void appendVar(final StringBuilder sb, //
		final String key, final Object value)
	{
		// check for illegal identifiers
		if (ArrayUtils.contains(RESERVED_WORDS, key)) return;
		if (key.matches(".*[^a-zA-Z0-9_].*")) return;

		if (value == null) return;
		sb.append(key).append(" = ").append(varValue(value, true)).append(";\n");
	}

	private void appendDeclaration(final StringBuilder pre, final String key) {
		// check for illegal identifiers
		if (ArrayUtils.contains(RESERVED_WORDS, key)) return;
		if (key.matches(".*[^a-zA-Z0-9_].*")) return;

		pre.append(key + ",");
	}

	private String varValue(final Object v, final boolean top) {
		if (top && v.getClass().isArray()) {
			// NB: ImageJ 1.x only supports 1-dimensional arrays.
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < Array.getLength(v); i++) {
				if (i > 0) sb.append(", ");
				sb.append(varValue(Array.get(v, i), false));
			}
			return "newArray(" + sb.toString() + ")";
		}
		else if (ij1Helper.isImagePlus(v)) {
			return varValue(ij1Helper.getImageID(v), false);
		}
		else if (v instanceof File) {
			return varValue(((File) v).getAbsolutePath(), false);
		}
		else if (v instanceof Number || v instanceof Boolean) {
			return v.toString();
		}
		else {
			return "\"" + quote(v.toString()) + "\"";
		}
	}

	private String quote(final String value) {
		String quoted = value.replaceAll("([\"\\\\])", "\\\\$1");
		quoted = quoted.replaceAll("\f", "\\\\f").replaceAll("\n", "\\\\n");
		quoted = quoted.replaceAll("\r", "\\\\r").replaceAll("\t", "\\\\t");
		return quoted;
	}

	private Object convertToImagePlus(final Object value) {
		if (value == null) return null;
		final int imageID;
		if (value instanceof Number) {
			imageID = ((Number) value).intValue();
		}
		else {
			try {
				imageID = Integer.parseInt(value.toString());
			}
			catch (final NumberFormatException exc) {
				return null;
			}
		}
		return ij1Helper.getImage(imageID);
	}

	// -- Helper classes --

	private static class IJ1MacroBindings extends HashMap<String, Object>
		implements Bindings
	{}

	// -- Deprecated --

	/** @deprecated Macros no longer call this method. Nor should you. */
	@Deprecated
	@SuppressWarnings("unused")
	public static void setOutput(final String key, final String value) {
		throw new UnsupportedOperationException();
	}
}
