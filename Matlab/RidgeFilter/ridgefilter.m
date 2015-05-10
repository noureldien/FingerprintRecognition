% RIDGEFILTER - enhances fingerprint image via oriented filters
%
% Function to enhance fingerprint image via oriented filters
%
% Usage:
%  newim =  ridgefilter(im, orientim, freqim, kx, ky, showfilter)
%
% Arguments:
%         im       - Image to be processed.
%         orientim - Ridge orientation image, obtained from RIDGEORIENT.
%         freqim   - Ridge frequency image, obtained from RIDGEFREQ.
%         kx, ky   - Scale factors specifying the filter sigma relative
%                    to the wavelength of the filter.  This is done so
%                    that the shapes of the filters are invariant to the
%                    scale.  kx controls the sigma in the x direction
%                    which is along the filter, and hence controls the
%                    bandwidth of the filter.  ky controls the sigma
%                    across the filter and hence controls the
%                    orientational selectivity of the filter. A value of
%                    0.5 for both kx and ky is a good starting point.
%         showfilter - An optional flag 0/1.  When set an image of the
%                      largest scale filter is displayed for inspection.
%
% Returns:
%         newim    - The enhanced image
%
% See also: RIDGEORIENT, RIDGEFREQ, RIDGESEGMENT

% Reference:
% Hong, L., Wan, Y., and Jain, A. K. Fingerprint image enhancement:
% Algorithm and performance evaluation. IEEE Transactions on Pattern
% Analysis and Machine Intelligence 20, 8 (1998), 777 789.

% Peter Kovesi
% School of Computer Science & Software Engineering
% The University of Western Australia
% pk at csse uwa edu au
% http://www.csse.uwa.edu.au/~pk
%
% January 2005

function newim = ridgefilter(im, orient, freq, medianFreq, kx, ky)

% Fixed angle increment between filter orientations in
% degrees. This should divide evenly into 180
angleInc = 3;
filterCount = round(180 / angleInc);

im = double(im);
[rows, cols] = size(im);
newim = zeros(rows,cols);

% Generate filters corresponding to these distinct frequencies and
% orientations in 'angleInc' increments.
filter = cell(1,filterCount);

sigmax = kx/medianFreq;
sigmay = ky/medianFreq;

sze = round(3*max(sigmax,sigmay));
[x,y] = meshgrid(-sze:sze);
reffilter1 = ((x.^2) / (sigmax^2)) + ((y.^2) / (sigmay^2));
reffilter1 = exp(-reffilter1/2); 
reffilter2 = cos(2*pi*medianFreq*x);
reffilter = reffilter1.*reffilter2;

% Generate rotated versions of the filter.  Note orientation
% image provides orientation *along* the ridges, hence +90
% degrees, and imrotate requires angles +ve anticlockwise, hence
% the minus sign.
for i = 1:filterCount
    filter{i} = imrotate(reffilter,-(i*angleInc+90),'bilinear','crop');
end

% Convert orientation matrix values from radians to an index value
% that corresponds to round(degrees/angleInc)
orientindex = round((filterCount/pi)*orient);
i = find(orientindex < 1);
orientindex(i) = orientindex(i)+filterCount;
i = find(orientindex > filterCount);
orientindex(i) = orientindex(i)-filterCount;

% Find where there is valid frequency data.
% Find indices of matrix points greater than sze from the image boundary
% Finally do the filtering
for r=1:rows
    for c=1:cols
        if (freq(r,c) > 0 && r > sze && r < (rows-sze) && c > sze && c < (cols-sze))
            newim(r,c) = sum(sum(im(r-sze:r+sze, c-sze:c+sze).*filter{orientindex(r,c)}));
        end
    end
end

end


