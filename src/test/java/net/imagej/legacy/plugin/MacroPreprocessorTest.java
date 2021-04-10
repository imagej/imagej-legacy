package net.imagej.legacy.plugin;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;
import org.scijava.ui.UIService;
import org.scijava.widget.Button;

import net.imagej.legacy.ui.LegacyUI;
import net.imagej.patcher.LegacyInjector;

public class MacroPreprocessorTest {

	static {
		LegacyInjector.preinit();
	}

	private Context context;
	private ScriptService scriptService;

	@Before
	public void setUp() {
		context = new Context();
		scriptService = context.service(ScriptService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testWithOptionString() throws InterruptedException, ExecutionException {
		context.service(UIService.class).showUI(LegacyUI.NAME);
		String macro = "" //
		+ "run(\"Test Command\", \"reason=[option string]\");\n" //
		+ "return \"done.\"";
		ScriptModule module = scriptService.run("macro.ijm", macro, true).get();
		assertEquals("done.", module.getOutput("result"));
	}

	@Test
	@Ignore
	public void testWithEmptyOptionString() throws InterruptedException, ExecutionException {
		context.service(UIService.class).showUI(LegacyUI.NAME);
		String macro = "" //
		+ "run(\"Test Command\", \"\");\n" //
		+ "return \"done.\"";
		ScriptModule module = scriptService.run("macro.ijm", macro, true).get();
		assertEquals("done.", module.getOutput("result"));
	}

	@Plugin(type = Command.class, menuPath = "Testing>Test Command")
	public static class TestCommand extends ContextCommand {

		@Parameter(required = false, persist = false)
		private String reason = "invalid";

		@Parameter(callback = "callback")
		private Button button;

		public void callback() {
			throw new IllegalStateException("Button pressed!");
		}

		@Override
		public void run() {
			throw new UnsupportedOperationException("Test Command running with: " + reason);
		}

	}
}
