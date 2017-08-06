# Function Reference

## Extended operations

These operations are not hardcoded in the virtual machine but instead they are expanded
into other ones.

* derive: Returns the derivate of a function. 
	+ Arguments:
		1. Variable to be derived
		2. Function to be derived
		3. (Version 3.2.2 and higher): Variable mappings (otherwise, variables are interpreted as constants)
	+ Errors:
		- If the function is not derivable (eg `conj`).
	+ Example: 
		- `derive(z, z^2 + z + 2)` returns `2z + 1`
		- (from Version 3.2.2): `derive(c, z^2 + c, [[z, foldvalue]])` returns `2 * z * foldvalue + 1`	
	+ Use cases: For Nova fractals and distance estimation.
* newton: Returns the term for the newton approximation to find the root of a function (see [Newton's method](https://en.wikipedia.org/wiki/Newton%27s_method))
	+ Arguments:
		1. Variable parameter
		2. Function of which a root should be found (wrt variable)
	+ Errors:
		- If the function is not derivable (eg `conj`).
	+ Example: `newton(z, z^2 + z + 2)` returns `z - (z^2 + z + 2) / (2z + 1)`
	+ Use cases: For Newton fractals.

## Functions for vectors
	
* length: Returns amount of elements in a vector (constant)
	+ Arguments:
		1. The vector
	+ Example: `length [1, 2, 3]` returns 3

* select: Selects one single argument in the vector that is in argument 2.
	+ Arguments
		1. The index (will always use the positive remainder wrt the length)
		2. The vector
	+ Errors:
		- If the vector is empty (TODO: test!)

## Boolean operations and comparisons

### Logic operations

* and: Logic and
* or: Logic or
* not: Logic negation

### Comparisons

* next: Logic increment operation: Increments an integer variable by 1 and returns true if the result is smaller than another value provided. Equivalent to the
C expression `++i < k` where `i` and `k` are the arguments.
	+ Arguments
		- 1. An integer variable
		- 2. The limit for the value.
* g: Usually written in infix notation `a > b`. Greater than comparison (only available for scalar types real and int).
* ge: Usually written in infix notation `a >= b`. Greater than or equal to comparison (only available for scalar types real and int).
* eq: Usually written in infix notation `a == b`. Equal to comparison (only available for scalar types real and int, but not really useful for real because of rounding mistakes).
* ne: Usually written in infix notation `a >< b`. Not qqual to comparison (only available for scalar types real and int, but not really useful for real because of rounding mistakes).
* le: Usually written in infix notation `a =< b`. Less than or equal to comparison (only available for scalar types real and int).
* l: Usually written in infix notation `a < b`. Less than comparison (only available for scalar types real and int).
	
### Special comparisons for complex numbers

* radless: Equivalent to `rad(a) < b`.
	+ Arguments
		- 1. The complex number `a`
		- 2. The scalar (int or real) `b`
* distless: Equivalent to `dist(a, b) < c`.
		- 1. The complex number `a`
		- 2. The complex number `b`
		- 3. The scalar (int or real) `c`
* radrange: Combination to the previous two: `radrange(a, b, upper, lower, block1, block2)` is a short form of the following code snippet

~~~
radless(a, upper) and { block1, true } or distless(a, b, lower) and { block2, true }
~~~

## Arithmetics 

### Mathematics

#### Standard operations with 64 bit precision

##### Binary

Binary operations can be chained in the following way:

~~~
op(a, b, c, ...) = op(...op(a, b), c)...)
~~~

* add: Addition (usually denoted using infixed `+`)
* sub: Subtraction (usually denoted using infixed `-`)
* mul: Multiplication (usually denoted using infixed `*` or no symbol at all)
	+ Example: `2 x` and `2 * x` are equivalent, but `2 x` binds the strongest. For instance, `2 x^2` is the same as `(2^2) * (x^2)` where `2 * x ^ 2` is 
	in fact `2 * (x ^ 2)`.
	+ TODO: Implementation for `quat`.
* div: Division (usually denoted using infixed `/`).
	+ Remark: Division of integers will always yield a real value.
	+ TODO: Implementation for `quat`.
	+ TODO: Remove integer implementation because of automated conversion
* mod: Remainder of the division, usually denoted using infixed `%`. 
	+ Remark: Only available for integers.
* pow: Power operation (usually denoted using `^`)
	+ Remark: Not implemented for `quat`.
	+ Remark: There are different implementations depending on whether the second argument is an integer, a real or a cplx.
	+ TODO: Remove implementation for `quat`

##### Unary

* neg: Additive inverse, usually denoted using prefixed `-`
* recip: Multiplicative inverse, equivalent to `1 / a`, usually denoted using prefixed `/`
	+ Example: `/3` is one third (about `0.333333...`).
	+ Remark: Reciprocal value of an integer is always a real value.
	+ TODO: Remove integer implementation because of automated conversion
* sqr: Square
	+ TODO (?): Remove implementation for `quat`

#### Standard operations

None of the following operations is available for the quat type.

##### Power functions
	
* sqrt: Square root
	+ Remark: `sqrt` of a negative real will return "not a number". Use cplx as input type if you want a complex result.
	+ TODO: Bug: complex root of 0 returns "not a number"
* exp: Exponential function. Equivalent to `E^x`.
	+ Remark: Since this operation grows very fast but is only supported for 32 bit, values higer than around 40 will return infinity
* log: Logarithm (inverse of exp)
	+ Remark: `log` of a negative number will return "not a number". Use cplx as input type if you want a complex result.

##### Trigonometric functions	

* sin: Sine function
* cos: Cosine function
* tan: Tangens function
* atan: Inverse of the tangens function
	
##### Hyperbolic functions
	
* sinh
* cosh
* tanh
* atanh

#### Non-differentialbe operations

The following operations are applied component-wise to cplx and quat.

* min: Minimum of two values
* max: Maximum of two valuesa(
* abs: Positive value
	+ Example: `abs (-2:-1)` is `2:1`.
* floor: Closest lower integer
	+ Example: `floor (2.5:-1.5)` is `2:-2`.
	+ TODO: Not implemented for quat
* ceil: Closest larger integer
	+ TODO: Not implemented for quat
* fract: Fractional part (equivalent to `x - floor(x)`)
	+ TODO: Not implemented for quat
* circlefn: Maps values between 0 and 1 (-1) to a 
	+ Motivation: Used for rounded orbit traps
	+ TODO: Remove implementation for cplx. It is not needed.
* real2int: Convert a real to an integer. Similar to `floor` but the return type is int.

### Special functions for complex and quat numbers
	
* cons: Creates either a cplx out of two real arguments (mostly using the infix operator `:`)
or a `quat` out of four.
* re: real part of a cplx. Also written as `value.x`.
* im: imaginary part of a cply. Also written as `value.y`.
* rad: Absolute value of a complex number.
* rad2: Squared absolute of a complex number. Faster than `rad` and with double precision.
* dist2: returns the square of the distance of two complex numbers
* dist: returns the distance of two complex numbers
* arc: Argument of a complex number. Value ranges from -pi to +pi.
* arcnorm: arc normalized in the range 0 to 1.
* polar: puts the absolute value into the real part and the argument into the imaginaty part of a complex number
* rect: Inverse of polar.
	+ TODO: This function has a useless output "real"-variant
* scalarmul: Performs a scalar multiplication of two complex numbers
* conj: Complex conjugate number
* dot: Dot-product of two numbers
* rabs: Replaces the real part by its absolute value
* iabs: Replaces the imaginary part by its absolute value
* flip: Exchanges real and imaginary part.
* mandelbrot: Short for `arg1^2 + arg2`
* smooth: Faulty `smoothen` function. Kept for compatibility with old versions of Fractview
	+ TODO: Replace by a macro function
* smoothen: Short form to obtain a smooth gradient for polynomial fractals.
	+ Arguments are in this order: last z value, bailout, max_power.

### For scaling

TODO: There is no support yet to use other scales except for the extern `scale` that is left implicit in most cases. A solution for this will involve a generic solution also for `__ld_palette`.

* map: Maps a point (either two real values or one complex value) to a complex value using the current scale.
	
### Geometry 

* circle(
			new Signature().r(Type.cplx).r(Type.real).r(Type.cplx).w(Type.real)
	) {
* line(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
* segment(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
* box(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.cplx).w(Type.real)
	) {

	// Translate coordinates

### Color handling
	
Colors are represented in multiple ways

- As integers in format `AARRGGBB` (there are some problems with alpha though)
	- The color output is supposed to be of this type.
- As quats (variable consisting of 4 floats)
	+ This quat can either be in LAB format (L ranges from 0 to 100, the others vary but are centered aroung 0)
	+ Or in RGB (all values range from 0 to 1)
	+ The 4th element is the alpha value.

#### Color functions
	
* over(
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
* lab2rgb(
			new Signature().r(Type.quat).w(Type.quat)
	) {
* rgb2lab(
			new Signature().r(Type.quat).w(Type.quat)
	) {
* int2rgb(
			new Signature().r(Type.integer).w(Type.quat)
	) {
* rgb2int(
			new Signature().r(Type.quat).w(Type.integer)
	) {
* int2lab(
			new Signature().r(Type.integer).w(Type.quat)
	) {
* lab2int(
			new Signature().r(Type.quat).w(Type.integer)
	) {

## Internal operations

* `mov`: Internal assignment function. TODO: Rename to `__mov`
* `whileOp`: Internal `while`-loop. TODO: Rename to `__while`
* `ifOp`: Internal `if`-condition. TODO: Rename to `__if`
* `forOp`: Internal `for`-iteration. TODO: Rename to `__for`
* `__jump`: Jump
* `__jumprel`: Relative jump
* `__ld_palette`: Loads the palette from storage. TODO: Palettes are very specialized. Having this function hardcoded in meelan is not very pretty.
	
## Experimental operations
	
* solve2: Solution of the quadratic equation `a * z^2 + b * z + c`.
	+ COMMENT: This function might be modified or removed in the future!
	+ Arguments:
		1-3. a, b and c
		4. Integer 1 or 2 for the index of the solution.
	+ Errors: 
		- If the index is neither 1 nor 2
	+ Examples: 
		- `solve2(1, 0, -1, 1)` returns `1`
		- `solve2(1, 0, -1, -1)` returns `-1`
	+ Errors:
		- If the function is not derivable (eg `conj`).
	+ Use cases: 
		- To easily get mandelinit for power-3 fractals.

## Want to have

* solve: Finds a root of a polynomial function up to at most degree 4
	+ Arguments:
		1. Variable for which the equation `function = 0` should be 0
		2. Function itself
	+ Use cases:
		- `function` is `z^4-2*z^3+z+c`
		- `mandelinit` is `solve(z, derive(function))`

