#pragma version(1)
#pragma rs java_package_name(at.searles.fractview)

rs_script gScript;
rs_allocation gOut;
rs_allocation gTileOut;

// --- Part 0: Data structures containing arguments ---

static double __attribute__((overloadable)) convert_real(int d) { return d; }
static double __attribute__((overloadable)) convert_real(double d) { return d; }
static double2 __attribute__((overloadable)) convert_cplx(int d) { return (double2) {d, 0}; }
static double2 __attribute__((overloadable)) convert_cplx(double d) { return (double2) {d, 0}; }
static double2 __attribute__((overloadable)) convert_cplx(double2 d) { return d; }
static double4 __attribute__((overloadable)) convert_quat(int d) { return (double4) {d, 0, 0, 0}; }
static double4 __attribute__((overloadable)) convert_quat(double d) { return (double4) {d, 0, 0, 0}; }
static double4 __attribute__((overloadable)) convert_quat(double2 d) { return (double4) {d.x, d.y, 0, 0}; }

/* --- New precision --- */

union double_bits {
    long l;
    double d;
};

static double __attribute__((overloadable)) nanum(void) {
    union double_bits bits;
    bits.l = 0x7ff7ffffffffffffl;
    return bits.d;
}

static double __attribute__((overloadable)) infty(int signum) {
    union double_bits bits;
    bits.l = 0x7ff0000000000000l;
    return signum ? -bits.d : bits.d;
}



static double __attribute__((overloadable)) frexp(double d, long *exponent) {
    union double_bits bits;
    bits.d = d;
    *exponent = (((bits.l >> 52) & 0x7ffl) - 1023l);

    bits.l = (bits.l & 0x800fffffffffffffl) | 0x3ff0000000000000l;
    return bits.d;
}

static double __attribute__((overloadable)) ldexp(double mantissa, long exponent) {
    exponent += 1023l;

    if(exponent < 0) {
        return 0.;
    } else if(exponent > 2047) {
        return mantissa < 0 ? infty(1) : infty(0);
    }

    exponent <<= 52;

    union double_bits bits;
    bits.d = mantissa;

    bits.l = (bits.l & 0x800fffffffffffffl) | exponent;

    return bits.d;
}



static double __attribute__((overloadable)) sqrt(double d) {
    if(d < 0) {
        return nanum();
    } else if(d == 0) {
        return 0.;
    } else {
        long exponent;

        double mantissa = frexp(d, &exponent);

        if((exponent & 1l) != 0l) {
            exponent--;
            mantissa = mantissa * 0.5 + 1;
        }

        double x = ldexp(mantissa,  exponent >> 1l);

        // start newton approximation
        for(int i = 0; i < 4; ++i) {
            x = (d / x + x) / 2.;
        }

        return x;
    }
}

/*
 * Based on the observation that you can pull out the exponent of sqrt(x^2 + y^2).
 * Better performance for large values
 * @param x
 * @param y
 * @return
 */
static double __attribute__((overloadable)) rad(double x, double y) {
    if(x < 0) x = -x;
    if(y < 0) y = -y;
    if(x < y) { double t = x; x = y; y = t; }

    // x and y are positive and x is larger.
    double m1, m2;
    long e1, e2;

    m1 = frexp(x, &e1);
    m2 = frexp(y, &e2);

    double x2 = m1;
    double y2 = ldexp(m2, e2 - e1);

    double rad = sqrt(x2 * x2 + y2 * y2);

    long e3;
    double m3 = frexp(rad, &e3);

    return ldexp(m3, e1 + e3);
}

static double __attribute__((overloadable)) atan2(double y, double x) {
    double m1, m2;
    long e1, e2;

    m1 = frexp(x, &e1);
    m2 = frexp(y, &e2);

    if(e1 < e2) {
        e1 -= e2;
        e2 = 0;
    } else {
        e2 -= e1;
        e1 = 0;
    }

    double x2 = ldexp(m1, e1);
    double y2 = ldexp(m2, e2);

    return atan2((float) y2, (float) x2);
}

/*
 * log2 (m * 2^e) = log m + e
 * @param d
 * @return
 */
static double __attribute__((overloadable)) log2(double d) {
    if(d < 0) {
        return nanum();
    } else {
        long e;
        double m = frexp(d, &e);

        return log2((float) m) + e;
    }
}

/*
 * 2^d = m * 2^e [1 <= m < 2]
 * d = log2(m) + e [0 <= log2(n) < 1]
 * Thus, e = floor(d), m = exp2(fract(d)). Observe that floor(d) must be in the range [-1024, 1023].
 * @param d
 * @return
 */
static double __attribute__((overloadable)) exp2(double d) {
    double exponent = floor((float) d);

    if(d < -1024) {
        return 0;
    } else if(d > 1023) {
        return infty(0);
    } else {
        double ld = d - exponent; // must be between 0 and 1.

        double base = exp2((float) ld); // no exp2 in java...

        // base is between 1 and 2.
        long e;
        double m = frexp(base, &e);

        return ldexp(m, (long) (exponent) + e);
    }
}

/*
 * x ^ y = (m * 2^e) ^ y = m' * 2^e'
 * (m * 2^e) ^ y = m' * 2^e' ==> / log2
 * y * log2(m) + y * e = e' + log2(m')
 * @param x
 * @param y
 * @return
 */
static double __attribute__((overloadable)) pow(double x, double y) {
    if(x < 0) {
        return nanum();
    } else if(x == 0) {
        return y > 0 ? 0 : y == 0 ? 1 : infty(0);
    } else {
        long e;
        double m = frexp(x, &e);

        double ld = log2(m);

        return exp2((ld + e) * y);
    }
}

static const double ln2 = 0.693147180559945309417232121458;

static double __attribute__((overloadable)) exp(double f) { return exp2(f / ln2); }
static double __attribute__((overloadable)) log(double f) { return log2(f) * ln2; }

static double __attribute__((overloadable)) sinh(double f) { double e = exp(f); return (e - 1. / e) / 2.; }
static double __attribute__((overloadable)) cosh(double f) { double e = exp(f); return (e + 1. / e) / 2.; }
static double __attribute__((overloadable)) tanh(double f) { double e = exp(f); return (e - 1. / e) / (e + 1. / e); }

/*
static double __attribute__((overloadable)) sqrt(double d) { return sqrt((float) d); }
static double __attribute__((overloadable)) rad(double x, double y) { return sqrt(x * x + y * y); }
static double __attribute__((overloadable)) log(double f) { return log((float) f); }
static double __attribute__((overloadable)) exp(double f) { return exp((float) f); }
static double __attribute__((overloadable)) pow(double y, double x) { return pow((float) y, (float) x); }
static double __attribute__((overloadable)) sinh(double f) { return sinh((float) f); }
static double __attribute__((overloadable)) cosh(double f) { return cosh((float) f); }
static double __attribute__((overloadable)) tanh(double f) { return tanh((float) f); }
static double __attribute__((overloadable)) atan2(double y, double x) { return atan2((float) y, (float) x); }
*/

// TODO Increase precision of the following

static double __attribute__((overloadable)) sin(double f) { return sin((float) f); }
static double __attribute__((overloadable)) cos(double f) { return cos((float) f); }
static double __attribute__((overloadable)) tan(double f) { return tan((float) f); }
static double __attribute__((overloadable)) atan(double f) { return atan((float) f); }
static double __attribute__((overloadable)) atanh(double f) { return atanh((float) f); }
static double __attribute__((overloadable)) cbrt(double f) { return cbrt((float) f); }

// The next ones are out of scope for increased precision

static double __attribute__((overloadable)) floor(double f) { return floor((float) f); }
static double __attribute__((overloadable)) ceil(double d) { return ceil((float) d); }
static double __attribute__((overloadable)) fract(double d) { return fract((float) d); }


// FIXME scale is missing

// structures cannot contain pointers in renderscript
struct palette {
    uint32_t w;
    uint32_t h;
    uint32_t offset; // index of first lab_surface in palette_data.
};

struct lab_surface {
    rs_matrix4x4 L;
    rs_matrix4x4 a;
    rs_matrix4x4 b;
    rs_matrix4x4 alpha;
};

// here goes the data

// scale
double2 xx;
double2 yy;
double2 tt;

// image
int width;
int height;

// program

int *code;
int codeLen;

// colors

struct palette * palettes;
struct lab_surface * palette_data;

static double2 __attribute__((overloadable)) mapcoordinates(double x, double y) {
    // todo use index to use multiple scales.
    double centerX = (width - 1.) / 2.;
    double centerY = (height - 1.) / 2.;
    double factor = 1. / (centerX < centerY ? centerX : centerY);

    double a = xx.x * factor;
    double b = yy.x * factor;
    double c = xx.y * factor;
    double d = yy.y * factor;

    double e = tt.x - (a * centerX + b * centerY);
    double f = tt.y - (c * centerX + d * centerY);

    return (double2) { a * x + b * y + e, c * x + d * y + f };
}

static double2 __attribute__((overloadable)) mapcoordinates(double2 xy) {
    return mapcoordinates(xy.x, xy.y); // apply affine tranformation
}

// ===================== from here serious renderscript =========================

// From here functions that are also needed in RS:
static int __attribute__((overloadable)) add(int a, int b) { return a + b;}
static double __attribute__((overloadable)) add(double a, double b) { return a + b;}
static double2 __attribute__((overloadable)) add(double2 a, double2 b) { return (double2){ a.x + b.x, a.y + b.y }; }
static double4 __attribute__((overloadable)) add(double4 a, double4 b) { return (double4){ a.s0 + b.s0, a.s1 + b.s1, a.s2 + b.s2, a.s3 + b.s3 }; }

static int __attribute__((overloadable)) sub(int a, int b) { return a - b;}
static double __attribute__((overloadable)) sub(double a, double b) { return a - b; }
static double2 __attribute__((overloadable)) sub(double2 a, double2 b) { return (double2){ a.x - b.x, a.y - b.y }; }
static double4 __attribute__((overloadable)) sub(double4 a, double4 b) { return (double4){ a.s0 - b.s0, a.s1 - b.s1, a.s2 - b.s2, a.s3 - b.s3 }; }

static int __attribute__((overloadable)) mul(int a, int b) { return a * b; }
static double __attribute__((overloadable)) mul(double a, double b) { return a * b; }
static double2 __attribute__((overloadable)) mul(double2 a, double2 b) { return (double2){ a.x * b.x - a.y * b.y, a.x * b.y + a.y * b.x }; }
static double4 __attribute__((overloadable)) mul(double4 a, double4 b) {
	return (double4){
		a.s0 * b.s0 - a.s1 * b.s1 - a.s2 * b.s2 - a.s3 * b.s3,
		a.s0 * b.s1 + a.s1 * b.s0 + a.s2 * b.s3 - a.s3 * b.s2,
		a.s0 * b.s2 - a.s1 * b.s3 + a.s2 * b.s0 + a.s3 * b.s1,
		a.s0 * b.s3 + a.s1 * b.s2 - a.s2 * b.s1 + a.s3 * b.s0 };
}

static double2 __attribute__((overloadable)) scalarmul(double2 a, double2 b) { return (double2){ a.x * b.x, a.y * b.y }; }


