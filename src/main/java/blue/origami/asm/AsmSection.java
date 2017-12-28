package blue.origami.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import blue.origami.chibi.Data$;
import blue.origami.common.OArrays;
import blue.origami.common.ODebug;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.ApplyCode;
import blue.origami.transpiler.code.AssignCode;
import blue.origami.transpiler.code.BoolCode;
import blue.origami.transpiler.code.BreakCode;
import blue.origami.transpiler.code.CastCode;
import blue.origami.transpiler.code.CastCode.BoxCastCode;
import blue.origami.transpiler.code.CastCode.UnboxCastCode;
import blue.origami.transpiler.code.CharCode;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DataCode;
import blue.origami.transpiler.code.DictCode;
import blue.origami.transpiler.code.DoubleCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.code.FuncRefCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.GroupCode;
import blue.origami.transpiler.code.HasCode;
import blue.origami.transpiler.code.IfCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.ListCode;
import blue.origami.transpiler.code.MappedCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.RangeCode;
import blue.origami.transpiler.code.ReturnCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.StringCode;
import blue.origami.transpiler.code.SwitchCode;
import blue.origami.transpiler.code.TemplateCode;
import blue.origami.transpiler.code.ThrowCode;
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.code.TupleIndexCode;
import blue.origami.transpiler.code.VarNameCode;
import blue.origami.transpiler.code.WhileCode;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.GenericTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMatcher;
import blue.origami.transpiler.type.VarParamTy;

public class AsmSection extends AsmBuilder implements CodeSection {

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
	public void pushNone(Code code) {
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
	public void pushChar(CharCode code) {
		this.mBuilder.push((char) code.getValue());
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
			this.pop(fc);
			return;
		}
		// ODebug.trace("calling cast %s => %s %s %s", f, t, code.getInner(),
		// code.getTemplate());
		if (t.match(true, f, TypeMatcher.Nop)) {
			code.getInner().emitCode(this);
			return;
		}
		// ODebug.trace("calling cast %s => %s %s %s", f, t, code.getInner(),
		// code.getTemplate());
		this.pushCall(code);
	}

	// I,+,
	@Override
	public void pushCall(MappedCode code) {
		final CodeMap cmap = code.getMapped();
		final String[] def = cmap.getDefined().split("\\|", -1);
		if (def[0].equals("X")) {
			this.pushCall(code, def[1]);
			return;
		}
		if (def[0].equals("N")) {
			this.mBuilder.visitTypeInsn(NEW, def[1]);
			this.mBuilder.visitInsn(DUP);
		}
		int c = 0;
		for (Code sub : code) {
			Ty gt = cmap.getParamTypes()[c];
			this.pushArgu(gt, sub);
			c++;
		}
		this.pushInst(cmap);
		this.checkAnyCast(code.getType(), cmap.getReturnType());
	}

	private void pushArgu(Ty gt, Code sub) {
		sub.emitCode(this);
		this.checkAnyCast(gt, sub.getType());
	}

	private void checkAnyCast(Ty toTy, Ty fromTy) {
		// Ty pt = sub.getType();
		Class<?> toClass = this.ts.toClass(toTy);
		Class<?> fromClass = this.ts.toClass(fromTy);
		if (toClass != fromClass && !toClass.isAssignableFrom(fromClass)) {
			if (fromClass.isAssignableFrom(toClass)) { // upcast
				this.mBuilder.checkCast(Type.getType(toClass));
				return;
			}
			ODebug.trace("::: MUST ANYCAST %s <- %s %s <- %s", toTy, fromTy, toClass.getSimpleName(),
					fromClass.getSimpleName());
			this.anyCast(toTy, toClass, fromTy, fromClass);
		}
	}

	private void anyCast(Ty toTy, Class<?> toClass, Ty fromTy, Class<?> fromClass) {
		// if (toTy.isFunc() && fromTy.isFunc()) {
		Ty f = fromTy.map(ty -> ty instanceof VarParamTy ? Ty.tAnyRef : ty);
		Ty t = toTy.map(ty -> ty instanceof VarParamTy ? Ty.tAnyRef : ty);
		CodeMap cmap = this.env().findArrow(this.env(), f, t);
		if (cmap.mapCost() != CodeMap.STUPID) {
			this.pushInst(cmap);
		} else {
			ODebug.trace("UNDEF (%s => %s)", f, t);
			this.mBuilder.checkCast(Type.getType(toClass));
		}
		// }
	}

