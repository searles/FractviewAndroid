#pragma version(1)
#pragma rs java_package_name(at.searles.fractview)

rs_allocation gOut;
rs_script gScript;

int stepsize;
int width;
int height;

// fill gaps
void root(const void *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {
    uint32_t x0 = x - x % stepsize; // left up
    uint32_t y0 = y - y % stepsize;

    uint32_t x1, y1;
    float dx, dy;

    float4 c00, c01, c10, c11;

    if(x != x0 || y != y0) {
        x1 = x0 + stepsize;
        y1 = y0 + stepsize;

		// if we are outside of the image, reuse x0/y0.
        if(x1 >= width) x1 = x0;
        if(y1 >= height) y1 = y0;

        c00 = rsUnpackColor8888(rsGetElementAt_uchar4(gOut, x0, y0));
        c01 = rsUnpackColor8888(rsGetElementAt_uchar4(gOut, x0, y1));
        c10 = rsUnpackColor8888(rsGetElementAt_uchar4(gOut, x1, y0));
        c11 = rsUnpackColor8888(rsGetElementAt_uchar4(gOut, x1, y1));

        dx = ((float) (x - x0)) / (float) stepsize;
        dy = ((float) (y - y0)) / (float) stepsize;

        // fixme use hermite spline interpolation?

        *v_out = rsPackColorTo8888(
            c00 * (1.f - dx) * (1.f - dy) +
            c01 * (1.f - dx) * dy +
            c10 * dx * (1.f - dy) +
            c11 * dx * dy
        );
    }
}