static int __attribute__((overloadable)) div(int a, int b) { return a / b; }
static double __attribute__((overloadable)) div(double a, double b) { return a / b; }
static double2 __attribute__((overloadable)) div(double2 a, double2 b) { double r = b.x * b.x + b.y * b.y; return (double2){ (a.x * b.x + a.y * b.y) / r, (-a.x * b.y + a.y * b.x) / r }; }
static double4 __attribute__((overloadable)) div(double4 a, double4 b) {
	double det = b.s0 * b.s0 + b.s1 * b.s1 + b.s2 * b.s2 + b.s3 * b.s3;
	return (double4) {
		(b.s0 * a.s0 + b.s1 * a.s1 + b.s2 * a.s2 + b.s3 * a.s3) / det,
		(b.s0 * a.s1 - b.s1 * a.s0 - b.s2 * a.s3 + b.s3 * a.s2) / det,
		(b.s0 * a.s2 + b.s1 * a.s3 - b.s2 * a.s0 - b.s3 * a.s1) / det,
		(b.s0 * a.s3 - b.s1 * a.s2 + b.s2 * a.s1 - b.s3 * a.s0) / det
	};
}

static int __attribute__((overloadable)) mod(int a, int b) { int c = a % b; return c < 0 ? b + c : c; }

static int __attribute__((overloadable)) neg(int a) { return -a; }
static double __attribute__((overloadable)) neg(double a) { return -a; }
static double2 __attribute__((overloadable)) neg(double2 a) { return (double2){ -a.x, -a.y }; }
static double4 __attribute__((overloadable)) neg(double4 a) { return (double4){ -a.s0, -a.s1, -a.s2, -a.s3 }; }

static double __attribute__((overloadable)) recip(int a) { return 1. / (double) a; }
static double __attribute__((overloadable)) recip(double a) { return 1. / a; }
static double2 __attribute__((overloadable)) recip(double2 a) { double r = a.x * a.x + a.y * a.y; return (double2){ a.x / r, -a.y / r }; }
static double4 __attribute__((overloadable)) recip(double4 a) { return (double4){ -a.s0, -a.s1, -a.s2, -a.s3 }; } // fixme


// static int __attribute__((overloadable)) min(int a, int b) { return a < b ? a : b;}
static double __attribute__((overloadable)) min(double a, double b) { return a < b ? a : b;}
static double2 __attribute__((overloadable)) min(double2 a, double2 b) { return (double2){ min(a.x, b.x), min(a.y, b.y) }; }
static double4 __attribute__((overloadable)) min(double4 a, double4 b) { return (double4){ min(a.s0, b.s0), min(a.s1, b.s1), min(a.s2, b.s2), min(a.s3, b.s3) }; }

// static int __attribute__((overloadable)) max(int a, int b) { return a < b ? b : a;}
static double __attribute__((overloadable)) max(double a, double b) { return a < b ? b : a;}
static double2 __attribute__((overloadable)) max(double2 a, double2 b) { return (double2){ max(a.x, b.x), max(a.y, b.y) }; }
static double4 __attribute__((overloadable)) max(double4 a, double4 b) { return (double4){ max(a.s0, b.s0), max(a.s1, b.s1), max(a.s2, b.s2), max(a.s3, b.s3) }; }

static double2 __attribute__((overloadable)) cons(double x, double y) { return (double2){ x, y }; }
static double4 __attribute__((overloadable)) cons(double s0, double s1, double s2, double s3) { return (double4){ s0, s1, s2, s3 }; }


// static int __attribute__((overloadable)) abs(int a) { return a < 0 ? -a : a; }
static double __attribute__((overloadable)) abs(double a) { return a < 0. ? -a : a; }
static double2 __attribute__((overloadable)) abs(double2 a) { return (double2){ abs(a.x), abs(a.y) }; }
static double4 __attribute__((overloadable)) abs(double4 a) { return (double4){ abs(a.s0), abs(a.s1), abs(a.s2), abs(a.s3) }; }

static int __attribute__((overloadable)) sqr(int a) { return a * a; }
static double __attribute__((overloadable)) sqr(double a) { return a * a; }
static double2 __attribute__((overloadable)) sqr(double2 a) { return (double2){ a.x * a.x - a.y * a.y, 2 * a.x * a.y }; }
static double4 __attribute__((overloadable)) sqr(double4 a) { return (double4){ sqr(a.s0), sqr(a.s1), sqr(a.s2), sqr(a.s3) }; } // fixme

static double __attribute__((overloadable)) rad(double2 f) { return rad(f.x, f.y); }
static double __attribute__((overloadable)) rad2(double2 f) { return f.x * f.x + f.y * f.y; }

static double __attribute__((overloadable)) dot(double2 a, double2 b) { return a.x * b.x + a.y * b.y; }
static double __attribute__((overloadable)) dist2(double2 a, double2 b) { return rad2(a - b); }
static double __attribute__((overloadable)) dist(double2 a, double2 b) { return rad(a - b); }

static double __attribute__((overloadable)) arc(double2 f) { return atan2(f.y, f.x); }
static double __attribute__((overloadable)) arcnorm(double2 f) { return atan2(f.y, f.x) / (2 * M_PI); }

static double __attribute__((overloadable)) re(double2 f) { return f.x; }
static double __attribute__((overloadable)) im(double2 f) { return f.y; }

static int __attribute__((overloadable)) real2int(double f) { return (int) f; }

static double2 __attribute__((overloadable)) sqrt(double2 f) {
	double r = rad(f);
	double2 ret = { sqrt((r + f.x) / 2), sqrt((r - f.x) / 2) };
	if(f.y < 0) ret.y = -ret.y;
	return ret;
}


static double2 __attribute__((overloadable)) conj(double2 a) { return (double2){ a.x, -a.y }; }
static double2 __attribute__((overloadable)) rabs(double2 a) { return (double2){ a.x < 0 ? -a.x : a.x, a.y }; }
static double2 __attribute__((overloadable)) iabs(double2 a) { return (double2){ a.x, a.y < 0 ? -a.y : a.y }; }
static double2 __attribute__((overloadable)) flip(double2 a) { return (double2){ a.y, a.x }; }


static int __attribute__((overloadable)) pow(int base, int exp) {
    int r = 1; if(exp < 0) { base = 1 / base; exp = -exp; }
    for(;exp; exp >>= 1, base *= base) if(exp & 1) r *= base;
    return r;
}

static double __attribute__((overloadable)) pow(double base, int exp) {
    double r = 1; if(exp < 0) { base = 1 / base; exp = -exp; }
    for(;exp; exp >>= 1, base *= base) if(exp & 1) r *= base;
    return r;
}

static double2 __attribute__((overloadable)) pow(double2 base, int exp) {
    double2 r = (double2){1., 0.};
    if(exp < 0) { base = recip(base); exp = -exp; }
    for(;exp; exp >>= 1, base = mul(base, base)) if(exp & 1) r = mul(r, base);
    return r;
}

static double4 __attribute__((overloadable)) pow(double4 base, int exp) {
    double4 r = (double4){1., 0., 0., 0.};
    if(exp < 0) { base = recip(base); exp = -exp; }
    for(;exp; exp >>= 1, base = mul(base, base)) if(exp & 1) r = mul(r, base);
    return r;
}

static double2 __attribute__((overloadable)) log(double2 f) {
	return (double2) { log(rad(f)), arc(f) };
}

static double2 __attribute__((overloadable)) exp(double2 f) {
	double e = exp(f.x);
	double c = cos(f.y);
	double s = sin(f.y);
	return (double2) { e * c, e * s };
}

static double2 __attribute__((overloadable)) pow(double2 base, double power) {
	if(base.x == 0 && base.y == 0) return (double2) {0, 0};

    // FIXME
	// base^power = exp(log base * power) =
	// = exp ((log rad base + i arc base) * power)
	// = exp (power * log rad base + i power arc base)
	// = rad base ^ power * exp(i power arc base)
	// = rad base ^ power * (cos power arc base + i sin power arc base)
	double r = pow(rad(base), power);
	double pa = power * atan2(base.y, base.x);

	double c = cos(pa); double s = sin(pa);

	return (double2) {r * c, r * s};
}

static double2 __attribute__((overloadable)) pow(double2 base, double2 power) {
    // FIXME
	// base^power = exp(log base * power) =
	// = exp ((log rad base + i ab) * power)
	// = exp ((log rb + i arc base) * power)
	if(base.x == 0 && base.y == 0) return (double2) {0, 0};

	double lrb = log(rad(base));
	double ab = atan2(base.y, base.x);

	// = exp (lrb * pr - ab * pi, lrb * pi + ab * pr)
	double2 prod = (double2) {lrb * power.x - ab * power.y, lrb * power.y + ab * power.x};

	return exp(prod);
}

static double2 __attribute__((overloadable)) sin(double2 a) {
	double2 eia = exp((double2) {-a.y, a.x});
	double2 eia2 = sqr(eia);

    eia2 = (double2) { -eia2.y, eia2.x - 1 };
    eia = (double2) { 2 * eia.x, 2 * eia.y };

	return div(eia2, eia);
}

static double2 __attribute__((overloadable)) cos(double2 a) {
	// cos x = (e^iz - e^-iz) / 2i = (e^2iz + 1) / (2i e^iz)
	double2 eia = exp((double2) {-a.y, a.x});
	double2 eia2 = sqr(eia);

    eia2.x += 1;
    eia = (double2) { 2 * eia.x, 2 * eia.y };

	return div(eia2, eia);
}

static double2 __attribute__((overloadable)) tan(double2 a) {
	// (e^2iz - 1) / (e^2iz + 1)
	double2 eia = exp((double2) {-2 * a.y, 2 * a.x});
	double2 eiai = (double2) {-eia.y, eia.x + 1};

    eia.x -= 1;

	return div(eia, eiai);
}

static double2 __attribute__((overloadable)) atan(double2 a) {
	double2 b = div((double2) { 1 + a.y, -a.x }, (double2) { 1 - a.y, a.x }); // (1 - a*i) / (1 + a*i)
	double2 c = log(b);
 	return (double2) { -c.y / 2, c.x / 2};
}

static double2 __attribute__((overloadable)) sinh(double2 a) {
	double2 ea = exp(a);
	double2 ea2 = sqr(ea);

	return div((double2) {ea2.x - 1, ea2.y}, (double2) {2 * ea.x, 2 * ea.y});
}

static double2 __attribute__((overloadable)) cosh(double2 a) {
	double2 ea = exp(a);
	double2 ea2 = sqr(ea);

	return div((double2){ea2.x + 1, ea2.y}, (double2){2 * ea.x, 2 * ea.y});
}

static double2 __attribute__((overloadable)) tanh(double2 a) {
	double2 ea = exp((double2) {2 * a.x, 2 * a.y});

	return div((double2){ea.x - 1, ea.y}, (double2){ea.x + 1, ea.y});
}

static double2 __attribute__((overloadable)) atanh(double2 a) {
   	double2 b = div((double2){1 + a.x, a.y}, (double2){1 - a.x, -a.y});
   	double2 c = log(b);
   	return (double2) { c.x / 2, c.y / 2};
}

static double2 __attribute__((overloadable)) mandelbrot(double2 z, double2 c) {
   	return add(sqr(z), c);
}

static double __attribute__((overloadable)) circlefn(double z) {
	return sqrt(1 - z * z);
}

static double2 __attribute__((overloadable)) circlefn(double2 z) {
	double2 zsqr = sqr(z);
	return sqrt((double2) { 1 - zsqr.x, -zsqr.y });
}

static double2 __attribute__((overloadable)) floor(double2 z) {
	return (double2) { floor(z.x), floor(z.y) };
}

static double2 __attribute__((overloadable)) ceil(double2 z) {
	return (double2) { ceil((float) z.x), ceil((float) z.y) };
}

static double2 __attribute__((overloadable)) fract(double2 z) {
	return (double2) { fract((float) z.x), fract((float) z.y) };
}

