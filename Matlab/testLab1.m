
clc;

img = imread('./images/101_5.tif');
img = imread('./images/thumb.jpg');
%img = imread('./images/12.jpg');
%img = imread('./images/Gabor.png');
%img = imread('./images/lenna.png');
%img = tonemap(double(img));

imgGs = grayscaleImage(img);
imgAcc = imgGs;
imgAcc = histeq(imgAcc);

%imgAcc = edge(imgAcc,'Prewitt');
%imgAcc = edge(imgAcc,'Sobel');
%imgAcc = edge(imgAcc,'log',0,5);
%imgAcc = im2bw(imgAcc, level);

u = 3; v = 10; m = 20; d2 = 2;
[~, ~, imgAcc] = gaborFeatures(imgAcc,gaborFilterBank(u,v,m,m), d2, d2);
imgAcc = adaptiveThresh(imgAcc, 4, 1, 'gaussian', 'relative');
%imgAcc = scaleImage(bwmorph(imgAcc, 'thin', 'inf'), 0, 255);

% 'bothat', 'branchpoints', 'bridge', 'clean', 'close', 'diag', 'dilate',
% 'endpoints', 'erode', 'fatten', 'fill', 'hbreak', 'majority', 'perim4',
% 'perim8', 'open', 'remove', 'shrink', 'skeleton', 'spur',
% 'thicken', 'thin', 'tophat'
%imgAcc = bwmorph(imgAcc, 'thin', 'inf');
%imgAcc = bwmorph(imgAcc, 'close', 'inf');

imgGs = grayscaleImage(imread('./images/101_2.jpg'));
imgAcc = grayscaleImage(imread('./images/sift_1.jpg'));

[f1,d1] = vl_sift(im2single(imgGs));
[f2,d2] = vl_sift(im2single(imgAcc));
[matches, scores] = vl_ubcmatch(d1,d2);
matchesOk = ransac(f1, f2, matches);

figure(21); clf;
subplot(1,2,1);
axis equal;
axis off;
axis tight;
hold on;
imshow(imgGs);
h1 = vl_plotframe(f1);
h2 = vl_plotframe(f1);
set(h1,'color','k','linewidth',4);
set(h2,'color','y','linewidth',3);
subplot(1,2,2);
axis equal;
axis off;
axis tight;
hold on;
imshow(imgAcc);
h1 = vl_plotframe(f2);
h2 = vl_plotframe(f2);
set(h1,'color','k','linewidth',4);
set(h2,'color','y','linewidth',3);
axis image off;

figure(22); clf;
imagesc(cat(2, imgGs, imgAcc));
colormap(gray);
x1 = f1(1,matches(1,:));
x2 = f2(1,matches(2,:)) + size(imgGs,2);
y1 = f1(2,matches(1,:));
y2 = f2(2,matches(2,:));
hold on;
h = line([x1; x2], [y1; y2]);
set(h,'linewidth', 1, 'color', 'b');
vl_plotframe(f1(:,matches(1,:)));
f2(1,:) = f2(1,:) + size(imgGs,2);
vl_plotframe(f2(:,matches(2,:)));
axis image off;







