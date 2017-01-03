package at.searles.meelan;

import at.searles.parsing.parser.Buffer;

import java.util.HashMap;
import java.util.Map;

public class Meelan {

	public static Tree parse(String source, ExternalData data) throws CompileException {
		Tree ast = Syntax.program.parse(new Buffer.StringAdapter(source));

		// put extern-entries into data.
		ast.exportEntries(data);

		return ast;
	}

	public static int[] compile(Tree ast, ScopeTable table) throws CompileException {
		Tree inlinedAst = ast.inline(table, false);

		Program p = new Program();

		inlinedAst.linearizeStmt(new DataScope(), p);

		// System.out.println("Instruction Count = " + p.instructions.size());

		return p.intCode();
	}

	public static void main(String... args) {

		// def s = "Hello\\ world\"";

		// is there anything useful for f(a,b)(1)?
		// f(a,b) { lambda(x) {a(b(x))} }

		String s =
				"var i = 1; var j = 3 i;" +
				"\n";



		final Map<String, Integer> map = new HashMap<String, Integer>();

		ExternalData data = new ExternalData() {
			@Override
			public void add(String id, String type, Tree init) {
				map.put(id, ((Value.Const.Int) init).value);
			}
		};

		try {
			Tree t = parse(s, data);

			System.out.println("Tree\n" + t);

			ScopeTable table = new ScopeTable();

			for (Map.Entry<String, Integer> entry : map.entrySet()) {
				System.out.print(entry.getKey() + ": ");
				table.addDef(entry.getKey(), new Value.Real(4));
			}

			Tree u = t.inline(table, false);

			System.out.println("\nInlined Tree\n" + u);

			Program p = new Program();

			System.out.println("\nLinearized Code");

			u.linearizeStmt(new DataScope(), p);

			System.out.println(p);

			int[] code = p.intCode();

			System.out.println("    int len = " + code.length + ";");

			System.out.print("    int is[] = {");
			for (int i = 0; i < code.length; ++i) {
				if (i % 10 == 0) System.out.print("\n      ");

				System.out.print(String.format("%4d", code[i]));

				if (i < code.length - 1) System.out.print(", ");
			}

			System.out.println("\n    };");
		} catch (CompileException e) {
			e.printStackTrace();
			System.out.println(e.toString());
		}
	}
}
/*

extern a bla = 1; extern b bla = 2; var c = a + b;


var i real = 1; if i < 0 then i = -i;


var i = 1; print i;

// sum of 1+2..etc..9
var i = 1;
var sum = 0;

while {
	print i;
	sum = sum + i;
	next(i, 10);
}
print sum;

$$

// some functions:

func isprime(i) { // true if i is a prime
	var j = 2;
	while j < i and i % j > 0 do j = j + 1;
	j == i
}

func countprimes(i) { // counts primes up to i.
    var count = 0

    // this is a mean scope-test.
    var j = 1;
    while next(j, i) do {
        if isprime j then {
            print j;
            count = count + 1;
        }
    }

    count
}

print countprimes 100

$

// show prime decomposition of i

func showprimes(i) {
    var n = i
    var j = 2

    while n > 1 do
        if n % j == 0 then {
            print j;
            n = n / j
        } else {
            j = j + 1
        }
}

showprimes(100)

// fibonnacci numbers

func fib(i) {
    var a = 0;
    var b = 1;

    for j in 1 to i do {
        a = a + b;
        b = a - b;
    };

    a
}

print fib 151

// now, using call by value to add two fractions
func gcd(a, b) {
    // euclid'string algorithm.
    var i = a; var j = b;

    while i >< j do {
        var k = i % j;
        j = i;
        i = k;
    };
    i
}

func addfrac(a0, a1, b0, b1, c0, c1) {
    var g = gcd(a1, b1);
    c1 = a1 / g * b1;
    c0 = a0 * (b1 / g) + b0 * (a1 / g);
    // does not return anything.
}


var a int;
var b int;
addfrac(3, 8, 7, 6, a, b);
print a;
print b;

// testing call by name


var a = 1:2;
a.x = 3;
a.y = a.x + a.y;
print a;


// ====

var a int;
func f(x,y) { x = 2; y = 3;};
f(a,a);
print a;

// reading numbers

var sum = 0;
var a : i;

// I really like this one

while { a = readint(); a > 0 } do {
    sum = sum + a;
}

print sum;
$

// calculating pi

// calculating e

func fac(n) {
    var f real = 1;
    var i = 2;
    while { f = f * i; next(i, n) }
    f
}

var sum = 1.

var i = 1;
while {
    var b = fac i
    var a = 1 / b;
    sum = sum + a;
    print sum;
    next(i, 40);
}

print sum

$


// testing __select statement

def arr [15,76,34,50,11];

for i in 0 to 15 do {
    print arr.i
}

def arr


// find the nth prime

func isprime(i) { // true if i is a prime
	var j = 2;
	while j < i and i % j > 0 do j = j + 1;
	j == i
}

func findprime(i) {
	var n = 1;

    // this is a mean scope-test.
    var j = 0;
    for i in 0 to i do {
    	while { n = n + 1; not isprime(n) }; // this is a much nicer construct than do-while!
    }

    n
}

print findprime 100



var v = 1.:2.; print v;

// testing unions
union u:f4;
var a:f; var b:f; var c:f; var d:f;

u = 1.:2.:3.:4.;

print u;

a = 5.;
print u;

u = 9.:8.:7.:6.;

u = u / 2.;

print u;

// testing colors

var rgb = [1, 0.5, 0.25, 1];

print rgb;

var lab = rgb2lab rgb;

print lab;

var rgb2 = lab2rgb lab;

print rgb2;



 */