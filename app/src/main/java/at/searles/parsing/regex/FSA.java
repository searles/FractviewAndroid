package at.searles.parsing.regex;

import org.jetbrains.annotations.NotNull;

import at.searles.parsing.parser.Buffer;
import at.searles.utils.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is the FSA-Automaton for the lexer. It is always kept in a deterministic
 * state which was actually fun to create. Accepting states return an acceptor.
 *
 */
class FSA {

	/*public static <A> FSA parse(String regex, Acceptor<A> acceptor) {
		RegexParser<A> parser = new RegexParser<A>(regex);

		FSA fsa = parser.regex();
		if(!parser.string.isEmpty()) throw new IllegalArgumentException("unexpected char");

		fsa.removeUnusedNodes();

		return fsa;
	}*/

	private final Counter counter; // we need one counter so that we obtain unique node ids.
	private final Table<Node, LexicalSet<Node>> dfaTable; // Table for generating DFA

	// every node has multiple outgoing charsets
	// that connects it with other nodes.
	// nodes are numbers
	private Node start;
	private LinkedList<Node> nodes = new LinkedList<>();
	private Acceptor<?> acceptor;

	/**
	 * @param counter Counter is used to create unique pointer Ids
	 * @param dfaTable dfaTable is acceptor table used for determinization (counter and dfaTable MUST be
	 * shared amongst all FSA that are merged with this one)
	 * @param acceptor acceptor is
	 * acceptor function that is applied when we reach an accepting state. It can be but not necessarily
	 * is shared amongst FSAs.
	 * @param set Set of characters that are accepted by this FSA. More complex FSAs are created by applying
	 *            the or/concat/rep/opt-functions. Yet, we must start with something.
	 */
	FSA(@NotNull Counter counter, @NotNull Table<Node, LexicalSet<Node>> dfaTable, @NotNull Acceptor<?> acceptor, @NotNull CharSet set) {
		this.counter = counter;
		this.dfaTable = dfaTable; // I share the dfaTable.

		this.acceptor = acceptor;

		start = freshNode(false); // not accepting state
		Node end = freshNode(true);

		for(Range r : set) {
			start.transitions.put(r, end);
		}
	}

	/**
	 * Temporary variable. Avoids that we have to create acceptor Range for every character.
	 */
	private Range cache = new Range(0, Integer.MAX_VALUE);

	/**
	 * Returns the last acceptor that is reached from buf. Apart from setting the mark
	 * in the buffer, nothing is done. It is the caller'string responsibility to call the
	 * acceptor'string apply-function
	 * and to call buf.clear() before proceeding to the next token.
	 * @param buf buffer from which characters are read. A mark is set in buffer at the corresponding position.
	 * @return the acceptor that should be invoked on buf.seq.
	 * @throws Buffer.ReadException exception thrown if the underlying reader omits an IOException.
	 */
	Acceptor<?> accept(Buffer buf) throws Buffer.ReadException {
		Node n = start;
		Acceptor<?> acceptor = null;

		buf.mark(); // mark beginning of buf.

		while(n != null) {
			if(n.acceptor != null) { // it is an accepting state
				buf.mark(); // mark position in buffer.
				acceptor = n.acceptor; // n.acceptor is not null.
			}

			cache.start = buf.next(); // set
			n = n.accept(cache);
		}

		if(acceptor == null) {
			// no match, set pos in buf back to mark.
			buf.setToTokenStart();
			return null; // fixme because of this the acceptor MUST NOT return null.
		} else {
			// match.
			return acceptor;
		}
	}

	private Node freshNode(boolean accepting) {
		Node n = new Node(this, accepting ? this.acceptor : null);
		n.transitions = new TreeMap<>();
		dfaTable.add(n, LexicalSet.single(n));
		return n;
	}


	private Node union(Node a, Node b) {
		LexicalSet<Node> set = new LexicalSet<>();
		// find all source nodes
		set.addAll(dfaTable.r(a));
		set.addAll(dfaTable.r(b));

		if(dfaTable.containsB(set)) {
			return dfaTable.l(set);
		} else {
			// create new node.
			// FIXME which acceptor to use?
			Acceptor<?> acceptor;

			if(!a.isAccepting()) {
				acceptor = b.acceptor;
			} else if(!b.isAccepting()) {
				acceptor = a.acceptor;
			} else {
				acceptor = this.acceptor;
				// fixme test this one.
				// System.out.println("multiple acceptors: " + a.acceptor + " || " + b.acceptor + " ==> " + this.acceptor);
			}

			// this way, acceptor new node uses the acceptor of the FSA. Hence
			// old acceptors are NOT used!
			Node n = new Node(this, acceptor);

			dfaTable.add(n, set);
			n.transitions = union(a.transitions, b.transitions);

			return n;
		}
	}

