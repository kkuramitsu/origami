package blue.origami.asm;

import java.util.HashMap;
import java.util.Iterator;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.konoha5.Data$;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.AssignCode;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.CallCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CastCode.BoxCastCode;
import blue.origami.transpiler.code.CastCode.UnboxCastCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.ExistFieldCode;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.GroupCode;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.NameCode;
import blue.origami.transpiler.code.NoneCode;
import blue.origami.transpiler.code.ReturnCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.transpiler.code.TemplateCode;
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.code.TupleIndexCode;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.ListTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;

public class AsmSection implements CodeSection, Opcodes {
	private final static String APIs = Type.getInternalName(APIs.class);

	AsmType ts;
	String cname;
	GeneratorAdapter mBuilder; // method writer

	AsmSection(AsmType ts, String cname, GeneratorAdapter mw) {
		this.ts = ts;
		this.cname = cname;
		this.mBuilder = mw;
	}

	@Override
	public Env env() {
		return this.ts.env();
	}

	@Override
	public void pushNone(NoneCode code) {
		this.mBuilder.visitInsn(ACONST_NULL);
	}

	@Override
	public void pushBool(BoolCode code) {
		this.mBuilder.push((boolean) code.getValue());
	}

	@Override
	public void pushInt(IntCode code) {
		this.mBuilder.push((int) code.getValue());
	}

	@Override
	public void pushDouble(DoubleCode code) {
		this.mBuilder.push((double) code.getValue());
	}

	@Override
	public void pushString(StringCode code) {
		this.mBuilder.push((String) code.getValue());
	}

	@Override
	public void pushCast(CastCode code) {
		Ty f = code.getInner().getType();
		Ty t = code.getType();
		Class<?> fc = this.ts.toClass(f);
		Class<?> tc = this.ts.toClass(t);
		if (code instanceof BoxCastCode) {
			code.getInner().emitCode(this);
			this.box(tc);
			return;
		}
		if (code instanceof UnboxCastCode) {
			code.getInner().emitCode(this);
			this.unbox(tc);
			return;
		}
		if (tc == void.class) {
			code.getInner().emitCode(this);
			if (fc == double.class || fc == long.class) {
				this.mBuilder.pop2();
			} else {
				this.mBuilder.pop();
			}
			return;
		}
		// ODebug.trace("calling cast %s => %s %s %s", f, t, code.getInner(),
		// code.getTemplate());
		if (t.acceptTy(true, f, VarLogger.Nop)) {
			code.getInner().emitCode(this);
			return;
		}
		// ODebug.trace("calling cast %s => %s %s %s", f, t, code.getInner(),
		// code.getTemplate());
		this.pushCall(code);
	}

	// I,+,
	@Override
	public void pushCall(CallCode code) {
		final CodeMap tp = code.getMapped();
		final String[] def = tp.getDefined().split("\\|", -1);
		if (def[0].equals("X")) {
			this.pushCall(code, def[1]);
			return;
		}
		if (def[0].equals("N")) {
			this.mBuilder.visitTypeInsn(NEW, def[1]);
			this.mBuilder.visitInsn(DUP);
		}
		for (Code sub : code) {
			sub.emitCode(this);
		}
		String desc = null;
		switch (def[0]) {
		case "-":
		case "%s": // NOP
			return;
		case "F":
		case "GETSTATIC":
			desc = this.ts.desc(tp.getReturnType());
			// ODebug.trace("GETSTATIC %s,%s,%s", def[1], def[2], desc);
			this.mBuilder.visitFieldInsn(GETSTATIC, def[1], def[2], desc);
			return;
		case "S":
		case "INVOKESTATIC":
			// this.mw.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt",
			// "(D)D", false);
			desc = this.ts.desc(tp.getReturnType(), tp.getParamTypes());
			// ODebug.trace("INVOKESTATIC %s,%s,%s", def[1], def[2], desc);
			// ODebug.trace("template %s", tp);
			this.mBuilder.visitMethodInsn(INVOKESTATIC, def[1], def[2], desc, false);
			return;
		case "V":
		case "INVOKEVIRTUAL":
			desc = this.ts.desc(tp.getReturnType(), OArrays.ltrim(tp.getParamTypes()));
			ODebug.trace("::::: desc=%s, %s", desc, tp);
			this.mBuilder.visitMethodInsn(INVOKEVIRTUAL, def[1], def[2], desc, false);
			return;
		case "I":
		case "INVOKEINTERFACE":
			desc = this.ts.desc(tp.getReturnType(), OArrays.ltrim(tp.getParamTypes()));
			this.mBuilder.visitMethodInsn(INVOKEINTERFACE, def[1], def[2], desc, false);
			return;
		case "N":
		case "INVOKESPECIAL":
			desc = this.ts.desc(tp.getReturnType(), tp.getParamTypes());
			this.mBuilder.visitMethodInsn(INVOKESPECIAL, def[1], def[2], desc, false);
			return;
		case "O":
			int op = op(def[1]);
			if (op != -1) {
				this.mBuilder.visitInsn(op);
				return;
			}
		case "C":
			this.mBuilder.checkCast(this.ts.ti(code.getType()));
			return;
		default:
			ODebug.trace("undefined call '%s' %s", tp.getDefined(), code.getClass().getName());
			assert (tp.getDefined().length() > 0) : tp;
		}
	}

