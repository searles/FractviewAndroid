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

static long __attribute__((overloadable)) dbl2bits(double d) { return *((long*) &d); }
static double __attribute__((overloadable)) bits2dbl(long l) { return *((double*) &l); }

static long2 __attribute__((overloadable)) split(double d) { 
    long bits = dbl2bits(d);
    return (long2){ bits  & 0x000fffffffffffffL, ((bits & 0x7FF0000000000000L) >> 52) - 1023 };
}

// fixme nan + infinity as constants if they don't exist yet
static const double nanum = 0;
static const double infty = 0;


static double __attribute__((overloadable)) unsplit(long m, long e) {
	if(e > 1024) {
		// this is infinity
		 return bits2dbl(0x7ff0000000000000L);
	} else if(e < 1023) {
		// this is 0
		return 0.;
	} else {
		return bits2dbl(m | ((e + 1023) << 52));
	}
}

/*
 * Good precision for proper doubles. For values very close to 0 a bit worse but manageable.
 * @param d
 * @return
 */
static double __attribute__((overloadable)) sqrt(double d) {
    if(d < 0) {
        return nanum;
    } else if(d == 0) {
        return d;
    } else {
        long2 l = split(d);

        if((l.y & 1) != 0) {
            l.x |= 0x0010000000000000L;
            l.y -= 1;
        }

        double x = unsplit(l.x >> 1, l.y >> 1);

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

    long2 l1 = split(x);
    long2 l2 = split(y);

    double x2 = unsplit(l1.x, 0);
    double y2 = unsplit(l2.x, l2.y - l1.y);

    double rad = sqrt(x2 * x2 + y2 * y2);

    long2 r = split(rad);

    return unsplit(r.x, r.y + l1.y);
}

static double __attribute__((overloadable)) atan2(double y, double x) {
    long2 l1 = split(x);
    long2 l2 = split(y);

    if(l1.y > l2.y) {
        l1.y -= l2.y;
        l2.y = 0;
    } else {
        l2.y -= l1.y;
        l1.y = 0;
    }

    double x2 = unsplit(l1.x, l1.y);
    double y2 = unsplit(l2.x, l2.y);

    if(x < 0) x2 = -x2;
    if(y < 0) y2 = -y2;

    return atan2(y2, x2);
}

/*
 * log2 (m * 2^e) = log m + e
 * @param d
 * @return
 */
static double __attribute__((overloadable)) log2(double d) {
    if(d < 0) {
        return nanum;
    } else {
        long2 l = split(d);
        double d2 = unsplit(l.x, 0);
        return log2(d2) + l.y;
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
        return infty;
    } else {
        double ld = d - exponent; // must be between 0 and 1.

        double base = exp2(ld); // no exp2 in java...

        // base is between 1 and 2.

        long2 l = split(base);

        return unsplit(l.x, (long) (exponent) + l.y);
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
        return nanum;
    } else if(x == 0) {
        return y > 0 ? 0 : y == 0 ? 1 : infty;
    } else {
        long2 l = split(x);

        double ld = log2(unsplit(l.x, 0));

        return exp2((ld + l.y) * y);
    }
}

static const double ln2 = 0.693147180559945309417232121458;

static double __attribute__((overloadable)) exp(double f) { return exp2(f * ln2); }
static double __attribute__((overloadable)) log(double f) { return log2(f) * ln2; }

static double __attribute__((overloadable)) sinh(double f) { return sinh((float) f); }
static double __attribute__((overloadable)) cosh(double f) { return cosh((float) f); }
static double __attribute__((overloadable)) tanh(double f) { return tanh((float) f); }

// For the next ones only the float version is available
static double __attribute__((overloadable)) sin(double f) { return sin((float) f); }
static double __attribute__((overloadable)) cos(double f) { return cos((float) f); }
static double __attribute__((overloadable)) tan(double f) { return tan((float) f); }
static double __attribute__((overloadable)) atan(double f) { return atan((float) f); }
static double __attribute__((overloadable)) atanh(double f) { return atanh((float) f); }
static double __attribute__((overloadable)) cbrt(double f) { return cbrt((float) f); }
static double __attribute__((overloadable)) floor(double f) { return floor((float) f); }


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

// From here RS-code.

/*typedef struct {
    double s0;
    double s1;
    double s2;
    double s3;
} double4;*/

// here goes the data


// scale
double2 xx;
double2 yy;
double2 tt;

int *program;
int len;

struct palette * palettes;
struct lab_surface * palette_data;




static double2 __attribute__((overloadable)) map(double x, double y) {
    return (double2) { xx.x * x + yy.x * y + tt.x, xx.y * x + yy.y * y  + tt.y }; // apply affine tranformation
}

static double2 __attribute__((overloadable)) map(double2 xy) {
    return map(xy.x, xy.y); // apply affine tranformation
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
static double __attribute__((overloadable)) dist2(double2 a, double2 b) { double t = a.x - b.x; double u = a.y - b.y; return t * t + u * u; }
static double __attribute__((overloadable)) dist(double2 a, double2 b) { return sqrt(dist2(a, b)); }

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


static int __attribute__((overloadable)) ipow(int base, int exp) {
    int r = 1; if(exp < 0) { base = 1 / base; exp = -exp; }
    for(;exp; exp >>= 1, base *= base) if(exp & 1) r *= base;
    return r;
}

static double __attribute__((overloadable)) ipow(double base, int exp) {
    double r = 1; if(exp < 0) { base = 1 / base; exp = -exp; }
    for(;exp; exp >>= 1, base *= base) if(exp & 1) r *= base;
    return r;
}

static double2 __attribute__((overloadable)) ipow(double2 base, int exp) {
    double2 r = (double2){1., 0.};
    if(exp < 0) { base = recip(base); exp = -exp; }
    for(;exp; exp >>= 1, base = mul(base, base)) if(exp & 1) r = mul(r, base);
    return r;
}

static double4 __attribute__((overloadable)) ipow(double4 base, int exp) {
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

static double __attribute__((overloadable)) ceil(double d) {
	return ceil((float) d);
}

static double2 __attribute__((overloadable)) ceil(double2 z) {
	return (double2) { ceil((float) z.x), ceil((float) z.y) };
}

static double __attribute__((overloadable)) fract(double d) {
	return fract((float) d);
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

/*static double2 __attribute__((overloadable)) floor(double2 d) {
    return (double2) { floor(d.x), floor(d.y) };
}*/

// helper function for the palette:
static double z(rs_matrix4x4 * m, double2 c) {
    // assert 0 <= x <= 1
    // assert 0 <= y <= 1
    // FIXME: It is transposed, but why?
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

int width;
int height;

static uchar4 calc(int x, int y) {
    // temp vars
    int ti_0, ti_1;
    float tf_0, tf_1, tf_2;
    double td_0;
    float2 tf2_0, tf2_1;
    float4 tf4_0, tf4_1;
    double2 td2_0, td2_1;

    // program counter + current instruction
    int * is = program;

	int data[1024];
	data[0] = x; data[1] = y;
	// int* color = &data[2];
	data[3] = width; data[4] = height;


	int pc = 0; ///// Start of generated code
	while(pc < len) {
		switch(is[pc]) {
			///// mov[20]
			case 0: /* integer reg[integer] */	(* (int*) &data[is[pc + 2]]) = ((* (int*) &is[pc + 1])); pc += 3; break;
			case 1: /* reg[integer] reg[integer] */	(* (int*) &data[is[pc + 2]]) = ((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 2: /* integer reg[real] */	(* (double*) &data[is[pc + 2]]) = convert_real((* (int*) &is[pc + 1])); pc += 3; break;
			case 3: /* reg[integer] reg[real] */	(* (double*) &data[is[pc + 2]]) = convert_real((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 4: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = ((* (double*) &is[pc + 1])); pc += 4; break;
			case 5: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = ((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 6: /* integer reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = convert_cplx((* (int*) &is[pc + 1])); pc += 3; break;
			case 7: /* reg[integer] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = convert_cplx((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 8: /* real reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = convert_cplx((* (double*) &is[pc + 1])); pc += 4; break;
			case 9: /* reg[real] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = convert_cplx((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 10: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = ((* (double2*) &is[pc + 1])); pc += 6; break;
			case 11: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = ((* (double2*) &data[is[pc + 1]])); pc += 3; break;
			case 12: /* integer reg[quat] */	(* (double4*) &data[is[pc + 2]]) = convert_quat((* (int*) &is[pc + 1])); pc += 3; break;
			case 13: /* reg[integer] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = convert_quat((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 14: /* real reg[quat] */	(* (double4*) &data[is[pc + 3]]) = convert_quat((* (double*) &is[pc + 1])); pc += 4; break;
			case 15: /* reg[real] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = convert_quat((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 16: /* cplx reg[quat] */	(* (double4*) &data[is[pc + 5]]) = convert_quat((* (double2*) &is[pc + 1])); pc += 6; break;
			case 17: /* reg[cplx] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = convert_quat((* (double2*) &data[is[pc + 1]])); pc += 3; break;
			case 18: /* quat reg[quat] */	(* (double4*) &data[is[pc + 9]]) = ((* (double4*) &is[pc + 1])); pc += 10; break;
			case 19: /* reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = ((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// next[2]
			case 20: /* reg[integer] integer label label */	pc = (++(* (int*) &data[is[pc + 1]])) < (* (int*) &is[pc + 2]) ? is[pc + 3] : is[pc + 4]; break;
			case 21: /* reg[integer] reg[integer] label label */	pc = (++(* (int*) &data[is[pc + 1]])) < (* (int*) &data[is[pc + 2]]) ? is[pc + 3] : is[pc + 4]; break;

			///// g[8]
			case 22: /* integer integer label label */	pc = ((* (int*) &is[pc + 1]) > (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 23: /* reg[integer] integer label label */	pc = ((* (int*) &data[is[pc + 1]]) > (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 24: /* integer reg[integer] label label */	pc = ((* (int*) &is[pc + 1]) > (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 25: /* reg[integer] reg[integer] label label */	pc = ((* (int*) &data[is[pc + 1]]) > (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 26: /* real real label label */	pc = ((* (double*) &is[pc + 1]) > (* (double*) &is[pc + 3])) ? is[pc + 5] : is[pc + 6];  break;
			case 27: /* reg[real] real label label */	pc = ((* (double*) &data[is[pc + 1]]) > (* (double*) &is[pc + 2])) ? is[pc + 4] : is[pc + 5];  break;
			case 28: /* real reg[real] label label */	pc = ((* (double*) &is[pc + 1]) > (* (double*) &data[is[pc + 3]])) ? is[pc + 4] : is[pc + 5];  break;
			case 29: /* reg[real] reg[real] label label */	pc = ((* (double*) &data[is[pc + 1]]) > (* (double*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;

			///// ge[8]
			case 30: /* integer integer label label */	pc = ((* (int*) &is[pc + 1]) >= (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 31: /* reg[integer] integer label label */	pc = ((* (int*) &data[is[pc + 1]]) >= (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 32: /* integer reg[integer] label label */	pc = ((* (int*) &is[pc + 1]) >= (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 33: /* reg[integer] reg[integer] label label */	pc = ((* (int*) &data[is[pc + 1]]) >= (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 34: /* real real label label */	pc = ((* (double*) &is[pc + 1]) >= (* (double*) &is[pc + 3])) ? is[pc + 5] : is[pc + 6];  break;
			case 35: /* reg[real] real label label */	pc = ((* (double*) &data[is[pc + 1]]) >= (* (double*) &is[pc + 2])) ? is[pc + 4] : is[pc + 5];  break;
			case 36: /* real reg[real] label label */	pc = ((* (double*) &is[pc + 1]) >= (* (double*) &data[is[pc + 3]])) ? is[pc + 4] : is[pc + 5];  break;
			case 37: /* reg[real] reg[real] label label */	pc = ((* (double*) &data[is[pc + 1]]) >= (* (double*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;

			///// eq[8]
			case 38: /* integer integer label label */	pc = ((* (int*) &is[pc + 1]) == (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 39: /* reg[integer] integer label label */	pc = ((* (int*) &data[is[pc + 1]]) == (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 40: /* integer reg[integer] label label */	pc = ((* (int*) &is[pc + 1]) == (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 41: /* reg[integer] reg[integer] label label */	pc = ((* (int*) &data[is[pc + 1]]) == (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 42: /* real real label label */	pc = ((* (double*) &is[pc + 1]) == (* (double*) &is[pc + 3])) ? is[pc + 5] : is[pc + 6];  break;
			case 43: /* reg[real] real label label */	pc = ((* (double*) &data[is[pc + 1]]) == (* (double*) &is[pc + 2])) ? is[pc + 4] : is[pc + 5];  break;
			case 44: /* real reg[real] label label */	pc = ((* (double*) &is[pc + 1]) == (* (double*) &data[is[pc + 3]])) ? is[pc + 4] : is[pc + 5];  break;
			case 45: /* reg[real] reg[real] label label */	pc = ((* (double*) &data[is[pc + 1]]) == (* (double*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;

			///// ne[8]
			case 46: /* integer integer label label */	pc = ((* (int*) &is[pc + 1]) != (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 47: /* reg[integer] integer label label */	pc = ((* (int*) &data[is[pc + 1]]) != (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 48: /* integer reg[integer] label label */	pc = ((* (int*) &is[pc + 1]) != (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 49: /* reg[integer] reg[integer] label label */	pc = ((* (int*) &data[is[pc + 1]]) != (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 50: /* real real label label */	pc = ((* (double*) &is[pc + 1]) != (* (double*) &is[pc + 3])) ? is[pc + 5] : is[pc + 6];  break;
			case 51: /* reg[real] real label label */	pc = ((* (double*) &data[is[pc + 1]]) != (* (double*) &is[pc + 2])) ? is[pc + 4] : is[pc + 5];  break;
			case 52: /* real reg[real] label label */	pc = ((* (double*) &is[pc + 1]) != (* (double*) &data[is[pc + 3]])) ? is[pc + 4] : is[pc + 5];  break;
			case 53: /* reg[real] reg[real] label label */	pc = ((* (double*) &data[is[pc + 1]]) != (* (double*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;

			///// le[8]
			case 54: /* integer integer label label */	pc = ((* (int*) &is[pc + 1]) <= (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 55: /* reg[integer] integer label label */	pc = ((* (int*) &data[is[pc + 1]]) <= (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 56: /* integer reg[integer] label label */	pc = ((* (int*) &is[pc + 1]) <= (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 57: /* reg[integer] reg[integer] label label */	pc = ((* (int*) &data[is[pc + 1]]) <= (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 58: /* real real label label */	pc = ((* (double*) &is[pc + 1]) <= (* (double*) &is[pc + 3])) ? is[pc + 5] : is[pc + 6];  break;
			case 59: /* reg[real] real label label */	pc = ((* (double*) &data[is[pc + 1]]) <= (* (double*) &is[pc + 2])) ? is[pc + 4] : is[pc + 5];  break;
			case 60: /* real reg[real] label label */	pc = ((* (double*) &is[pc + 1]) <= (* (double*) &data[is[pc + 3]])) ? is[pc + 4] : is[pc + 5];  break;
			case 61: /* reg[real] reg[real] label label */	pc = ((* (double*) &data[is[pc + 1]]) <= (* (double*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;

			///// l[8]
			case 62: /* integer integer label label */	pc = ((* (int*) &is[pc + 1]) < (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 63: /* reg[integer] integer label label */	pc = ((* (int*) &data[is[pc + 1]]) < (* (int*) &is[pc + 2])) ? is[pc + 3] : is[pc + 4];  break;
			case 64: /* integer reg[integer] label label */	pc = ((* (int*) &is[pc + 1]) < (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 65: /* reg[integer] reg[integer] label label */	pc = ((* (int*) &data[is[pc + 1]]) < (* (int*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;
			case 66: /* real real label label */	pc = ((* (double*) &is[pc + 1]) < (* (double*) &is[pc + 3])) ? is[pc + 5] : is[pc + 6];  break;
			case 67: /* reg[real] real label label */	pc = ((* (double*) &data[is[pc + 1]]) < (* (double*) &is[pc + 2])) ? is[pc + 4] : is[pc + 5];  break;
			case 68: /* real reg[real] label label */	pc = ((* (double*) &is[pc + 1]) < (* (double*) &data[is[pc + 3]])) ? is[pc + 4] : is[pc + 5];  break;
			case 69: /* reg[real] reg[real] label label */	pc = ((* (double*) &data[is[pc + 1]]) < (* (double*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4];  break;

			///// radless[4]
			case 70: /* cplx real label label */	pc = rad2((* (double2*) &is[pc + 1])) < sqr((* (double*) &is[pc + 5])) ? is[pc + 7] : is[pc + 8]; break;
			case 71: /* reg[cplx] real label label */	pc = rad2((* (double2*) &data[is[pc + 1]])) < sqr((* (double*) &is[pc + 2])) ? is[pc + 4] : is[pc + 5]; break;
			case 72: /* cplx reg[real] label label */	pc = rad2((* (double2*) &is[pc + 1])) < sqr((* (double*) &data[is[pc + 5]])) ? is[pc + 6] : is[pc + 7]; break;
			case 73: /* reg[cplx] reg[real] label label */	pc = rad2((* (double2*) &data[is[pc + 1]])) < sqr((* (double*) &data[is[pc + 2]])) ? is[pc + 3] : is[pc + 4]; break;

			///// distless[8]
			case 74: /* cplx cplx real label label */	pc = rad2((* (double2*) &is[pc + 1]) - (* (double2*) &is[pc + 5])) < sqr((* (double*) &is[pc + 9])) ? is[pc + 11] : is[pc + 12]; break;
			case 75: /* reg[cplx] cplx real label label */	pc = rad2((* (double2*) &data[is[pc + 1]]) - (* (double2*) &is[pc + 2])) < sqr((* (double*) &is[pc + 6])) ? is[pc + 8] : is[pc + 9]; break;
			case 76: /* cplx reg[cplx] real label label */	pc = rad2((* (double2*) &is[pc + 1]) - (* (double2*) &data[is[pc + 5]])) < sqr((* (double*) &is[pc + 6])) ? is[pc + 8] : is[pc + 9]; break;
			case 77: /* reg[cplx] reg[cplx] real label label */	pc = rad2((* (double2*) &data[is[pc + 1]]) - (* (double2*) &data[is[pc + 2]])) < sqr((* (double*) &is[pc + 3])) ? is[pc + 5] : is[pc + 6]; break;
			case 78: /* cplx cplx reg[real] label label */	pc = rad2((* (double2*) &is[pc + 1]) - (* (double2*) &is[pc + 5])) < sqr((* (double*) &data[is[pc + 9]])) ? is[pc + 10] : is[pc + 11]; break;
			case 79: /* reg[cplx] cplx reg[real] label label */	pc = rad2((* (double2*) &data[is[pc + 1]]) - (* (double2*) &is[pc + 2])) < sqr((* (double*) &data[is[pc + 6]])) ? is[pc + 7] : is[pc + 8]; break;
			case 80: /* cplx reg[cplx] reg[real] label label */	pc = rad2((* (double2*) &is[pc + 1]) - (* (double2*) &data[is[pc + 5]])) < sqr((* (double*) &data[is[pc + 6]])) ? is[pc + 7] : is[pc + 8]; break;
			case 81: /* reg[cplx] reg[cplx] reg[real] label label */	pc = rad2((* (double2*) &data[is[pc + 1]]) - (* (double2*) &data[is[pc + 2]])) < sqr((* (double*) &data[is[pc + 3]])) ? is[pc + 4] : is[pc + 5]; break;

			///// radrange[16]
			case 82: /* cplx cplx real real label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double*) &is[pc + 9]), (* (double*) &is[pc + 11]), is[pc + 13], is[pc + 14], is[pc + 15]); break;
			case 83: /* reg[cplx] cplx real real label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double*) &is[pc + 6]), (* (double*) &is[pc + 8]), is[pc + 10], is[pc + 11], is[pc + 12]); break;
			case 84: /* cplx reg[cplx] real real label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double*) &is[pc + 6]), (* (double*) &is[pc + 8]), is[pc + 10], is[pc + 11], is[pc + 12]); break;
			case 85: /* reg[cplx] reg[cplx] real real label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double*) &is[pc + 3]), (* (double*) &is[pc + 5]), is[pc + 7], is[pc + 8], is[pc + 9]); break;
			case 86: /* cplx cplx reg[real] real label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double*) &data[is[pc + 9]]), (* (double*) &is[pc + 10]), is[pc + 12], is[pc + 13], is[pc + 14]); break;
			case 87: /* reg[cplx] cplx reg[real] real label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double*) &data[is[pc + 6]]), (* (double*) &is[pc + 7]), is[pc + 9], is[pc + 10], is[pc + 11]); break;
			case 88: /* cplx reg[cplx] reg[real] real label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double*) &data[is[pc + 6]]), (* (double*) &is[pc + 7]), is[pc + 9], is[pc + 10], is[pc + 11]); break;
			case 89: /* reg[cplx] reg[cplx] reg[real] real label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double*) &data[is[pc + 3]]), (* (double*) &is[pc + 4]), is[pc + 6], is[pc + 7], is[pc + 8]); break;
			case 90: /* cplx cplx real reg[real] label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double*) &is[pc + 9]), (* (double*) &data[is[pc + 11]]), is[pc + 12], is[pc + 13], is[pc + 14]); break;
			case 91: /* reg[cplx] cplx real reg[real] label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double*) &is[pc + 6]), (* (double*) &data[is[pc + 8]]), is[pc + 9], is[pc + 10], is[pc + 11]); break;
			case 92: /* cplx reg[cplx] real reg[real] label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double*) &is[pc + 6]), (* (double*) &data[is[pc + 8]]), is[pc + 9], is[pc + 10], is[pc + 11]); break;
			case 93: /* reg[cplx] reg[cplx] real reg[real] label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double*) &is[pc + 3]), (* (double*) &data[is[pc + 5]]), is[pc + 6], is[pc + 7], is[pc + 8]); break;
			case 94: /* cplx cplx reg[real] reg[real] label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double*) &data[is[pc + 9]]), (* (double*) &data[is[pc + 10]]), is[pc + 11], is[pc + 12], is[pc + 13]); break;
			case 95: /* reg[cplx] cplx reg[real] reg[real] label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double*) &data[is[pc + 6]]), (* (double*) &data[is[pc + 7]]), is[pc + 8], is[pc + 9], is[pc + 10]); break;
			case 96: /* cplx reg[cplx] reg[real] reg[real] label label label */	pc = radrange((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double*) &data[is[pc + 6]]), (* (double*) &data[is[pc + 7]]), is[pc + 8], is[pc + 9], is[pc + 10]); break;
			case 97: /* reg[cplx] reg[cplx] reg[real] reg[real] label label label */	pc = radrange((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double*) &data[is[pc + 3]]), (* (double*) &data[is[pc + 4]]), is[pc + 5], is[pc + 6], is[pc + 7]); break;

			///// add[16]
			case 98: /* integer integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = add((* (int*) &is[pc + 1]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 99: /* reg[integer] integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = add((* (int*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 100: /* integer reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = add((* (int*) &is[pc + 1]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 101: /* reg[integer] reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = add((* (int*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 102: /* real real reg[real] */	(* (double*) &data[is[pc + 5]]) = add((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 103: /* reg[real] real reg[real] */	(* (double*) &data[is[pc + 4]]) = add((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 104: /* real reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = add((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 105: /* reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 3]]) = add((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 106: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = add((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 107: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = add((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 108: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = add((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 109: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = add((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;
			case 110: /* quat quat reg[quat] */	(* (double4*) &data[is[pc + 17]]) = add((* (double4*) &is[pc + 1]), (* (double4*) &is[pc + 9])); pc += 18; break;
			case 111: /* reg[quat] quat reg[quat] */	(* (double4*) &data[is[pc + 10]]) = add((* (double4*) &data[is[pc + 1]]), (* (double4*) &is[pc + 2])); pc += 11; break;
			case 112: /* quat reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = add((* (double4*) &is[pc + 1]), (* (double4*) &data[is[pc + 9]])); pc += 11; break;
			case 113: /* reg[quat] reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = add((* (double4*) &data[is[pc + 1]]), (* (double4*) &data[is[pc + 2]])); pc += 4; break;

			///// sub[16]
			case 114: /* integer integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = sub((* (int*) &is[pc + 1]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 115: /* reg[integer] integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = sub((* (int*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 116: /* integer reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = sub((* (int*) &is[pc + 1]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 117: /* reg[integer] reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = sub((* (int*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 118: /* real real reg[real] */	(* (double*) &data[is[pc + 5]]) = sub((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 119: /* reg[real] real reg[real] */	(* (double*) &data[is[pc + 4]]) = sub((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 120: /* real reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = sub((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 121: /* reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 3]]) = sub((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 122: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = sub((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 123: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = sub((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 124: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = sub((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 125: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = sub((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;
			case 126: /* quat quat reg[quat] */	(* (double4*) &data[is[pc + 17]]) = sub((* (double4*) &is[pc + 1]), (* (double4*) &is[pc + 9])); pc += 18; break;
			case 127: /* reg[quat] quat reg[quat] */	(* (double4*) &data[is[pc + 10]]) = sub((* (double4*) &data[is[pc + 1]]), (* (double4*) &is[pc + 2])); pc += 11; break;
			case 128: /* quat reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = sub((* (double4*) &is[pc + 1]), (* (double4*) &data[is[pc + 9]])); pc += 11; break;
			case 129: /* reg[quat] reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = sub((* (double4*) &data[is[pc + 1]]), (* (double4*) &data[is[pc + 2]])); pc += 4; break;

			///// mul[16]
			case 130: /* integer integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = mul((* (int*) &is[pc + 1]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 131: /* reg[integer] integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = mul((* (int*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 132: /* integer reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = mul((* (int*) &is[pc + 1]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 133: /* reg[integer] reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = mul((* (int*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 134: /* real real reg[real] */	(* (double*) &data[is[pc + 5]]) = mul((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 135: /* reg[real] real reg[real] */	(* (double*) &data[is[pc + 4]]) = mul((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 136: /* real reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = mul((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 137: /* reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 3]]) = mul((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 138: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = mul((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 139: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = mul((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 140: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = mul((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 141: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = mul((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;
			case 142: /* quat quat reg[quat] */	(* (double4*) &data[is[pc + 17]]) = mul((* (double4*) &is[pc + 1]), (* (double4*) &is[pc + 9])); pc += 18; break;
			case 143: /* reg[quat] quat reg[quat] */	(* (double4*) &data[is[pc + 10]]) = mul((* (double4*) &data[is[pc + 1]]), (* (double4*) &is[pc + 2])); pc += 11; break;
			case 144: /* quat reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = mul((* (double4*) &is[pc + 1]), (* (double4*) &data[is[pc + 9]])); pc += 11; break;
			case 145: /* reg[quat] reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = mul((* (double4*) &data[is[pc + 1]]), (* (double4*) &data[is[pc + 2]])); pc += 4; break;

			///// scalarmul[4]
			case 146: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = scalarmul((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 147: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = scalarmul((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 148: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = scalarmul((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 149: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = scalarmul((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;

			///// div[12]
			case 150: /* real real reg[real] */	(* (double*) &data[is[pc + 5]]) = div((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 151: /* reg[real] real reg[real] */	(* (double*) &data[is[pc + 4]]) = div((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 152: /* real reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = div((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 153: /* reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 3]]) = div((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 154: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = div((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 155: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = div((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 156: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = div((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 157: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = div((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;
			case 158: /* quat quat reg[quat] */	(* (double4*) &data[is[pc + 17]]) = div((* (double4*) &is[pc + 1]), (* (double4*) &is[pc + 9])); pc += 18; break;
			case 159: /* reg[quat] quat reg[quat] */	(* (double4*) &data[is[pc + 10]]) = div((* (double4*) &data[is[pc + 1]]), (* (double4*) &is[pc + 2])); pc += 11; break;
			case 160: /* quat reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = div((* (double4*) &is[pc + 1]), (* (double4*) &data[is[pc + 9]])); pc += 11; break;
			case 161: /* reg[quat] reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = div((* (double4*) &data[is[pc + 1]]), (* (double4*) &data[is[pc + 2]])); pc += 4; break;

			///// mod[4]
			case 162: /* integer integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = mod((* (int*) &is[pc + 1]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 163: /* reg[integer] integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = mod((* (int*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 164: /* integer reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = mod((* (int*) &is[pc + 1]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 165: /* reg[integer] reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = mod((* (int*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;

			///// pow[24]
			case 166: /* integer integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = ipow((* (int*) &is[pc + 1]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 167: /* reg[integer] integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = ipow((* (int*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 168: /* integer reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = ipow((* (int*) &is[pc + 1]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 169: /* reg[integer] reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = ipow((* (int*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 170: /* real integer reg[real] */	(* (double*) &data[is[pc + 4]]) = ipow((* (double*) &is[pc + 1]), (* (int*) &is[pc + 3])); pc += 5; break;
			case 171: /* reg[real] integer reg[real] */	(* (double*) &data[is[pc + 3]]) = ipow((* (double*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 172: /* real reg[integer] reg[real] */	(* (double*) &data[is[pc + 4]]) = ipow((* (double*) &is[pc + 1]), (* (int*) &data[is[pc + 3]])); pc += 5; break;
			case 173: /* reg[real] reg[integer] reg[real] */	(* (double*) &data[is[pc + 3]]) = ipow((* (double*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 174: /* cplx integer reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = ipow((* (double2*) &is[pc + 1]), (* (int*) &is[pc + 5])); pc += 7; break;
			case 175: /* reg[cplx] integer reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = ipow((* (double2*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 176: /* cplx reg[integer] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = ipow((* (double2*) &is[pc + 1]), (* (int*) &data[is[pc + 5]])); pc += 7; break;
			case 177: /* reg[cplx] reg[integer] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = ipow((* (double2*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 178: /* quat integer reg[quat] */	(* (double4*) &data[is[pc + 10]]) = ipow((* (double4*) &is[pc + 1]), (* (int*) &is[pc + 9])); pc += 11; break;
			case 179: /* reg[quat] integer reg[quat] */	(* (double4*) &data[is[pc + 3]]) = ipow((* (double4*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 180: /* quat reg[integer] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = ipow((* (double4*) &is[pc + 1]), (* (int*) &data[is[pc + 9]])); pc += 11; break;
			case 181: /* reg[quat] reg[integer] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = ipow((* (double4*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 182: /* real real reg[real] */	(* (double*) &data[is[pc + 5]]) = pow((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 183: /* reg[real] real reg[real] */	(* (double*) &data[is[pc + 4]]) = pow((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 184: /* real reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = pow((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 185: /* reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 3]]) = pow((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 186: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = pow((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 187: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = pow((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 188: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = pow((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 189: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = pow((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;

			///// min[16]
			case 190: /* integer integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = min((* (int*) &is[pc + 1]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 191: /* reg[integer] integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = min((* (int*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 192: /* integer reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = min((* (int*) &is[pc + 1]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 193: /* reg[integer] reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = min((* (int*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 194: /* real real reg[real] */	(* (double*) &data[is[pc + 5]]) = min((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 195: /* reg[real] real reg[real] */	(* (double*) &data[is[pc + 4]]) = min((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 196: /* real reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = min((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 197: /* reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 3]]) = min((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 198: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = min((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 199: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = min((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 200: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = min((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 201: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = min((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;
			case 202: /* quat quat reg[quat] */	(* (double4*) &data[is[pc + 17]]) = min((* (double4*) &is[pc + 1]), (* (double4*) &is[pc + 9])); pc += 18; break;
			case 203: /* reg[quat] quat reg[quat] */	(* (double4*) &data[is[pc + 10]]) = min((* (double4*) &data[is[pc + 1]]), (* (double4*) &is[pc + 2])); pc += 11; break;
			case 204: /* quat reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = min((* (double4*) &is[pc + 1]), (* (double4*) &data[is[pc + 9]])); pc += 11; break;
			case 205: /* reg[quat] reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = min((* (double4*) &data[is[pc + 1]]), (* (double4*) &data[is[pc + 2]])); pc += 4; break;

			///// max[16]
			case 206: /* integer integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = max((* (int*) &is[pc + 1]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 207: /* reg[integer] integer reg[integer] */	(* (int*) &data[is[pc + 3]]) = max((* (int*) &data[is[pc + 1]]), (* (int*) &is[pc + 2])); pc += 4; break;
			case 208: /* integer reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = max((* (int*) &is[pc + 1]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 209: /* reg[integer] reg[integer] reg[integer] */	(* (int*) &data[is[pc + 3]]) = max((* (int*) &data[is[pc + 1]]), (* (int*) &data[is[pc + 2]])); pc += 4; break;
			case 210: /* real real reg[real] */	(* (double*) &data[is[pc + 5]]) = max((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 211: /* reg[real] real reg[real] */	(* (double*) &data[is[pc + 4]]) = max((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 212: /* real reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = max((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 213: /* reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 3]]) = max((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 214: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = max((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 215: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = max((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 216: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = max((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 217: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = max((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;
			case 218: /* quat quat reg[quat] */	(* (double4*) &data[is[pc + 17]]) = max((* (double4*) &is[pc + 1]), (* (double4*) &is[pc + 9])); pc += 18; break;
			case 219: /* reg[quat] quat reg[quat] */	(* (double4*) &data[is[pc + 10]]) = max((* (double4*) &data[is[pc + 1]]), (* (double4*) &is[pc + 2])); pc += 11; break;
			case 220: /* quat reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = max((* (double4*) &is[pc + 1]), (* (double4*) &data[is[pc + 9]])); pc += 11; break;
			case 221: /* reg[quat] reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = max((* (double4*) &data[is[pc + 1]]), (* (double4*) &data[is[pc + 2]])); pc += 4; break;

			///// cons[20]
			case 222: /* real real reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = cons((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 223: /* reg[real] real reg[cplx] */	(* (double2*) &data[is[pc + 4]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 224: /* real reg[real] reg[cplx] */	(* (double2*) &data[is[pc + 4]]) = cons((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 225: /* reg[real] reg[real] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 226: /* real real real real reg[quat] */	(* (double4*) &data[is[pc + 9]]) = cons((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3]), (* (double*) &is[pc + 5]), (* (double*) &is[pc + 7])); pc += 10; break;
			case 227: /* reg[real] real real real reg[quat] */	(* (double4*) &data[is[pc + 8]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &is[pc + 4]), (* (double*) &is[pc + 6])); pc += 9; break;
			case 228: /* real reg[real] real real reg[quat] */	(* (double4*) &data[is[pc + 8]]) = cons((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]]), (* (double*) &is[pc + 4]), (* (double*) &is[pc + 6])); pc += 9; break;
			case 229: /* reg[real] reg[real] real real reg[quat] */	(* (double4*) &data[is[pc + 7]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &is[pc + 3]), (* (double*) &is[pc + 5])); pc += 8; break;
			case 230: /* real real reg[real] real reg[quat] */	(* (double4*) &data[is[pc + 8]]) = cons((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3]), (* (double*) &data[is[pc + 5]]), (* (double*) &is[pc + 6])); pc += 9; break;
			case 231: /* reg[real] real reg[real] real reg[quat] */	(* (double4*) &data[is[pc + 7]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &data[is[pc + 4]]), (* (double*) &is[pc + 5])); pc += 8; break;
			case 232: /* real reg[real] reg[real] real reg[quat] */	(* (double4*) &data[is[pc + 7]]) = cons((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]]), (* (double*) &data[is[pc + 4]]), (* (double*) &is[pc + 5])); pc += 8; break;
			case 233: /* reg[real] reg[real] reg[real] real reg[quat] */	(* (double4*) &data[is[pc + 6]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &data[is[pc + 3]]), (* (double*) &is[pc + 4])); pc += 7; break;
			case 234: /* real real real reg[real] reg[quat] */	(* (double4*) &data[is[pc + 8]]) = cons((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3]), (* (double*) &is[pc + 5]), (* (double*) &data[is[pc + 7]])); pc += 9; break;
			case 235: /* reg[real] real real reg[real] reg[quat] */	(* (double4*) &data[is[pc + 7]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &is[pc + 4]), (* (double*) &data[is[pc + 6]])); pc += 8; break;
			case 236: /* real reg[real] real reg[real] reg[quat] */	(* (double4*) &data[is[pc + 7]]) = cons((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]]), (* (double*) &is[pc + 4]), (* (double*) &data[is[pc + 6]])); pc += 8; break;
			case 237: /* reg[real] reg[real] real reg[real] reg[quat] */	(* (double4*) &data[is[pc + 6]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &is[pc + 3]), (* (double*) &data[is[pc + 5]])); pc += 7; break;
			case 238: /* real real reg[real] reg[real] reg[quat] */	(* (double4*) &data[is[pc + 7]]) = cons((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3]), (* (double*) &data[is[pc + 5]]), (* (double*) &data[is[pc + 6]])); pc += 8; break;
			case 239: /* reg[real] real reg[real] reg[real] reg[quat] */	(* (double4*) &data[is[pc + 6]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &data[is[pc + 4]]), (* (double*) &data[is[pc + 5]])); pc += 7; break;
			case 240: /* real reg[real] reg[real] reg[real] reg[quat] */	(* (double4*) &data[is[pc + 6]]) = cons((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]]), (* (double*) &data[is[pc + 4]]), (* (double*) &data[is[pc + 5]])); pc += 7; break;
			case 241: /* reg[real] reg[real] reg[real] reg[real] reg[quat] */	(* (double4*) &data[is[pc + 5]]) = cons((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &data[is[pc + 3]]), (* (double*) &data[is[pc + 4]])); pc += 6; break;

			///// neg[8]
			case 242: /* integer reg[integer] */	(* (int*) &data[is[pc + 2]]) = neg((* (int*) &is[pc + 1])); pc += 3; break;
			case 243: /* reg[integer] reg[integer] */	(* (int*) &data[is[pc + 2]]) = neg((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 244: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = neg((* (double*) &is[pc + 1])); pc += 4; break;
			case 245: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = neg((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 246: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = neg((* (double2*) &is[pc + 1])); pc += 6; break;
			case 247: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = neg((* (double2*) &data[is[pc + 1]])); pc += 3; break;
			case 248: /* quat reg[quat] */	(* (double4*) &data[is[pc + 9]]) = neg((* (double4*) &is[pc + 1])); pc += 10; break;
			case 249: /* reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = neg((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// recip[8]
			case 250: /* integer reg[real] */	(* (double*) &data[is[pc + 2]]) = recip((* (int*) &is[pc + 1])); pc += 3; break;
			case 251: /* reg[integer] reg[real] */	(* (double*) &data[is[pc + 2]]) = recip((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 252: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = recip((* (double*) &is[pc + 1])); pc += 4; break;
			case 253: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = recip((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 254: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = recip((* (double2*) &is[pc + 1])); pc += 6; break;
			case 255: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = recip((* (double2*) &data[is[pc + 1]])); pc += 3; break;
			case 256: /* quat reg[quat] */	(* (double4*) &data[is[pc + 9]]) = recip((* (double4*) &is[pc + 1])); pc += 10; break;
			case 257: /* reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = recip((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// abs[8]
			case 258: /* integer reg[integer] */	(* (int*) &data[is[pc + 2]]) = abs((* (int*) &is[pc + 1])); pc += 3; break;
			case 259: /* reg[integer] reg[integer] */	(* (int*) &data[is[pc + 2]]) = abs((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 260: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = abs((* (double*) &is[pc + 1])); pc += 4; break;
			case 261: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = abs((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 262: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = abs((* (double2*) &is[pc + 1])); pc += 6; break;
			case 263: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = abs((* (double2*) &data[is[pc + 1]])); pc += 3; break;
			case 264: /* quat reg[quat] */	(* (double4*) &data[is[pc + 9]]) = abs((* (double4*) &is[pc + 1])); pc += 10; break;
			case 265: /* reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = abs((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// sqr[8]
			case 266: /* integer reg[integer] */	(* (int*) &data[is[pc + 2]]) = sqr((* (int*) &is[pc + 1])); pc += 3; break;
			case 267: /* reg[integer] reg[integer] */	(* (int*) &data[is[pc + 2]]) = sqr((* (int*) &data[is[pc + 1]])); pc += 3; break;
			case 268: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = sqr((* (double*) &is[pc + 1])); pc += 4; break;
			case 269: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = sqr((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 270: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = sqr((* (double2*) &is[pc + 1])); pc += 6; break;
			case 271: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = sqr((* (double2*) &data[is[pc + 1]])); pc += 3; break;
			case 272: /* quat reg[quat] */	(* (double4*) &data[is[pc + 9]]) = sqr((* (double4*) &is[pc + 1])); pc += 10; break;
			case 273: /* reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = sqr((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// conj[2]
			case 274: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = conj((* (double2*) &is[pc + 1])); pc += 6; break;
			case 275: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = conj((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// exp[4]
			case 276: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = exp((* (double*) &is[pc + 1])); pc += 4; break;
			case 277: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = exp((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 278: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = exp((* (double2*) &is[pc + 1])); pc += 6; break;
			case 279: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = exp((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// log[4]
			case 280: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = log((* (double*) &is[pc + 1])); pc += 4; break;
			case 281: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = log((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 282: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = log((* (double2*) &is[pc + 1])); pc += 6; break;
			case 283: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = log((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// sqrt[4]
			case 284: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = sqrt((* (double*) &is[pc + 1])); pc += 4; break;
			case 285: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = sqrt((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 286: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = sqrt((* (double2*) &is[pc + 1])); pc += 6; break;
			case 287: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = sqrt((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// sin[4]
			case 288: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = sin((* (double*) &is[pc + 1])); pc += 4; break;
			case 289: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = sin((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 290: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = sin((* (double2*) &is[pc + 1])); pc += 6; break;
			case 291: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = sin((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// cos[4]
			case 292: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = cos((* (double*) &is[pc + 1])); pc += 4; break;
			case 293: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = cos((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 294: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = cos((* (double2*) &is[pc + 1])); pc += 6; break;
			case 295: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = cos((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// tan[4]
			case 296: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = tan((* (double*) &is[pc + 1])); pc += 4; break;
			case 297: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = tan((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 298: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = tan((* (double2*) &is[pc + 1])); pc += 6; break;
			case 299: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = tan((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// atan[4]
			case 300: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = atan((* (double*) &is[pc + 1])); pc += 4; break;
			case 301: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = atan((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 302: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = atan((* (double2*) &is[pc + 1])); pc += 6; break;
			case 303: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = atan((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// sinh[4]
			case 304: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = sinh((* (double*) &is[pc + 1])); pc += 4; break;
			case 305: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = sinh((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 306: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = sinh((* (double2*) &is[pc + 1])); pc += 6; break;
			case 307: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = sinh((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// cosh[4]
			case 308: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = cosh((* (double*) &is[pc + 1])); pc += 4; break;
			case 309: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = cosh((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 310: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = cosh((* (double2*) &is[pc + 1])); pc += 6; break;
			case 311: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = cosh((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// tanh[4]
			case 312: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = tanh((* (double*) &is[pc + 1])); pc += 4; break;
			case 313: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = tanh((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 314: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = tanh((* (double2*) &is[pc + 1])); pc += 6; break;
			case 315: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = tanh((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// atanh[4]
			case 316: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = atanh((* (double*) &is[pc + 1])); pc += 4; break;
			case 317: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = atanh((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 318: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = atanh((* (double2*) &is[pc + 1])); pc += 6; break;
			case 319: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = atanh((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// floor[4]
			case 320: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = floor((* (double*) &is[pc + 1])); pc += 4; break;
			case 321: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = floor((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 322: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = floor((* (double2*) &is[pc + 1])); pc += 6; break;
			case 323: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = floor((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// ceil[4]
			case 324: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = ceil((* (double*) &is[pc + 1])); pc += 4; break;
			case 325: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = ceil((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 326: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = ceil((* (double2*) &is[pc + 1])); pc += 6; break;
			case 327: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = ceil((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// fract[4]
			case 328: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = fract((* (double*) &is[pc + 1])); pc += 4; break;
			case 329: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = fract((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 330: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = fract((* (double2*) &is[pc + 1])); pc += 6; break;
			case 331: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = fract((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// circlefn[4]
			case 332: /* real reg[real] */	(* (double*) &data[is[pc + 3]]) = circlefn((* (double*) &is[pc + 1])); pc += 4; break;
			case 333: /* reg[real] reg[real] */	(* (double*) &data[is[pc + 2]]) = circlefn((* (double*) &data[is[pc + 1]])); pc += 3; break;
			case 334: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = circlefn((* (double2*) &is[pc + 1])); pc += 6; break;
			case 335: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = circlefn((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// real2int[2]
			case 336: /* real reg[integer] */	(* (int*) &data[is[pc + 3]]) = real2int((* (double*) &is[pc + 1])); pc += 4; break;
			case 337: /* reg[real] reg[integer] */	(* (int*) &data[is[pc + 2]]) = real2int((* (double*) &data[is[pc + 1]])); pc += 3; break;

			///// re[2]
			case 338: /* cplx reg[real] */	(* (double*) &data[is[pc + 5]]) = re((* (double2*) &is[pc + 1])); pc += 6; break;
			case 339: /* reg[cplx] reg[real] */	(* (double*) &data[is[pc + 2]]) = re((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// im[2]
			case 340: /* cplx reg[real] */	(* (double*) &data[is[pc + 5]]) = im((* (double2*) &is[pc + 1])); pc += 6; break;
			case 341: /* reg[cplx] reg[real] */	(* (double*) &data[is[pc + 2]]) = im((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// mandelbrot[4]
			case 342: /* cplx cplx reg[cplx] */	(* (double2*) &data[is[pc + 9]]) = mandelbrot((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 343: /* reg[cplx] cplx reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = mandelbrot((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 344: /* cplx reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 6]]) = mandelbrot((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 345: /* reg[cplx] reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = mandelbrot((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;

			///// dot[4]
			case 346: /* cplx cplx reg[real] */	(* (double*) &data[is[pc + 9]]) = dot((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 347: /* reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 6]]) = dot((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 348: /* cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 6]]) = dot((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 349: /* reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 3]]) = dot((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;

			///// rad2[2]
			case 350: /* cplx reg[real] */	(* (double*) &data[is[pc + 5]]) = rad2((* (double2*) &is[pc + 1])); pc += 6; break;
			case 351: /* reg[cplx] reg[real] */	(* (double*) &data[is[pc + 2]]) = rad2((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// rad[2]
			case 352: /* cplx reg[real] */	(* (double*) &data[is[pc + 5]]) = rad((* (double2*) &is[pc + 1])); pc += 6; break;
			case 353: /* reg[cplx] reg[real] */	(* (double*) &data[is[pc + 2]]) = rad((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// arc[2]
			case 354: /* cplx reg[real] */	(* (double*) &data[is[pc + 5]]) = arc((* (double2*) &is[pc + 1])); pc += 6; break;
			case 355: /* reg[cplx] reg[real] */	(* (double*) &data[is[pc + 2]]) = arc((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// arcnorm[2]
			case 356: /* cplx reg[real] */	(* (double*) &data[is[pc + 5]]) = arcnorm((* (double2*) &is[pc + 1])); pc += 6; break;
			case 357: /* reg[cplx] reg[real] */	(* (double*) &data[is[pc + 2]]) = arcnorm((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// dist2[4]
			case 358: /* cplx cplx reg[real] */	(* (double*) &data[is[pc + 9]]) = dist2((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 359: /* reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 6]]) = dist2((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 360: /* cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 6]]) = dist2((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 361: /* reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 3]]) = dist2((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;

			///// dist[4]
			case 362: /* cplx cplx reg[real] */	(* (double*) &data[is[pc + 9]]) = dist((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5])); pc += 10; break;
			case 363: /* reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 6]]) = dist((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2])); pc += 7; break;
			case 364: /* cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 6]]) = dist((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]])); pc += 7; break;
			case 365: /* reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 3]]) = dist((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]])); pc += 4; break;

			///// rabs[2]
			case 366: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = rabs((* (double2*) &is[pc + 1])); pc += 6; break;
			case 367: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = rabs((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// iabs[2]
			case 368: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = iabs((* (double2*) &is[pc + 1])); pc += 6; break;
			case 369: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = iabs((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// flip[2]
			case 370: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = flip((* (double2*) &is[pc + 1])); pc += 6; break;
			case 371: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = flip((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// polar[2]
			case 372: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = polar((* (double2*) &is[pc + 1])); pc += 6; break;
			case 373: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = polar((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// rect[2]
			case 374: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = rect((* (double2*) &is[pc + 1])); pc += 6; break;
			case 375: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = rect((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// circle[8]
			case 376: /* cplx real cplx reg[real] */	(* (double*) &data[is[pc + 11]]) = circle((* (double2*) &is[pc + 1]), (* (double*) &is[pc + 5]), (* (double2*) &is[pc + 7])); pc += 12; break;
			case 377: /* reg[cplx] real cplx reg[real] */	(* (double*) &data[is[pc + 8]]) = circle((* (double2*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double2*) &is[pc + 4])); pc += 9; break;
			case 378: /* cplx reg[real] cplx reg[real] */	(* (double*) &data[is[pc + 10]]) = circle((* (double2*) &is[pc + 1]), (* (double*) &data[is[pc + 5]]), (* (double2*) &is[pc + 6])); pc += 11; break;
			case 379: /* reg[cplx] reg[real] cplx reg[real] */	(* (double*) &data[is[pc + 7]]) = circle((* (double2*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double2*) &is[pc + 3])); pc += 8; break;
			case 380: /* cplx real reg[cplx] reg[real] */	(* (double*) &data[is[pc + 8]]) = circle((* (double2*) &is[pc + 1]), (* (double*) &is[pc + 5]), (* (double2*) &data[is[pc + 7]])); pc += 9; break;
			case 381: /* reg[cplx] real reg[cplx] reg[real] */	(* (double*) &data[is[pc + 5]]) = circle((* (double2*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double2*) &data[is[pc + 4]])); pc += 6; break;
			case 382: /* cplx reg[real] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 7]]) = circle((* (double2*) &is[pc + 1]), (* (double*) &data[is[pc + 5]]), (* (double2*) &data[is[pc + 6]])); pc += 8; break;
			case 383: /* reg[cplx] reg[real] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 4]]) = circle((* (double2*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double2*) &data[is[pc + 3]])); pc += 5; break;

			///// line[8]
			case 384: /* cplx cplx cplx reg[real] */	(* (double*) &data[is[pc + 13]]) = line((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double2*) &is[pc + 9])); pc += 14; break;
			case 385: /* reg[cplx] cplx cplx reg[real] */	(* (double*) &data[is[pc + 10]]) = line((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double2*) &is[pc + 6])); pc += 11; break;
			case 386: /* cplx reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 10]]) = line((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double2*) &is[pc + 6])); pc += 11; break;
			case 387: /* reg[cplx] reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 7]]) = line((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double2*) &is[pc + 3])); pc += 8; break;
			case 388: /* cplx cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 10]]) = line((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double2*) &data[is[pc + 9]])); pc += 11; break;
			case 389: /* reg[cplx] cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 7]]) = line((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double2*) &data[is[pc + 6]])); pc += 8; break;
			case 390: /* cplx reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 7]]) = line((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double2*) &data[is[pc + 6]])); pc += 8; break;
			case 391: /* reg[cplx] reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 4]]) = line((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double2*) &data[is[pc + 3]])); pc += 5; break;

			///// segment[8]
			case 392: /* cplx cplx cplx reg[real] */	(* (double*) &data[is[pc + 13]]) = segment((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double2*) &is[pc + 9])); pc += 14; break;
			case 393: /* reg[cplx] cplx cplx reg[real] */	(* (double*) &data[is[pc + 10]]) = segment((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double2*) &is[pc + 6])); pc += 11; break;
			case 394: /* cplx reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 10]]) = segment((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double2*) &is[pc + 6])); pc += 11; break;
			case 395: /* reg[cplx] reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 7]]) = segment((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double2*) &is[pc + 3])); pc += 8; break;
			case 396: /* cplx cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 10]]) = segment((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double2*) &data[is[pc + 9]])); pc += 11; break;
			case 397: /* reg[cplx] cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 7]]) = segment((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double2*) &data[is[pc + 6]])); pc += 8; break;
			case 398: /* cplx reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 7]]) = segment((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double2*) &data[is[pc + 6]])); pc += 8; break;
			case 399: /* reg[cplx] reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 4]]) = segment((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double2*) &data[is[pc + 3]])); pc += 5; break;

			///// box[8]
			case 400: /* cplx cplx cplx reg[real] */	(* (double*) &data[is[pc + 13]]) = box((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double2*) &is[pc + 9])); pc += 14; break;
			case 401: /* reg[cplx] cplx cplx reg[real] */	(* (double*) &data[is[pc + 10]]) = box((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double2*) &is[pc + 6])); pc += 11; break;
			case 402: /* cplx reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 10]]) = box((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double2*) &is[pc + 6])); pc += 11; break;
			case 403: /* reg[cplx] reg[cplx] cplx reg[real] */	(* (double*) &data[is[pc + 7]]) = box((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double2*) &is[pc + 3])); pc += 8; break;
			case 404: /* cplx cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 10]]) = box((* (double2*) &is[pc + 1]), (* (double2*) &is[pc + 5]), (* (double2*) &data[is[pc + 9]])); pc += 11; break;
			case 405: /* reg[cplx] cplx reg[cplx] reg[real] */	(* (double*) &data[is[pc + 7]]) = box((* (double2*) &data[is[pc + 1]]), (* (double2*) &is[pc + 2]), (* (double2*) &data[is[pc + 6]])); pc += 8; break;
			case 406: /* cplx reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 7]]) = box((* (double2*) &is[pc + 1]), (* (double2*) &data[is[pc + 5]]), (* (double2*) &data[is[pc + 6]])); pc += 8; break;
			case 407: /* reg[cplx] reg[cplx] reg[cplx] reg[real] */	(* (double*) &data[is[pc + 4]]) = box((* (double2*) &data[is[pc + 1]]), (* (double2*) &data[is[pc + 2]]), (* (double2*) &data[is[pc + 3]])); pc += 5; break;

			///// map[6]
			case 408: /* real real reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = map((* (double*) &is[pc + 1]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 409: /* reg[real] real reg[cplx] */	(* (double2*) &data[is[pc + 4]]) = map((* (double*) &data[is[pc + 1]]), (* (double*) &is[pc + 2])); pc += 5; break;
			case 410: /* real reg[real] reg[cplx] */	(* (double2*) &data[is[pc + 4]]) = map((* (double*) &is[pc + 1]), (* (double*) &data[is[pc + 3]])); pc += 5; break;
			case 411: /* reg[real] reg[real] reg[cplx] */	(* (double2*) &data[is[pc + 3]]) = map((* (double*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]])); pc += 4; break;
			case 412: /* cplx reg[cplx] */	(* (double2*) &data[is[pc + 5]]) = map((* (double2*) &is[pc + 1])); pc += 6; break;
			case 413: /* reg[cplx] reg[cplx] */	(* (double2*) &data[is[pc + 2]]) = map((* (double2*) &data[is[pc + 1]])); pc += 3; break;

			///// smooth[8]
			case 414: /* cplx real real reg[real] */	(* (double*) &data[is[pc + 9]]) = smooth((* (double2*) &is[pc + 1]), (* (double*) &is[pc + 5]), (* (double*) &is[pc + 7])); pc += 10; break;
			case 415: /* reg[cplx] real real reg[real] */	(* (double*) &data[is[pc + 6]]) = smooth((* (double2*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &is[pc + 4])); pc += 7; break;
			case 416: /* cplx reg[real] real reg[real] */	(* (double*) &data[is[pc + 8]]) = smooth((* (double2*) &is[pc + 1]), (* (double*) &data[is[pc + 5]]), (* (double*) &is[pc + 6])); pc += 9; break;
			case 417: /* reg[cplx] reg[real] real reg[real] */	(* (double*) &data[is[pc + 5]]) = smooth((* (double2*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 418: /* cplx real reg[real] reg[real] */	(* (double*) &data[is[pc + 8]]) = smooth((* (double2*) &is[pc + 1]), (* (double*) &is[pc + 5]), (* (double*) &data[is[pc + 7]])); pc += 9; break;
			case 419: /* reg[cplx] real reg[real] reg[real] */	(* (double*) &data[is[pc + 5]]) = smooth((* (double2*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &data[is[pc + 4]])); pc += 6; break;
			case 420: /* cplx reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 7]]) = smooth((* (double2*) &is[pc + 1]), (* (double*) &data[is[pc + 5]]), (* (double*) &data[is[pc + 6]])); pc += 8; break;
			case 421: /* reg[cplx] reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = smooth((* (double2*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &data[is[pc + 3]])); pc += 5; break;

			///// smoothen[8]
			case 422: /* cplx real real reg[real] */	(* (double*) &data[is[pc + 9]]) = smoothen((* (double2*) &is[pc + 1]), (* (double*) &is[pc + 5]), (* (double*) &is[pc + 7])); pc += 10; break;
			case 423: /* reg[cplx] real real reg[real] */	(* (double*) &data[is[pc + 6]]) = smoothen((* (double2*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &is[pc + 4])); pc += 7; break;
			case 424: /* cplx reg[real] real reg[real] */	(* (double*) &data[is[pc + 8]]) = smoothen((* (double2*) &is[pc + 1]), (* (double*) &data[is[pc + 5]]), (* (double*) &is[pc + 6])); pc += 9; break;
			case 425: /* reg[cplx] reg[real] real reg[real] */	(* (double*) &data[is[pc + 5]]) = smoothen((* (double2*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &is[pc + 3])); pc += 6; break;
			case 426: /* cplx real reg[real] reg[real] */	(* (double*) &data[is[pc + 8]]) = smoothen((* (double2*) &is[pc + 1]), (* (double*) &is[pc + 5]), (* (double*) &data[is[pc + 7]])); pc += 9; break;
			case 427: /* reg[cplx] real reg[real] reg[real] */	(* (double*) &data[is[pc + 5]]) = smoothen((* (double2*) &data[is[pc + 1]]), (* (double*) &is[pc + 2]), (* (double*) &data[is[pc + 4]])); pc += 6; break;
			case 428: /* cplx reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 7]]) = smoothen((* (double2*) &is[pc + 1]), (* (double*) &data[is[pc + 5]]), (* (double*) &data[is[pc + 6]])); pc += 8; break;
			case 429: /* reg[cplx] reg[real] reg[real] reg[real] */	(* (double*) &data[is[pc + 4]]) = smoothen((* (double2*) &data[is[pc + 1]]), (* (double*) &data[is[pc + 2]]), (* (double*) &data[is[pc + 3]])); pc += 5; break;

			///// over[4]
			case 430: /* quat quat reg[quat] */	(* (double4*) &data[is[pc + 17]]) = over((* (double4*) &is[pc + 1]), (* (double4*) &is[pc + 9])); pc += 18; break;
			case 431: /* reg[quat] quat reg[quat] */	(* (double4*) &data[is[pc + 10]]) = over((* (double4*) &data[is[pc + 1]]), (* (double4*) &is[pc + 2])); pc += 11; break;
			case 432: /* quat reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 10]]) = over((* (double4*) &is[pc + 1]), (* (double4*) &data[is[pc + 9]])); pc += 11; break;
			case 433: /* reg[quat] reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = over((* (double4*) &data[is[pc + 1]]), (* (double4*) &data[is[pc + 2]])); pc += 4; break;

			///// lab2rgb[2]
			case 434: /* quat reg[quat] */	(* (double4*) &data[is[pc + 9]]) = lab2rgb((* (double4*) &is[pc + 1])); pc += 10; break;
			case 435: /* reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = lab2rgb((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// rgb2lab[2]
			case 436: /* quat reg[quat] */	(* (double4*) &data[is[pc + 9]]) = rgb2lab((* (double4*) &is[pc + 1])); pc += 10; break;
			case 437: /* reg[quat] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = rgb2lab((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// int2rgb[2]
			case 438: /* integer reg[quat] */	(* (double4*) &data[is[pc + 2]]) = int2rgb((* (int*) &is[pc + 1])); pc += 3; break;
			case 439: /* reg[integer] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = int2rgb((* (int*) &data[is[pc + 1]])); pc += 3; break;

			///// rgb2int[2]
			case 440: /* quat reg[integer] */	(* (int*) &data[is[pc + 9]]) = rgb2int((* (double4*) &is[pc + 1])); pc += 10; break;
			case 441: /* reg[quat] reg[integer] */	(* (int*) &data[is[pc + 2]]) = rgb2int((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// int2lab[2]
			case 442: /* integer reg[quat] */	(* (double4*) &data[is[pc + 2]]) = int2lab((* (int*) &is[pc + 1])); pc += 3; break;
			case 443: /* reg[integer] reg[quat] */	(* (double4*) &data[is[pc + 2]]) = int2lab((* (int*) &data[is[pc + 1]])); pc += 3; break;

			///// lab2int[2]
			case 444: /* quat reg[integer] */	(* (int*) &data[is[pc + 9]]) = lab2int((* (double4*) &is[pc + 1])); pc += 10; break;
			case 445: /* reg[quat] reg[integer] */	(* (int*) &data[is[pc + 2]]) = lab2int((* (double4*) &data[is[pc + 1]])); pc += 3; break;

			///// __jump[1]
			case 446: /* label */	pc = is[pc + 1]; break;

			///// __jumprel[2]
			case 447: /* integer */	pc = is[pc + 2 + (* (int*) &is[pc + 1])]; break;
			case 448: /* reg[integer] */	pc = is[pc + 2 + (* (int*) &data[is[pc + 1]])]; break;

			///// __ld_palette[2]
			case 449: /* label cplx reg[quat] */	(* (double4*) &data[is[pc + 6]]) = palette_lab(is[pc + 1], (* (double2*) &is[pc + 2])); pc += 7; break;
			case 450: /* label reg[cplx] reg[quat] */	(* (double4*) &data[is[pc + 3]]) = palette_lab(is[pc + 1], (* (double2*) &data[is[pc + 2]])); pc += 4; break;

		}
	} ///// End of generated code


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

void root(uchar4 *v_out, uint32_t x) {
	if(x >= tilelength) return;

    int index = index_aligned(x + offset);

	int w0 = (width + stepsize - 1) / stepsize; // pixels to be drawn per row

	int x0 = (index % w0) * stepsize;
	int y0 = (index / w0) * stepsize;

	uchar4 color = calc(x0, y0);

	*v_out = color; // write into tile

	// and set pixel
	rsSetElementAt_uchar4(gOut, color, x0, y0);
} // function