	void removeUnusedNodes() {
		// kinda garbage collection :)
		for(Node n : nodes) n.mark = false;

		start.markConnected();

		for(Iterator<Node> i = nodes.iterator(); i.hasNext();) {
			if(!i.next().mark) {
				// remove unmarked nodes.
				i.remove();
			}
		}
	}

	/**
	 * @return Iterator over all accepting nodes.
	 */
	private Iterable<Node> accepting() {
		return () -> new Filter<>(nodes.iterator(), Node::isAccepting);
	}

	/**
	 * Creates the union of these two nodes. If a and b have a non-empty
	 * intersection, multiple nodes are created.
	 * @param a First node
	 * @param b Second node
	 * @return union of these two nodes
	 */
	private TreeMap<Range, Node> union(
			final TreeMap<Range, Node> a,
			final TreeMap<Range, Node> b) {

		// put all transitions into acceptor sorted set
		PriorityQueue<Pair<Range, Node>> pairs = new PriorityQueue<>();

		for(Map.Entry<Range, Node> entry : a.entrySet())
			pairs.add(new Pair<>(entry.getKey(), entry.getValue()));

		for(Map.Entry<Range, Node> entry : b.entrySet())
			pairs.add(new Pair<>(entry.getKey(), entry.getValue()));

		TreeMap<Range, Node> union = new TreeMap<>();

		if(pairs.isEmpty()) return union; // empty

		// now fetch pair from pairs. They are in sorted order.
		// If there is an intersection, it will be put back into union.

		// if this was acceptor sorted stack, I would call it pop.
		// fixme maybe acceptor priorityqueue???
		Pair<Range, Node> entry = pairs.poll();
		Pair<Range, Node> next = null;

		while(true) {
			if(next == null) {
				if(pairs.isEmpty()) break; // fixme this plus next check???
				// we do not advance in every loop
				next = pairs.poll();
				if(next == null) break; // we are done.
				pairs.remove(next); // remove first.
			}

			if(entry.a.end < next.a.start) {
				// no overlap and no touching
				union.put(entry.a, entry.b);
				entry = next;
				next = null;
			} else if(entry.a.end == next.a.start) {
				if(entry.b.compareTo(next.b) == 0) {
					// modify range.
					entry = new Pair<>(new Range(entry.a.start, Math.max(entry.a.end, next.a.end)), entry.b);
					next = null;
				} else {
					// they touch but they are different.
					// hence same as before
					union.put(entry.a, entry.b);
					entry = next;
					next = null; // advance to next one.
				}
			} else if(entry.a.start < next.a.start /* && entry.acceptor.end > next.acceptor.start */) {
				// r0 starts before r1, hence split off r0.start-r1.start
				Range r = new Range(entry.a.start, next.a.start);
				union.put(r, entry.b);
				entry = new Pair<>(new Range(next.a.start, entry.a.end), entry.b);
				// keep next. Since next and entry now start at the same entry
				// the next condition will be used in the next loop run.
			} else /*if(r0.start == r1.start)*/ {
				// now we have acceptor union.
				// since we split up ranges, we don't know which end is bigger.
				int min = Math.min(entry.a.end, next.a.end);

				// split things off (if necessary)
				if(min < entry.a.end) {
					pairs.offer(new Pair<>(new Range(min, entry.a.end), entry.b));
				}

				// this one and the previous if are mutual exculsive because of min.
				if(min < next.a.end) {
					pairs.offer(new Pair<>(new Range(min, next.a.end), next.b));
				}

				Range r = new Range(entry.a.start, min);

				Node unionNode = union(entry.b, next.b);

				entry = new Pair<>(r, unionNode);

				// advance to next element (which can be ahead of the split off entries that we just added)
				next = null;
			}
		}

		// finally insert last entry.
		union.put(entry.a, entry.b);

		return union;
	}

	// The following are for parsing regexes.

	/**
	 * Regular expression this | that
	 * @param that the other regular expression
	 */
	void or(FSA that) {
		// the start-node of fsa1 becomes acceptor normal node here.
		// but we add all out-going edges to the start node of fsa.
		// yet, this leaves the problem of eg (acceptor*b | c*).
		// in this case, the start node accepts. So, I need acceptor new start node.
		this.nodes.addAll(that.nodes);
		this.start = union(start, that.start);
		removeUnusedNodes();
	}

