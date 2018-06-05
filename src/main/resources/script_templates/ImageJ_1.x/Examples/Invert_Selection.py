###
# #%L
# ImageJ software for multidimensional image processing and analysis.
# %%
# Copyright (C) 2009 - 2018 Board of Regents of the University of
# Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
# Institute of Molecular Cell Biology and Genetics.
# %%
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice,
#    this list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# #L%
###
# Inverse ROI: replaces the current ROI by its inverse.
from ij import IJ
from ij.gui import Roi, ShapeRoi
# Get current ImagePlus
image = IJ.getImage()
# Get current ROI
roi = image.getRoi()
if roi is not None:
  # Convert current roi to a ShapeRoi object
  shape_1 = ShapeRoi(roi)
  # Create a ShapeRoi that grabs the whole image
  shape_2 = ShapeRoi(Roi(0,0, image.getWidth(), image.getHeight()))
  # Compute inverse by XOR operation
  image.setRoi(shape_1.xor(shape_2))