static double2 polar(double2 z) {
	return (double2) { rad(z), arc(z.x) };
}

static double2 rect(double2 z) {
	return z.r * (double2) { cos(z.y), sin(z.y) };
}

static double circle(double2 mid, double r, double2 p) {
	return abs(r - rad(p - mid));
}

static double line(double2 p1, double2 p2, double2 p) {
	double a = p2.y - p1.y;
	double b = p1.x - p2.x;
	double c = p2.x * p1.y - p2.y * p1.x;
	double len = sqrt(a * a + b * b);
	return abs(a * p.x + b * p.y + c) / len;
}

static double segment(double2 a, double2 b, double2 p) {
    // see http://stackoverflow.com/questions/25800286/how-to-get-the-point-to-line-segment-distance-in-2d
    double dab = rad(sub(a, b));
    double dap = rad(sub(a, p));
    double dbp = rad(sub(b, p));

	if(dot(sub(a, b), sub(p, b)) * dot(sub(b, a), sub(p, a)) >= 0) {
	    double det = a.x * b.y + b.x * p.y + p.x * a.y -
	    	p.x * b.y - b.x * a.y - a.x * p.y;

	    return abs(det) / dab;
	} else {
	    return min(dap, dbp);
	}
}


static double box(double2 p1, double2 p2, double2 p) {
	double d1 = segment(p1, (double2) {p1.x, p2.y}, p);
	double d2 = segment(p1, (double2) {p2.x, p1.y}, p);
	double d3 = segment(p2, (double2) {p1.x, p2.y}, p);
	double d4 = segment(p2, (double2) {p2.x, p1.y}, p);

	return min(min(d1, d2), min(d3, d4));
}

static double4 over(double4 a, double4 b) {    // first one is in the front
    double alpha = a.s3 + b.s3 - a.s3 * b.s3;
    if(alpha == 0) return (double4) {0, 0, 0, 0};

    return (double4) {
        a.s0 * a.s3 / alpha + b.s0 * b.s3 * (1 - a.s3) / alpha,
        a.s1 * a.s3 / alpha + b.s1 * b.s3 * (1 - a.s3) / alpha,
        a.s2 * a.s3 / alpha + b.s2 * b.s3 * (1 - a.s3) / alpha,
        alpha
    };
}

static int __attribute__((overloadable)) radrange(double2 z, double2 zz, double bailout, double epsilon, int bailoutLabel, int epsilonLabel, int falseLabel) {
    if(rad2(z) > bailout * bailout) return bailoutLabel;

    double2 dz2 = sub(z, zz);
    if(rad2(dz2) < epsilon * epsilon) return epsilonLabel;

    return falseLabel;
}

static double finv(double t) {
    return ((t > (216.f / 24389.f)) ?
            t * t * t : (108.f / 841.f * (t - 4.f / 29.f)));
}

static double K(double g) {
    if(g > 0.0031308) {
        return 1.055 * pow(g, 1. / 2.4) - 0.055;
    } else {
        return 12.92 * g;
    }
}

static double4 lab2rgb(double4 lab) {
    double fx = (lab.s0 + 16.) / 116. + lab.s1 / 500.;
    double fy = (lab.s0 + 16.) / 116.;
    double fz = (lab.s0 + 16.) / 116. - lab.s2 / 200.;

    double X = 0.9505 * finv(fx);
    double Y = 1. * finv(fy);
    double Z = 1.0890 * finv(fz);

    double r0 = X * 3.2404542 - Y * 1.5371385 - Z * 0.4985314; // red
    double g0 = -X * 0.9692660 + Y * 1.8760108 + Z * 0.0415560; // green
    double b0 = X * 0.0556434 - Y * 0.2040259 + Z * 1.0572252; // blue

    return (double4) {K(r0), K(g0), K(b0), lab.s3};
}



static double f(double t) {
    return ((t > (216. / 24389.)) ?
            cbrt(t) : (841. / 108. * t + 4. / 29.));
}

static double g(double K) {
    if(K > 0.04045) {
        return pow((K + 0.055) / 1.055f, 2.4);
    } else {
        return K / 12.92;
    }
}

static double4 rgb2lab(double4 rgb) {
    // fixme
    // clamp values


    // Convert to sRGB
    double r0 = g(rgb.s0);
    double g0 = g(rgb.s1);
    double b0 = g(rgb.s2);

    // see http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
    double X = 0.4124564 * r0 + 0.3575761 * g0 + 0.1804375 * b0;
    double Y = 0.2126729 * r0 + 0.7151522 * g0 + 0.0721750 * b0;
    double Z = 0.0193339 * r0 + 0.1191920 * g0 + 0.9503041 * b0;

    double fx = f(X / 0.9505);
    double fy = f(Y / 1.);
    double fz = f(Z / 1.0890);

    return (double4) {116. * fy - 16., 500. * (fx - fy), 200. * (fy - fz), rgb.s3};
}


static int rgb2int(double4 rgb) {
    int r = (int) (256. * rgb.s0);
    int g = (int) (256. * rgb.s1);
    int b = (int) (256. * rgb.s2);
    int a = (int) (256. * rgb.s3);

    // clamp values
    if(r < 0) r = 0; else if(r > 255) r = 255;
    if(g < 0) g = 0; else if(g > 255) g = 255;
    if(b < 0) b = 0; else if(b > 255) b = 255;
    if(a < 0) a = 0; else if(a > 255) a = 255;

    return a << 24 | r << 16 | g << 8 | b;
}


static double4 int2rgb(int i) {
    return (double4) {
        ((i & 0x00ff0000) >> 16) / 255.,
        ((i & 0x0000ff00) >> 8) / 255.,
        ((i & 0x000000ff)) / 255.,
        ((i & 0xff000000) >> 24) / 255.,
     };
}

static int lab2int(double4 lab) {
    return rgb2int(lab2rgb(lab));
}

static double4 int2lab(int rgb) {
    return rgb2lab(int2rgb(rgb));
}
// parameter zsqr is component-wise sqr
static double smooth(double2 z, double bailout, double power) {
	// bad function!
	double bv = z.x * z.x + z.y * z.y;
	double s = log(log(bv) / log(bailout)) / log(power);
	return 1 - s;
}

// parameter zsqr is component-wise sqr
static double smoothen(double2 z, double bailout, double power) {
	// 1 + ln(ln M / ln r0) / ln p

	double s = log(2 * log(bailout) / log(rad2(z))) / log(power);
	return 1 + s;
}

static double2 __attribute((overloadable)) norm(double2 d) {
    return d / rad(d);
}

/*static double2 __attribute__((overloadable)) floor(double2 d) {
    return (double2) { floor(d.x), floor(d.y) };
}*/

static void __attribute((overloadable)) debug(int i) {
    rsDebug("int", i);
}

static void __attribute((overloadable)) debug(double d) {
    rsDebug("double", d);
}

static void __attribute((overloadable)) debug(double2 d2) {
    rsDebug("double2", d2);
}

static void __attribute((overloadable)) debug(double4 d4) {
    rsDebug("double4", d4);
}

// helper function for the palette:
static double z(rs_matrix4x4 * m, double2 c) {
    // assert 0 <= x <= 1
    // assert 0 <= y <= 1
    // XXX It is transposed, but why?
    return (((((((m->m[15] * c.y) + m->m[11]) * c.y + m->m[7]) * c.y) + m->m[3]) * c.x +
            (((((m->m[14] * c.y) + m->m[10]) * c.y + m->m[6]) * c.y) + m->m[2])) * c.x +
            (((((m->m[13] * c.y) + m->m[9]) * c.y + m->m[5]) * c.y) + m->m[1])) * c.x +
            (((((m->m[12] * c.y) + m->m[8]) * c.y + m->m[4]) * c.y) + m->m[0]);
}


// following order: l, a, b, alpha.
static double4 __attribute__((overloadable)) palette_lab(int i, double2 xy) {
    uint32_t x0, y0;

    struct palette * p = &(palettes[i]);

    //rsDebug("w = ", p->w);
    //rsDebug("h = ", p->h);

    // scale up (color palette has range 0..width-1/0..height-1)
    xy.x *= p->w;
    xy.y *= p->h;

    //rsDebug("x = ", c.x);
    //rsDebug("y = ", c.y);

    double2 xy0 = floor(xy);

    // modulo behaves weird in renderscript for negative numbers.
    //

    if(xy0.x < 0) {
        x0 = (uint32_t) -xy0.x;
        x0 %= p->w;
        if(x0 != 0) {
            x0 = p->w - x0;
        }
    } else  {
        x0 = (uint32_t) xy0.x;
        x0 %= p->w;
    }

    if(xy0.y < 0) {
        y0 = (uint32_t) -xy0.y;
        y0 %= p->h;
        if(y0 != 0) {
            y0 = p->h - y0;
        }
    } else  {
        y0 = (uint32_t) xy0.y;
        y0 %= p->h;
    }

    //rsDebug("x0 = ", x0);
    //rsDebug("y0 = ", y0);

    // determine which palette to use
    uint32_t offset = p->offset + (x0 + y0 * p->w);

    xy = sub(xy, xy0);

    //rsDebug("cx = ", c.x);
    //rsDebug("cy = ", c.y);

    double4 retval;

    // get all z-values
    retval.s0 = z(&(palette_data[offset].L), xy);
    retval.s1 = z(&(palette_data[offset].a), xy);
    retval.s2 = z(&(palette_data[offset].b), xy);
    retval.s3 = z(&(palette_data[offset].alpha), xy);

    return retval;
}

static double4 __attribute__((overloadable)) palette_lab(int i, double x, double y) {
	return palette_lab(i, (double2) {x, y});
}

static int palette_int(int i, double2 xy) {
    return lab2int(palette_lab(i, xy));
}


// ------------------- Data --------------------


