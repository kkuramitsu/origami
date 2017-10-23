
package blue.origami.main;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import blue.origami.nez.ast.Symbol;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.parser.pasm.Inop;
import blue.origami.nez.parser.pasm.PAsmCode;
import blue.origami.nez.parser.pasm.PAsmInst;
import blue.origami.util.OCommonWriter;
import blue.origami.util.OOption;

public class Opasm extends Main {
	@Override
	public void exec(OOption options) throws Throwable {
		Parser parser = this.getParser(options);
		PAsmCode pcode = (PAsmCode) parser.compile();
		List<PAsmInst> code = pcode.codeList();
		this.updateDataSection(code);
		this.out = new OCommonWriter();

		this.writeDataSection(code);
		this.writeCodeSection(code);
	}

	private OCommonWriter out;
	private HashMap<PAsmInst, Integer> lineMap = new HashMap<>();
	private ArrayList<String> dataList = new ArrayList<>();
	private HashMap<Object, String> nameMap = new HashMap<>();
	private HashMap<String, Object> dataMap = new HashMap<>();

	void updateDataSection(List<PAsmInst> code) {
		int line = 0;
		for (PAsmInst inst : code) {
			this.lineMap.put(inst, line++);
			for (Field f : inst.getClass().getDeclaredFields()) {
				if (Modifier.isPublic(f.getModifiers())) {
					this.updateData(inst, f);
				}
			}
		}
	}

	private void updateData(PAsmInst inst, Field f) {
		String name = f.getName();
		try {
			Object value = f.get(inst);
			if (value instanceof PAsmInst[]) {
				this.updateData("J", value);
			} else if (value instanceof byte[]) {
				this.updateData("B", value);
			} else if (value instanceof int[]) {
				this.updateData("I", value);
			} else if (value instanceof Symbol) {
				this.updateData("S", value);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			System.err.println("ERR " + name + " " + e);
		}
	}

	private void updateData(String name, Object value) {
		if (value != null) {
			String n = this.nameMap.get(value);
			if (n == null) {
				n = name + this.nameMap.size();
				this.nameMap.put(value, n);
				this.dataMap.put(n, value);
				this.dataList.add(n);
				// System.out.println(name + ", " + value.getClass());
			}
		}
	}

	int linenum(PAsmInst inst) {
		return this.lineMap.getOrDefault(inst, 0);
	}

	void writeDataSection(List<PAsmInst> code) {
		this.out.println("SIZE\t%d %d", this.dataList.size(), code.size());
		for (String name : this.dataList) {
			Object value = this.dataMap.get(name);
			this.out.printf("%s\t", name);
			if (value instanceof PAsmInst[]) {
				this.writeJumpTable((PAsmInst[]) value);
			} else if (value instanceof byte[]) {
				this.writeByte((byte[]) value);
			} else if (value instanceof int[]) {
				this.writeBits((int[]) value);
			} else if (value instanceof Symbol) {
				this.writeSymbol((Symbol) value);
			}
			this.out.println();
		}
	}

	private void writeByte(byte[] value) {
		this.out.print("" + value.length + " ");
		for (byte b : value) {
			this.out.print(String.format("%02x", b & 0xff));
		}
	}

	private void writeJumpTable(PAsmInst[] value) {
		this.out.print("" + value.length);
		for (PAsmInst inst : value) {
			this.out.print(" L" + this.linenum(inst));
		}
	}

	private void writeBits(int[] value) {
		this.out.print("" + value.length);
		for (int num : value) {
			this.out.print(String.format(" %d", num));
		}
	}

	private void writeSymbol(Symbol s) {
		this.out.print("" + s.getSymbol().length() + " " + s);
	}

	void writeCodeSection(List<PAsmInst> code) {
		for (PAsmInst inst : code) {
			if (inst instanceof Inop) {
				this.out.println("%s:", ((Inop) inst).name);
				continue;
			}
			int line = this.linenum(inst);
			this.out.printf("%d\t%s", line, inst.getName());
			Field[] fields = inst.getClass().getDeclaredFields();
			if (fields.length > 0) {
				this.out.printf("%d", fields.length);
			}
			for (Field f : fields) {
				if (Modifier.isPublic(f.getModifiers())) {
					this.writeData(inst, f);
				}
			}
			this.out.println();
			int jump = this.linenum(inst.next);
			if (jump != 0 && line + 1 != jump) {
				this.out.println("\tIjump L%d", jump);
			}
		}
	}

	private void writeData(PAsmInst inst, Field f) {
		// String name = f.getName();
		try {
			Object value = f.get(inst);
			if (value == null) {
				this.out.print(" NUL");
			} else if (value instanceof Number) {
				this.out.print(" " + value);
			} else if (value instanceof String) {
				this.out.print(" ;;" + value);
			} else if (value instanceof PAsmInst) {
				this.out.print(" L" + this.linenum((PAsmInst) value));
			} else {
				String var = this.nameMap.get(value);
				if (var != null) {
					this.out.print(" " + var);
				} else {
					this.out.print(" *" + value + "(" + value.getClass().getSimpleName() + ")");
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
		}
	}

}