	private void pushCall(CallCode code, String ext) {
		Iterator<Code> iter = code.iterator();
		Code first = iter.next();
		Code second = iter.next();
		switch (ext) {
		case "band": {
			Label elseLabel = this.mBuilder.newLabel();
			Label mergeLabel = this.mBuilder.newLabel();
			this.pushIfFalse(first, elseLabel);
			this.pushIfFalse(second, elseLabel);
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
			this.pushIfTrue(first, thenLabel);
			this.pushIfTrue(second, thenLabel);
			this.mBuilder.push(false);
			this.mBuilder.goTo(mergeLabel);
			this.mBuilder.mark(thenLabel);
			this.mBuilder.push(true);
			this.mBuilder.mark(mergeLabel);
			break;
		}
		case "orElse": {
			Label elseLabel = this.mBuilder.newLabel();
			Label mergeLabel = this.mBuilder.newLabel();
			first.emitCode(this);
			this.mBuilder.dup();
			this.emitAsType(new ExprCode("some?", new EmptyAsmCode(first.getType())), Ty.tBool);
			this.mBuilder.visitJumpInsn(IFNE, elseLabel);
			this.emitAsType(new CastCode(second.getType(), new EmptyAsmCode(first.getType())), second.getType());
			this.mBuilder.goTo(mergeLabel);
			this.mBuilder.mark(elseLabel);
			this.mBuilder.pop();
			second.emitCode(this);
			this.mBuilder.mark(mergeLabel);
			break;
		}
		default: {
			ODebug.trace("undefined %s", code.getMapped().getDefined());
		}
		}
	}

	private void emitAsType(Code tempCode, Ty ret) {
		tempCode.asType(this.env(), ret).emitCode(this);
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
			return this.varIndex + (AsmType.isDouble(this.varType) ? 2 : 1);
		}

