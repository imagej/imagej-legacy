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

package net.imagej.legacy.search;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import net.imagej.legacy.IJ1Helper;
import net.miginfocom.swing.MigLayout;

import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.prefs.PrefService;
import org.scijava.search.SearchAction;
import org.scijava.ui.swing.search.SwingSearchBar;

/**
 * Hacks the ImageJ search bar into place.
 * 
 * @author Curtis Rueden
 */
public class SearchBarHacker {

	private final Context context;

	public SearchBarHacker(final Context context) {
		this.context = context;
	}

	/** Adds a search bar to the main image frame. */
	public Object addSearchBar(final Object imagej, final IJ1Helper ij1Helper) {
		if (!(imagej instanceof Window)) return null; // NB: Avoid headless issues.

		// Retrieve user preferences.
		boolean fullBar = false, embedded = false;
		boolean overrideShortcut = true, mouseoverEnabled = false;
		int resultLimit = 8;
		// HACK: We cannot use OptionsService here, because it will create
		// all the OptionsPlugin instances before needed services are ready.
		// While this is indicative of a design issue with OptionsService and
		// maybe SciJava Common's application framework in general, the best we
		// can do here is to extract the persisted options in a lower-level way.
		final PrefService prefService = context.getService(PrefService.class);
		if (prefService != null) {
			final String style = prefService.get(SearchOptions.class, "style");
			if ("None".equals(style)) return null; // search disabled
			fullBar = "Full".equals(style);
			embedded = prefService.getBoolean(SearchOptions.class, "embedded",
				embedded);
			overrideShortcut = prefService.getBoolean(SearchOptions.class,
				"overrideShortcut", overrideShortcut);
			mouseoverEnabled = prefService.getBoolean(SearchOptions.class,
				"mouseoverEnabled", mouseoverEnabled);
			resultLimit = prefService.getInt(SearchOptions.class, "resultLimit",
				resultLimit);
		}

		final Component[] ijc = ((Container) imagej).getComponents();
		if (ijc.length < 2) return null;
		final Component ijc1 = ijc[1];
		if (!(ijc1 instanceof Container)) return null;

		// rebuild the main panel (status label + progress bar)
		final Container panel = (Container) ijc1;
		final Component[] pc = panel.getComponents();
		panel.removeAll();
		panel.setLayout(new MigLayout("fillx, insets 0", "[0:0]p![p!]"));

		if (fullBar) { // FULL mode
			for (int i = 0; i < pc.length; i++) {
				panel.add(pc[i], i == pc.length - 1 ? "wrap" : "");
			}
		}
		else { // MINI mode
			Arrays.stream(pc).forEach(panel::add);
		}

		// Define a subclass that closes on default action as appropriate.
		class LegacySearchBar extends SwingSearchBar {
			public LegacySearchBar() { super(context); }

			@Override
			protected void runAction(final SearchAction action,
				final boolean isDefault)
			{
				super.runAction(action, isDefault);
				if (!isDefault || prefService == null) return;
				final boolean closeOnDefaultAction = prefService.getBoolean(
					SearchOptions.class, "closeOnDefaultAction", true);
				if (closeOnDefaultAction) reset();
			}
		}

		// add the search bar
		final SwingSearchBar searchBar;
		if (embedded) { // EMBEDDED mode
			searchBar = new LegacySearchBar() {
				@Override
				protected void showPanel(final Container p) {
					getParent().add(p, "south,height 300!", //
						getParent().getComponentCount() - 1);
					getParent().doLayout();
					getParent().revalidate();
					SwingUtilities.getWindowAncestor(this).pack();
					getParent().repaint();
					EventQueue.invokeLater(() -> {
						p.setVisible(true);
						try {
							Thread.sleep(100);
						}
						catch (final InterruptedException exc) {}
						requestFocusInWindow();
					});
				}

				@Override
				protected void hidePanel(final Container p) {
					getParent().remove(p);
					getParent().revalidate();
					getParent().repaint();
					final Window w = SwingUtilities.getWindowAncestor(this);
					w.revalidate();
					w.pack();
					w.repaint();
				}
			};
		}
		else { // DIALOG mode
			searchBar = new LegacySearchBar();
		}
		searchBar.setMouseoverEnabled(mouseoverEnabled);
		searchBar.setResultLimit(resultLimit);

		// add toolbar buttons
		// NB: Unfortunately, the gear (\u2699) does not appear on MacOS.
		searchBar.addButton("...", "Configure search preferences", ae -> {
			final CommandService commandService = //
				context.getService(CommandService.class);
			if (commandService == null) return;
			final ModuleService moduleService = //
				context.getService(ModuleService.class);
			if (moduleService == null) return;
			final CommandInfo searchOptions = //
				commandService.getCommand(SearchOptions.class);
			if (searchOptions == null) return;
			moduleService.run(searchOptions, true);
		});
		if (embedded) {
			searchBar.addButton("\u2715", "Close the search results pane",
				ae -> searchBar.close());
		}

		if (fullBar) { // FULL mode
			panel.add(searchBar, "south");
		}
		else { // MINI mode
			panel.add(searchBar);
		}

		if (overrideShortcut) {
			// disable the old Command Finder's shortcut
			nullShortcut(ij1Helper, "Plugins", "Utilities", "Find Commands...");
			ij1Helper.getShortcuts().put(KeyEvent.VK_L, "Focus Search Bar");
		}

		return searchBar;
	}

	private void nullShortcut(final IJ1Helper ij1Helper, final String menuLabel,
		final String subMenuLabel, final String itemLabel)
	{
		final MenuBar menuBar = ij1Helper.getMenuBar();
		for (int m = 0; m < menuBar.getMenuCount(); m++) {
			final Menu menu = menuBar.getMenu(m);
			if (!menuLabel.equals(menu.getLabel())) continue;
			for (int s = 0; s < menu.getItemCount(); s++) {
				final MenuItem ms = menu.getItem(s);
				if (!(ms instanceof Menu)) continue;
				final Menu subMenu = (Menu) ms;
				if (!subMenuLabel.equals(subMenu.getLabel())) continue;
				for (int i = 0; i < subMenu.getItemCount(); i++) {
					final MenuItem mi = subMenu.getItem(i);
					if (!itemLabel.equals(mi.getLabel())) continue;
					subMenu.remove(i);
					mi.deleteShortcut();
					subMenu.insert(mi, i);
				}
			}
		}
	}
}