	private void pushInst(final CodeMap cmap) {
		final String[] def = cmap.getDefined().split("\\|", -1);
		String desc = null;
		switch (def[0]) {
		case "-":
			return;
		case "F":
		case "GETSTATIC":
			desc = this.ts.desc(cmap.getReturnType());
			// ODebug.trace("GETSTATIC %s,%s,%s", def[1], def[2], desc);
			this.mBuilder.visitFieldInsn(GETSTATIC, def[1], def[2], desc);
			return;
		case "S":
		case "INVOKESTATIC":
			// this.mw.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt",
			// "(D)D", false);
			desc = this.ts.desc(cmap.getReturnType(), cmap.getParamTypes());
			// ODebug.trace("INVOKESTATIC %s,%s,%s", def[1], def[2], desc);
			// ODebug.trace("template %s", tp);
			this.mBuilder.visitMethodInsn(INVOKESTATIC, def[1], def[2], desc, false);
			return;
		case "V":
		case "INVOKEVIRTUAL":
			desc = this.ts.desc(cmap.getReturnType(), OArrays.ltrim(cmap.getParamTypes()));
			// ODebug.trace("::::: desc=%s, %s", desc, tp);
			this.mBuilder.visitMethodInsn(INVOKEVIRTUAL, def[1], def[2], desc, false);
			return;
		case "I":
		case "INVOKEINTERFACE":
			desc = this.ts.desc(cmap.getReturnType(), OArrays.ltrim(cmap.getParamTypes()));
			this.mBuilder.visitMethodInsn(INVOKEINTERFACE, def[1], def[2], desc, false);
			return;
		case "N":
		case "INVOKESPECIAL":
			desc = this.ts.desc(cmap.getReturnType(), cmap.getParamTypes());
			this.mBuilder.visitMethodInsn(INVOKESPECIAL, def[1], def[2], desc, false);
			return;
		case "O":
			int opCode = op(def[1]);
			if (opCode != -1) {
				this.mBuilder.visitInsn(opCode);
				return;
			}
		default:
			ODebug.trace("************ UNDEFINED INST '%s'", cmap.getDefined());
			// assert (tp.getDefined().length() > 0) : tp;
		}
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
	public void pushName(VarNameCode code) {
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
	public void pushAssign(AssignCode code) {
		VarEntry var = this.varStack.find(code.getName());
		// ODebug.trace("load %s %s", code.getName(), var);
		Type typeDesc = (this.ts.ti(var.varType));
		code.getInner().emitCode(this);
		if (!code.getType().isVoid()) {
			this.dup(this.ts.toClass(code.getType()));
		}
		this.mBuilder.visitVarInsn(typeDesc.getOpcode(Opcodes.ISTORE), var.varIndex);
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

	private Label breakLabel = null;

	@Override
	public void pushWhile(WhileCode code) {
		Label stacked = this.breakLabel;
		Label beginLabel = this.mBuilder.newLabel();
		Label endLabel = this.mBuilder.newLabel();
		this.breakLabel = endLabel;
		;
		this.mBuilder.mark(beginLabel);
		this.pushIfFalse(code.condCode(), endLabel);
		code.bodyCode();
		this.mBuilder.goTo(beginLabel);
		this.mBuilder.mark(endLabel);
		this.breakLabel = stacked;
	}

	@Override
	public void pushBreak(BreakCode code) {
		if (this.breakLabel == null) {
			this.env().reportError(code.getSource(), TFmt.YY1_cannot_be_used, TFmt.quote("break"));
		}
		this.mBuilder.goTo(this.breakLabel);
	}

	@Override
	public void pushReturn(ReturnCode code) {
		/* weaving before */
		Code expr = code.getInner();
		if (!expr.getType().isVoid()) {
			expr.emitCode(this);
		}
		this.mBuilder.returnValue();
	}

	@Override
	public void pushThrow(ThrowCode code) {
		code.getInner().emitCode(this);
		// TODO
		this.mBuilder.throwException();
	}

	@Override
	public void pushSwitch(SwitchCode code) {
		// OBreakBlock block = this.mBuilder.pushBlock(new OBreakBlock(this.mBuilder,
		// null));
		// Label condLabel = this.mBuilder.newLabel();
		// Label breakLabel = block.endLabel;
		//
		// int size = code.caseCode().length;
		// Label dfltLabel = breakLabel;
		// Label[] labels = new Label[size];
		// HashMap<Object, Integer> map = new HashMap<>();
		// OCode[] caseCodes = code.caseCode();
		// int c = 0;
		// boolean disOrdered = false;
		// for (; c < size;) {
		// CaseCode cc = ((CaseCode) caseCodes[c]);
		// labels[c] = this.mBuilder.newLabel();
		// if (cc == null) {
		// dfltLabel = labels[c];
		// continue;
		// }
		// map.put(cc.value, c);
		// if (!(cc.value instanceof Number) || ((Number) cc.value).intValue() != c) {
		// disOrdered = true;
		// }
		// c++;
		// }
		// this.mBuilder.mark(condLabel);
		// if (disOrdered) {
		// this.pushValue(this.newValueCode(map));
		// code.condition().generate(this);
		// if (code.condition().getType().isPrimitive()) {
		// this.mBuilder.box(code.condition().getType().asmType());
		// }
		// this.mBuilder.push(((Number) c).intValue());
		// this.mBuilder.box(Type.INT_TYPE);
		// this.mBuilder.invokeVirtual(Type.getType(HashMap.class),
		// Method.getMethod("Object getOrDefault(Object, Object)"));
		// this.mBuilder.unbox(Type.INT_TYPE);
		// } else {
		// code.condition().generate(this);
		// }
		// ArrayList<Label> labelList = new ArrayList<>(Arrays.asList(labels));
		// labelList.remove(dfltLabel);
		// this.mBuilder.visitTableSwitchInsn(0, c - 1, dfltLabel, labelList.toArray(new
		// Label[0]));
		//
		// for (int i = 0; i < size; i++) {
		// Label l = labels[i];
		// CaseCode cc = (CaseCode) caseCodes[i];
		// this.mBuilder.mark(l);
		// // push((ITree) pairs[i + 1]);
		// cc.caseClause().generate(this);
		// }
		// // mBuilder.getLoopLabels().pop();
		// this.mBuilder.mark(breakLabel);
		// this.mBuilder.popBlock();
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
		//ODebug.p("type:%s, base:%s, names:%s, args:%s", code.getType(), code.getType().base(), code.getNames(), code.args());
		Ty type = code.getType();
		Class<?> c = this.ts.loadDataClass((DataTy) ((type instanceof DataTy) ? type : type.base()));
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

	@Override
	public void pushList(ListCode code) {
		GenericTy dt = (GenericTy) code.getType().base();
		Class<?> c = this.ts.toClass(dt);
		this.mBuilder.push(code.isMutable());
		if (c == blue.origami.chibi.List$.class) {
			Type ty = Type.getType(Object.class);
			this.pushArray(ty, true, code.args());
			String desc = String.format("(Z[%s)%s", ty.getDescriptor(), Type.getDescriptor(this.ts.toClass(dt)));
			this.mBuilder.visitMethodInsn(INVOKESTATIC, Type.getInternalName(c), "newArray", desc, false);
		} else {
			Ty t = dt.getParamType();
			this.pushArray(this.ts.ti(t), false, code.args());
			String desc = String.format("(Z[%s)%s", Type.getDescriptor(this.ts.toClass(t)),
					Type.getDescriptor(this.ts.toClass(dt)));
			this.mBuilder.visitMethodInsn(INVOKESTATIC, Type.getInternalName(c), "newArray", desc, false);
		}
	}

	@Override
	public void pushRange(RangeCode code) {
		GenericTy dt = (GenericTy) code.getType().base();
		for (Code sub : code) {
			sub.emitCode(this);
		}
		String desc = String.format("(II)%s", Type.getDescriptor(this.ts.toClass(dt)));
		this.mBuilder.visitMethodInsn(INVOKESTATIC, APIs, "range", desc, false);
	}

	@Override
	public void pushDict(DictCode code) {

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
		CodeMap tp = code.getMapped();
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
				AST.names(code.getParamNames()), code.getParamTypes(), code.getReturnType(), code.getInner());
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
	}

	@Override
	public void pushApply(ApplyCode code) {
		for (Code sub : code) {
			sub.emitCode(this);
		}
		FuncTy funcType = (FuncTy) code.args()[0].getType().base();
		String desc = this.ts.desc(funcType.getReturnType(), funcType.getParamTypes());
		String cname = Type.getInternalName(this.ts.toClass(funcType));
		this.mBuilder.visitMethodInsn(INVOKEINTERFACE, cname, AsmType.nameApply(funcType.getReturnType()), desc, true);
		return;
	}

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
	public void pushHas(HasCode code) {
		code.getInner().emitCode(this);
		Class<?> ifield = this.ts.gen(code.getName());
		this.mBuilder.instanceOf(Type.getType(ifield));
	}

	@Override
	public void pushGroup(GroupCode code) {
		code.getInner().emitCode(this);

	}

	/* Imperative Programming */

}
