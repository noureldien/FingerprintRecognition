function imgGray = grayscaleImage(img)

if (size(img,3)==3)
    imgGray = rgb2gray(img);
else
    imgGray = img;
end

end