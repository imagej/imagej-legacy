#@ File    (label = "Input directory", style = "directory") srcFile
#@ File    (label = "Output directory", style = "directory") dstFile
#@ String  (label = "File extension", value=".tif") ext
#@ String  (label = "File name contains", value = "") containString
#@ Boolean (label = "Keep directory structure when saving", value = true) keepDirectories

import ij.IJ

def main() {
	srcFile.eachFileRecurse {
		name = it.getName()
		if (name.endsWith(ext) && name.contains(containString)) {
			process(it, srcFile, dstFile, keepDirectories)
		}
	}
}

def process(file, src, dst, keep) {
	println "Processing $file"

	// Opening the image
	imp = IJ.openImage(file.getAbsolutePath())

	// Put your processing steps here

	// Saving the result
	relativePath = keep ?
			src.toPath().relativize(file.getParentFile().toPath()).toString()
			: "" // no relative path
	saveDir = new File(dst.toPath().toString(), relativePath)
	if (!saveDir.exists()) saveDir.mkdirs()
	saveFile = new File(saveDir, file.getName()) // customize name if needed
	IJ.saveAs(imp, "Tiff", saveFile.getAbsolutePath());

	// Clean up
	imp.close()
}

main()
