function result = scaleImage(m, x, y)

% Normalize to [0, 1]:
result = min(min(m));
range1 = max(max(m)) - result;
m = (m - result) / range1;

% Then scale to [x,y]:
range2 = y - x;
result = (m*range2) + x;
result = uint8(result);

% dR = diff( m );
% result = zeros(size(m));
% result =  result - min( result(:)); % set range of A between [0, inf)
% result =  result ./ max( result(:)) ; % set range of A between [0, 1]
% result =  result .* dR ; % set range of A between [0, dRange]
% result =  result + m(1); % shift range of A to R

end