		VarEntry find(String name) {
			for (VarEntry e = this; e != null; e = e.parent) {
				if (name.equals(e.name)) {
					return e;
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return String.format("var[%s %s :: %s]", this.name, this.varIndex, this.varType);
		}

	}

	VarEntry varStack = null;

	VarEntry addVariable(String name, Ty varType) {
		int index = this.varStack == null ? 0 : this.varStack.nextIndex();
		this.varStack = new VarEntry(this.varStack, name, index, varType);
		return this.varStack;
	}

	@Override
	public void pushLet(LetCode code) {
		VarEntry var = this.addVariable(code.getName(), code.getDeclType());
		Type typeDesc = this.ts.ti(this.varStack.varType);
		// ODebug.trace("store %s %s %s", code.getName(), code.getDeclType(),
		// var);
		code.getInner().emitCode(this);
		this.mBuilder.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), var.varIndex);
	}

	@Override
	public void pushName(NameCode code) {
		// ODebug.trace("name=%s", code.getName());
		if (code.getRefLevel() > 0) {
			this.mBuilder.loadThis();
			this.mBuilder.getField(Type.getType("L" + this.cname + ";"), code.getName(), this.ts.ti(code.getType()));
		} else {
			VarEntry var = this.varStack.find(code.getName());
			// ODebug.trace("load %s %s", code.getName(), var);
			Type typeDesc = (this.ts.ti(var.varType));
			this.mBuilder.visitVarInsn(typeDesc.getOpcode(ILOAD), var.varIndex);
		}
	}

	@Override
	public void pushIf(IfCode code) {
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.pushIfFalse(code.condCode(), elseLabel);

		// then
		code.thenCode().emitCode(this);
		this.mBuilder.goTo(mergeLabel);

		// else
		this.mBuilder.mark(elseLabel);
		code.elseCode().emitCode(this);

		// merge
		this.mBuilder.mark(mergeLabel);
		// mBuilder.visitFrame(F_FULL, 5, new Object[] { "[Ljava/lang/String;",
		// Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER },
		// 1, new Object[] { "java/io/PrintStream" });
	}

	private void pushIfTrue(Code cond, Label jump) {
		cond.emitCode(this);
		this.mBuilder.visitJumpInsn(IFNE, jump);
	}

	private void pushIfFalse(Code cond, Label jump) {
		cond.emitCode(this);
		this.mBuilder.visitJumpInsn(IFEQ, jump);
	}

	@Override
	public void pushReturn(ReturnCode code) {

	}

	@Override
	public void pushMulti(MultiCode code) {
		for (Code sub : code) {
			sub.emitCode(this);
		}
	}

	void pushArray(Type ty, boolean boxing, Code... subs) {
		this.mBuilder.push(subs.length);
		this.mBuilder.newArray(ty);
		int c = 0;
		for (Code sub : subs) {
			this.mBuilder.dup();
			this.mBuilder.push(c++);
			sub.emitCode(this);
			if (boxing) {
				this.box(this.ts.toClass(sub.getType()));
			}
			this.mBuilder.arrayStore(ty);
		}
	}

	@Override
	public void pushTemplate(TemplateCode code) {
		Type ty = Type.getType(String.class);
		this.pushArray(ty, false, code.args());
		this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "join", "([Ljava/lang/String;)Ljava/lang/String;", false);

	}

	private void box(Class<?> c) {
		if (c.isPrimitive()) {
			Class<?> bc = AsmType.boxType(c);
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
	public void pushTuple(TupleCode code) {
		Class<?> c = code.getType().mapType(this.ts);
		String cname = Type.getInternalName(c);
		this.mBuilder.visitTypeInsn(NEW, cname);
		this.mBuilder.dup();
		this.mBuilder.visitMethodInsn(INVOKESPECIAL, cname, "<init>", "()V", false);
		int cnt = 0;
		for (Code sub : code) {
			this.mBuilder.dup();
			sub.emitCode(this);
			this.mBuilder.visitFieldInsn(PUTFIELD, cname/* internal */, this.ts.tupleAt(cnt),
					this.ts.desc(sub.getType()));
			cnt++;
		}
	}

	@Override
	public void pushTupleIndex(TupleIndexCode code) {
		Class<?> c = code.getInner().getType().mapType(this.ts);
		String cname = Type.getInternalName(c);
		code.getInner().emitCode(this);
		this.mBuilder.visitFieldInsn(GETFIELD, cname/* internal */, this.ts.tupleAt(code.getIndex()),
				this.ts.desc(code.getType()));
	}

	@Override
	public void pushData(DataCode code) {
		if (code.isRange()) {
			ListTy dt = (ListTy) code.getType();
			for (Code sub : code) {
				sub.emitCode(this);
			}
			String desc = String.format("(II)%s", Type.getDescriptor(this.ts.toClass(dt)));
			this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "range", desc, false);
			return;
		}
		if (code.isList()) {
			ListTy dt = (ListTy) code.getType();
			Class<?> c = this.ts.toClass(dt);
			this.mBuilder.push(code.isMutable());
			if (c == blue.origami.konoha5.List$.class) {
				Type ty = Type.getType(Object.class);
				this.pushArray(ty, true, code.args());
				String desc = String.format("(Z[%s)%s", ty.getDescriptor(), Type.getDescriptor(this.ts.toClass(dt)));
				this.mBuilder.visitMethodInsn(INVOKESTATIC, Type.getInternalName(c), "newArray", desc, false);
			} else {
				Ty t = dt.getInnerTy();
				this.pushArray(this.ts.ti(t), false, code.args());
				String desc = String.format("(Z[%s)%s", Type.getDescriptor(this.ts.toClass(t)),
						Type.getDescriptor(this.ts.toClass(dt)));
				this.mBuilder.visitMethodInsn(INVOKESTATIC, Type.getInternalName(c), "newArray", desc, false);
			}
			return;
		}
		if (code.isDict()) {
			// } else if (code.isDict()) {
			//
			return;
		}
		Class<?> c = this.ts.loadDataClass((DataTy) code.getType());
		String cname = Type.getInternalName(c);
		this.mBuilder.visitTypeInsn(NEW, cname);
		this.mBuilder.dup();
		this.mBuilder.visitMethodInsn(INVOKESPECIAL, cname, "<init>", "()V", false);
		String[] names = code.getNames();
		Code[] args = code.args();
		for (int i = 0; i < names.length; i++) {
			this.mBuilder.dup();
			args[i].emitCode(this);
			this.mBuilder.visitFieldInsn(PUTFIELD, cname/* internal */, names[i], this.ts.desc(args[i].getType()));
		}
	}

	// Code[] symbols(String... values) {
	// Code[] v = new Code[values.length];
	// int c = 0;
	// for (String s : values) {
	// v[c] = new IntCode(DSymbol.id(s));
	// c++;
	// }
	// return v;
	// }

	@Override
	public void pushError(ErrorCode code) {
		this.env().reportLog(code.getLog());
	}

	@Override
	public void pushFuncRef(FuncRefCode code) {
		CodeMap tp = code.getRef();
		// ODebug.trace("funcref %s %s", code.getType(), tp);
		Class<?> c = this.ts.loadFuncRefClass(this.env(), tp);
		String cname = Type.getInternalName(c);
		this.mBuilder.visitTypeInsn(NEW, cname);
		this.mBuilder.dup();
		this.mBuilder.visitMethodInsn(INVOKESPECIAL, cname, "<init>", "()V", false);
	}

	@Override
	public void pushFuncExpr(FuncCode code) {
		String[] fieldNames = code.getFieldNames();
		Ty[] fieldTypes = code.getFieldTypes();
		Class<?> c = this.ts.loadFuncExprClass(this.env(), fieldNames, fieldTypes, code.getStartIndex(),
				code.getParamNames(), code.getParamTypes(), code.getReturnType(), code.getInner());
		Code[] inits = code.getFieldCode();
		String cname = Type.getInternalName(c);
		this.mBuilder.visitTypeInsn(NEW, cname);
		this.mBuilder.dup();
		this.mBuilder.visitMethodInsn(INVOKESPECIAL, cname, "<init>", "()V", false);
		for (int i = 0; i < fieldNames.length; i++) {
			this.mBuilder.dup();
			inits[i].emitCode(this);
			this.mBuilder.visitFieldInsn(PUTFIELD, cname/* internal */, fieldNames[i] + i, this.ts.desc(fieldTypes[i]));
		}
		// ODebug.trace("FuncCode.asType %s", code.getType());

	}

	@Override
	public void pushApply(ApplyCode code) {
		for (Code sub : code) {
			sub.emitCode(this);
		}
		FuncTy funcType = (FuncTy) code.args()[0].getType();
		String desc = this.ts.desc(funcType.getReturnType(), funcType.getParamTypes());
		String cname = Type.getInternalName(this.ts.toClass(funcType));
		this.mBuilder.visitMethodInsn(INVOKEINTERFACE, cname, AsmType.nameApply(funcType.getReturnType()), desc, true);
		return;
	}
	//
	// private void emitSugar(TEnv env, Code code, Ty ret) {
	// Code sugar = env.catchCode(() -> code.asType(env, ret));
	// sugar.emitCode(env, this);
	// }

	@Override
	public void pushGet(GetCode code) {
		Code recv = code.args()[0];
		String desc = this.ts.desc(code.getType(), OArrays.emptyTypes);
		recv.emitCode(this);
		Class<?> ifield = this.ts.gen(code.getName());
		Class<?> base = this.ts.toClass(recv.getType());
		if (base == Data$.class) {
			this.mBuilder.checkCast(Type.getType(ifield));
		}
		this.mBuilder.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ifield), code.getName(), desc, true);
		// this.mBuilder.visitMethodInsn(INVOKEVIRTUAL,
		// Type.getInternalName(base), code.getName(), desc, false);
	}

	@Override
	public void pushSet(SetCode code) {
		Code recv = code.args()[0];
		Code right = code.args()[1];
		String desc = this.ts.desc(Ty.tVoid, right.getType());
		recv.emitCode(this);
		Class<?> ifield = this.ts.gen(code.getName());
		Class<?> base = this.ts.toClass(recv.getType());
		if (base == Data$.class) {
			this.mBuilder.checkCast(Type.getType(ifield));
		}
		right.emitCode(this);
		this.mBuilder.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(ifield), code.getName(), desc, true);
		// this.mBuilder.visitMethodInsn(INVOKEVIRTUAL,
		// Type.getInternalName(base), code.getName(), desc, false);
	}

	@Override
	public void pushExistField(ExistFieldCode code) {
		code.getInner().emitCode(this);
		Class<?> ifield = this.ts.gen(code.getName());
		this.mBuilder.instanceOf(Type.getType(ifield));
	}

	@Override
	public void pushGroup(GroupCode code) {
		code.getInner().emitCode(this);

	}

	/* Imperative Programming */

	@Override
	public void pushAssign(AssignCode code) {

	}

}
