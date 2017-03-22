/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package origami.asm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Stack;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import origami.ODebug;
import origami.code.OCode;
import origami.lang.OMethodHandle;
import origami.type.OType;

public class OGeneratorAdapter extends GeneratorAdapter {
	private final VarScopes varScopes;
	private int currentLineNum = -1;

	public OGeneratorAdapter(MethodVisitor mv, int acc, String name, String methodDesc) {
		super(Opcodes.ASM5, mv, acc, name, methodDesc);
		int startIndex = 0;
		if ((acc & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
			startIndex = 1;
		}
		this.varScopes = new VarScopes(startIndex);
	}

	private int maxLocalSlots = 0;
	private int maxStackSlots = 0;
	private int usedStackSlots = 0;

	public void visitMaxs() {
		// ODebug.trace("local=%d, stack=%d", maxLocalSlots,
		// this.maxStackSlots);
		// this.visitMaxs(maxLocalSlots + 1, maxStackSlots + 1);
	}

	@Override
	public void mark(Label label) {
		super.mark(label);
	}

	void useStackSlots(OType t) {
		if (t.is(long.class) || t.is(double.class)) {
			this.useStackSlots(2);
		} else if (!t.is(void.class)) {
			this.useStackSlots(1);
		}
	}

	void useStackSlots(int size) {
		this.usedStackSlots += size;
		if (this.maxStackSlots < this.usedStackSlots) {
			this.maxStackSlots = this.usedStackSlots;
		}

	}

	// void unuseStackSlots(OType t) {
	// if (t.is(long.class) || t.is(double.class)) {
	// this.unuseStackSlots(2);
	// } else if (!t.is(void.class)) {
	// this.unuseStackSlots(1);
	// }
	// }
	//
	// void unuseStackSlots(int size) {
	// this.usedStackSlots -= size;
	// }

	int usedStackSlots() {
		return this.usedStackSlots;
	}

	void popStackSlots(int stack) {
		this.usedStackSlots = stack;
	}

	public final void dup(OType t) {
		if (t.is(long.class) || t.is(double.class)) {
			this.dup2();
		} else if (!t.is(void.class)) {
			this.dup();
		}
		this.useStackSlots(t);
	}

	public void dupX1(OType t) {
		if (t.is(long.class) || t.is(double.class)) {
			this.dup2X1();
		} else if (!t.is(void.class)) {
			this.dupX1();
		}
		this.useStackSlots(t);
	}

	public void dupX2(OType t) {
		if (t.is(long.class) || t.is(double.class)) {
			this.dup2X2();
		} else if (!t.is(void.class)) {
			this.dupX2();
		}
		this.useStackSlots(t);
	}

	public void pop(OType t) {
		if (t.is(long.class) || t.is(double.class)) {
			this.pop2();
			this.usedStackSlots -= 2;
		} else if (t.is(void.class)) {
			this.pop();
			this.usedStackSlots -= 1;
		}
	}

	/* binary, unary */

	private boolean checkBinary(OMethodHandle mh, Class<?> ret, Class<?> pat) {
		if (mh.getReturnType().is(ret)) {
			OType[] p = mh.getThisParamTypes();
			return p[0].is(pat) && p[1].is(pat);
		}
		return false;
	}

	private boolean checkUnary(OMethodHandle mh, Class<?> ret, Class<?> pat) {
		if (mh.getReturnType().is(ret)) {
			OType[] p = mh.getThisParamTypes();
			return p[0].is(pat);
		}
		return false;
	}

	private Class<?> checkMath(OMethodHandle mh) {
		if (this.checkBinary(mh, int.class, int.class)) {
			return int.class;
		}
		if (this.checkBinary(mh, double.class, double.class)) {
			return double.class;
		}
		if (this.checkBinary(mh, long.class, long.class)) {
			return long.class;
		}
		if (this.checkBinary(mh, float.class, float.class)) {
			return float.class;
		}
		return null;
	}

	private final Class<?> checkBitwise(OMethodHandle mh) {
		if (this.checkBinary(mh, int.class, int.class)) {
			return int.class;
		}
		if (this.checkBinary(mh, long.class, long.class)) {
			return long.class;
		}
		return null;
	}

	private Class<?> checkMath1(OMethodHandle mh) {
		if (this.checkUnary(mh, int.class, int.class)) {
			return int.class;
		}
		if (this.checkUnary(mh, double.class, double.class)) {
			return double.class;
		}
		if (this.checkUnary(mh, long.class, long.class)) {
			return long.class;
		}
		if (this.checkUnary(mh, float.class, float.class)) {
			return float.class;
		}
		return null;
	}

	boolean tryInvokeBinary(OMethodHandle mh) {
		int op = -1;
		Class<?> type = null;
		// ODebug.trace("binary %s", mh.getLocalName());
		if (mh.getThisParamSize() == 2) {
			switch (mh.getLocalName()) {
			case "+":
				op = ADD;
				type = this.checkMath(mh);
				break;
			case "-":
				op = SUB;
				type = this.checkMath(mh);
				break;
			case "*":
				op = MUL;
				type = this.checkMath(mh);
				break;
			case "/":
				op = DIV;
				type = this.checkMath(mh);
				break;
			case "%":
				op = REM;
				type = this.checkMath(mh);
				break;
			case "||":
				op = OR;
				type = this.checkBitwise(mh);
				break;
			case "&&":
				op = AND;
				type = this.checkBitwise(mh);
				break;
			case "^":
				op = XOR;
				type = this.checkBitwise(mh);
				break;
			case "<<":
				op = SHL;
				type = this.checkBitwise(mh);
				break;
			case ">>":
				op = SHR;
				type = this.checkBitwise(mh);
				break;
			case ">>>":
				op = USHR;
				type = this.checkBitwise(mh);
				break;
			default:
			}
			if (type != null) {
				this.math(op, Type.getType(type));
				return true;
			}
		}
		if (mh.getThisParamSize() == 1) {
			switch (mh.getLocalName()) {
			case "!":
				if (this.checkUnary(mh, boolean.class, boolean.class)) {
					this.not();
					return true;
				}
				return false;
			case "-":
				type = this.checkMath1(mh);
				op = NEG;
				break;
			default:
			}
			if (type != null) {
				this.math(op, Type.getType(type));
				return true;
			}
		}
		return false;
	}

	/**
	 * enter block scope
	 */
	public void enterScope() {
		this.varScopes.createNewScope();
	}

	/**
	 * exit block scope
	 */

	public void exitScope() {
		this.varScopes.removeCurrentScope();
	}

	void countLocalSlots(OType t) {
		if (t.is(long.class) || t.is(double.class)) {
			this.maxLocalSlots += 2;
		} else {
			this.maxLocalSlots += 1;
		}
	}

	/**
	 * reserve local variable entry of argument.
	 *
	 * @param t
	 * @return
	 */

	public VarEntry defineArgument(String name, OType t) {
		assert this.varScopes.size() == 1;
		this.countLocalSlots(t);
		return this.varScopes.peek().newVarEntry(name, t);
	}

	/**
	 * create new local variable
	 *
	 * @param varClass
	 * @return
	 */

	public VarEntry createNewVar(String varName, OType t) {
		this.countLocalSlots(t);
		return this.varScopes.peek().newVarEntry(varName, t);
	}

	/**
	 * create new local variable entry and store stack top value to created
	 * entry
	 *
	 * @param varName
	 * @param t
	 * @return
	 */

	public VarEntry createNewVarAndStore(String varName, OType t) {
		VarEntry entry = this.varScopes.peek().newVarEntry(varName, t);
		this.countLocalSlots(t);
		Type typeDesc = Type.getType(t.typeDesc(0));
		this.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), entry.getVarIndex());
		return entry;
	}

	/**
	 * store stack top value to local variable.
	 *
	 * @param entry
	 */
	public void storeToVar(VarEntry entry) {
		Type typeDesc = Type.getType(entry.getVarClass().typeDesc(0));
		this.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), entry.getVarIndex());
	}

	/**
	 * load value from local variable and put it at stack top.
	 *
	 * @param entry
	 */
	public void loadFromVar(VarEntry entry) {
		Type typeDesc = Type.getType(entry.getVarClass().typeDesc(0));
		this.visitVarInsn(typeDesc.getOpcode(Opcodes.ILOAD), entry.getVarIndex());
	}

	public VarEntry getVar(String varName) {
		return this.varScopes.getFirst().getLocalVar(varName);
	}

	// public void callIinc(VarEntry entry, int amount) {
	// this.iinc(entry.getVarIndex(), amount);
	// }

	/**
	 * generate line number.
	 *
	 * @param lineNum
	 */

	public void setLineNum(int lineNum) {
		if (lineNum > this.currentLineNum) {
			this.visitLineNumber(lineNum, this.mark());
		}
	}

	public void pushNull() {
		this.visitInsn(Opcodes.ACONST_NULL);
	}

	static class VarScopes extends ArrayDeque<LocalVarScope> {
		private static final long serialVersionUID = 8905256606042979610L;

		protected final int startVarIndex;

		VarScopes(int startIndex) {
			super();
			this.startVarIndex = startIndex;
		}

		public void createNewScope() {
			int startIndex = this.startVarIndex;
			if (!this.isEmpty()) {
				startIndex = this.peek().getEndIndex();
			}
			this.push(new LocalVarScope(startIndex, this.peek()));
		}

		public void removeCurrentScope() {
			this.pop();
		}

	}

	static class LocalVarScope {
		private final int localVarBaseIndex;
		private int currentLocalVarIndex;
		private LocalVarScope parent;
		private ArrayList<VarEntry> localVars;

		protected LocalVarScope(int localVarBaseIndex) {
			this.localVarBaseIndex = localVarBaseIndex;
			this.currentLocalVarIndex = this.localVarBaseIndex;
			this.localVars = new ArrayList<>();
		}

		protected LocalVarScope(int localVarBaseIndex, LocalVarScope parent) {
			this(localVarBaseIndex);
			this.parent = parent;
		}

		public VarEntry newVarEntry(String varName, OType t) {
			int valueSize = Type.getType(t.typeDesc(0)).getSize();
			return this.newVarEntry(valueSize, varName, t);
		}

		public VarEntry newRetAddressEntry() {
			return this.newVarEntry(1, "", null);
		}

		public VarEntry getLocalVar(String varName) {
			for (VarEntry entry : this.localVars) {
				if (entry.getVarName().equals(varName)) {
					return entry;
				}
			}
			LocalVarScope parent = this.parent;
			VarEntry entry = null;
			while (parent != null && entry == null) {
				entry = parent.getLocalVar(varName);
				parent = parent.getParent();
			}
			return entry;
		}

		private VarEntry newVarEntry(int valueSize, String varName, OType varClass) {
			assert valueSize > 0;
			int index = this.currentLocalVarIndex;
			VarEntry entry = new VarEntry(index, varName, varClass);
			this.currentLocalVarIndex += valueSize;
			this.localVars.add(entry);
			return entry;
		}

		public int getEndIndex() {
			return this.currentLocalVarIndex;
		}

		public LocalVarScope getParent() {
			return this.parent;
		}
	}

	static class VarEntry {
		private final int varIndex;
		private final OType varClass;
		private final String varName;

		VarEntry(int varIndex, String varName, OType varClass) {
			this.varIndex = varIndex;
			this.varClass = varClass;
			this.varName = varName;
		}

		public String getVarName() {
			return this.varName;
		}

		int getVarIndex() {
			return this.varIndex;
		}

		public OType getVarClass() {
			return this.varClass;
		}

	}

	public static class Pair<L, R> {
		private L left;
		private R right;

		public Pair(L left, R right) {
			this.left = left;
			this.right = right;
		}

		public void setLeft(L left) {
			this.left = left;
		}

		public L getLeft() {
			return this.left;
		}

		public void setRight(R right) {
			this.right = right;
		}

		public R getRight() {
			return this.right;
		}

		@Override
		public String toString() {
			return "(" + this.left + ", " + this.right + ")";
		}
	}

	public void checkCast(OType type, OType ret) {
		if (type.is(void.class)) {
			if (!ret.is(void.class)) {
				this.pop(ret);
			}
			return;
		}
		if (type.isUntyped()) {
			type = type.newType(Object.class);
		}
		if (type.getBaseType().isAssignableFrom(ret.getBaseType())) {
			return;
		}
		if (type.isPrimitive() && type.eq(ret.unboxType())) {
			this.unbox(type.asmType());
			return;
		}
		ODebug.trace("checkCast %s <= %s", type, ret);
		if (ret.isPrimitive()) {
			this.box(ret.asmType());
		}
		this.checkCast(type.asmType());
	}

	private Stack<OBlock> blockStack = new Stack<>();

	public <T extends OBlock> T pushBlock(T block) {
		this.blockStack.push(block);
		return block;
	}

	public OBlock popBlock() {
		return this.blockStack.pop();
	}

	public OBlock[] stackBlock() {
		OBlock[] b = new OBlock[this.blockStack.size()];
		int c = 0;
		for (int i = this.blockStack.size() - 1; i >= 0; i--) {
			b[c] = this.blockStack.get(i);
			c++;
		}
		return b;
	}

	public interface BlockMatcher<X> {
		boolean match(X x);
	}

	@SuppressWarnings("unchecked")
	public <X extends OBlock> X findBlock(Class<X> c, BlockMatcher<X> f) {
		for (int i = this.blockStack.size() - 1; i >= 0; i--) {
			OBlock b = this.blockStack.get(i);
			if (c.isInstance(b) && f.match((X) b)) {
				return (X) b;
			}
		}
		return null;
	}

	void weaveBefore(OCode code, OBlock until, OAsm gen) {
		for (int i = this.blockStack.size() - 1; i >= 0; i--) {
			OBlock b = this.blockStack.get(i);
			if (b == until) {
				return;
			}
			OCode weaveCode = b.getBeforeCode(code);
			if (weaveCode != null) {
				weaveCode.generate(gen);
			}
		}
	}

	void weaveAfter(OCode code, OBlock until, OAsm gen) {
		for (int i = this.blockStack.size() - 1; i >= 0; i--) {
			OBlock b = this.blockStack.get(i);
			if (b == until) {
				return;
			}
			OCode weaveCode = b.getBeforeCode(code);
			if (weaveCode != null) {
				weaveCode.generate(gen);
			}
		}
	}

}
