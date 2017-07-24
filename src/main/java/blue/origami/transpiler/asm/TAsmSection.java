package blue.origami.transpiler.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TIfCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TLetCode;
import blue.origami.transpiler.code.TMultiCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TReturnCode;
import blue.origami.util.OLog;

public class TAsmSection implements TCodeSection, Opcodes {
	GeneratorAdapter methodVisitor; // method writer

	@Override
	public void push(String t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void push(TCode t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushLog(OLog log) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushBool(TEnv env, TBoolCode code) {
		this.methodVisitor.push((boolean) code.getValue());
	}

	@Override
	public void pushInt(TEnv env, TIntCode code) {
		this.methodVisitor.push((int) code.getValue());
	}

	@Override
	public void pushDouble(TEnv env, TDoubleCode code) {
		this.methodVisitor.push((double) code.getValue());
	}

	@Override
	public void pushCast(TEnv env, TCastCode code) {
		// TODO Auto-generated method stub
		this.pushCall(env, code);
	}

	// I,+,
	@Override
	public void pushCall(TEnv env, TCode code) {
		Template tp = code.getTemplate(env);
		String[] def = tp.getDefined().split("|");
		if (def[0].equals("N")) {
			this.methodVisitor.visitTypeInsn(NEW, def[1]);
			this.methodVisitor.visitInsn(DUP);
		}
		for (TCode sub : code) {
			sub.emitCode(env, this);
		}
		switch (def[0]) {
		case "S":
		case "INVOKESTATIC":
			// this.mw.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt",
			// "(D)D", false);
			this.methodVisitor.visitMethodInsn(INVOKESTATIC, def[1], def[2], def[3], false);
			return;
		case "V":
		case "INVOKEVIRTUAL":
			this.methodVisitor.visitMethodInsn(INVOKEVIRTUAL, def[1], def[2], def[3], false);
			return;
		case "I":
		case "INVOKEINTERFACE":
			this.methodVisitor.visitMethodInsn(INVOKEINTERFACE, def[1], def[2], def[3], false);
			return;
		case "N":
		case "INVOKESPECIAL":
			this.methodVisitor.visitMethodInsn(INVOKESPECIAL, def[1], def[2], def[3], false);
			return;
		case "IADD":
			this.methodVisitor.visitInsn(IADD);
			return;
		}
	}

	static class VarEntry {
		final VarEntry parent;
		final String name;
		final int varIndex;
		final TType varType;

		VarEntry(VarEntry parent, String name, int varIndex, TType varType) {
			this.parent = parent;
			this.name = name;
			this.varIndex = varIndex;
			this.varType = varType;
		}

		int nextIndex() {
			return this.varIndex + 1;
		}

		VarEntry find(String name) {
			for (VarEntry e = this; e != null; e = e.parent) {
				if (name.equals(e.name)) {
					return e;
				}
			}
			return null;
		}

	}

	VarEntry varStack = null;

	void addVariable(String name, TType varType) {
		int index = this.varStack == null ? 0 : this.varStack.nextIndex();
		this.varStack = new VarEntry(this.varStack, name, index, varType);
	}

	@Override
	public void pushLet(TEnv env, TLetCode code) {
		this.addVariable(code.getName(), code.getDeclType());
		Type typeDesc = Type.getType(TAsm.toClass(this.varStack.varType));
		code.getInner().emitCode(env, this);
		this.methodVisitor.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), this.varStack.varIndex);
	}

	@Override
	public void pushName(TEnv env, TNameCode code) {
		VarEntry var = this.varStack.find(code.getName());
		Type typeDesc = Type.getType(TAsm.toClass(var.varType));
		this.methodVisitor.visitVarInsn(typeDesc.getOpcode(ILOAD), var.varIndex);
	}

	@Override
	public void pushIf(TEnv env, TIfCode code) {

	}

	@Override
	public void pushReturn(TEnv env, TReturnCode code) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushMulti(TEnv env, TMultiCode tMultiCode) {
		// TODO Auto-generated method stub

	}

}
