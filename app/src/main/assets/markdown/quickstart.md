# Quick start

Fractview is a free, [open source](https://github.com/searles/FractviewAndroid) Android app for Android 5 and higher to render fractals. Fractals are images that you can enlarge as much as possible and still see a structure. When you enlarge them, you will encounter all kinds of nice shapes, spirals, dendrites, and also the fractal itself (a phenomenon called self-similarity). 

## First steps

After starting Fractview, a picture of the Mandelbrot Set is rendered. The progress of the rendering is indicated by a progress bar below the title bar (it is hidden after the rendering has finished). Using pinch-to-zoom and double-tap you can zoom into the fractal to discover further structures. 

![Example zoom into the seahorse valley]()

In the "Presets"-menu there is a large collection of fractals to explore further. In the first screen you can select a program, in the second one a set of parameters.

In the "Parameters"-menu you can set further parameters like custom formulas, color palettes or 3D effects.

Fractview contains a Bookmarking system and also allows you to share your fractals with others.

## For technically interested users

Fractview uses a framework called Renderscript to generate images. A compiler creates some bytecode out of the fractal and its parameters that are executed in a virtual machine in Renderscript. It is possible to write your own programs in the backing programming language Meelan (that is heavily inspired from Python and C).

All exported data use easily readable Json that can also be manipulated.

* Further reading:
	+ [User Interface and Menus](): More details on the user interface
	+ [Creating Custom Fractals](): Modify parameters to your liking
	+ [Function Reference](): All functions implemented.

Please refer to [the blog](http://fractview.wordpress.com) for further topics on the app. 

Thanks for using Fractview. If you miss a feature, use the feedback function in the play store. Please consider [leaving a rating/review](https://play.google.com/store/apps/details?id=at.searles.fractview).

-- Karl
