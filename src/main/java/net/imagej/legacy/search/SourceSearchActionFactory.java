/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2020 ImageJ developers.
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

package net.imagej.legacy.search;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

import org.scijava.command.CommandInfo;
import org.scijava.log.LogService;
import org.scijava.module.ModuleInfo;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptInfo;
import org.scijava.search.DefaultSearchAction;
import org.scijava.search.SearchAction;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;
import org.scijava.search.module.ModuleSearchResult;
import org.scijava.ui.UIService;
import org.scijava.util.AppUtils;
import org.scijava.util.FileUtils;
import org.scijava.util.Manifest;
import org.scijava.util.POM;
import org.scijava.util.Types;
import org.xml.sax.SAXException;

/**
 * Search action for viewing the source code of a SciJava module.
 *
 * @author Curtis Rueden
 */
@Plugin(type = SearchActionFactory.class)
public class SourceSearchActionFactory implements SearchActionFactory {

	@Parameter
	private LogService log;

	@Parameter
	private UIService uiService;

	@Parameter
	private PlatformService platformService;

	@Override
	public boolean supports(final SearchResult result) {
		return result instanceof ModuleSearchResult;
	}

	@Override
	public SearchAction create(final SearchResult result) {
		return new DefaultSearchAction("Source", //
			() -> source(((ModuleSearchResult) result).info()));
	}

	private void source(final ModuleInfo info) {
		// HACK: For now, we special case each kind of module.
		// In the future, we should make it more extensible.
		// HelpService? ModuleService#help? Some other type of plugin?

		if (info instanceof ScriptInfo) {
			// SciJava script.
			sourceForScript((ScriptInfo) info);
			return;
		}

		final String id = info.getIdentifier();
		if (id != null && id.startsWith("legacy:")) {
			// ImageJ 1.x command.
			sourceForLegacyCommand(info);
			return;
		}

		try {
			// Some other kind of module; use the delegate class.
			sourceForClass(info, info.loadDelegateClass());
		}
		catch (final ClassNotFoundException exc) {
			log.debug(exc);
			errorMessage(info);
		}
	}

	private void sourceForScript(final ScriptInfo info) {
		legacyService().openScriptInTextEditor(info);
	}

	private void sourceForLegacyCommand(final ModuleInfo info) {
		if (!(info instanceof CommandInfo)) {
			log.debug("Not a CommandInfo: " + info.getTitle());
			errorMessage(info);
			return;
		}
		final CommandInfo cInfo = (CommandInfo) info;
		final Object className = cInfo.getPresets().get("className");
		if (className == null || !(className instanceof String)) {
			log.debug("Weird class name: " + className);
			errorMessage(info);
			return;
		}
		final IJ1Helper ij1Helper = legacyService().getIJ1Helper();
		if (ij1Helper == null) {
			log.debug("Null IJ1Helper");
			errorMessage(info);
			return;
		}
		final ClassLoader classLoader = ij1Helper.getClassLoader();
		if (classLoader == null) {
			log.debug("Null IJ1 class loader");
			errorMessage(info);
			return;
		}
		try {
			final Class<?> c = classLoader.loadClass((String) className);
			sourceForClass(info, c);
		}
		catch (final ClassNotFoundException exc) {
			log.debug(exc);
			errorMessage(info);
		}
	}

	private void sourceForClass(final ModuleInfo info, final Class<?> c) {
		final POM pom = getPOM(c, null, null);
		if (pom == null) {
			log.debug("No Maven POM found for class: " + c.getName());
			errorMessage(info);
			return;
		}

		final String scmURL = pom.getSCMURL();
		if (scmURL == null) {
			if (log.isDebug()) log.debug("No <scm><url> for " + coord(pom));
			errorMessage(info);
			return;
		}
		if (!scmURL.matches("^(git|http|https)://github.com/[^/]+/[^/]+/?$")) {
			log.debug("Not a standard GitHub project URL: " + scmURL);
			openURL(scmURL);
			return;
		}

		// Try to extract a tag or commit hash.
		final String tag;
		final String scmTag = pom.getSCMTag();
		if (scmTag == null || scmTag.equals("HEAD")) {
			if (log.isDebug()) {
				log.debug(scmTag == null ? //
					"No SCM tag available; using commit hash." : //
					"Weird SCM tag '" + scmTag + "'; using commit hash.");
			}
			final Manifest m = Manifest.getManifest(c);
			tag = m == null ? null : m.getImplementationBuild();
			if (tag == null) log.debug("No commit hash found.");
		}
		else tag = scmTag;
		if (tag == null) {
			// No tag or commit hash could be extracted.
			openURL(scmURL);
			return;
		}

		// Build a precise GitHub URL.
		final StringBuilder url = new StringBuilder();
		url.append(scmURL);
		if (!scmURL.endsWith("/")) url.append("/");
		url.append("blob/");
		url.append(tag);
		url.append("/src/main/java/");
		url.append(c.getName().replaceAll("\\.", "/"));
		url.append(".java");
		openURL(url.toString());
	}

	private void openURL(final String urlPath) {
		try {
			platformService.open(new URL(urlPath));
		}
		catch (final IOException exc) {
			log.error(exc);
			uiService.showDialog("Platform error opening source URL: " + urlPath);
		}
	}

	private LegacyService legacyService() {
		return log.context().service(LegacyService.class);
	}

	private String coord(final POM pom) {
		final String g = pom.getGroupId();
		final String a = pom.getArtifactId();
		final String v = pom.getVersion();
		return g + ":" + a + ":" + v;
	}

	private void errorMessage(final ModuleInfo info) {
		uiService.showDialog("Source location unknown for " + info.getTitle());
	}

	// TODO: Migrate this improved routine to org.scijava.util.POM.

	/**
	 * Gets the Maven POM associated with the given class.
	 * 
	 * @param c The class to use as a base when searching for a pom.xml.
	 * @param groupId The Maven groupId of the desired POM.
	 * @param artifactId The Maven artifactId of the desired POM.
	 */
	private static POM getPOM(final Class<?> c, final String groupId,
		final String artifactId)
	{
		try {
			final URL location = Types.location(c);
			if (!location.getProtocol().equals("file") ||
				location.toString().endsWith(".jar"))
			{
				// look for pom.xml in JAR's META-INF/maven subdirectory
				if (groupId == null || artifactId == null) {
					// groupId and/or artifactId is unknown; scan for the POM
					final URL pomBase = new URL("jar:" + //
						location.toString() + "!/META-INF/maven");
					for (final URL url : FileUtils.listContents(pomBase, true, true)) {
						if (url.toExternalForm().endsWith("/pom.xml")) {
							return new POM(url);
						}
					}
				}
				else {
					// known groupId and artifactId; grab it directly
					final String pomPath =
						"META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml";
					final URL pomURL =
						new URL("jar:" + location.toString() + "!/" + pomPath);
					return new POM(pomURL);
				}
			}
			// look for the POM in the class's base directory
			final File file = FileUtils.urlToFile(location);
			final File baseDir = AppUtils.getBaseDirectory(file, null);
			final File pomFile = new File(baseDir, "pom.xml");
			return new POM(pomFile);
		}
		catch (final IOException | ParserConfigurationException | SAXException e) {
			return null;
		}
	}
}
