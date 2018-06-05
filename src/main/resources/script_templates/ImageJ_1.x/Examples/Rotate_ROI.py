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
# Rotate current ROI
from ij import IJ
from ij.gui import PolygonRoi, Roi
import math
angle = 0.1 # must be in radian
# Get current ImagePlus
image = IJ.getImage()
# Get current ROI
roi = image.getRoi()
if roi is not None:
  # Get ROI points
  polygon = roi.getPolygon()
  n_points = polygon.npoints
  x = polygon.xpoints
  y = polygon.ypoints
  # Compute center of mass
  xc = 0
  yc = 0
  for i in range(n_points):
    xc = xc + x[i]
    yc = yc + y[i]
  xc = xc / n_points
  yc = yc / n_points
  # Compute new rotated points
  new_x = []
  new_y = []
  for i in range(n_points):
    new_x.append( int ( xc + (x[i]-xc)*math.cos(angle) - (y[i]-yc)*math.sin(angle) ) )
    new_y.append( int ( yc + (x[i]-xc)*math.sin(angle) + (y[i]-yc)*math.cos(angle) ) )
  # Create new ROI
  new_roi = PolygonRoi(new_x, new_y, n_points,  Roi.POLYGON )
  image.setRoi(new_roi)
