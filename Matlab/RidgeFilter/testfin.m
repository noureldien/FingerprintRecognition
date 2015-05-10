% TESTFIN
%
% Function to demonstrate use of fingerprint code
%
% Usage:  [newim, binim, mask, reliability] =  testfin(im);
%
% Argument:   im -  Fingerprint image to be enhanced.
%
% Returns:    newim - Ridge enhanced image.
%             binim - Binary version of enhanced image.
%             mask  - Ridge-like regions of the image
%             reliability - 'Reliability' of orientation data

% Peter Kovesi
% School of Computer Science & Software Engineering
% The University of Western Australia
% pk at csse uwa edu au
% http://www.csse.uwa.edu.au/~pk
%
% January 2005


function [newim, binim, mask] =  testfin(im, blksze, thresh, gradientsigma, blocksigma, orientsmoothsigma)

% identify ridge-like regions and normalise image
[normim, mask] = ridgesegment(im, blksze, thresh);

% determine ridge orientations
orientim = ridgeorient(normim, gradientsigma, blocksigma, orientsmoothsigma);
plotridgeorient(orientim, 20, im, 2);

% determine ridge frequency values across the image
blksze = 36;
freq = ridgefreq(normim, mask, orientim, blksze, 5, 1, 25);

% apply filters to enhance the ridge pattern
newim = ridgefilter(normim, orientim, freq, 0.5, 0.5);

% binarise, ridge/valley threshold is 0
binim = newim > 0;

end





