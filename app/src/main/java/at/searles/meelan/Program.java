package at.searles.meelan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Program {

	List<Instr> instructions = new LinkedList<>();
	int pc = 0; // program counter

	public String toString() {
		StringBuilder sb = new StringBuilder();

		int address = 0;
		sb.append(String.format("%04d\t", address));
		for(Instr i : instructions) {
			sb.append(i.toString());
			sb.append("\n");
			address += i.size();
			sb.append(String.format("%04d\t", address));
		}

		return sb.toString();
	}

	/**
	 * convert arguments and add conversions and instruction to program.
	 * @param op
	 * @param args
	 * @param currentScope
	 * @throws CompileException
	 */
	public void add(Op op, Signature signature, List<Value> args, DataScope currentScope) throws CompileException {
		List<Value> convertedArgs  = new ArrayList<>();

		List<Integer> postAssignments = new LinkedList<>();

		int i = 0;
		for(Value arg : args) {
			Sort s = signature.get(i);

			if(s.p == Sort.Permission.c || s.p == Sort.Permission.r) {
				// this is the only case where arg might not be a Reg.
				convertedArgs.add(arg.addConversion(s, currentScope, this));
			} else { /* no need to distinguish w and rw. That was already taken care of before. */
				if(arg.type == null) {
					// if it is an uninitialized register, we can already set the type.
					((Value.Reg) arg).initType(s.type);
					convertedArgs.add(arg);
				} else if(arg.type == s.type) {
					convertedArgs.add(arg);
				} else {
					// create a register and add it to post-assignments
					Value.Reg r = new Value.Reg(currentScope, "$$");
					r.initType(s.type);
					convertedArgs.add(r);
					postAssignments.add(i);
				}
			}

			i++;
		}

		addRaw(op, signature, convertedArgs);

		for(Integer index : postAssignments) {
			Sort s = signature.get(index);
			Value.Reg src = (Value.Reg) convertedArgs.get(index);
			Value.Reg dst = (Value.Reg) args.get(index);
			Op.mov.addToProgram(Arrays.asList((Value) src, dst), currentScope, this);
		}
	}


	/** add instruction with arguments. No checks on the signature are performed
	 * apart from determininig the signatureIndex.
	 * @param op
	 * @param signature
	 * @param values
	 */
	public void addRaw(Op op, Signature signature, List<Value> values) {
		// Step 1: find the offset.
		int signatureOffset = 0;

		for(Signature s : op.signatures) {
			if(s == signature) {
				// it is converted.
				Instr i = new Instr(op, signatureOffset + signature.valuesToIndex(values), values);
				instructions.add(i);
				pc += i.size();
				return;
			}
			signatureOffset += s.combinationCount();
		}

		throw new IllegalArgumentException();
	}

	/*public void add(Op op, Value...values) throws CompileException {
		// adds jump instruction with signature.index
		// increments program counter index.

		// fixme: Find appropriate signature, and initialize type of registers without type.
		boolean canConvert = false;

		int signatureOffset = 0;

		for(Signature signature : op.signatures()) {
			canConvert = true;

			// take the first one that matches
			Iterator<Sort> i1 = signature.iterator();


			for(int i = 0; canConvert && i < values.length; ++i) {
				if(!i1.hasNext()) canConvert = false;
				else if(!i1.next().canConvert(values[i])) {
					canConvert = false;
				}
			}

			// arity does not match?
			if(i1.hasNext()) canConvert = false;

			if(canConvert) {
				i1 = signature.iterator();

				// s is thread-safe.

				for(int i = 0; i < values.length; ++i) {
					values[i] = i1.next().convert(values[i]);
				}

				// it is converted.
				Instr i = new Instr(op, signatureOffset + signature.valuesToIndex(values), values);
				instructions.add(i);
				pc += i.size();

				break; // we successfully converted and found our signature.
			} else {
				signatureOffset += signature.combinationCount();
			}
		}

		if(!canConvert) {
			// no matching signature found
			throw new CompileException("no matching signature found for " + op + " with " + Arrays.toString(values));
		}
	}*/

	public void addLabel(Value.Label label) {
		label.setPosition(pc);
	}

	public int[] intCode() {
		int size = 0;

		LinkedList<int[]> instrInts = new LinkedList<>();

		for(Instr instr : instructions) {
			int[] intCode = instr.intCode();
			size += intCode.length;
			instrInts.add(intCode);
		}

		int[] ret = new int[size];
		int i = 0;

		for(int[] ints : instrInts) {
			for(int b : ints) {
				ret[i++] = b;
			}
		}

		return ret;
	}

	static class Instr {
		Op op;
		int signatureOffset;
		List<Value> values;

		Instr(Op op, int signatureOffset, List<Value> values) {
			this.op = op;
			this.signatureOffset = signatureOffset;
			this.values = values;
		}

		/** How many bytes does this instruction take */
		int size() {
			int size = 1; // 1 for instruction

			for(Value v : values) {
				size += v.vmCodeSize();
			}

			return size;
		}

		int[] intCode() {
			int[] code = new int[size()];

			code[0] = op.intOffset() + signatureOffset;

			int idx = 1;

			for(Value value : values) {
				for (int b : value.generateVMCode()) {
					code[idx++] = b;
				}
			}

			return code;
		}

		public String toString() {
			return op.name() + values.toString();
		}
	}
}
