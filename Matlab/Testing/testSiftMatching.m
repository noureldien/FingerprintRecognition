clc;

img1 = imread('./images/sift_1.jpg');
img2 = imread('./images/sift_2.jpg');
%img2 = imread('./images/101_2.jpg');

imgS1 = im2single(rgb2gray(img1));
imgS2 = im2single(rgb2gray(img2));

[f1,d1] = vl_sift(imgS1);
[f2,d2] = vl_sift(imgS2);
[matches, scores] = vl_ubcmatch(d1,d2);

% figure(1); clf;
% imagesc(cat(2, img1, img2));
% axis image off;

figure(2); clf;
imagesc(cat(2, img1, img2));
x1 = f1(1,matches(1,:));
x2 = f2(1,matches(2,:)) + size(img1,2);
y1 = f1(2,matches(1,:));
y2 = f2(2,matches(2,:));
hold on;
h = line([x1; x2], [y1; y2]);
set(h,'linewidth', 1, 'color', 'b');
vl_plotframe(f1(:,matches(1,:)));
f2(1,:) = f2(1,:) + size(img1,2);
vl_plotframe(f2(:,matches(2,:)));
axis image off;








