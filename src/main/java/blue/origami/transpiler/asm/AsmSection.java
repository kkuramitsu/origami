package blue.origami.transpiler.asm;

import java.util.HashMap;
import java.util.Iterator;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import blue.origami.konoha5.DSymbol;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.FuncTy;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.TApplyCode;
import blue.origami.transpiler.code.TBoolCode;
import blue.origami.transpiler.code.TCastCode;
import blue.origami.transpiler.code.TCastCode.TBoxCode;
import blue.origami.transpiler.code.TCastCode.TUnboxCode;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDataCode;
import blue.origami.transpiler.code.TDoubleCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TFuncCode;
import blue.origami.transpiler.code.TFuncRefCode;
import blue.origami.transpiler.code.TIfCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TLetCode;
import blue.origami.transpiler.code.TMultiCode;
import blue.origami.transpiler.code.TNameCode;
import blue.origami.transpiler.code.TReturnCode;
import blue.origami.transpiler.code.TStringCode;
import blue.origami.transpiler.code.TemplateCode;
import blue.origami.util.ODebug;

public class AsmSection implements TCodeSection, Opcodes {
	private final static String APIs = "blue/origami/transpiler/asm/APIs";

	String cname;
	GeneratorAdapter mBuilder; // method writer

	AsmSection(String cname, GeneratorAdapter mw) {
		this.cname = cname;
		this.mBuilder = mw;

	}

