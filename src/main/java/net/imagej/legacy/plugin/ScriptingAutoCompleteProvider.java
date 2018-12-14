package net.imagej.legacy.plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;
import org.scijava.ui.swing.script.TextEditor;

/**
 * ScriptingAutoCompleteProvider
 * <p>
 * <p>
 * <p>
 * @author Robert Haase
 * October 2018
 */
public class ScriptingAutoCompleteProvider extends DefaultCompletionProvider
{
    public ScriptingAutoCompleteProvider(final Context context) {
    	List<PluginInfo<?>> plugins = context.getPluginIndex().getAll();
    	int i = 0, size = plugins.size();
			for (final PluginInfo<?> plugin : plugins) {
    		if (i++ % 100 == 0) System.out.println(100f * i / size + "%");
				try {
					Class<?> c = plugin.loadClass();
					final String prefix = shortName(c);
					addClassToAutoCompletion(c, prefix + ".");
				}
				catch (InstantiableException exc) {
					// FIXME
					exc.printStackTrace();
				}
    	}
    }

    private static final Map<String, String> specialCaseNames = specialCases();
		private static Map<String, String> specialCases() {
			final Map<String, String> specialCases = new HashMap<>();
			specialCases.put("net.imagej.ImageJ", "ij");
			specialCases.put("org.scijava.SciJava", "sj");
			return specialCases;
		}

		private String shortName(final Class<?> c) {
			final String specialCase = specialCaseNames.get(c.getName());
			if (specialCase != null) return specialCase;
			final String simpleName = c.getSimpleName();
			if (!isCapital(simpleName, 0)) return simpleName;
			if (simpleName.length() < 2) return simpleName.toLowerCase();
			if (isCapital(simpleName, 1)) {
				// N>=2 capital chars followed by non-capital char: lower case first N-1 chars.
				// Ex: PNGFormat -> pngFormat
				int index = 2; // first non-capital index
				while (isCapital(simpleName, index)) index++;
				return simpleName.substring(0, index - 1).toLowerCase() + //
					simpleName.substring(index - 1);
			}
			// One capital char followed by non-capital char: lower case first char.
			// Ex: ImageJ -> imageJ
			return simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
		}

		private boolean isCapital(String s, int index) {
			if (index >= s.length()) return false;
			final String ch = s.substring(index, index + 1);
			return ch.toUpperCase().equals(ch);
		}

		private void addClassToAutoCompletion(Class<?> c, String prefix) {
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {

            String description = "<ul>";

            //System.out.println(method.toString());
            String headline = prefix + method.getName() + "(";
            String name = prefix + method.getName();

            String parameters = "-";
            for (Parameter parameter : method.getParameters()) {
                //System.out.println(parameter);
                if (!headline.endsWith("(")) {
                    headline = headline + ", ";
                }
                headline = headline + parameter.getName();
                description = description + "<li>" + parameter.getType().getSimpleName() + " " + parameter.getName() + "</li>";
                parameters = parameters + parameter.getType().getCanonicalName() + "-";
            }
            headline = headline + ");";
            description = description + "</ul>";

            if (parameters.length() == 1) {
                parameters = parameters + "-";
            }

            String classLink = getJavadocPrefix(c.getCanonicalName()) + "/" + c.getCanonicalName().replace(".", "/") + ".html";
            String methodLink = classLink + "#" + method.getName() + parameters;
            //System.out.println("Link: " + methodLink);

            description = "<a href=\"" + methodLink + "\">" + method.getName() + "</a><br>" + description;

            description = description + "<br>" +
                    "Defined in " +
                    "<a href=\"" + methodLink + "\">" + c.getCanonicalName() + "</a><br>";

            description = description + "<br>" +
                    "returns " + method.getReturnType().getSimpleName();

            addCompletion(makeListEntry(this, headline, name, description));

        }
        if (c.getSuperclass() != null && c.getSuperclass() != Object.class) {
            addClassToAutoCompletion(c.getSuperclass(), prefix);
        }
    }

    @Override
		protected boolean isValidChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '.' || ch == '"' || ch == '(' ||  ch == ')';
    }

    String getJavadocPrefix(String canonicalClassName) {
    	// TODO: Think about a more extensible way of doing this.
        if (canonicalClassName.startsWith("org.scijava.")) {
            return "https://javadoc.scijava.org/SciJava";
        } else if (canonicalClassName.startsWith("net.imagej.")) {
            return "https://javadoc.scijava.org/ImageJ";
        } else if (canonicalClassName.startsWith("ij.")) {
            return "https://javadoc.scijava.org/ImageJ1";
        } else if (canonicalClassName.startsWith("net.imglib2.")) {
            return "https://javadoc.scijava.org/ImgLib2";
        } else if (canonicalClassName.startsWith("io.scif.")) {
            return "https://javadoc.scijava.org/SCIFIO";
        } else if (canonicalClassName.startsWith("sc.fiji.")) {
            return "https://javadoc.scijava.org/Fiji";
        } else if (canonicalClassName.startsWith("java.")) {
            return "https://javadoc.scijava.org/Java";
        } else if (canonicalClassName.startsWith("javax.")) {
            return "https://javadoc.scijava.org/Java";
        } else {
            return null;
        }
    }



    private BasicCompletion makeListEntry(
            final ScriptingAutoCompleteProvider provider, String headline,
            final String name, String description)
    {
        return new BasicCompletion(provider, headline, null, description);
    }

    public static void main(String... args){
    	Context context = new Context();
//        new ScriptingAutoCompleteProvider(context);
			new TextEditor(context).setVisible(true);
    }
}
