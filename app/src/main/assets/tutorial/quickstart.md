# Quick start

Fractview is a free, [open source](https://github.com/searles/FractviewAndroid) app for Android 5+ to render fractals. Fractals are images that you can enlarge as much as possible and still see a structure. When you enlarge them, you will encounter all kinds of nice shapes, spirals, dendrites, and also the original shape itself (a phenomenon called self-similarity).

No app is perfect, therefore, please use the [feedback function in the play store]((https://play.google.com/store/apps/details?id=at.searles.fractview) in case of any malfunctioning or missing functions.

## First steps

After starting Fractview, a picture of the Mandelbrot Set is rendered. The progress of the rendering is indicated by a progress bar below the title bar (it is hidden after the rendering has finished). Using pinch-to-zoom and double-tap you can zoom into the fractal to discover further structures. 

![Example zoom into the seahorse valley](zoom_sequence.png)

## Presets

Fractview is very versatile. To demonstrate this versatility, the app comes with a lot of demos, accessible via ![the demos icon](demos_icon.png). The demos-view uses two screens. In the first screen you pick a program that will render the image. There are many different programs to draw basic fractals, newton fractals, lyapunov fractals, magnetic pendulum simulations or simply complex functions. You can select a program by a single tap. The first entry ("Current") will reuse the program that you used in your last fractal. Right after start-up it is the "Default" preset.

Each program contains some parameters that can be directly manipulated. In the second screen you can pick such a parameter set. Depending on the program you will see different parameters. The description of the parameter set will point out for which program the parameter set is intended but you can usually also use them for other programs.

![Screenshots of the Demos-menu](demos_screens.png) 

## Parameters

You also can manipulate these parameters by selecting "Parameters" in the menu ![the edit icon](edit_icon.png). There, you can set further parameters like custom formulas, color palettes or 3D effects. In the beginning, the most interesting one for you will most likely be "maxdepth" because by increasing this number you can increase the amount of details at deeper zooms.

![Changing maximum depth](changing_parameter.png) 

Notice that the parameters that were modified appear now in bold letters. These parameters are the non-default ones. Using a long tap, a menu appears in which you can revert all parameters.

Using the "light" parameter you can enable a nice 3D effect.

![Adding a 3D effect](3d_effect.png) 

Fractview allows you to create custom (two dimensional) color palettes using an intuitive palette editor and color picker. The *bailoutpalette* contains the colors used of the outside of the fractal (in the Default program it uses only one dimension), the *lakepalette* is used for the inside (this one uses two dimensions).

You find more information on parameters in this article: [Creating Custom Fractals](custom.html).

## Favorites

Fractview contains a Bookmarking system and also allows you to save and share your fractals with other users. From the main view, you can add your favorites using "Add to Favorites". Also the menu to save the image allows you to directly add a favorite. 

To access your favorites and to share your collection, pick "Favorites" from menu. It will open a new view that resembles the Demo view. 

In this view you can pick a favorite using a single tap. A long tap allows you to select multiple favorites and share them with others (to import them in Fractview) or to rename or delete them.

![Demo of Favorites](demo_favorites.png)

## Further reading:
	
* [User Interface and Menus](ui.html): More details on the user interface
* [Creating Custom Fractals](custom.html): Modify parameters to your liking
* [Function Reference](functionref.html): All functions implemented.

Please refer to [the blog](http://fractview.wordpress.com) for more topics on the app.

If you grew up in the 90ies and you liked fractals back then you most likely saw a program called [FractInt](http://www.nahee.com/spanky/www/fractint/fractint.html). This program is the original motivation behind Fractview.

Thanks for using my app. If you miss a feature, please use the feedback function in the play store. Please consider [leaving a rating/review](https://play.google.com/store/apps/details?id=at.searles.fractview).

-- Karl

## For technically interested users

Fractview uses a framework called Renderscript to generate images. A compiler creates some bytecode out of the program and its parameters that are executed in a virtual machine in Renderscript. It is possible to write your own programs in the backing programming language Meelan (that is heavily inspired from Python and C).

All exported data use easily readable Json that can also be manipulated.

Color palettes use the [Lab color space](https://en.wikipedia.org/wiki/Lab_color_space) and two dimensional hermite spline interpolation of the color components. When creating the first version I spent some time to compare various color spaces and nothing beats Lab in my opinion. Fractview is currently the only app known to me that actually supports two dimensional color palettes.
