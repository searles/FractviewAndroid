package at.searles.meelan;

import java.util.*;

public class Signature implements Iterable<Sort> {

	List<Sort> s = new LinkedList<>();

	// idea is as follows:
	// real[r] means everything that can be read as a real, ie also int-registers. This one also includes Const-Values.
	// real[w] means everything that can be written as a real, ie also cplx-registers.
	// real[ws] means it must be a real-register
	// and labels are labels.

	/**
	 * This one returns the valuesToIndex of a specific combination of values.
	 * values may be larger than signature-size because there might be
	 * additional data (eg a jump table in relative jumps)
	 * @param values
	 * @return
	 */
	public int valuesToIndex(List<Value> values) {
		// get the valuesToIndex for this signature
		assert values.size() == s.size();

		Iterator<Sort> it = s.iterator();

		int index = 0;
		int multiplicator = 1;

		for (Value value : values) {
			if(!it.hasNext()) break;

			Sort t = it.next();

			index += t.caseIndex(value) * multiplicator;

			multiplicator *= t.countCases();
		}

		return index;
	}

	/**
	 * This one is the inverse of the previous: It generates the C-code for all cases.
	 * @param combinationIndex get types of the arguments
	 * @return
	 */
	public List<Value> valuesFromIndex(int combinationIndex) throws CompileException {
		// get the signature for this valuesToIndex
		LinkedList<Value> l = new LinkedList<>(); // we create generic arguments to get a sample how this instruction looks like.

		int multiplicator = combinationCount();

		// we use a reverse iterator
		ListIterator<Sort> it = s.listIterator(s.size());

		while(it.hasPrevious()) {
			Sort t = it.previous();
			multiplicator /= t.countCases();

			int valueIndex = combinationIndex / multiplicator; // integer division.
			l.addFirst(t.caseValue(valueIndex));

			combinationIndex -= valueIndex * multiplicator;
		}

		return l;
	}



	/**
	 * This one counts all possible combinations of parameters.
	 * @return
	 */
	public int combinationCount() {
		int count = 1;

		for(Sort t : s) {
			count *= t.countCases();
		}

		return count;
	}

	Signature label() {
		s.add(new Sort(Type.label, Sort.Permission.c));
		return this;
	}

	Signature r(Type t) {
		s.add(new Sort(t, Sort.Permission.r));
		return this;
	}

	Signature rw(Type t) {
		s.add(new Sort(t, Sort.Permission.rw));
		return this;
	}

	Signature w(Type t) {
		s.add(new Sort(t, Sort.Permission.w));
		return this;
	}


	@Override
	public Iterator<Sort> iterator() {
		return s.iterator();
	}


	public String toString() {
		return s.toString();
	}

	public Sort get(int i) {
		return s.get(i);
	}

	public int size() {
		return s.size();
	}

	public static Signature findSignature(Signature[] signatures, List<Value> as) {
		// find the first signature that matches
		// all values must have a valid type except for w-types. They are ignored.
		// rw-types must be registers. r-types may be consts.
		for (Signature s : signatures) {
			if (s.size() == as.size()) {
				boolean foundMatch = true;

				for (int j = 0; j < as.size() && foundMatch; ++j) {
					if (!s.get(j).matches(as.get(j))) foundMatch = false;
				}

				if (foundMatch) return s;
			}
		}

		// did not find anything.
		return null;
	}

	public static Signature findSignatureStrict(Signature[] signatures, List<Value> as) {
		// similar to
		for (Signature s : signatures) {
			if (s.size() == as.size()) {
				boolean foundMatch = true;

				for (int j = 0; j < as.size() && foundMatch; ++j) {
					if (!s.get(j).matchesStrict(as.get(j))) foundMatch = false;
				}

				if (foundMatch) return s;
			}
		}

		// did not find anything.
		return null;
	}

}