	/**
	 * Concatenates two fsas
	 * @param that the next fsa
	 */
	void concat(FSA that) {
		// I must combine the nodes-list after.
		// yet, the dfaTable must be complete before
		// that'string why sharing the dfa-table is acceptor good idea.

		// all accepting states tokenPosition the outgoing edges of that'string start-state.
		List<Node> as = new LinkedList<>();

		for(Node n : this.accepting()) {
			as.add(n);
		}

		this.nodes.addAll(that.nodes);

		// since new nodes are added (which are unions of this-states and that-states, hence
		// not affected by the union) we store the accepted states before.

		for(Node n : as) {
			// this.accepting are only accepting if the next one is.
			if(!that.start.isAccepting()) n.setNonAccepting();
			n.transitions = union(n.transitions, that.start.transitions);
		}

		removeUnusedNodes();
	}

	/**
	 * Introduces a repetition of FSAs
	 * @param mayBeEmpty If the repetition may also be empty.
	 */
	void rep(boolean mayBeEmpty) {
		List<Node> as = new LinkedList<>();

		for(Node n : this.accepting()) {
			as.add(n);
		}

		// fixme similar thought as before for concat
		// fixme is it possible that I create acceptor node
		// fixme for which there should be acceptor loop?

		// similar to concat
		for(Node n : as) {
			// add start-edges also to all accepting states.
			n.transitions = union(n.transitions, start.transitions);
		}

		if(mayBeEmpty) opt();

		removeUnusedNodes();
	}

	/**
	 * Makes this FSA optional
	 */
	void opt() {
		// this one's easy.
		this.start.setAccepting(this);
	}

	/**
	 * Node of this FSA
	 */
	static class Node implements Comparable<Node> {
		final int id;

		TreeMap<Range, Node> transitions;

		Acceptor<?> acceptor; // if null, state is not accepting

		boolean mark = false; // for mark-and-sweep

		Node(FSA parent, Acceptor<?> acceptor) {
			this.id = parent.counter.next();
			this.acceptor = acceptor;
			parent.nodes.add(this);
		}

		/**
		 * returns the next node for character ch.
		 * @param r checks whether this node has an outgoing transition for r.start.
		 * @return null, if there is no node.
		 */
		Node accept(Range r) {
			// how can I find this one without creating acceptor Range?
			// fixme also here an object is created, right? Don't like that :(
			Map.Entry<Range, Node> entry = transitions.floorEntry(r);
			if(entry != null && entry.getKey().end > r.start) return entry.getValue();
			return null;
		}

		boolean isAccepting() {
			return acceptor != null;
		}

		Node setNonAccepting() {
			this.acceptor = null;
			return this;
		}

		Node setAccepting(FSA parent) {
			this.acceptor = parent.acceptor;
			return this;
		}

		void markConnected() {
			if(!mark) {
				mark = true;

				for(Node n : transitions.values()) {
					n.markConnected();
				}
			}
		}

		public String toString() {
			return "q" + id;
		}

		/**
		 * This method is useful for debugging.
		 * @return A more expressive output of this node.
		 *
		public String fullString() {
			String s = "";
			for(Map.Entry<Range, FSA.Node> entry : transitions.entrySet()) {
				s += "\"" + entry.getKey() + "\" -> " + entry.getValue() + "(" + entry.getValue().acceptor + ")\n";
			}
			return s;
		}*/

		@Override
		public int compareTo(@NotNull Node that) {
			return Integer.compare(this.id, that.id);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("digraph finite_state_machine {\n\trankdir=LR;\n\tsize=\"8,5\"\n\t");

		sb.append("node [shape = doublecircle];");

		for(Node n : accepting()) {
			sb.append(" ").append(n.toString());
		}

		sb.append(";\n\t");
		sb.append("node [shape = point]; start;\n\t");

		sb.append("node [shape = circle];\n\t");

		sb.append("start -> ").append(start).append(";\n");

		for(Node n : nodes) {
			for(Map.Entry<Range, Node> entry : n.transitions.entrySet()) {
				Node m = entry.getValue();

				sb.append("\t").append(n).append(" -> ").append(m);
				sb.append(" [label=\"").append(entry.getKey()).append("\"];\n");
			}
		}

		sb.append("}");

		return sb.toString();
	}





	// we actually parse this one.
		// grammar:
		// regex: seq ("|" seq)*
		// seq: (rep)+
		// rep: token ('*'|'+'|'{' NUM (',' NUM?)? '}')?
		// token: '[' charset ']'
		//      | '(' regex ')'
		//      | '.'
		//      | '^'
		//      | '$'
		//      | EXTCHAR
		// charset: ^? ( CHAR '-' CHAR | CHAR )*
		// CHAR: CHAR | '\' ([acceptor-zA-Z0-9]+|.)
		//
		//

		// characters that don't match themselves: .[\*^$
}
