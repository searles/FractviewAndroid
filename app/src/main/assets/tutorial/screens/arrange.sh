convert $1?.png -splice 40x0 -background "#ffffff" +append -crop +40-0 $1.png
mogrify -resize 1024x $1.png
