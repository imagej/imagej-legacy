package net.imagej.legacy.plugin;

import ij.IJ;
import ij.plugin.frame.RoiManager;
import io.scif.SCIFIOService;
import net.imagej.DatasetService;
import net.imagej.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import net.imagej.animation.AnimationService;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.ScreenCaptureService;
import net.imagej.display.WindowService;
import net.imagej.lut.LUTService;
import net.imagej.ops.Ops;
import net.imagej.ops.coloc.ColocNamespace;
import net.imagej.ops.convert.ConvertNamespace;
import net.imagej.ops.copy.CopyNamespace;
import net.imagej.ops.create.CreateNamespace;
import net.imagej.ops.deconvolve.DeconvolveNamespace;
import net.imagej.ops.features.haralick.HaralickNamespace;
import net.imagej.ops.features.lbp2d.LBPNamespace;
import net.imagej.ops.features.tamura2d.TamuraNamespace;
import net.imagej.ops.features.zernike.ZernikeNamespace;
import net.imagej.ops.filter.FilterNamespace;
import net.imagej.ops.geom.GeomNamespace;
import net.imagej.ops.image.ImageNamespace;
import net.imagej.ops.imagemoments.ImageMomentsNamespace;
import net.imagej.ops.labeling.LabelingNamespace;
import net.imagej.ops.logic.LogicNamespace;
import net.imagej.ops.math.MathNamespace;
import net.imagej.ops.morphology.MorphologyNamespace;
import net.imagej.ops.stats.StatsNamespace;
import net.imagej.ops.threshold.ThresholdNamespace;
import net.imagej.ops.topology.TopologyNamespace;
import net.imagej.ops.transform.TransformNamespace;
import net.imagej.render.RenderingService;
import net.imagej.sampler.SamplerService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.Regions;
import net.imglib2.view.Views;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.module.ModuleService;
import org.scijava.script.ScriptService;
import org.scijava.ui.UIService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.RandomAccess;

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
    private static ScriptingAutoCompleteProvider instance = null;

    public static ScriptingAutoCompleteProvider getInstance () {
        if (instance == null) {
            instance = new ScriptingAutoCompleteProvider();
        }
        return instance;
    }

    private ScriptingAutoCompleteProvider() {
        // imagej1
        addClassToAutoCompletion(IJ.class, "IJ.", "ImageJ1");
        addClassToAutoCompletion(ImagePlus.class, "imp.", "ImageJ1");
        addClassToAutoCompletion(ImageProcessor.class, "ip.", "ImageJ1");
        addClassToAutoCompletion(Roi.class, "roi.", "ImageJ1");
        addClassToAutoCompletion(RoiManager.class, "roimanager.", "ImageJ1");

        // image2
        addClassToAutoCompletion(Cursor.class, "cur.", "ImageJ");
        addClassToAutoCompletion(RandomAccess.class, "ra.", "ImageJ");
        addClassToAutoCompletion(RandomAccessibleInterval.class, "rai.", "ImageJ");
        addClassToAutoCompletion(Img.class, "img.", "ImageJ");
        addClassToAutoCompletion(IterableInterval.class, "ii.", "ImageJ");

        // static classes
        addClassToAutoCompletion(ArrayImgs.class, "ArrayImgs.", "ImgLib2");
        addClassToAutoCompletion(Views.class, "Views.", "ImgLib2");
        addClassToAutoCompletion(Regions.class, "Regions.", "ImgLib2");
        addClassToAutoCompletion(ImageJFunctions.class, "ImageJFunctions.", "ImgLib2");

        // ij services and ops
        addClassToAutoCompletion(ImageJ.class, "ij.", "ImageJ");
        addClassToAutoCompletion(AnimationService.class, "ij.animation().", "ImageJ");
        addClassToAutoCompletion(CommandService.class, "ij.command().", "ImageJ");
        addClassToAutoCompletion(DatasetService.class, "ij.dataset().", "ImageJ");
        addClassToAutoCompletion(DisplayService.class, "ij.display().", "ImageJ");
        addClassToAutoCompletion(DisplayService.class, "display.", "ImageJ");
        addClassToAutoCompletion(ImageDisplayService.class, "ij.imageDisplay().", "ImageJ");
        addClassToAutoCompletion(LogService.class, "ij.log().", "ImageJ");
        addClassToAutoCompletion(LUTService.class, "ij.lut().", "ImageJ");
        addClassToAutoCompletion(ModuleService.class, "ij.module().", "ImageJ");
        addClassToAutoCompletion(RenderingService.class, "ij.render().", "ImageJ");
        addClassToAutoCompletion(SamplerService.class, "ij.sampler().", "ImageJ");
        addClassToAutoCompletion(ScreenCaptureService.class, "ij.screenCapture().", "ImageJ");
        addClassToAutoCompletion(SCIFIOService.class, "ij.scifio().", "ImageJ");
        addClassToAutoCompletion(ScriptService.class, "ij.script().", "ImageJ");
        addClassToAutoCompletion(UIService.class, "ij.ui().", "ImageJ");
        addClassToAutoCompletion(UIService.class, "ui.", "ImageJ");
        addClassToAutoCompletion(WindowService.class, "ij.window().", "ImageJ");

        // ij ops
        for (String prefix : new String[]{"ops", "ij.op()."}){
            addClassToAutoCompletion(Ops.class, prefix, "ImageJ");

            addClassToAutoCompletion(ColocNamespace.class, prefix + "coloc().", "ImageJ");
            addClassToAutoCompletion(ConvertNamespace.class, prefix + "convert().", "ImageJ");
            addClassToAutoCompletion(CopyNamespace.class, prefix + "copy().", "ImageJ");
            addClassToAutoCompletion(CreateNamespace.class, prefix + "create().", "ImageJ");
            addClassToAutoCompletion(DeconvolveNamespace.class, prefix + "deconvolve().", "ImageJ");
            addClassToAutoCompletion(FilterNamespace.class, prefix + "filter().", "ImageJ");
            addClassToAutoCompletion(GeomNamespace.class, prefix + "geom().", "ImageJ");
            addClassToAutoCompletion(HaralickNamespace.class, prefix + "haralick().", "ImageJ");
            addClassToAutoCompletion(MathNamespace.class, prefix + "math().", "ImageJ");
            addClassToAutoCompletion(MorphologyNamespace.class, prefix + "morphology().", "ImageJ");
            addClassToAutoCompletion(ImageNamespace.class, prefix + "image().", "ImageJ");
            addClassToAutoCompletion(ImageMomentsNamespace.class, prefix + "imagemoments().", "ImageJ");
            addClassToAutoCompletion(LabelingNamespace.class, prefix + "labeling().", "ImageJ");
            addClassToAutoCompletion(LBPNamespace.class, prefix + "lbp().", "ImageJ");
            addClassToAutoCompletion(LogicNamespace.class, prefix + "logic().", "ImageJ");
            addClassToAutoCompletion(StatsNamespace.class, prefix + "stats().", "ImageJ");
            addClassToAutoCompletion(TamuraNamespace.class, prefix + "tamura().", "ImageJ");
            addClassToAutoCompletion(ThresholdNamespace.class, prefix + "threshold().", "ImageJ");
            addClassToAutoCompletion(TopologyNamespace.class, prefix + "topology().", "ImageJ");
            addClassToAutoCompletion(TransformNamespace.class, prefix + "transform().", "ImageJ");
            addClassToAutoCompletion(ZernikeNamespace.class, prefix + "zernike().", "ImageJ");
        }

    }

    private void addClassToAutoCompletion(Class c, String prefix, String library) {
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

            String classLink = getUrlPrefix(c.getCanonicalName(), library) + "/" + c.getCanonicalName().replace(".", "/") + ".html";
            String methodLink = classLink + "#" + method.getName() + parameters;
            ////System.out.println("Link: " + link);

            description = "<a href=\"" + methodLink + "\">" + method.getName() + "</a><br>" + description;

            description = description + "<br>" +
                    "Defined in " +
                    "<a href=\"" + methodLink + "\">" + c.getCanonicalName() + "</a><br>";

            description = description + "<br>" +
                    "returns " + method.getReturnType().getSimpleName();

            addCompletion(makeListEntry(this, headline, name, description));

        }
        if (c.getSuperclass() != null && c.getSuperclass() != Object.class) {
            addClassToAutoCompletion(c.getSuperclass(), prefix, library);
        }
    }

    protected boolean isValidChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '.' || ch == '"' || ch == '(' ||  ch == ')';
    }

    String getUrlPrefix(String canonicalClassName, String defaultLibrary) {
        if (canonicalClassName.startsWith("org.scijava.")) {
            return "https://javadoc.scijava.org/SciJava";
        } else if (canonicalClassName.startsWith("net.imagej.")) {
            return "https://javadoc.scijava.org/ImageJ";
        } else if (canonicalClassName.startsWith("ij.")) {
            return "https://javadoc.scijava.org/ImageJ1";
        } else if (canonicalClassName.startsWith("net.imglib2.")) {
            return "https://javadoc.scijava.org/ImgLib2";
        } else if (canonicalClassName.startsWith("java.awt.")) {
            return "https://docs.oracle.com/javase/8/docs/api/";
        } else {
            return "https://javadoc.scijava.org/" + defaultLibrary;
        }
    }



    private BasicCompletion makeListEntry(
            final ScriptingAutoCompleteProvider provider, String headline,
            final String name, String description)
    {
        return new BasicCompletion(provider, headline, null, description);
    }

    public static void main(String... args){
        ScriptingAutoCompleteProvider.getInstance();
    }
}
