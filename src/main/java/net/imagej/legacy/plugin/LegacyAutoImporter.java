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

import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.scijava.log.LogService;
import org.scijava.log.StderrLogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.AutoImporter;
import org.scijava.util.FileUtils;
import org.scijava.util.Types;

@Plugin(type = AutoImporter.class)
public class LegacyAutoImporter implements AutoImporter {

	@Parameter
	private LogService log;

	private static Map<String, List<String>> defaultImports;

	@Override
	public synchronized Map<String, List<String>> getDefaultImports() {
		if (defaultImports != null)
			return defaultImports;

		defaultImports = new HashMap<>();
		final String[] classNames =
			{ "ij.IJ", "ini.trakem2.Project", "script.imglib.math.Compute" };

		final StringBuilder builder = new StringBuilder();
		builder.append("(");
		for (String className : classNames) {
			int dot = className.startsWith("script.") ? className.indexOf('.')
					: className.lastIndexOf('.');
			if (builder.length() > 1) {
				builder.append("|");
			}
			builder.append(className.substring(0, dot + 1).replace(".", "/"));
		}
		builder.append(").*\\.class");
		final Pattern prefixPattern = Pattern.compile(builder.toString());

		for (String baseClassName : classNames) {
			URL base = Types.location(Types.load(baseClassName));
			if (base == null) {
				continue;
			}
			String baseString = base.toString();
			if (baseString.startsWith("file:") && baseString.endsWith(".jar")) try {
				baseString = "jar:" + baseString + "!/";
				base = new URL(baseString);
			} catch (MalformedURLException e) {
					log.warn("Could not determine location for class "
							+ baseClassName, e);
					continue;
			}
			final int baseLength = baseString.length();
			for (final URL url : FileUtils.listContents(base)) {
				final String path = url.toString().substring(baseLength);
				if (!prefixPattern.matcher(path).matches()) {
					continue;
				}
				// skip anonymous classes
				if (path.matches(".*\\$[0-9].*")) continue;
				final String className = path.substring(0, path.length() - 6)
						.replace('/', '.').replace('$', '.');
				if (!isPublicClass(className)) continue;
				int dot = className.lastIndexOf('.');
				final String packageName = className.substring(0, dot);
				final String baseName = className.substring(dot + 1);
				List<String> list = defaultImports.get(packageName);
				if (list == null) {
					list = new ArrayList<>();
					defaultImports.put(packageName, list);
				}
				list.add(baseName);
			}
		}

		// remove non-unique class names
		Map<String, String> reverse = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : defaultImports.entrySet()) {
			final String packageName = entry.getKey();
			for (final Iterator<String> iter = entry.getValue().iterator(); iter
					.hasNext();) {
				final String className = iter.next();
				if (reverse.containsKey(className)) {
					log.debug("Not auto-importing " + className
							+ " (is in both " + packageName + " and "
							+ reverse.get(className) + ")");
					iter.remove();
					defaultImports.get(reverse.get(className))
							.remove(className);
				} else
					reverse.put(className, packageName);
			}
		}
		return defaultImports;
	}

	private boolean isPublicClass(String className) {
		try {
			return (Class.forName(className).getModifiers() & Modifier.PUBLIC) != 0;
		}
		catch (Throwable t) {
			// ignore class that cannot even be loaded
		}
		return false;
	}

	public static void main(String... args) {
		LegacyAutoImporter importer = new LegacyAutoImporter();
		importer.log = new StderrLogService();
		System.err.println(importer.getDefaultImports());
	}
}
