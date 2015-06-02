import os
from ij import IJ, ImagePlus
from ij.gui import GenericDialog
  
def run():
  srcDir = IJ.getDirectory("Input_directory")
  if not srcDir:
    return
  dstDir = IJ.getDirectory("Output_directory")
  if not dstDir:
    return
  gd = GenericDialog("Process Folder")
  gd.addStringField("File_extension", ".tif")
  gd.addStringField("File_name_contains", "")
  gd.addCheckbox("Keep directory structure when saving", True)
  gd.showDialog()
  if gd.wasCanceled():
    return
  ext = gd.getNextString()
  containString = gd.getNextString()
  keepDirectories = gd.getNextBoolean()
  for root, directories, filenames in os.walk(srcDir):
    for filename in filenames:
      # Check for file extension
      if not filename.endswith(ext):
        continue
      # Check for file name pattern
      if containString not in filename:
        continue
      process(srcDir, dstDir, root, filename, keepDirectories)
 
def process(srcDir, dstDir, currentDir, fileName, keepDirectories):
  print "Processing:"
   
  # Opening the image
  print "Open image file", fileName
  imp = IJ.openImage(os.path.join(currentDir, fileName))
   
  # Put your processing commands here!
   
  # Saving the image
  saveDir = currentDir.replace(srcDir, dstDir) if keepDirectories else dstDir
  if not os.path.exists(saveDir):
    os.makedirs(saveDir)
  print "Saving to", saveDir
  IJ.saveAs(imp, "Tiff", os.path.join(saveDir, fileName));
  imp.close()
 
run()