	@Override
	public void push(String t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void push(TCode t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushBool(TEnv env, TBoolCode code) {
		this.mBuilder.push((boolean) code.getValue());
	}

	@Override
	public void pushInt(TEnv env, TIntCode code) {
		this.mBuilder.push((int) code.getValue());
	}

	@Override
	public void pushDouble(TEnv env, TDoubleCode code) {
		this.mBuilder.push((double) code.getValue());
	}

	@Override
	public void pushString(TEnv env, TStringCode code) {
		this.mBuilder.push((String) code.getValue());
	}

	@Override
	public void pushCast(TEnv env, TCastCode code) {
		Ty f = code.getInner().getType();
		Ty t = code.getType();
		Class<?> fc = Asm.toClass(f);
		Class<?> tc = Asm.toClass(t);
		if (code instanceof TBoxCode) {
			code.getInner().emitCode(env, this);
			this.box(tc);
			return;
		}
		if (code instanceof TUnboxCode) {
			code.getInner().emitCode(env, this);
			this.unbox(tc);
			return;
		}
		if (tc == void.class) {
			code.getInner().emitCode(env, this);
			if (fc == double.class || fc == long.class) {
				this.mBuilder.pop2();
			} else {
				this.mBuilder.pop();
			}
			return;
		}
		if (f.equals(t)) {
			code.getInner().emitCode(env, this);
			return;
		}
		ODebug.trace("calling cast %s => %s %s", f, t, code.getInner());
		this.pushCall(env, code);
	}

	// I,+,
	@Override
	public void pushCall(TEnv env, TCode code) {
		final Template tp = code.getTemplate(env);
		final String[] def = tp.getDefined().split("\\|", -1);
		if (def[0].equals("X")) {
			this.pushCall(env, code, def[1]);
			return;
		}
		if (def[0].equals("N")) {
			this.mBuilder.visitTypeInsn(NEW, def[1]);
			this.mBuilder.visitInsn(DUP);
		}
		for (TCode sub : code) {
			sub.emitCode(env, this);
		}
		String desc = null;
		switch (def[0]) {
		case "-": // NOP
			return;
		case "F":
		case "GETSTATIC":
			desc = Asm.toTypeDesc(tp.getReturnType());
			// ODebug.trace("GETSTATIC %s,%s,%s", def[1], def[2], desc);
			this.mBuilder.visitFieldInsn(GETSTATIC, def[1], def[2], desc);
			return;
		case "S":
		case "INVOKESTATIC":
			// this.mw.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt",
			// "(D)D", false);
			desc = Asm.toTypeDesc(tp.getReturnType(), tp.getParamTypes());
			// ODebug.trace("INVOKESTATIC %s,%s,%s", def[1], def[2], desc);
			this.mBuilder.visitMethodInsn(INVOKESTATIC, def[1], def[2], desc, false);
			return;
		case "V":
		case "INVOKEVIRTUAL":
			desc = Asm.toTypeDesc(tp.getReturnType(), TArrays.ltrim(tp.getParamTypes()));
			this.mBuilder.visitMethodInsn(INVOKEVIRTUAL, def[1], def[2], desc, false);
			return;
		case "I":
		case "INVOKEINTERFACE":
			desc = Asm.toTypeDesc(tp.getReturnType(), TArrays.ltrim(tp.getParamTypes()));
			this.mBuilder.visitMethodInsn(INVOKEINTERFACE, def[1], def[2], desc, false);
			return;
		case "N":
		case "INVOKESPECIAL":
			desc = Asm.toTypeDesc(tp.getReturnType(), tp.getParamTypes());
			this.mBuilder.visitMethodInsn(INVOKESPECIAL, def[1], def[2], desc, false);
			return;
		case "O":
			int op = op(def[1]);
			if (op != -1) {
				this.mBuilder.visitInsn(op);
				return;
			}
		default:
			ODebug.trace("undefined call %s %s", tp.getDefined(), code.getClass().getName());
		}
	}

	private void pushCall(TEnv env, TCode code, String ext) {
		Iterator<TCode> iter = code.iterator();
		TCode first = iter.next();
		TCode second = iter.next();
		switch (ext) {
		case "band": {
			Label elseLabel = this.mBuilder.newLabel();
			Label mergeLabel = this.mBuilder.newLabel();
			this.pushIfFalse(env, first, elseLabel);
			this.pushIfFalse(env, second, elseLabel);
			this.mBuilder.push(true);
			this.mBuilder.goTo(mergeLabel);
			this.mBuilder.mark(elseLabel);
			this.mBuilder.push(false);
			this.mBuilder.mark(mergeLabel);
			break;
		}
		case "bor": {
			Label thenLabel = this.mBuilder.newLabel();
			Label mergeLabel = this.mBuilder.newLabel();
			this.pushIfTrue(env, first, thenLabel);
			this.pushIfTrue(env, second, thenLabel);
			this.mBuilder.push(false);
			this.mBuilder.goTo(mergeLabel);
			this.mBuilder.mark(thenLabel);
			this.mBuilder.push(true);
			this.mBuilder.mark(mergeLabel);
			break;
		}
		default: {
			ODebug.trace("undefined %s", code.getTemplate(env).getDefined());
		}
		}
	}

	static HashMap<String, Integer> opMap = new HashMap<>();

	static int op(String op) {
		return opMap.getOrDefault(op.toLowerCase(), -1);
	}

	static {
		opMap.put("", NOP); // visitInsn
		opMap.put("", ACONST_NULL); // -
		opMap.put("", ICONST_M1); // -
		opMap.put("", ICONST_0); // -
		opMap.put("", ICONST_1); // -
		opMap.put("", ICONST_2); // -
		opMap.put("", ICONST_3); // -
		opMap.put("", ICONST_4); // -
		opMap.put("", ICONST_5); // -
		opMap.put("", LCONST_0); // -
		opMap.put("", LCONST_1); // -
		opMap.put("", FCONST_0); // -
		opMap.put("", FCONST_1); // -
		opMap.put("", FCONST_2); // -
		opMap.put("", DCONST_0); // -
		opMap.put("", DCONST_1); // -
		opMap.put("", BIPUSH); // visitIntInsn
		opMap.put("", SIPUSH); // -
		opMap.put("", LDC); // visitLdcInsn
		// opMap.put("", LDC_W = 19; // -
		// opMap.put("", LDC2_W = 20; // -
		opMap.put("", ILOAD); // visitVarInsn
		opMap.put("", LLOAD); // -
		opMap.put("", FLOAD); // -
		opMap.put("", DLOAD); // -
		opMap.put("", ALOAD); // -
		// opMap.put("", ILOAD_0 = 26; // -
		// opMap.put("", ILOAD_1 = 27; // -
		// opMap.put("", ILOAD_2 = 28; // -
		// opMap.put("", ILOAD_3 = 29; // -
		// opMap.put("", LLOAD_0 = 30; // -
		// opMap.put("", LLOAD_1 = 31; // -
		// opMap.put("", LLOAD_2 = 32; // -
		// opMap.put("", LLOAD_3 = 33; // -
		// opMap.put("", FLOAD_0 = 34; // -
		// opMap.put("", FLOAD_1 = 35; // -
		// opMap.put("", FLOAD_2 = 36; // -
		// opMap.put("", FLOAD_3 = 37; // -
		// opMap.put("", DLOAD_0 = 38; // -
		// opMap.put("", DLOAD_1 = 39; // -
		// opMap.put("", DLOAD_2 = 40; // -
		// opMap.put("", DLOAD_3 = 41; // -
		// opMap.put("", ALOAD_0 = 42; // -
		// opMap.put("", ALOAD_1 = 43; // -
		// opMap.put("", ALOAD_2 = 44; // -
		// opMap.put("", ALOAD_3 = 45; // -
		opMap.put("", IALOAD); // visitInsn
		opMap.put("", LALOAD); // -
		opMap.put("", FALOAD); // -
		opMap.put("", DALOAD); // -
		opMap.put("", AALOAD); // -
		opMap.put("", BALOAD); // -
		opMap.put("", CALOAD); // -
		opMap.put("", SALOAD); // -
		opMap.put("", ISTORE); // visitVarInsn
		opMap.put("", LSTORE); // -
		opMap.put("", FSTORE); // -
		opMap.put("", DSTORE); // -
		opMap.put("", ASTORE); // -
		// opMap.put("", ISTORE_0 = 59; // -
		// opMap.put("", ISTORE_1 = 60; // -
		// opMap.put("", ISTORE_2 = 61; // -
		// opMap.put("", ISTORE_3 = 62; // -
		// opMap.put("", LSTORE_0 = 63; // -
		// opMap.put("", LSTORE_1 = 64; // -
		// opMap.put("", LSTORE_2 = 65; // -
		// opMap.put("", LSTORE_3 = 66; // -
		// opMap.put("", FSTORE_0 = 67; // -
		// opMap.put("", FSTORE_1 = 68; // -
		// opMap.put("", FSTORE_2 = 69; // -
		// opMap.put("", FSTORE_3 = 70; // -
		// opMap.put("", DSTORE_0 = 71; // -
		// opMap.put("", DSTORE_1 = 72; // -
		// opMap.put("", DSTORE_2 = 73; // -
		// opMap.put("", DSTORE_3 = 74; // -
		// opMap.put("", ASTORE_0 = 75; // -
		// opMap.put("", ASTORE_1 = 76; // -
		// opMap.put("", ASTORE_2 = 77; // -
		// opMap.put("", ASTORE_3 = 78; // -
		opMap.put("", IASTORE); // visitInsn
		opMap.put("", LASTORE); // -
		opMap.put("", FASTORE); // -
		opMap.put("", DASTORE); // -
		opMap.put("", AASTORE); // -
		opMap.put("", BASTORE); // -
		opMap.put("", CASTORE); // -
		opMap.put("", SASTORE); // -
		opMap.put("", POP); // -
		opMap.put("", POP2); // -
		opMap.put("", DUP); // -
		opMap.put("", DUP_X1); // -
		opMap.put("", DUP_X2); // -
		opMap.put("", DUP2); // -
		opMap.put("", DUP2_X1); // -
		opMap.put("", DUP2_X2); // -
		opMap.put("", SWAP); // -
		opMap.put("iadd", IADD); // -
		opMap.put("ladd", LADD); // -
		opMap.put("fadd", FADD); // -
		opMap.put("dadd", DADD); // -
		opMap.put("isub", ISUB); // -
		opMap.put("lsub", LSUB); // -
		opMap.put("fsub", FSUB); // -
		opMap.put("dsub", DSUB); // -
		opMap.put("imul", IMUL); // -
		opMap.put("lmul", LMUL); // -
		opMap.put("fmul", FMUL); // -
		opMap.put("dmul", DMUL); // -
		opMap.put("idiv", IDIV); // -
		opMap.put("ldiv", LDIV); // -
		opMap.put("fdiv", FDIV); // -
		opMap.put("ddiv", DDIV); // -
		opMap.put("irem", IREM); // -
		opMap.put("lrem", LREM); // -
		opMap.put("frem", FREM); // -
		opMap.put("drem", DREM); // -
		opMap.put("ineg", INEG); // -
		opMap.put("lneg", LNEG); // -
		opMap.put("fneg", FNEG); // -
		opMap.put("dneg", DNEG); // -
		opMap.put("ishl", ISHL); // -
		opMap.put("lshl", LSHL); // -
		opMap.put("ishr", ISHR); // -
		opMap.put("lshr", LSHR); // -
		opMap.put("lushr", IUSHR); // -
		opMap.put("lushr", LUSHR); // -
		opMap.put("iand", IAND); // -
		opMap.put("land", LAND); // -
		opMap.put("ior", IOR); // -
		opMap.put("lor", LOR); // -
		opMap.put("ixor", IXOR); // -
		opMap.put("lxor", LXOR); // -
		opMap.put("iinc", IINC); // visitIincInsn
		opMap.put("i2l", I2L); // visitInsn
		opMap.put("i2f", I2F); // -
		opMap.put("i2d", I2D); // -
		opMap.put("l2i", L2I); // -
		opMap.put("l2f", L2F); // -
		opMap.put("l2d", L2D); // -
		opMap.put("f2i", F2I); // -
		opMap.put("f2l", F2L); // -
		opMap.put("f2d", F2D); // -
		opMap.put("d2i", D2I); // -
		opMap.put("d2l", D2L); // -
		opMap.put("d2f", D2F); // -
		opMap.put("i2b", I2B); // -
		opMap.put("i2c", I2C); // -
		opMap.put("i2s", I2S); // -
		opMap.put("lcmp", LCMP); // -
		opMap.put("fcmpl", FCMPL); // -
		opMap.put("fcmpg", FCMPG); // -
		opMap.put("dcmpl", DCMPL); // -
		opMap.put("dcmpg", DCMPG); // -
		opMap.put("", IFEQ); // visitJumpInsn
		opMap.put("", IFNE); // -
		opMap.put("", IFLT); // -
		opMap.put("", IFGE); // -
		opMap.put("", IFGT); // -
		opMap.put("", IFLE); // -
		opMap.put("", IF_ICMPEQ); // -
		opMap.put("", IF_ICMPNE); // -
		opMap.put("", IF_ICMPLT); // -
		opMap.put("", IF_ICMPGE); // -
		opMap.put("", IF_ICMPGT); // -
		opMap.put("", IF_ICMPLE); // -
		opMap.put("", IF_ACMPEQ); // -
		opMap.put("", IF_ACMPNE); // -
		opMap.put("", GOTO); // -
		opMap.put("", JSR); // -
		opMap.put("", RET); // visitVarInsn
		opMap.put("", TABLESWITCH); // visiTableSwitchInsn
		opMap.put("", LOOKUPSWITCH); // visitLookupSwitch
		opMap.put("", IRETURN); // visitInsn
		opMap.put("", LRETURN); // -
		opMap.put("", FRETURN); // -
		opMap.put("", DRETURN); // -
		opMap.put("", ARETURN); // -
		opMap.put("", RETURN); // -
		opMap.put("", GETSTATIC); // visitFieldInsn
		opMap.put("", PUTSTATIC); // -
		opMap.put("", GETFIELD); // -
		opMap.put("", PUTFIELD); // -
		opMap.put("", INVOKEVIRTUAL); // visitMethodInsn
		opMap.put("", INVOKESPECIAL); // -
		opMap.put("", INVOKESTATIC); // -
		opMap.put("", INVOKEINTERFACE); // -
		opMap.put("", INVOKEDYNAMIC); // visitInvokeDynamicInsn
		opMap.put("", NEW); // visitTypeInsn
		opMap.put("", NEWARRAY); // visitIntInsn
		opMap.put("", ANEWARRAY); // visitTypeInsn
		opMap.put("", ARRAYLENGTH); // visitInsn
		opMap.put("", ATHROW); // -
		opMap.put("", CHECKCAST); // visitTypeInsn
		opMap.put("", INSTANCEOF); // -
		opMap.put("", MONITORENTER); // visitInsn
		opMap.put("", MONITOREXIT); // -
		// opMap.put("", WIDE = 196; // NOT VISITED
		opMap.put("", MULTIANEWARRAY); // visitMultiANewArrayInsn
		opMap.put("", IFNULL); // visitJumpInsn
		opMap.put("", IFNONNULL); // -
		// opMap.put("", GOTO_W = 200; // -
		// opMap.put("", JSR_W = 201; // -
	}

	static class VarEntry {
		final VarEntry parent;
		final String name;
		final int varIndex;
		final Ty varType;

		VarEntry(VarEntry parent, String name, int varIndex, Ty varType) {
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

	void addVariable(String name, Ty varType) {
		int index = this.varStack == null ? 0 : this.varStack.nextIndex();
		this.varStack = new VarEntry(this.varStack, name, index, varType);
	}

	@Override
	public void pushLet(TEnv env, TLetCode code) {
		this.addVariable(code.getName(), code.getDeclType());
		Type typeDesc = Asm.ti(this.varStack.varType);
		code.getInner().emitCode(env, this);
		this.mBuilder.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), this.varStack.varIndex);
	}

	@Override
	public void pushName(TEnv env, TNameCode code) {
		// ODebug.trace("name=%s", code.getName());
		if (code.getRefLevel() > 0) {
			this.mBuilder.loadThis();
			this.mBuilder.getField(Type.getType("L" + this.cname + ";"), code.getName(), Asm.ti(code.getType()));
		} else {
			VarEntry var = this.varStack.find(code.getName());
			Type typeDesc = (Asm.ti(var.varType));
			this.mBuilder.visitVarInsn(typeDesc.getOpcode(ILOAD), var.varIndex);
		}
	}

	@Override
	public void pushIf(TEnv env, TIfCode code) {
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.pushIfFalse(env, code.condCode(), elseLabel);

		// then
		code.thenCode().emitCode(env, this);
		this.mBuilder.goTo(mergeLabel);

		// else
		this.mBuilder.mark(elseLabel);
		code.elseCode().emitCode(env, this);

		// merge
		this.mBuilder.mark(mergeLabel);
		// mBuilder.visitFrame(F_FULL, 5, new Object[] { "[Ljava/lang/String;",
		// Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER },
		// 1, new Object[] { "java/io/PrintStream" });
	}

	private void pushIfTrue(TEnv env, TCode cond, Label jump) {
		cond.emitCode(env, this);
		this.mBuilder.visitJumpInsn(IFNE, jump);
	}

	private void pushIfFalse(TEnv env, TCode cond, Label jump) {
		cond.emitCode(env, this);
		this.mBuilder.visitJumpInsn(IFEQ, jump);
	}

	@Override
	public void pushReturn(TEnv env, TReturnCode code) {

	}

	@Override
	public void pushMulti(TEnv env, TMultiCode code) {
		for (TCode sub : code) {
			sub.emitCode(env, this);
		}
	}

	void pushArray(TEnv env, Type ty, boolean boxing, TCode... subs) {
		this.mBuilder.push(subs.length);
		this.mBuilder.newArray(ty);
		int c = 0;
		for (TCode sub : subs) {
			this.mBuilder.dup();
			this.mBuilder.push(c++);
			sub.emitCode(env, this);
			if (boxing) {
				this.box(Asm.toClass(sub.getType()));
			}
			this.mBuilder.arrayStore(ty);
		}
	}

	@Override
	public void pushTemplate(TEnv env, TemplateCode code) {
		Type ty = Type.getType(String.class);
		this.pushArray(env, ty, false, code.args());
		this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "join", "([Ljava/lang/String;)Ljava/lang/String;", false);

	}

	private void box(Class<?> c) {
		if (c.isPrimitive()) {
			Class<?> bc = Asm.boxType(c);
			String desc = String.format("(%s)%s", Type.getDescriptor(c), Type.getDescriptor(bc));
			this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "box", desc, false);
		}
	}

