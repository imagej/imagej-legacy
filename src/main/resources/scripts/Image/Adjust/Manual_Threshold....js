#@ ImagePlus imp
#@ Integer(label="Lower threshold level", value=0) min
#@ Integer(label="Upper threshold level", value=255) max

Packages.ij.IJ.setRawThreshold(imp, min, max, null);
