
clc;

%img = imread('./images/101_5.tif');
img = imread('./images/thumb.jpg');
img = imread('./images/18.jpg');
%img = imread('./images/12.jpg');
%img = imread('./images/Gabor.png');
%img = imread('./images/lenna.png');
%img = tonemap(double(img));

imgGs = grayscaleImage(img);
imgAcc = imgGs;
imgAcc = histeq(imgAcc);
[newim, binim, mask, reliability] = testfin(imgGs);

%[~, imgAcc] = gaborFilter1(imgAcc,300,300,3,3);
%[G1,G2,gabout1,gabout2] = gaborFilter3(imgAcc,30,30,13,13);
%imgAcc = histeq(imgAcc);
%imgAcc = adaptiveThresh(imgAcc, 12, 5, 'gaussian', 'relative');

imgAcc = scaleImage(bwmorph(binim, 'thin', 'inf'), 0, 255);
show(imgAcc);
return;

figure(1); clf;
imgAcc = scaleImage(binim, 0, 255);
imshow(imgAcc);
imgAcc = scaleImage(bwmorph(imgAcc, 'thin', 'inf'), 0, 255);
figure(2); clf;
imshow(imgAcc);
return;

imgAcc_ = sauvola(imgAcc, [150 150]);
figure(1)
plot();