static uchar4 calc(int x, int y) {
    // temp vars
    int ti_0, ti_1;
    float tf_0, tf_1, tf_2;
    double td_0;
    float2 tf2_0, tf2_1;
    float4 tf4_0, tf4_1;
    double2 td2_0, td2_1;

    // program counter + current instruction
    int * is = code;

	int data[192];
	data[0] = x; data[1] = y;
	// int* color = &data[2];
	data[3] = width; data[4] = height;

    // The VM requires two int-arrays to operate:
    // * The data-array holds all registers and thus is not shared amongst instances.
    //   The required size is obtained by calling 'dataSize' in IntCode.
    // * The code-array, that contains the actual program. It is read-only.

    int pc = 0; // program counter, current index in 'code'-array

    while(pc < codeLen) {
        switch(code[pc]) {
            // Cons with 20 cases

            case   0: // Cons[real, real, cplxReg]
                *((double2*) &data[code[pc + 5]]) = (double2) {*((double*) &code[pc + 1]), *((double*) &code[pc + 3])}; pc += 6;
                break;
            case   1: // Cons[real, realReg, cplxReg]
                *((double2*) &data[code[pc + 4]]) = (double2) {*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])}; pc += 5;
                break;
            case   2: // Cons[realReg, real, cplxReg]
                *((double2*) &data[code[pc + 4]]) = (double2) {*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])}; pc += 5;
                break;
            case   3: // Cons[realReg, realReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = (double2) {*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])}; pc += 4;
                break;
            case   4: // Cons[real, real, real, real, quatReg]
                *((double4*) &data[code[pc + 9]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &code[pc + 3])}; pc += 10;
                break;
            case   5: // Cons[real, real, real, realReg, quatReg]
                *((double4*) &data[code[pc + 8]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &code[pc + 3])}; pc += 9;
                break;
            case   6: // Cons[real, real, realReg, real, quatReg]
                *((double4*) &data[code[pc + 8]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &code[pc + 3])}; pc += 9;
                break;
            case   7: // Cons[real, real, realReg, realReg, quatReg]
                *((double4*) &data[code[pc + 7]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &code[pc + 3])}; pc += 8;
                break;
            case   8: // Cons[real, realReg, real, real, quatReg]
                *((double4*) &data[code[pc + 8]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])}; pc += 9;
                break;
            case   9: // Cons[real, realReg, real, realReg, quatReg]
                *((double4*) &data[code[pc + 7]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])}; pc += 8;
                break;
            case  10: // Cons[real, realReg, realReg, real, quatReg]
                *((double4*) &data[code[pc + 7]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])}; pc += 8;
                break;
            case  11: // Cons[real, realReg, realReg, realReg, quatReg]
                *((double4*) &data[code[pc + 6]]) = (double4) {*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])}; pc += 7;
                break;
            case  12: // Cons[realReg, real, real, real, quatReg]
                *((double4*) &data[code[pc + 8]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])}; pc += 9;
                break;
            case  13: // Cons[realReg, real, real, realReg, quatReg]
                *((double4*) &data[code[pc + 7]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])}; pc += 8;
                break;
            case  14: // Cons[realReg, real, realReg, real, quatReg]
                *((double4*) &data[code[pc + 7]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])}; pc += 8;
                break;
            case  15: // Cons[realReg, real, realReg, realReg, quatReg]
                *((double4*) &data[code[pc + 6]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])}; pc += 7;
                break;
            case  16: // Cons[realReg, realReg, real, real, quatReg]
                *((double4*) &data[code[pc + 7]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])}; pc += 8;
                break;
            case  17: // Cons[realReg, realReg, real, realReg, quatReg]
                *((double4*) &data[code[pc + 6]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])}; pc += 7;
                break;
            case  18: // Cons[realReg, realReg, realReg, real, quatReg]
                *((double4*) &data[code[pc + 6]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])}; pc += 7;
                break;
            case  19: // Cons[realReg, realReg, realReg, realReg, quatReg]
                *((double4*) &data[code[pc + 5]]) = (double4) {*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])}; pc += 6;
                break;

            // RealToInt with 2 cases

            case  20: // RealToInt[real, integerReg]
                data[code[pc + 3]] = (int) *((double*) &code[pc + 1]); pc += 4;
                break;
            case  21: // RealToInt[realReg, integerReg]
                data[code[pc + 2]] = (int) *((double*) &data[code[pc + 1]]); pc += 3;
                break;

            // IntToReal with 2 cases

            case  22: // IntToReal[integer, realReg]
                *((double*) &data[code[pc + 2]]) = (double) code[pc + 1]; pc += 3;
                break;
            case  23: // IntToReal[integerReg, realReg]
                *((double*) &data[code[pc + 2]]) = (double) data[code[pc + 1]]; pc += 3;
                break;

            // Add with 16 cases

            case  24: // Add[integer, integer, integerReg]
                data[code[pc + 3]] = code[pc + 1] + code[pc + 2]; pc += 4;
                break;
            case  25: // Add[integer, integerReg, integerReg]
                data[code[pc + 3]] = code[pc + 1] + data[code[pc + 2]]; pc += 4;
                break;
            case  26: // Add[integerReg, integer, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] + code[pc + 2]; pc += 4;
                break;
            case  27: // Add[integerReg, integerReg, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] + data[code[pc + 2]]; pc += 4;
                break;
            case  28: // Add[real, real, realReg]
                *((double*) &data[code[pc + 5]]) = *((double*) &code[pc + 1]) + *((double*) &code[pc + 3]); pc += 6;
                break;
            case  29: // Add[real, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &code[pc + 1]) + *((double*) &data[code[pc + 3]]); pc += 5;
                break;
            case  30: // Add[realReg, real, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &data[code[pc + 1]]) + *((double*) &code[pc + 2]); pc += 5;
                break;
            case  31: // Add[realReg, realReg, realReg]
                *((double*) &data[code[pc + 3]]) = *((double*) &data[code[pc + 1]]) + *((double*) &data[code[pc + 2]]); pc += 4;
                break;
            case  32: // Add[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = *((double2*) &code[pc + 1]) + *((double2*) &code[pc + 5]); pc += 10;
                break;
            case  33: // Add[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = *((double2*) &code[pc + 1]) + *((double2*) &data[code[pc + 5]]); pc += 7;
                break;
            case  34: // Add[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = *((double2*) &data[code[pc + 1]]) + *((double2*) &code[pc + 2]); pc += 7;
                break;
            case  35: // Add[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = *((double2*) &data[code[pc + 1]]) + *((double2*) &data[code[pc + 2]]); pc += 4;
                break;
            case  36: // Add[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = *((double4*) &code[pc + 1]) + *((double4*) &code[pc + 9]); pc += 18;
                break;
            case  37: // Add[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = *((double4*) &code[pc + 1]) + *((double4*) &data[code[pc + 9]]); pc += 11;
                break;
            case  38: // Add[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = *((double4*) &data[code[pc + 1]]) + *((double4*) &code[pc + 2]); pc += 11;
                break;
            case  39: // Add[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = *((double4*) &data[code[pc + 1]]) + *((double4*) &data[code[pc + 2]]); pc += 4;
                break;

            // Sub with 16 cases

            case  40: // Sub[integer, integer, integerReg]
                data[code[pc + 3]] = code[pc + 1] - code[pc + 2]; pc += 4;
                break;
            case  41: // Sub[integer, integerReg, integerReg]
                data[code[pc + 3]] = code[pc + 1] - data[code[pc + 2]]; pc += 4;
                break;
            case  42: // Sub[integerReg, integer, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] - code[pc + 2]; pc += 4;
                break;
            case  43: // Sub[integerReg, integerReg, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] - data[code[pc + 2]]; pc += 4;
                break;
            case  44: // Sub[real, real, realReg]
                *((double*) &data[code[pc + 5]]) = *((double*) &code[pc + 1]) - *((double*) &code[pc + 3]); pc += 6;
                break;
            case  45: // Sub[real, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &code[pc + 1]) - *((double*) &data[code[pc + 3]]); pc += 5;
                break;
            case  46: // Sub[realReg, real, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &data[code[pc + 1]]) - *((double*) &code[pc + 2]); pc += 5;
                break;
            case  47: // Sub[realReg, realReg, realReg]
                *((double*) &data[code[pc + 3]]) = *((double*) &data[code[pc + 1]]) - *((double*) &data[code[pc + 2]]); pc += 4;
                break;
            case  48: // Sub[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = *((double2*) &code[pc + 1]) - *((double2*) &code[pc + 5]); pc += 10;
                break;
            case  49: // Sub[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = *((double2*) &code[pc + 1]) - *((double2*) &data[code[pc + 5]]); pc += 7;
                break;
            case  50: // Sub[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = *((double2*) &data[code[pc + 1]]) - *((double2*) &code[pc + 2]); pc += 7;
                break;
            case  51: // Sub[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = *((double2*) &data[code[pc + 1]]) - *((double2*) &data[code[pc + 2]]); pc += 4;
                break;
            case  52: // Sub[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = *((double4*) &code[pc + 1]) - *((double4*) &code[pc + 9]); pc += 18;
                break;
            case  53: // Sub[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = *((double4*) &code[pc + 1]) - *((double4*) &data[code[pc + 9]]); pc += 11;
                break;
            case  54: // Sub[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = *((double4*) &data[code[pc + 1]]) - *((double4*) &code[pc + 2]); pc += 11;
                break;
            case  55: // Sub[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = *((double4*) &data[code[pc + 1]]) - *((double4*) &data[code[pc + 2]]); pc += 4;
                break;

            // Mul with 16 cases

            case  56: // Mul[integer, integer, integerReg]
                data[code[pc + 3]] = code[pc + 1] * code[pc + 2]; pc += 4;
                break;
            case  57: // Mul[integer, integerReg, integerReg]
                data[code[pc + 3]] = code[pc + 1] * data[code[pc + 2]]; pc += 4;
                break;
            case  58: // Mul[integerReg, integer, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] * code[pc + 2]; pc += 4;
                break;
            case  59: // Mul[integerReg, integerReg, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] * data[code[pc + 2]]; pc += 4;
                break;
            case  60: // Mul[real, real, realReg]
                *((double*) &data[code[pc + 5]]) = *((double*) &code[pc + 1]) * *((double*) &code[pc + 3]); pc += 6;
                break;
            case  61: // Mul[real, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &code[pc + 1]) * *((double*) &data[code[pc + 3]]); pc += 5;
                break;
            case  62: // Mul[realReg, real, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &data[code[pc + 1]]) * *((double*) &code[pc + 2]); pc += 5;
                break;
            case  63: // Mul[realReg, realReg, realReg]
                *((double*) &data[code[pc + 3]]) = *((double*) &data[code[pc + 1]]) * *((double*) &data[code[pc + 2]]); pc += 4;
                break;
            case  64: // Mul[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = mul(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case  65: // Mul[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = mul(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case  66: // Mul[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = mul(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case  67: // Mul[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = mul(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;
            case  68: // Mul[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = mul(*((double4*) &code[pc + 1]), *((double4*) &code[pc + 9])); pc += 18;
                break;
            case  69: // Mul[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = mul(*((double4*) &code[pc + 1]), *((double4*) &data[code[pc + 9]])); pc += 11;
                break;
            case  70: // Mul[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = mul(*((double4*) &data[code[pc + 1]]), *((double4*) &code[pc + 2])); pc += 11;
                break;
            case  71: // Mul[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = mul(*((double4*) &data[code[pc + 1]]), *((double4*) &data[code[pc + 2]])); pc += 4;
                break;

            // Div with 12 cases

            case  72: // Div[real, real, realReg]
                *((double*) &data[code[pc + 5]]) = *((double*) &code[pc + 1]) / *((double*) &code[pc + 3]); pc += 6;
                break;
            case  73: // Div[real, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &code[pc + 1]) / *((double*) &data[code[pc + 3]]); pc += 5;
                break;
            case  74: // Div[realReg, real, realReg]
                *((double*) &data[code[pc + 4]]) = *((double*) &data[code[pc + 1]]) / *((double*) &code[pc + 2]); pc += 5;
                break;
            case  75: // Div[realReg, realReg, realReg]
                *((double*) &data[code[pc + 3]]) = *((double*) &data[code[pc + 1]]) / *((double*) &data[code[pc + 2]]); pc += 4;
                break;
            case  76: // Div[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = div(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case  77: // Div[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = div(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case  78: // Div[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = div(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case  79: // Div[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = div(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;
            case  80: // Div[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = div(*((double4*) &code[pc + 1]), *((double4*) &code[pc + 9])); pc += 18;
                break;
            case  81: // Div[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = div(*((double4*) &code[pc + 1]), *((double4*) &data[code[pc + 9]])); pc += 11;
                break;
            case  82: // Div[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = div(*((double4*) &data[code[pc + 1]]), *((double4*) &code[pc + 2])); pc += 11;
                break;
            case  83: // Div[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = div(*((double4*) &data[code[pc + 1]]), *((double4*) &data[code[pc + 2]])); pc += 4;
                break;

            // Mod with 4 cases

            case  84: // Mod[integer, integer, integerReg]
                data[code[pc + 3]] = code[pc + 1] % code[pc + 2]; pc += 4;
                break;
            case  85: // Mod[integer, integerReg, integerReg]
                data[code[pc + 3]] = code[pc + 1] % data[code[pc + 2]]; pc += 4;
                break;
            case  86: // Mod[integerReg, integer, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] % code[pc + 2]; pc += 4;
                break;
            case  87: // Mod[integerReg, integerReg, integerReg]
                data[code[pc + 3]] = data[code[pc + 1]] % data[code[pc + 2]]; pc += 4;
                break;

            // Pow with 24 cases

            case  88: // Pow[integer, integer, integerReg]
                data[code[pc + 3]] = pow(code[pc + 1], code[pc + 2]); pc += 4;
                break;
            case  89: // Pow[integer, integerReg, integerReg]
                data[code[pc + 3]] = pow(code[pc + 1], data[code[pc + 2]]); pc += 4;
                break;
            case  90: // Pow[integerReg, integer, integerReg]
                data[code[pc + 3]] = pow(data[code[pc + 1]], code[pc + 2]); pc += 4;
                break;
            case  91: // Pow[integerReg, integerReg, integerReg]
                data[code[pc + 3]] = pow(data[code[pc + 1]], data[code[pc + 2]]); pc += 4;
                break;
            case  92: // Pow[real, integer, realReg]
                *((double*) &data[code[pc + 4]]) = pow(*((double*) &code[pc + 1]), code[pc + 3]); pc += 5;
                break;
            case  93: // Pow[real, integerReg, realReg]
                *((double*) &data[code[pc + 4]]) = pow(*((double*) &code[pc + 1]), data[code[pc + 3]]); pc += 5;
                break;
            case  94: // Pow[realReg, integer, realReg]
                *((double*) &data[code[pc + 3]]) = pow(*((double*) &data[code[pc + 1]]), code[pc + 2]); pc += 4;
                break;
            case  95: // Pow[realReg, integerReg, realReg]
                *((double*) &data[code[pc + 3]]) = pow(*((double*) &data[code[pc + 1]]), data[code[pc + 2]]); pc += 4;
                break;
            case  96: // Pow[real, real, realReg]
                *((double*) &data[code[pc + 5]]) = pow(*((double*) &code[pc + 1]), *((double*) &code[pc + 3])); pc += 6;
                break;
            case  97: // Pow[real, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = pow(*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])); pc += 5;
                break;
            case  98: // Pow[realReg, real, realReg]
                *((double*) &data[code[pc + 4]]) = pow(*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])); pc += 5;
                break;
            case  99: // Pow[realReg, realReg, realReg]
                *((double*) &data[code[pc + 3]]) = pow(*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])); pc += 4;
                break;
            case 100: // Pow[cplx, integer, cplxReg]
                *((double2*) &data[code[pc + 6]]) = pow(*((double2*) &code[pc + 1]), code[pc + 5]); pc += 7;
                break;
            case 101: // Pow[cplx, integerReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = pow(*((double2*) &code[pc + 1]), data[code[pc + 5]]); pc += 7;
                break;
            case 102: // Pow[cplxReg, integer, cplxReg]
                *((double2*) &data[code[pc + 3]]) = pow(*((double2*) &data[code[pc + 1]]), code[pc + 2]); pc += 4;
                break;
            case 103: // Pow[cplxReg, integerReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = pow(*((double2*) &data[code[pc + 1]]), data[code[pc + 2]]); pc += 4;
                break;
            case 104: // Pow[cplx, real, cplxReg]
                *((double2*) &data[code[pc + 7]]) = pow(*((double2*) &code[pc + 1]), *((double*) &code[pc + 5])); pc += 8;
                break;
            case 105: // Pow[cplx, realReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = pow(*((double2*) &code[pc + 1]), *((double*) &data[code[pc + 5]])); pc += 7;
                break;
            case 106: // Pow[cplxReg, real, cplxReg]
                *((double2*) &data[code[pc + 4]]) = pow(*((double2*) &data[code[pc + 1]]), *((double*) &code[pc + 2])); pc += 5;
                break;
            case 107: // Pow[cplxReg, realReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = pow(*((double2*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])); pc += 4;
                break;
            case 108: // Pow[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = pow(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case 109: // Pow[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = pow(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case 110: // Pow[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = pow(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 111: // Pow[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = pow(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;

            // Recip with 6 cases

            case 112: // Recip[real, realReg]
                *((double*) &data[code[pc + 3]]) = 1.0 / *((double*) &code[pc + 1]); pc += 4;
                break;
            case 113: // Recip[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = 1.0 / *((double*) &data[code[pc + 1]]); pc += 3;
                break;
            case 114: // Recip[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = recip(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 115: // Recip[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = recip(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;
            case 116: // Recip[quat, quatReg]
                *((double4*) &data[code[pc + 9]]) = recip(*((double4*) &code[pc + 1])); pc += 10;
                break;
            case 117: // Recip[quatReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = recip(*((double4*) &data[code[pc + 1]])); pc += 3;
                break;

            // Neg with 8 cases

            case 118: // Neg[integer, integerReg]
                data[code[pc + 2]] = -code[pc + 1]; pc += 3;
                break;
            case 119: // Neg[integerReg, integerReg]
                data[code[pc + 2]] = -data[code[pc + 1]]; pc += 3;
                break;
            case 120: // Neg[real, realReg]
                *((double*) &data[code[pc + 3]]) = -*((double*) &code[pc + 1]); pc += 4;
                break;
            case 121: // Neg[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = -*((double*) &data[code[pc + 1]]); pc += 3;
                break;
            case 122: // Neg[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = -*((double2*) &code[pc + 1]); pc += 6;
                break;
            case 123: // Neg[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = -*((double2*) &data[code[pc + 1]]); pc += 3;
                break;
            case 124: // Neg[quat, quatReg]
                *((double4*) &data[code[pc + 9]]) = -*((double4*) &code[pc + 1]); pc += 10;
                break;
            case 125: // Neg[quatReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = -*((double4*) &data[code[pc + 1]]); pc += 3;
                break;

            // Atan with 4 cases

            case 126: // Atan[real, realReg]
                *((double*) &data[code[pc + 3]]) = atan(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 127: // Atan[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = atan(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 128: // Atan[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = atan(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 129: // Atan[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = atan(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Atanh with 4 cases

            case 130: // Atanh[real, realReg]
                *((double*) &data[code[pc + 3]]) = atanh(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 131: // Atanh[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = atanh(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 132: // Atanh[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = atanh(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 133: // Atanh[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = atanh(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Cos with 4 cases

            case 134: // Cos[real, realReg]
                *((double*) &data[code[pc + 3]]) = cos(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 135: // Cos[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = cos(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 136: // Cos[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = cos(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 137: // Cos[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = cos(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Cosh with 4 cases

            case 138: // Cosh[real, realReg]
                *((double*) &data[code[pc + 3]]) = cosh(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 139: // Cosh[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = cosh(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 140: // Cosh[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = cosh(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 141: // Cosh[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = cosh(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Exp with 4 cases

            case 142: // Exp[real, realReg]
                *((double*) &data[code[pc + 3]]) = exp(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 143: // Exp[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = exp(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 144: // Exp[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = exp(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 145: // Exp[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = exp(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Log with 4 cases

            case 146: // Log[real, realReg]
                *((double*) &data[code[pc + 3]]) = log(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 147: // Log[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = log(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 148: // Log[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = log(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 149: // Log[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = log(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Sin with 4 cases

            case 150: // Sin[real, realReg]
                *((double*) &data[code[pc + 3]]) = sin(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 151: // Sin[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = sin(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 152: // Sin[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = sin(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 153: // Sin[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = sin(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Sinh with 4 cases

            case 154: // Sinh[real, realReg]
                *((double*) &data[code[pc + 3]]) = sinh(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 155: // Sinh[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = sinh(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 156: // Sinh[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = sinh(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 157: // Sinh[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = sinh(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Sqr with 8 cases

            case 158: // Sqr[integer, integerReg]
                data[code[pc + 2]] = code[pc + 1] * code[pc + 1]; pc += 3;
                break;
            case 159: // Sqr[integerReg, integerReg]
                data[code[pc + 2]] = data[code[pc + 1]] * data[code[pc + 1]]; pc += 3;
                break;
            case 160: // Sqr[real, realReg]
                *((double*) &data[code[pc + 3]]) = *((double*) &code[pc + 1]) * *((double*) &code[pc + 1]); pc += 4;
                break;
            case 161: // Sqr[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = *((double*) &data[code[pc + 1]]) * *((double*) &data[code[pc + 1]]); pc += 3;
                break;
            case 162: // Sqr[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = mul(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 1])); pc += 6;
                break;
            case 163: // Sqr[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = mul(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 1]])); pc += 3;
                break;
            case 164: // Sqr[quat, quatReg]
                *((double4*) &data[code[pc + 9]]) = mul(*((double4*) &code[pc + 1]), *((double4*) &code[pc + 1])); pc += 10;
                break;
            case 165: // Sqr[quatReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = mul(*((double4*) &data[code[pc + 1]]), *((double4*) &data[code[pc + 1]])); pc += 3;
                break;

            // Sqrt with 4 cases

            case 166: // Sqrt[real, realReg]
                *((double*) &data[code[pc + 3]]) = sqrt(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 167: // Sqrt[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = sqrt(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 168: // Sqrt[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = sqrt(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 169: // Sqrt[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = sqrt(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Tan with 4 cases

            case 170: // Tan[real, realReg]
                *((double*) &data[code[pc + 3]]) = tan(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 171: // Tan[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = tan(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 172: // Tan[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = tan(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 173: // Tan[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = tan(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Tanh with 4 cases

            case 174: // Tanh[real, realReg]
                *((double*) &data[code[pc + 3]]) = tanh(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 175: // Tanh[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = tanh(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 176: // Tanh[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = tanh(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 177: // Tanh[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = tanh(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Abs with 8 cases

            case 178: // Abs[integer, integerReg]
                data[code[pc + 2]] = abs(code[pc + 1]); pc += 3;
                break;
            case 179: // Abs[integerReg, integerReg]
                data[code[pc + 2]] = abs(data[code[pc + 1]]); pc += 3;
                break;
            case 180: // Abs[real, realReg]
                *((double*) &data[code[pc + 3]]) = abs(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 181: // Abs[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = abs(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 182: // Abs[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = abs(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 183: // Abs[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = abs(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;
            case 184: // Abs[quat, quatReg]
                *((double4*) &data[code[pc + 9]]) = abs(*((double4*) &code[pc + 1])); pc += 10;
                break;
            case 185: // Abs[quatReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = abs(*((double4*) &data[code[pc + 1]])); pc += 3;
                break;

            // Floor with 4 cases

            case 186: // Floor[real, realReg]
                *((double*) &data[code[pc + 3]]) = floor(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 187: // Floor[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = floor(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 188: // Floor[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = floor(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 189: // Floor[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = floor(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Ceil with 4 cases

            case 190: // Ceil[real, realReg]
                *((double*) &data[code[pc + 3]]) = ceil(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 191: // Ceil[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = ceil(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 192: // Ceil[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = ceil(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 193: // Ceil[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = ceil(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Fract with 4 cases

            case 194: // Fract[real, realReg]
                *((double*) &data[code[pc + 3]]) = fract(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 195: // Fract[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = fract(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 196: // Fract[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = fract(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 197: // Fract[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = fract(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Dot with 4 cases

            case 198: // Dot[cplx, cplx, realReg]
                *((double*) &data[code[pc + 9]]) = dot(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case 199: // Dot[cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 6]]) = dot(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case 200: // Dot[cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 6]]) = dot(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 201: // Dot[cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 3]]) = dot(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;

            // CircleFn with 4 cases

            case 202: // CircleFn[real, realReg]
                *((double*) &data[code[pc + 3]]) = circlefn(*((double*) &code[pc + 1])); pc += 4;
                break;
            case 203: // CircleFn[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = circlefn(*((double*) &data[code[pc + 1]])); pc += 3;
                break;
            case 204: // CircleFn[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = circlefn(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 205: // CircleFn[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = circlefn(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // ScalarMul with 8 cases

            case 206: // ScalarMul[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = *((double2*) &code[pc + 1]) * *((double2*) &code[pc + 5]); pc += 10;
                break;
            case 207: // ScalarMul[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = *((double2*) &code[pc + 1]) * *((double2*) &data[code[pc + 5]]); pc += 7;
                break;
            case 208: // ScalarMul[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = *((double2*) &data[code[pc + 1]]) * *((double2*) &code[pc + 2]); pc += 7;
                break;
            case 209: // ScalarMul[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = *((double2*) &data[code[pc + 1]]) * *((double2*) &data[code[pc + 2]]); pc += 4;
                break;
            case 210: // ScalarMul[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = *((double4*) &code[pc + 1]) * *((double4*) &code[pc + 9]); pc += 18;
                break;
            case 211: // ScalarMul[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = *((double4*) &code[pc + 1]) * *((double4*) &data[code[pc + 9]]); pc += 11;
                break;
            case 212: // ScalarMul[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = *((double4*) &data[code[pc + 1]]) * *((double4*) &code[pc + 2]); pc += 11;
                break;
            case 213: // ScalarMul[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = *((double4*) &data[code[pc + 1]]) * *((double4*) &data[code[pc + 2]]); pc += 4;
                break;

            // Max with 16 cases

            case 214: // Max[integer, integer, integerReg]
                data[code[pc + 3]] = max(code[pc + 1], code[pc + 2]); pc += 4;
                break;
            case 215: // Max[integer, integerReg, integerReg]
                data[code[pc + 3]] = max(code[pc + 1], data[code[pc + 2]]); pc += 4;
                break;
            case 216: // Max[integerReg, integer, integerReg]
                data[code[pc + 3]] = max(data[code[pc + 1]], code[pc + 2]); pc += 4;
                break;
            case 217: // Max[integerReg, integerReg, integerReg]
                data[code[pc + 3]] = max(data[code[pc + 1]], data[code[pc + 2]]); pc += 4;
                break;
            case 218: // Max[real, real, realReg]
                *((double*) &data[code[pc + 5]]) = max(*((double*) &code[pc + 1]), *((double*) &code[pc + 3])); pc += 6;
                break;
            case 219: // Max[real, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = max(*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])); pc += 5;
                break;
            case 220: // Max[realReg, real, realReg]
                *((double*) &data[code[pc + 4]]) = max(*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])); pc += 5;
                break;
            case 221: // Max[realReg, realReg, realReg]
                *((double*) &data[code[pc + 3]]) = max(*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])); pc += 4;
                break;
            case 222: // Max[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = max(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case 223: // Max[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = max(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case 224: // Max[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = max(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 225: // Max[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = max(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;
            case 226: // Max[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = max(*((double4*) &code[pc + 1]), *((double4*) &code[pc + 9])); pc += 18;
                break;
            case 227: // Max[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = max(*((double4*) &code[pc + 1]), *((double4*) &data[code[pc + 9]])); pc += 11;
                break;
            case 228: // Max[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = max(*((double4*) &data[code[pc + 1]]), *((double4*) &code[pc + 2])); pc += 11;
                break;
            case 229: // Max[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = max(*((double4*) &data[code[pc + 1]]), *((double4*) &data[code[pc + 2]])); pc += 4;
                break;

            // Min with 16 cases

            case 230: // Min[integer, integer, integerReg]
                data[code[pc + 3]] = min(code[pc + 1], code[pc + 2]); pc += 4;
                break;
            case 231: // Min[integer, integerReg, integerReg]
                data[code[pc + 3]] = min(code[pc + 1], data[code[pc + 2]]); pc += 4;
                break;
            case 232: // Min[integerReg, integer, integerReg]
                data[code[pc + 3]] = min(data[code[pc + 1]], code[pc + 2]); pc += 4;
                break;
            case 233: // Min[integerReg, integerReg, integerReg]
                data[code[pc + 3]] = min(data[code[pc + 1]], data[code[pc + 2]]); pc += 4;
                break;
            case 234: // Min[real, real, realReg]
                *((double*) &data[code[pc + 5]]) = min(*((double*) &code[pc + 1]), *((double*) &code[pc + 3])); pc += 6;
                break;
            case 235: // Min[real, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = min(*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])); pc += 5;
                break;
            case 236: // Min[realReg, real, realReg]
                *((double*) &data[code[pc + 4]]) = min(*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])); pc += 5;
                break;
            case 237: // Min[realReg, realReg, realReg]
                *((double*) &data[code[pc + 3]]) = min(*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])); pc += 4;
                break;
            case 238: // Min[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = min(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case 239: // Min[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = min(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case 240: // Min[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = min(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 241: // Min[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = min(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;
            case 242: // Min[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = min(*((double4*) &code[pc + 1]), *((double4*) &code[pc + 9])); pc += 18;
                break;
            case 243: // Min[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = min(*((double4*) &code[pc + 1]), *((double4*) &data[code[pc + 9]])); pc += 11;
                break;
            case 244: // Min[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = min(*((double4*) &data[code[pc + 1]]), *((double4*) &code[pc + 2])); pc += 11;
                break;
            case 245: // Min[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = min(*((double4*) &data[code[pc + 1]]), *((double4*) &data[code[pc + 2]])); pc += 4;
                break;

            // Conj with 2 cases

            case 246: // Conj[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = conj(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 247: // Conj[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = conj(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Flip with 2 cases

            case 248: // Flip[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = flip(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 249: // Flip[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = flip(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // RAbs with 2 cases

            case 250: // RAbs[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = rabs(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 251: // RAbs[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = rabs(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // IAbs with 2 cases

            case 252: // IAbs[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = iabs(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 253: // IAbs[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = iabs(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Norm with 2 cases

            case 254: // Norm[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = norm(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 255: // Norm[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = norm(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Polar with 2 cases

            case 256: // Polar[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = polar(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 257: // Polar[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = polar(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Rect with 2 cases

            case 258: // Rect[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = rect(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 259: // Rect[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = rect(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Arc with 2 cases

            case 260: // Arc[cplx, realReg]
                *((double*) &data[code[pc + 5]]) = arc(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 261: // Arc[cplxReg, realReg]
                *((double*) &data[code[pc + 2]]) = arc(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Arcnorm with 2 cases

            case 262: // Arcnorm[cplx, realReg]
                *((double*) &data[code[pc + 5]]) = arc(*((double2*) &code[pc + 1])) / M_PI; pc += 6;
                break;
            case 263: // Arcnorm[cplxReg, realReg]
                *((double*) &data[code[pc + 2]]) = arc(*((double2*) &data[code[pc + 1]])) / M_PI; pc += 3;
                break;

            // Rad with 2 cases

            case 264: // Rad[cplx, realReg]
                *((double*) &data[code[pc + 5]]) = rad(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 265: // Rad[cplxReg, realReg]
                *((double*) &data[code[pc + 2]]) = rad(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Rad2 with 2 cases

            case 266: // Rad2[cplx, realReg]
                *((double*) &data[code[pc + 5]]) = rad2(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 267: // Rad2[cplxReg, realReg]
                *((double*) &data[code[pc + 2]]) = rad2(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Dist with 4 cases

            case 268: // Dist[cplx, cplx, realReg]
                *((double*) &data[code[pc + 9]]) = dist(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case 269: // Dist[cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 6]]) = dist(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case 270: // Dist[cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 6]]) = dist(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 271: // Dist[cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 3]]) = dist(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;

            // Dist2 with 4 cases

            case 272: // Dist2[cplx, cplx, realReg]
                *((double*) &data[code[pc + 9]]) = dist2(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case 273: // Dist2[cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 6]]) = dist2(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case 274: // Dist2[cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 6]]) = dist2(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 275: // Dist2[cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 3]]) = dist2(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;

            // Re with 2 cases

            case 276: // Re[cplx, realReg]
                *((double*) &data[code[pc + 5]]) = ((double2*) &code[pc + 1])->x; pc += 6;
                break;
            case 277: // Re[cplxReg, realReg]
                *((double*) &data[code[pc + 2]]) = ((double2*) &data[code[pc + 1]])->x; pc += 3;
                break;

            // Im with 2 cases

            case 278: // Im[cplx, realReg]
                *((double*) &data[code[pc + 5]]) = ((double2*) &code[pc + 1])->y; pc += 6;
                break;
            case 279: // Im[cplxReg, realReg]
                *((double*) &data[code[pc + 2]]) = ((double2*) &data[code[pc + 1]])->y; pc += 3;
                break;

            // Equal with 8 cases

            case 280: // Equal[integer, integer, integer, integer]
                pc = code[pc + 1] == code[pc + 2] ? code[pc + 3] : code[pc + 4];
                break;
            case 281: // Equal[integer, integerReg, integer, integer]
                pc = code[pc + 1] == data[code[pc + 2]] ? code[pc + 3] : code[pc + 4];
                break;
            case 282: // Equal[integerReg, integer, integer, integer]
                pc = data[code[pc + 1]] == code[pc + 2] ? code[pc + 3] : code[pc + 4];
                break;
            case 283: // Equal[integerReg, integerReg, integer, integer]
                pc = data[code[pc + 1]] == data[code[pc + 2]] ? code[pc + 3] : code[pc + 4];
                break;
            case 284: // Equal[real, real, integer, integer]
                pc = *((double*) &code[pc + 1]) == *((double*) &code[pc + 3]) ? code[pc + 5] : code[pc + 6];
                break;
            case 285: // Equal[real, realReg, integer, integer]
                pc = *((double*) &code[pc + 1]) == *((double*) &data[code[pc + 3]]) ? code[pc + 4] : code[pc + 5];
                break;
            case 286: // Equal[realReg, real, integer, integer]
                pc = *((double*) &data[code[pc + 1]]) == *((double*) &code[pc + 2]) ? code[pc + 4] : code[pc + 5];
                break;
            case 287: // Equal[realReg, realReg, integer, integer]
                pc = *((double*) &data[code[pc + 1]]) == *((double*) &data[code[pc + 2]]) ? code[pc + 3] : code[pc + 4];
                break;

            // Less with 8 cases

            case 288: // Less[integer, integer, integer, integer]
                pc = code[pc + 1] < code[pc + 2] ? code[pc + 3] : code[pc + 4];
                break;
            case 289: // Less[integer, integerReg, integer, integer]
                pc = code[pc + 1] < data[code[pc + 2]] ? code[pc + 3] : code[pc + 4];
                break;
            case 290: // Less[integerReg, integer, integer, integer]
                pc = data[code[pc + 1]] < code[pc + 2] ? code[pc + 3] : code[pc + 4];
                break;
            case 291: // Less[integerReg, integerReg, integer, integer]
                pc = data[code[pc + 1]] < data[code[pc + 2]] ? code[pc + 3] : code[pc + 4];
                break;
            case 292: // Less[real, real, integer, integer]
                pc = *((double*) &code[pc + 1]) < *((double*) &code[pc + 3]) ? code[pc + 5] : code[pc + 6];
                break;
            case 293: // Less[real, realReg, integer, integer]
                pc = *((double*) &code[pc + 1]) < *((double*) &data[code[pc + 3]]) ? code[pc + 4] : code[pc + 5];
                break;
            case 294: // Less[realReg, real, integer, integer]
                pc = *((double*) &data[code[pc + 1]]) < *((double*) &code[pc + 2]) ? code[pc + 4] : code[pc + 5];
                break;
            case 295: // Less[realReg, realReg, integer, integer]
                pc = *((double*) &data[code[pc + 1]]) < *((double*) &data[code[pc + 2]]) ? code[pc + 3] : code[pc + 4];
                break;

            // Box with 8 cases

            case 296: // Box[cplx, cplx, cplx, realReg]
                *((double*) &data[code[pc + 13]]) = box(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5]), *((double2*) &code[pc + 9])); pc += 14;
                break;
            case 297: // Box[cplx, cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 10]]) = box(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5]), *((double2*) &data[code[pc + 9]])); pc += 11;
                break;
            case 298: // Box[cplx, cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 10]]) = box(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]]), *((double2*) &code[pc + 6])); pc += 11;
                break;
            case 299: // Box[cplx, cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 7]]) = box(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]]), *((double2*) &data[code[pc + 6]])); pc += 8;
                break;
            case 300: // Box[cplxReg, cplx, cplx, realReg]
                *((double*) &data[code[pc + 10]]) = box(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2]), *((double2*) &code[pc + 6])); pc += 11;
                break;
            case 301: // Box[cplxReg, cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 7]]) = box(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2]), *((double2*) &data[code[pc + 6]])); pc += 8;
                break;
            case 302: // Box[cplxReg, cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 7]]) = box(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]]), *((double2*) &code[pc + 3])); pc += 8;
                break;
            case 303: // Box[cplxReg, cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 4]]) = box(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]]), *((double2*) &data[code[pc + 3]])); pc += 5;
                break;

            // Circle with 8 cases

            case 304: // Circle[cplx, real, cplx, realReg]
                *((double*) &data[code[pc + 11]]) = circle(*((double2*) &code[pc + 1]), *((double*) &code[pc + 5]), *((double2*) &code[pc + 7])); pc += 12;
                break;
            case 305: // Circle[cplx, real, cplxReg, realReg]
                *((double*) &data[code[pc + 8]]) = circle(*((double2*) &code[pc + 1]), *((double*) &code[pc + 5]), *((double2*) &data[code[pc + 7]])); pc += 9;
                break;
            case 306: // Circle[cplx, realReg, cplx, realReg]
                *((double*) &data[code[pc + 10]]) = circle(*((double2*) &code[pc + 1]), *((double*) &data[code[pc + 5]]), *((double2*) &code[pc + 6])); pc += 11;
                break;
            case 307: // Circle[cplx, realReg, cplxReg, realReg]
                *((double*) &data[code[pc + 7]]) = circle(*((double2*) &code[pc + 1]), *((double*) &data[code[pc + 5]]), *((double2*) &data[code[pc + 6]])); pc += 8;
                break;
            case 308: // Circle[cplxReg, real, cplx, realReg]
                *((double*) &data[code[pc + 8]]) = circle(*((double2*) &data[code[pc + 1]]), *((double*) &code[pc + 2]), *((double2*) &code[pc + 4])); pc += 9;
                break;
            case 309: // Circle[cplxReg, real, cplxReg, realReg]
                *((double*) &data[code[pc + 5]]) = circle(*((double2*) &data[code[pc + 1]]), *((double*) &code[pc + 2]), *((double2*) &data[code[pc + 4]])); pc += 6;
                break;
            case 310: // Circle[cplxReg, realReg, cplx, realReg]
                *((double*) &data[code[pc + 7]]) = circle(*((double2*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]]), *((double2*) &code[pc + 3])); pc += 8;
                break;
            case 311: // Circle[cplxReg, realReg, cplxReg, realReg]
                *((double*) &data[code[pc + 4]]) = circle(*((double2*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]]), *((double2*) &data[code[pc + 3]])); pc += 5;
                break;

            // Line with 8 cases

            case 312: // Line[cplx, cplx, cplx, realReg]
                *((double*) &data[code[pc + 13]]) = line(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5]), *((double2*) &code[pc + 9])); pc += 14;
                break;
            case 313: // Line[cplx, cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 10]]) = line(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5]), *((double2*) &data[code[pc + 9]])); pc += 11;
                break;
            case 314: // Line[cplx, cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 10]]) = line(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]]), *((double2*) &code[pc + 6])); pc += 11;
                break;
            case 315: // Line[cplx, cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 7]]) = line(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]]), *((double2*) &data[code[pc + 6]])); pc += 8;
                break;
            case 316: // Line[cplxReg, cplx, cplx, realReg]
                *((double*) &data[code[pc + 10]]) = line(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2]), *((double2*) &code[pc + 6])); pc += 11;
                break;
            case 317: // Line[cplxReg, cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 7]]) = line(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2]), *((double2*) &data[code[pc + 6]])); pc += 8;
                break;
            case 318: // Line[cplxReg, cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 7]]) = line(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]]), *((double2*) &code[pc + 3])); pc += 8;
                break;
            case 319: // Line[cplxReg, cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 4]]) = line(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]]), *((double2*) &data[code[pc + 3]])); pc += 5;
                break;

            // Segment with 8 cases

            case 320: // Segment[cplx, cplx, cplx, realReg]
                *((double*) &data[code[pc + 13]]) = segment(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5]), *((double2*) &code[pc + 9])); pc += 14;
                break;
            case 321: // Segment[cplx, cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 10]]) = segment(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5]), *((double2*) &data[code[pc + 9]])); pc += 11;
                break;
            case 322: // Segment[cplx, cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 10]]) = segment(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]]), *((double2*) &code[pc + 6])); pc += 11;
                break;
            case 323: // Segment[cplx, cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 7]]) = segment(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]]), *((double2*) &data[code[pc + 6]])); pc += 8;
                break;
            case 324: // Segment[cplxReg, cplx, cplx, realReg]
                *((double*) &data[code[pc + 10]]) = segment(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2]), *((double2*) &code[pc + 6])); pc += 11;
                break;
            case 325: // Segment[cplxReg, cplx, cplxReg, realReg]
                *((double*) &data[code[pc + 7]]) = segment(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2]), *((double2*) &data[code[pc + 6]])); pc += 8;
                break;
            case 326: // Segment[cplxReg, cplxReg, cplx, realReg]
                *((double*) &data[code[pc + 7]]) = segment(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]]), *((double2*) &code[pc + 3])); pc += 8;
                break;
            case 327: // Segment[cplxReg, cplxReg, cplxReg, realReg]
                *((double*) &data[code[pc + 4]]) = segment(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]]), *((double2*) &data[code[pc + 3]])); pc += 5;
                break;

            // Int2Lab with 2 cases

            case 328: // Int2Lab[integer, quatReg]
                *((double4*) &data[code[pc + 2]]) = int2lab(code[pc + 1]); pc += 3;
                break;
            case 329: // Int2Lab[integerReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = int2lab(data[code[pc + 1]]); pc += 3;
                break;

            // Int2Rgb with 2 cases

            case 330: // Int2Rgb[integer, quatReg]
                *((double4*) &data[code[pc + 2]]) = int2rgb(code[pc + 1]); pc += 3;
                break;
            case 331: // Int2Rgb[integerReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = int2rgb(data[code[pc + 1]]); pc += 3;
                break;

            // Lab2Int with 2 cases

            case 332: // Lab2Int[quat, integerReg]
                data[code[pc + 9]] = lab2int(*((double4*) &code[pc + 1])); pc += 10;
                break;
            case 333: // Lab2Int[quatReg, integerReg]
                data[code[pc + 2]] = lab2int(*((double4*) &data[code[pc + 1]])); pc += 3;
                break;

            // Lab2Rgb with 2 cases

            case 334: // Lab2Rgb[quat, quatReg]
                *((double4*) &data[code[pc + 9]]) = lab2rgb(*((double4*) &code[pc + 1])); pc += 10;
                break;
            case 335: // Lab2Rgb[quatReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = lab2rgb(*((double4*) &data[code[pc + 1]])); pc += 3;
                break;

            // Rgb2Int with 2 cases

            case 336: // Rgb2Int[quat, integerReg]
                data[code[pc + 9]] = rgb2int(*((double4*) &code[pc + 1])); pc += 10;
                break;
            case 337: // Rgb2Int[quatReg, integerReg]
                data[code[pc + 2]] = rgb2int(*((double4*) &data[code[pc + 1]])); pc += 3;
                break;

            // Rgb2Lab with 2 cases

            case 338: // Rgb2Lab[quat, quatReg]
                *((double4*) &data[code[pc + 9]]) = rgb2lab(*((double4*) &code[pc + 1])); pc += 10;
                break;
            case 339: // Rgb2Lab[quatReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = rgb2lab(*((double4*) &data[code[pc + 1]])); pc += 3;
                break;

            // Over with 4 cases

            case 340: // Over[quat, quat, quatReg]
                *((double4*) &data[code[pc + 17]]) = over(*((double4*) &code[pc + 1]), *((double4*) &code[pc + 9])); pc += 18;
                break;
            case 341: // Over[quat, quatReg, quatReg]
                *((double4*) &data[code[pc + 10]]) = over(*((double4*) &code[pc + 1]), *((double4*) &data[code[pc + 9]])); pc += 11;
                break;
            case 342: // Over[quatReg, quat, quatReg]
                *((double4*) &data[code[pc + 10]]) = over(*((double4*) &data[code[pc + 1]]), *((double4*) &code[pc + 2])); pc += 11;
                break;
            case 343: // Over[quatReg, quatReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = over(*((double4*) &data[code[pc + 1]]), *((double4*) &data[code[pc + 2]])); pc += 4;
                break;

            // DistLess with 8 cases

            case 344: // DistLess[cplx, cplx, real, integer, integer]
                pc = rad2(*((double2*) &code[pc + 1]) - *((double2*) &code[pc + 5])) < sqr(*((double*) &code[pc + 9])) ? code[pc + 11] : code[pc + 12];
                break;
            case 345: // DistLess[cplx, cplx, realReg, integer, integer]
                pc = rad2(*((double2*) &code[pc + 1]) - *((double2*) &code[pc + 5])) < sqr(*((double*) &data[code[pc + 9]])) ? code[pc + 10] : code[pc + 11];
                break;
            case 346: // DistLess[cplx, cplxReg, real, integer, integer]
                pc = rad2(*((double2*) &code[pc + 1]) - *((double2*) &data[code[pc + 5]])) < sqr(*((double*) &code[pc + 6])) ? code[pc + 8] : code[pc + 9];
                break;
            case 347: // DistLess[cplx, cplxReg, realReg, integer, integer]
                pc = rad2(*((double2*) &code[pc + 1]) - *((double2*) &data[code[pc + 5]])) < sqr(*((double*) &data[code[pc + 6]])) ? code[pc + 7] : code[pc + 8];
                break;
            case 348: // DistLess[cplxReg, cplx, real, integer, integer]
                pc = rad2(*((double2*) &data[code[pc + 1]]) - *((double2*) &code[pc + 2])) < sqr(*((double*) &code[pc + 6])) ? code[pc + 8] : code[pc + 9];
                break;
            case 349: // DistLess[cplxReg, cplx, realReg, integer, integer]
                pc = rad2(*((double2*) &data[code[pc + 1]]) - *((double2*) &code[pc + 2])) < sqr(*((double*) &data[code[pc + 6]])) ? code[pc + 7] : code[pc + 8];
                break;
            case 350: // DistLess[cplxReg, cplxReg, real, integer, integer]
                pc = rad2(*((double2*) &data[code[pc + 1]]) - *((double2*) &data[code[pc + 2]])) < sqr(*((double*) &code[pc + 3])) ? code[pc + 5] : code[pc + 6];
                break;
            case 351: // DistLess[cplxReg, cplxReg, realReg, integer, integer]
                pc = rad2(*((double2*) &data[code[pc + 1]]) - *((double2*) &data[code[pc + 2]])) < sqr(*((double*) &data[code[pc + 3]])) ? code[pc + 4] : code[pc + 5];
                break;

            // RadLess with 4 cases

            case 352: // RadLess[cplx, real, integer, integer]
                pc = rad2(*((double2*) &code[pc + 1])) < sqr(*((double*) &code[pc + 5])) ? code[pc + 7] : code[pc + 8];
                break;
            case 353: // RadLess[cplx, realReg, integer, integer]
                pc = rad2(*((double2*) &code[pc + 1])) < sqr(*((double*) &data[code[pc + 5]])) ? code[pc + 6] : code[pc + 7];
                break;
            case 354: // RadLess[cplxReg, real, integer, integer]
                pc = rad2(*((double2*) &data[code[pc + 1]])) < sqr(*((double*) &code[pc + 2])) ? code[pc + 4] : code[pc + 5];
                break;
            case 355: // RadLess[cplxReg, realReg, integer, integer]
                pc = rad2(*((double2*) &data[code[pc + 1]])) < sqr(*((double*) &data[code[pc + 2]])) ? code[pc + 3] : code[pc + 4];
                break;

            // RadRange with 4 cases

            case 356: // RadRange[cplx, cplx, real, real, integer, integer, integer]
                pc = radrange(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5]), *((double*) &code[pc + 9]), *((double*) &code[pc + 11]), code[pc + 13], code[pc + 14], code[pc + 15]);
                break;
            case 357: // RadRange[cplxReg, cplx, real, real, integer, integer, integer]
                pc = radrange(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2]), *((double*) &code[pc + 6]), *((double*) &code[pc + 8]), code[pc + 10], code[pc + 11], code[pc + 12]);
                break;
            case 358: // RadRange[cplx, cplxReg, real, real, integer, integer, integer]
                pc = radrange(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]]), *((double*) &code[pc + 6]), *((double*) &code[pc + 8]), code[pc + 10], code[pc + 11], code[pc + 12]);
                break;
            case 359: // RadRange[cplxReg, cplxReg, real, real, integer, integer, integer]
                pc = radrange(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]]), *((double*) &code[pc + 3]), *((double*) &code[pc + 5]), code[pc + 7], code[pc + 8], code[pc + 9]);
                break;

            // Smooth with 8 cases

            case 368: // Smooth[cplx, real, real, realReg]
                *((double*) &data[code[pc + 9]]) = smooth(*((double2*) &code[pc + 1]), *((double*) &code[pc + 5]), *((double*) &code[pc + 7])); pc += 10;
                break;
            case 369: // Smooth[cplx, real, realReg, realReg]
                *((double*) &data[code[pc + 8]]) = smooth(*((double2*) &code[pc + 1]), *((double*) &code[pc + 5]), *((double*) &data[code[pc + 7]])); pc += 9;
                break;
            case 370: // Smooth[cplx, realReg, real, realReg]
                *((double*) &data[code[pc + 8]]) = smooth(*((double2*) &code[pc + 1]), *((double*) &data[code[pc + 5]]), *((double*) &code[pc + 6])); pc += 9;
                break;
            case 371: // Smooth[cplx, realReg, realReg, realReg]
                *((double*) &data[code[pc + 7]]) = smooth(*((double2*) &code[pc + 1]), *((double*) &data[code[pc + 5]]), *((double*) &data[code[pc + 6]])); pc += 8;
                break;
            case 372: // Smooth[cplxReg, real, real, realReg]
                *((double*) &data[code[pc + 6]]) = smooth(*((double2*) &data[code[pc + 1]]), *((double*) &code[pc + 2]), *((double*) &code[pc + 4])); pc += 7;
                break;
            case 373: // Smooth[cplxReg, real, realReg, realReg]
                *((double*) &data[code[pc + 5]]) = smooth(*((double2*) &data[code[pc + 1]]), *((double*) &code[pc + 2]), *((double*) &data[code[pc + 4]])); pc += 6;
                break;
            case 374: // Smooth[cplxReg, realReg, real, realReg]
                *((double*) &data[code[pc + 5]]) = smooth(*((double2*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]]), *((double*) &code[pc + 3])); pc += 6;
                break;
            case 375: // Smooth[cplxReg, realReg, realReg, realReg]
                *((double*) &data[code[pc + 4]]) = smooth(*((double2*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]]), *((double*) &data[code[pc + 3]])); pc += 5;
                break;

            // Mandelbrot with 4 cases

            case 376: // Mandelbrot[cplx, cplx, cplxReg]
                *((double2*) &data[code[pc + 9]]) = mandelbrot(*((double2*) &code[pc + 1]), *((double2*) &code[pc + 5])); pc += 10;
                break;
            case 377: // Mandelbrot[cplx, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 6]]) = mandelbrot(*((double2*) &code[pc + 1]), *((double2*) &data[code[pc + 5]])); pc += 7;
                break;
            case 378: // Mandelbrot[cplxReg, cplx, cplxReg]
                *((double2*) &data[code[pc + 6]]) = mandelbrot(*((double2*) &data[code[pc + 1]]), *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 379: // Mandelbrot[cplxReg, cplxReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = mandelbrot(*((double2*) &data[code[pc + 1]]), *((double2*) &data[code[pc + 2]])); pc += 4;
                break;

            // Next with 2 cases

            case 380: // Next[integerReg, integer, integer, integer]
                pc = (++data[code[pc + 1]]) < code[pc + 2] ? code[pc + 3] : code[pc + 4];
                break;
            case 381: // Next[integerReg, integerReg, integer, integer]
                pc = (++data[code[pc + 1]]) < data[code[pc + 2]] ? code[pc + 3] : code[pc + 4];
                break;

            // MapCoordinates with 6 cases

            case 382: // MapCoordinates[real, real, cplxReg]
                *((double2*) &data[code[pc + 5]]) = mapcoordinates(*((double*) &code[pc + 1]), *((double*) &code[pc + 3])); pc += 6;
                break;
            case 383: // MapCoordinates[real, realReg, cplxReg]
                *((double2*) &data[code[pc + 4]]) = mapcoordinates(*((double*) &code[pc + 1]), *((double*) &data[code[pc + 3]])); pc += 5;
                break;
            case 384: // MapCoordinates[realReg, real, cplxReg]
                *((double2*) &data[code[pc + 4]]) = mapcoordinates(*((double*) &data[code[pc + 1]]), *((double*) &code[pc + 2])); pc += 5;
                break;
            case 385: // MapCoordinates[realReg, realReg, cplxReg]
                *((double2*) &data[code[pc + 3]]) = mapcoordinates(*((double*) &data[code[pc + 1]]), *((double*) &data[code[pc + 2]])); pc += 4;
                break;
            case 386: // MapCoordinates[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = mapcoordinates(*((double2*) &code[pc + 1])); pc += 6;
                break;
            case 387: // MapCoordinates[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = mapcoordinates(*((double2*) &data[code[pc + 1]])); pc += 3;
                break;

            // Mov with 8 cases

            case 388: // Mov[integer, integerReg]
                data[code[pc + 2]] = code[pc + 1]; pc += 3;
                break;
            case 389: // Mov[integerReg, integerReg]
                data[code[pc + 2]] = data[code[pc + 1]]; pc += 3;
                break;
            case 390: // Mov[real, realReg]
                *((double*) &data[code[pc + 3]]) = *((double*) &code[pc + 1]); pc += 4;
                break;
            case 391: // Mov[realReg, realReg]
                *((double*) &data[code[pc + 2]]) = *((double*) &data[code[pc + 1]]); pc += 3;
                break;
            case 392: // Mov[cplx, cplxReg]
                *((double2*) &data[code[pc + 5]]) = *((double2*) &code[pc + 1]); pc += 6;
                break;
            case 393: // Mov[cplxReg, cplxReg]
                *((double2*) &data[code[pc + 2]]) = *((double2*) &data[code[pc + 1]]); pc += 3;
                break;
            case 394: // Mov[quat, quatReg]
                *((double4*) &data[code[pc + 9]]) = *((double4*) &code[pc + 1]); pc += 10;
                break;
            case 395: // Mov[quatReg, quatReg]
                *((double4*) &data[code[pc + 2]]) = *((double4*) &data[code[pc + 1]]); pc += 3;
                break;

            // Jump with 1 cases

            case 396: // Jump[integer]
                pc = code[pc + 1];
                break;

            // JumpRel with 1 cases

            case 397: // JumpRel[integerReg]
                pc = code[pc + data[code[pc + 1]] + 2] /* +2 because of instruction and argument */;
                break;

            // LdPalette with 4 cases

            case 398: // LdPalette[integer, cplx, quatReg]
                *((double4*) &data[code[pc + 6]]) = palette_lab(code[pc + 1], *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 399: // LdPalette[integer, cplxReg, quatReg]
                *((double4*) &data[code[pc + 3]]) = palette_lab(code[pc + 1], *((double2*) &data[code[pc + 2]])); pc += 4;
                break;
            case 400: // LdPalette[integer, cplx, integerReg]
                data[code[pc + 6]] = palette_int(code[pc + 1], *((double2*) &code[pc + 2])); pc += 7;
                break;
            case 401: // LdPalette[integer, cplxReg, integerReg]
                data[code[pc + 3]] = palette_int(code[pc + 1], *((double2*) &data[code[pc + 2]])); pc += 4;
                break;
        }
    }

	int color = data[2];

	return (uchar4) {
			(uchar) (color >> 16),
			(uchar) (color >> 8),
			(uchar) (color),
			(uchar) (color >> 24) };
}


