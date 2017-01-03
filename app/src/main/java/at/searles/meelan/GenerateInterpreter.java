package at.searles.meelan;

public class GenerateInterpreter {
	public static void main(String...args) {
		System.out.println("\tint pc = 0; ///// Start of generated code\n\twhile(pc < len) {\n\t\tswitch(is[pc]) {");

		for(Op op: Op.values()) {
			System.out.print(op.genereateInterpreter());
		}

		System.out.println("\t\t}\n\t} ///// End of generated code\n");
	}
}