	private void unbox(Class<?> c) {
		if (c.isPrimitive()) {
			String desc = String.format("(Ljava/lang/Object;)%s", Type.getDescriptor(c));
			this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "unbox" + Type.getDescriptor(c), desc, false);
		} else {
			this.mBuilder.checkCast(Type.getType(c));
		}
	}

	@Override
	public void pushData(TEnv env, TDataCode code) {
		if (code.isRange()) {
			DataTy dt = (DataTy) code.getType();
			for (TCode sub : code) {
				sub.emitCode(env, this);
			}
			String desc = String.format("(II)%s", Type.getDescriptor(Asm.toClass(dt)));
			this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "range", desc, false);
			return;
		}
		if (code.isArray()) {
			DataTy dt = (DataTy) code.getType();
			if (Asm.toClass(dt) == blue.origami.konoha5.ObjArray.class) {
				Type ty = Type.getType(Object.class);
				this.pushArray(env, ty, true, code.args());
				String desc = String.format("([%s)%s", ty.getDescriptor(), Type.getDescriptor(Asm.toClass(dt)));
				this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "array", desc, false);
			} else {
				Ty t = dt.getInnerType();
				this.pushArray(env, Asm.ti(t), false, code.args());
				String desc = String.format("([%s)%s", Type.getDescriptor(Asm.toClass(t)),
						Type.getDescriptor(Asm.toClass(dt)));
				this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "array", desc, false);
			}
			// } else if (code.isDict()) {
			//
		} else {
			this.pushArray(env, Type.getType(int.class), false, this.symbols(code.getNames()));
			this.pushArray(env, Type.getType(Object.class), true, code.args());
			this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "data",
					"([I[Ljava/lang/Object;)Lblue/origami/konoha5/Data;", false);
		}
	}

	TCode[] symbols(String... values) {
		TCode[] v = new TCode[values.length];
		int c = 0;
		for (String s : values) {
			v[c] = new TIntCode(DSymbol.id(s));
			c++;
		}
		return v;
	}

	@Override
	public void pushError(TEnv env, TErrorCode code) {
		env.reportLog(code.getLog());
	}

	@Override
	public void pushFuncRef(TEnv env, TFuncRefCode code) {
		Template tp = code.getRef();
		// ODebug.trace("funcref %s %s", code.getType(), tp);
		Class<?> c = AsmGenerator.loadFuncRefClass(env, tp);
		String cname = Type.getInternalName(c);
		this.mBuilder.visitTypeInsn(NEW, cname);
		this.mBuilder.dup();
		this.mBuilder.visitMethodInsn(INVOKESPECIAL, cname, "<init>", "()V", false);
	}

	@Override
	public void pushFuncExpr(TEnv env, TFuncCode code) {
		String[] fieldNames = code.getFieldNames();
		Ty[] fieldTypes = code.getFieldTypes();
		Class<?> c = AsmGenerator.loadFuncExprClass(env, fieldNames, fieldTypes, code.getStartIndex(),
				code.getParamNames(), code.getParamTypes(), code.getReturnType(), code.getInner());
		TCode[] inits = code.getFieldInitCode();
		String cname = Type.getInternalName(c);
		this.mBuilder.visitTypeInsn(NEW, cname);
		this.mBuilder.dup();
		this.mBuilder.visitMethodInsn(INVOKESPECIAL, cname, "<init>", "()V", false);
		for (int i = 0; i < fieldNames.length; i++) {
			this.mBuilder.dup();
			inits[i].emitCode(env, this);
			this.mBuilder.visitFieldInsn(PUTFIELD, cname/* internal */, fieldNames[i], Asm.toTypeDesc(fieldTypes[i]));
		}
		ODebug.trace("FuncCode.asType %s", code.getType());

	}

	@Override
	public void pushApply(TEnv env, TApplyCode code) {
		for (TCode sub : code) {
			sub.emitCode(env, this);
		}
		FuncTy funcType = (FuncTy) code.args()[0].getType();
		// String desc = Asm.toTypeDesc(funcType.getReturnType(),
		// TArrays.join(funcType, funcType.getParamTypes()));
		String desc = Asm.toTypeDesc(funcType.getReturnType(), funcType.getParamTypes());
		String cname = Type.getInternalName(Asm.toClass(funcType));
		this.mBuilder.visitMethodInsn(INVOKEINTERFACE, cname, AsmGenerator.nameApply(funcType.getReturnType()), desc,
				true);
		return;
	}

}
