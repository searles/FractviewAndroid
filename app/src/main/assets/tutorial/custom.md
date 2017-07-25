# Creating custom fractals

In Fractview, a fractal is rendered using a program and parameters for this program. The program that is used on start-up is called "Default" and it is used to render fractals like the mandelbrot set.

In the following, some parameters are explained. For further information on other parameters please refer to [my blog](http://fractview.wordpress.com).

## "Fractals like the Mandelbrot Set": The Default Preset.

### Orbits, lakes and bailouts

Fractals are usually created by applying some function to the outcome of the same function. For the mandelbrot set, this function is `f(z) = sqr z + p`. `p` can be any complex value, hence the images have two dimensions.

Repeatedly applying the function to itself creates a sequence `<start, f(start), f(f(start)), f(f(f(start))), ...>` which is called the *orbit*. Starting with 0 and picking `p = 2`, the sequence is `<0, 2, 6, 38, 1446>`. This sequence is unbounded. If we pick `p = 0.5`, the sequence is also unbounded although it grows much slower. 

For each point of the screen, a color is picked based on properties of the orbit, most commonly how fast it grows, or simpler, at what index does it exceed a certain value. This value is the *bailout* parameter.

For `p = 0` the sequence remains constant, for `p = -1` it is `<0, -1, 0, -1, ...>`. For other values of p, the orbit will be bounded but chaotic, and actually, these values of `p` are the Mandelbrot set (check out [Wikipedia](https://en.wikipedia.org/wiki/Mandelbrot_set) for more details). Such points are in the "lake" of the fractal. 

For many points in the lake, the orbit converges towards a value. We therefore can stop the calculation once consecutive values are close to each other. The value used for this speed-up is the "epsilon" parameter: If the difference of two consecutive elements in the orbit is below epsilon, the calculation terminates. If you encounter circular lake artifacts at very deep zooms into spiral structures, use a smaller value for epsilon.

![Comparison epsilon circle and no circle]()

### Julia sets and Mandelbrot sets

Depending on the start value of the orbit and the parameter `p` Fractview distinguishes Julia sets and Mandelbrot sets. In Mandelbrot set, the start value of the orbit is a constant ("mandelinit") and `p` is the current point. In Julia sets it is the other way round.

In the Mandelbrot set, a good value for `mandelinit` is a root of the first derivation of function. The function of the mandelbrot set is `f(z) = sqr z + p` and since `f'(z) = 2z` a start value should satisfy `2 z = 0`, therefore `mandelinit` is 0.

For the Julia set, the start value is always `c` and `p` is `julia_point`. The julia set will resemble the Mandelbrot set centered around this point.

![Close up of mandelbrot set with julia next to it]()

### Custom functions

The Default program is useful for fractals that use a polynom (something like `z^n + p`) as input. A nice function is `(1 - z^3) * z * p` (this is similar to the lambda preset). Yet, if you enter this function, you will just obtain a black screen because for a start value of 0, every orbit only consists of zeros.

The first derivation of `f(z) = (1 - z^3) * z * p` is `(1 - z^3) + (-3 * z^2) * z = 1 - 2 * z^3`. A root is therefore `(1 / 2)^(1 / 3)` or shorter `/2 ^ /3`. This should be `mandelinit`.

The Default program uses a method to provide a smooth gradient that depends on a parameter "max_power". This should contain the largest power of `z` in the function. For `(1 - z^3) * z * p = (z - z^4) * p` this is 4. After correcting this, we obtain a nice smooth image.

![Lambda with power 3]()

### Custom values and transfers

There are further parameters "bailout_value", "bailout_transfer", and "lake_value" and "lake_transfer". `*_value` is an expression that returns a real value. It is used as depth information if the image is rendered with a 3d effect. 

Transfer is an expression that returns a complex number. It is used to pick a color from the corresponding palette.

For the 3D effects there are many parameters:

* `valuetransfer` is a function that converts the height information. It is useful to divide it through some constant to smoothen it a bit or simply use `-/value` (same as `-1 / value`).
* There are two angles to specify the position of the light source
* Ambient light, diffuse light and specular reflection specify 
* Shininess is a parameter used in specular reflection

