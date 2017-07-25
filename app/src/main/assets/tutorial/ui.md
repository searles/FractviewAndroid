# User Interface and Menus

Fractview works in each orientation and it will also rotate the image to fill the screen as much as possible. If the image is rotated, a small triangle will point out the top left corner of the image. 

![Triangle showing orientation]()

## Fractview Mainscreen

Fractview allows very intuitive pinch-to-zoom gestures. Dragging one finger on the screen will move the image. Dragging two fingers will stretch and rotate the fractal like in pretty much every map viewer. With three fingers you obtain a shearing effect. Once you release the touch screen, the image is re-rendered.

You can modify this behaviour using some options in UI-Settings:

* Rotation Lock: Prevents rotations apart from 90-degrees angle.
* Confirm Zoom with Tab: After scaling the image, you have to confirm the selection with a single tap.
* Deactivate Zoom: Disables any interaction with the main screen.
* Show Grid: This option displays a grid that makes it easier to 

If you want to undo your changes, you can tap the back button (if there have not been any changes, this will terminate the app).

![Screenshot of the UI-Settings and the menu below it]()

### Technical Comment

* The app uses `double` precision which means you can double the image size about 100 times until the numerical precision will create artifacts that limit further depth. There are some arbitrary-precision fractal renderers, but this is outside the scope of Fractview.
* If you use functions like `sin` or `exp`, the app uses (is forced to use) 32-bit precision
due to the use of the GPU.

### Future development

* Show coordinates.
* Allow editing parameters on-screen.
	+ Allow color cycling
	+ Allow real time modifications of 3d parameters.

## Changing the image size

By default the image size is determined by the screen size. You can change the image size by selecting "Image Size" in the menu and entering a new size. The upper limit is restricted by the memory available (usually around 5000x5000). You can pick a default size that then will be used at start up. I personally use 900x500 on my Nexus 5X. For large renderings I then use sizes around 4000x3000, but I even managed to render images with size 9000x5000.

*Using a very high limit may cause the app to become slow and unstable.*

### Behaviour

* The image size is modified independently of the fractal, ie, tapping "back" will not revert changes of the image size but instead return to the last image using the current image size.
* Changing the ratio of the image size will not disort the image. The image always contains the same inner square instead.

### Future development

* Instead of allowing unlimited image size, add a "Create Rendering" menu.

## Favorites

The specification of a fractal (the "rendering plan") can be stored to favorites. For this, select "Add to favorites" and enter a non-empty name. If there already is an entry of this name in "Favorites", a new name is created by adding "(index)" as a suffix. 

In Favorites you can use a simple tap to return to a previous rendering and explore it further. A long tap opens a menu that lets you rename or delete an entry.

![Favorites image]()

TODO: Import

### Selection mode

After a long tap on an entry you can "Select" the entry. This toggles the selection mode in which you can select and unselect entries using a single tap. In selection mode, there are some new menus:

* Select all: Selects all entries. This is useful if you want to back up your collection.
* Export: All selected entries are put into a simple text file that then is shared.
* Delete: Deletes the selected entries and ends selection mode.

Selection mode ends after tapping the back button.

## Saving the image

You can save the image in PNG format using the ![Share icon](). In the following context menu, you can pick one of the following three options:

* Share image: Share image to some other app (eg share on Facebook, Dropbox, ...)
* Save image: Saves the image to the Media Gallery. You can also decide to instantly add the image to the Favorites under the same name as the file.
* Set as wallpaper: Sets the current image as wallpaper.

All these options will wait until the rendering is finished. If you want to save the image instantly you can select "Skip" in the waiting dialog.

![Screenshot of the waiting dialog]()

## Parameters

In Parameters you can modify parameters that are used to render the fractal. They might be a bit overwhelming in the beginning.

Each parameter is either set internally by the fractal program, or it has some custom value (in this case it is written in bold letters). Using a long tap, you can pick various options, for instance to reset the value to its default.

The most important parameters in the Default program are:

* "Scale": The current zoom, consisting of the current x-vector, y-vector and the current center.
* "max_depth": A fractal is calculated by repeatedly applying a function to itself. "max_depth" is the maximum amount of times this is done. If the edges of the fractals are too
smooth, increase this value. If you pick a too large value (around 10000 or higher), the app will be less responsive to changes. In any case, a value of 5000 should be enough even for very deep zooms.

![Screenshot with two different precisions.]()

* "light": You can select "light" to enable lighting effects.
* "julia_set": By selecting this, you can toggle between Mandelbrot sets and Julia sets (see below for a more detailed explanation what that means).
* "julia_point": The start point for julia sets.
* "bailout palette": Palette for points that exceed the bailout value (see below).
* "lake palette": Palette for points that remain below the bailout value (see below).

You can reset all parameters using "Reset All".

## Palettes and colors

Parameters contains different types of parameters: Integers like `max_depth`, Real values like `epsilon` or `bailout`, complex numbers like `julia_point`, colors and color palettes. Selecting a color palette opens a userfriendly two-dimensional palette editor (complex numbers consist of two dimensions, so why not use two-dimensional color palettes?).

In the palette editor you can use drag-and-drop to copy colors, move and shift columns and rows, and a long tap into the first or last value opens a menu to randomize or delete columns/rows or delete the entire palette. By dragging the last row/column you can resize the entire palette.

![Palette Editor]()

A simple tap on a color opens a color dialog that shows a colored hexagon. The slider on the right allows to modify the brightness. The philosophy behind this color picker is that all colors are arranged in a cube and the slider allows you to select a point in the palette. 

When dragging a color, the drag is locked in a color triangle to make it easier to select some color.

![Color Editor dragging]()

## Presets

In the presets menu you can select a program, and after selecting a program also a parameter set. 

## Copy and paste

The specification of the rendering can be copied to the clipboard (in case you know a bit about programming, it uses Json) and pasted in some test program (I use WhatsApp to share nice fractals with my daughter). This text representation can be pasted into the app again.

![Copying and pasting]()

This is really useful if you found a very nice spot in some fractal, added nice colors to it and just want others to also explore the area around your image.