// First, arguments for drawing
int stepsize;
int factor;
int offset;
int tilelength;

static int index_aligned(int index) {
    if(factor != 0) {
		// so we have to adjust index.
        int w0 = (width + stepsize - 1) / stepsize; // pixels to be drawn per row

        int laststepsize = stepsize * factor;

		int lw0 = (width + laststepsize - 1) / laststepsize; // pixels drawn in a row in the last iteration.

		int sectionsize = w0 * factor - lw0; // number of indices in a section

		// index in section
		int index0 = index % sectionsize; // index in (lw0, sectionsize]

        // add lw0 for each section.
        index += lw0 * (1 + index / sectionsize);

		if(index0 < w0 - lw0) { // is it in the first row?
            // pixels drawn inbetween two pixels is factor - 1, first pixel to be drawn is 1.
			index += 1 + index0 / (factor - 1) - lw0;
		}
	}

	return index;
}

uchar4 *tile;

uchar4 __attribute__((kernel)) root(uint32_t x) {
	if(x >= tilelength) return 0;

    int index = index_aligned(x + offset);

	int w0 = (width + stepsize - 1) / stepsize; // pixels to be drawn per row

	int x0 = (index % w0) * stepsize;
	int y0 = (index / w0) * stepsize;

	uchar4 color = calc(x0, y0);

	// todo: move into own script
	// set pixel in bitmap
	rsSetElementAt_uchar4(gOut, color, x0, y0);

	return color;
} // function