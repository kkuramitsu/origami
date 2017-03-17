package origami.asm;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import origami.ODebug;
import origami.OEnv;
import origami.OLog;
import origami.asm.OGeneratorAdapter.VarEntry;
import origami.asm.code.ArrayGetCode;
import origami.asm.code.ArrayLengthCode;
import origami.asm.code.AsmCode;
import origami.asm.code.CheckCastCode;
import origami.asm.code.DupCode;
import origami.asm.code.LoadArgCode;
import origami.asm.code.LoadThisCode;
import origami.code.OAndCode;
import origami.code.OArrayCode;
import origami.code.OAssignCode;
import origami.code.OLabelBlockCode;
import origami.code.OBreakCode;
import origami.code.OCastCode;
import origami.code.ClassInitCode;
import origami.code.OConstructorCode;
import origami.code.OContinueCode;
import origami.code.OEmptyCode;
import origami.code.OErrorCode;
import origami.code.ForCode;
import origami.code.GetIndexCode;
import origami.code.GetSizeCode;
import origami.code.GetterCode;
import origami.code.OIfCode;
import origami.code.OInstanceOfCode;
import origami.code.OMethodCode;
import origami.code.OMultiCode;
import origami.code.ONameCode;
import origami.code.ONotCode;
import origami.code.OCode;
import origami.code.OGenerator;
import origami.code.OOrCode;
import origami.code.OReturnCode;
import origami.code.SetIndexCode;
import origami.code.OSetterCode;
import origami.code.SwitchCode;
import origami.code.SwitchCode.CaseCode;
import origami.code.OThrowCode;
import origami.code.TryCatchCode;
import origami.code.TryCatchCode.CatchCode;
import origami.code.OValueCode;
import origami.code.OWarningCode;
import origami.code.OWhileCode;
import origami.lang.OClassDecl;
import origami.lang.OField;
import origami.lang.OFieldDecl;
import origami.lang.OMethodDecl;
import origami.lang.OMethodHandle;
import origami.trait.OArrayUtils;
import origami.trait.OTypeUtils;
import origami.type.OType;

//import origami.decl.ConstructorDecl;

public class OAsm implements OGenerator, OArrayUtils {

	private final OEnv env;
	public OClassWriter cBuilder = null;
	public OGeneratorAdapter mBuilder = null;

	public OAsm(OEnv env) {
		this.env = env;
	}

	private OType t(Class<?> c) {
		return env.t(c);
	}

	private OValueCode newValueCode(Object value) {
		return (OValueCode) env.v(value);
	}

	byte[] byteCompile(OClassDecl cdecl) {
		OClassWriter cw = new OClassWriter(cdecl);
		this.cBuilder = cw.push(this.cBuilder);
		ODebug.trace("compiling class %s %s", cdecl.getName(), cdecl.getSignature());
		ArrayList<OCode> staticInits = new ArrayList<>();
		ArrayList<OCode> fieldInits = new ArrayList<>();
		for (OField f : cdecl.fields()) {
			OFieldDecl fdecl = f.getDecl();
			fdecl.typeCheck(env);
			OCode init = fdecl.getInitCode(cdecl.env());
			if (init != null) {
				if (fdecl.isStatic()) {
					staticInits.add(init);
				} else {
					fieldInits.add(init);
				}
			}
			defineField(cw, fdecl);
		}
		if (staticInits.size() > 0) {
			staticInits.add(new OReturnCode(env));
			OMultiCode body = new OMultiCode(staticInits);
			OAnno anno = new OAnno("static,private");
			OMethodDecl mdecl = new OMethodDecl(cdecl.getType(), anno, env.t(void.class), "<clinit>", emptyNames, emptyTypes, emptyTypes, body);
			defineMethod(cw, mdecl);
		}

		if (fieldInits.size() > 0) {
			OMultiCode body = new OMultiCode(fieldInits);
			env.add(ClassInitCode.class, body);
		}
		/* if isInterface(), then unnecessary */
		boolean hasConstructor = cdecl.getType().isInterface();
		for (OMethodHandle mh : cdecl.methods()) {
			mh.getDecl().typeCheck(env);
			if (mh.isSpecial()) {
				hasConstructor = true;
			}
		}
		if (!hasConstructor) {
			cdecl.addDefaultConstructors();
		}
		for (OMethodHandle mh : cdecl.methods()) {
			defineMethod(cw, mh.getDecl());
		}

		OValueCode[] v = cdecl.getPooledValues();
		for (int id = 0; id < v.length; id++) {
			FieldNode fn = new FieldNode(Opcodes.ACC_STATIC, cdecl.constFieldName(id), v[id].getType().typeDesc(0), null, null);
			fn.accept(this.cBuilder);
		}

		byte[] b = cw.byteCompile();
		this.cBuilder = cw.pop();
		return b;
	}

	private void defineField(OClassWriter cw, OFieldDecl fdecl) {
		String sig = fdecl.getType().typeDesc(2);
		Object value = fdecl.getInitValue();
		cw.addField(fdecl.anno, fdecl.getName(), fdecl.getType(), sig/* signature */, value);
	}

	public void defineMethod(OClassWriter cw, OMethodDecl mdecl) {
		// ODebug.trace("compiling method %s %s", mdecl.getName(),
		// mdecl.getSignature());
		this.mBuilder = cw.newGeneratorAdapter(mdecl);
		if (mdecl.getName().equals("<init>")) {
			this.mBuilder.pushBlock(new OClassFieldInitBlock(env.get(ClassInitCode.class)));
		}
		if (mdecl.body != null) {
			mBuilder.enterScope();
			String[] paramNames = mdecl.getParamNames();
			OType[] paramTypes = mdecl.getParamTypes();
			if (paramNames != null) {
				for (int i = 0; i < paramNames.length; i++) {
					// ODebug.trace("[%d] %s %s", i, paramNames[i],
					// paramTypes[i]);
					mBuilder.defineArgument(paramNames[i], paramTypes[i]);
				}
			}
			mdecl.body.generate(this);
			mBuilder.exitScope();
			// if (!mdecl.body.hasReturnCode()) {
			// mBuilder.returnValue();
			// }
			mdecl.body = null;
		}
		mBuilder.visitMaxs();
		mBuilder.endMethod();
		mBuilder = null;
	}

	// Asm direct operation

	public void pushAsmCode(AsmCode node) {
		if (node instanceof LoadArgCode) {
			int arg = ((LoadArgCode) node).getHandled();
			this.mBuilder.loadArg(arg);
			this.mBuilder.useStackSlots(node.getType());
		}
		if (node instanceof LoadThisCode) {
			this.mBuilder.loadThis();
			this.mBuilder.useStackSlots(1);
		}
		if (node instanceof ArrayGetCode) {
			this.pushParams(node.getType(), node);
			this.mBuilder.arrayLoad(node.getType().asmType());
		}
		if (node instanceof ArrayLengthCode) {
			this.mBuilder.arrayLength();
			this.mBuilder.useStackSlots(1);
		}
		if (node instanceof DupCode) {
			this.mBuilder.dup();
			this.mBuilder.useStackSlots(1);
		}
		if (node instanceof CheckCastCode) {
			this.mBuilder.checkCast(((CheckCastCode) node).getHandled().asmType());
		}
	}

	// Method Body
	// --------------------------------------------------------------------------------

	// public void defineVariable(LocalVarDecl decl) {
	// mBuilder.createNewVar(decl.name, decl.type);
	// }

	@Override
	public void pushMulti(OMultiCode node) {
		if (node.hasDefinedLocalVariables()) {
			this.mBuilder.enterScope();
		}
		for (OCode c : node.getParams()) {
			c.generate(this);
		}
		if (node.hasDefinedLocalVariables()) {
			this.mBuilder.exitScope();
		}
	}

	@Override
	public void pushIf(OIfCode node) {
		Label elseLabel = mBuilder.newLabel();
		Label mergeLabel = mBuilder.newLabel();

		pushIfFalse(node.condition(), elseLabel);

		// then
		node.thenClause().generate(this);
		mBuilder.goTo(mergeLabel);

		// else
		mBuilder.mark(elseLabel);
		node.elseClause().generate(this);

		// merge
		mBuilder.mark(mergeLabel);
		// mBuilder.visitFrame(F_FULL, 5, new Object[] { "[Ljava/lang/String;",
		// Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER },
		// 1, new Object[] { "java/io/PrintStream" });
	}

	private void pushIfTrue(OCode cond, Label jump) {
		// if (cond.is(_EqExpr)) {
		// if (cond.is(_right, _NullExpr) &&
		// !cond.get(_left).getType().isPrimitive()) {
		// push(env, cond.get(_left));
		// mBuilder.ifNull(jump);
		// return;
		// }
		// }
		// if (cond.is(_NeExpr)) {
		// if (cond.is(_right, _NullExpr) &&
		// !cond.get(_right).getType().isPrimitive()) {
		// push(env, cond.get(_left));
		// mBuilder.ifNonNull(jump);
		// return;
		// }
		// }
		push(null, cond);
		// mBuilder.push(true);
		// mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, jump);
		mBuilder.visitJumpInsn(Opcodes.IFNE, jump);
	}

	private void pushIfFalse(OCode cond, Label jump) {
		// if (cond.is(_EqExpr)) {
		// if (cond.is(_right, _NullExpr) &&
		// !cond.get(_left).getType().isPrimitive()) {
		// push(env, cond.get(_left));
		// mBuilder.ifNonNull(jump);
		// return;
		// }
		// }
		// if (cond.is(_NeExpr)) {
		// if (cond.is(_right, _NullExpr) &&
		// !cond.get(_left).getType().isPrimitive()) {
		// push(env, cond.get(_left));
		// mBuilder.ifNull(jump);
		// return;
		// }
		// }
		push(null, cond);
		// mBuilder.push(true);
		// mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, jump);
		mBuilder.visitJumpInsn(Opcodes.IFEQ, jump);
	}

	@Override
	public void pushReturn(OReturnCode node) {
		mBuilder.weaveBlock(this, OFinallyBlock.class, null);
		OCode[] params = node.getParams();
		if (params.length == 1) {
			push(null, params[0]);
		}
		mBuilder.returnValue();

	}

	@Override
	public void pushTry(TryCatchCode code) {
		mBuilder.pushBlock(new OFinallyBlock(code.finallyClause()));

		Label mergeLabel = mBuilder.newLabel();
		// Label firstCatchEndLabel = null;
		Label tryStartLabel = mBuilder.newLabel();
		Label tryEndLabel = mBuilder.newLabel();
		Label tryFinallyLabel = mBuilder.newLabel();

		// String finallyVarName = " finally";
		// String exceptionVarName1 = " exceptionVarName1";
		// String exceptionVarName2 = " exceptionVarName2";

		/* initialize exception variables */
		// mBuilder.mark(labels.getStartLabel());

		// mBuilder.pushNull();
		// VarEntry excpVar1 = mBuilder.createNewVarAndStore(exceptionVarName1,
		// t(Throwable.class));
		// mBuilder.pushNull();
		// VarEntry excpVar2 = mBuilder.createNewVarAndStore(exceptionVarName2,
		// t(Throwable.class));

		/* insert resource block */
		// ResourceInfo[] resources = null;
		// if (code.withResourses().isPresent()) {
		// OCode[] resCode = code.withResourses().get();
		// resources = new ResourceInfo[resCode.length];
		// for (int i = 0; i < resCode.length; i++) {
		// WithResourceCode res = (WithResourceCode) resCode[i];
		// Label resDeclLabel = mBuilder.newLabel();
		// mBuilder.mark(resDeclLabel);
		// ODebug.NotAvailable();
		// // res.varDecl().generate(this);
		// OGeneratorAdapter.VarEntry resoVarEntry =
		// mBuilder.getVar(res.varDecl().getName());
		// resources[i] = new ResourceInfo(resoVarEntry, res.callClose(),
		// resDeclLabel, (i > 0) ? resources[i - 1] : null);
		// }
		// }

		/* try block */
		mBuilder.mark(tryStartLabel);
		code.tryClasuse().generate(this);
		mBuilder.mark(tryEndLabel);
		code.finallyClause().generate(this);
		mBuilder.goTo(mergeLabel);

		/* invoke close method */
		// for (int i = resources.length - 1; i >= 0; i--) {
		// ResourceInfo res = resources[i];
		// mBuilder.mark(res.closeLabel);
		// pushResourceCloseBlock(excpVar1, excpVar2, res, (res.prev != null) ?
		// res.prev.closeLabel : labels.getFinallyLabel(), tryStartLabel);
		// }

		/* catch blocks */
		for (CatchCode catchNode : code.catchClauses()) {

			// OVariable v = catchNode.parameter();
			OType exceptionType = catchNode.getType();
			Label catchStartLabel = mBuilder.newLabel();
			// Label catchEndLabel = mBuilder.newLabel();

			mBuilder.catchException(tryStartLabel, tryEndLabel, exceptionType.asmType());
			mBuilder.enterScope();

			/* start the current catch block Label */
			mBuilder.mark(catchStartLabel);
			mBuilder.createNewVarAndStore(catchNode.getName(), exceptionType);

			catchNode.catchClause().generate(this);
			code.finallyClause().generate(this);

			/* end the current catch block Label */
			// mBuilder.mark(catchEndLabel);

			mBuilder.exitScope();
			mBuilder.goTo(mergeLabel);
		}

		if (!(code.finallyClause() instanceof OEmptyCode)) {
			mBuilder.mark(tryFinallyLabel);
			mBuilder.catchException(tryStartLabel, tryEndLabel, null);
			// VarEntry fentry = mBuilder.createNewVarAndStore(finallyVarName,
			// t(Throwable.class));
			code.finallyClause().generate(this);
			// mBuilder.loadFromVar(fentry);
			mBuilder.throwException();
		}
		mBuilder.mark(mergeLabel);
		mBuilder.popBlock();
	}

	@Override
	public void pushUndefined(OCode node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pushValue(OValueCode node) {
		Object v = node.getHandled();
		OType t = node.getType();
		// ODebug.trace("pushValue %s %s", t, v);
		if (t.is(void.class)) {
			return;
		}
		mBuilder.useStackSlots(t);
		if (v == null) {
			if (t.is(boolean.class)) {
				this.mBuilder.push(false);
				return;
			}
			if (t.is(int.class) || t.is(short.class) || t.is(byte.class) || t.is(char.class)) {
				this.mBuilder.push(0);
				if (t.is(short.class)) {
					this.mBuilder.cast(Type.INT_TYPE, Type.SHORT_TYPE);
				}
				if (t.is(byte.class)) {
					this.mBuilder.cast(Type.INT_TYPE, Type.BYTE_TYPE);
				}
				if (t.is(char.class)) {
					this.mBuilder.cast(Type.INT_TYPE, Type.CHAR_TYPE);
				}
				return;
			}
			if (t.is(long.class)) {
				this.mBuilder.push(0L);
				return;
			}
			if (t.is(float.class)) {
				this.mBuilder.push(0.0f);
				return;
			}
			if (t.is(double.class)) {
				this.mBuilder.push(0.0);
				return;
			}
			this.mBuilder.pushNull();
			return;
		}
		if (t.is(boolean.class)) {
			this.mBuilder.push(((Boolean) v).booleanValue());
			return;
		}
		if (t.is(int.class) || t.is(short.class) || t.is(byte.class) || t.is(char.class)) {
			this.mBuilder.push(((Number) v).intValue());
			if (t.is(short.class)) {
				this.mBuilder.cast(Type.INT_TYPE, Type.SHORT_TYPE);
			}
			if (t.is(byte.class)) {
				this.mBuilder.cast(Type.INT_TYPE, Type.BYTE_TYPE);
			}
			if (t.is(char.class)) {
				this.mBuilder.cast(Type.INT_TYPE, Type.CHAR_TYPE);
			}
			return;
		}
		if (t.is(long.class)) {
			this.mBuilder.push(((Number) v).longValue());
			return;
		}
		if (t.is(float.class)) {
			this.mBuilder.push(((Number) v).floatValue());
			return;
		}
		if (t.is(double.class)) {
			this.mBuilder.push(((Number) v).doubleValue());
			return;
		}
		if (v instanceof String) {
			this.mBuilder.push((String) v);
			return;
		}
		if (v instanceof Class<?>) {
			this.mBuilder.push(Type.getType((Class<?>) v));
			return;
		}
		if (v instanceof OType) {
			this.mBuilder.push(((OType) v).asmType());
			return;
		}
		// ODebug.FIXME("constant");
		int id = this.cBuilder.cdecl.poolConstValue(node);
		String name = this.cBuilder.cdecl.constFieldName(id);
		this.mBuilder.getStatic(cBuilder.getTypeDesc(), name, Type.getType(v.getClass()));
	}

	public void pushAnd(OAndCode code) {
		Label elseLabel = mBuilder.newLabel();
		Label mergeLabel = mBuilder.newLabel();

		pushIfFalse(code.getParams()[0], elseLabel);
		pushIfFalse(code.getParams()[1], elseLabel);
		pushValue(newValueCode(true));
		mBuilder.goTo(mergeLabel);

		mBuilder.mark(elseLabel);
		pushValue(newValueCode(false));

		mBuilder.mark(mergeLabel);
	}

	public void pushOr(OOrCode code) {
		Label thenLabel = mBuilder.newLabel();
		Label mergeLabel = mBuilder.newLabel();
		pushIfTrue(code.getParams()[0], thenLabel);
		pushIfTrue(code.getParams()[1], thenLabel);

		pushValue(newValueCode(false));
		mBuilder.goTo(mergeLabel);

		mBuilder.mark(thenLabel);
		pushValue(newValueCode(true));

		mBuilder.mark(mergeLabel);
	}

	public void pushNot(ONotCode code) {
		// TODO Auto-generated method stub
	}

	public void pushInstanceOf(OInstanceOfCode code) {
		Type type = OAsmUtils.asmType(code.getInstanceOfType());
		code.getParams()[0].boxCode(env).generate(this);
		mBuilder.instanceOf(type);
	}

	@Override
	public void pushArray(OArrayCode node) {
		OType type = node.getType();
		OType ctype = type.getParamTypes()[0];
		Type elementType = ctype.asmType();
		OCode[] exprs = node.getParams();
		pushValue(newValueCode(exprs.length));
		mBuilder.newArray(elementType);
		for (int i = 0; i < exprs.length; i++) {
			int stack = mBuilder.usedStackSlots();
			mBuilder.dup(ctype);
			pushValue(newValueCode(i));
			exprs[i].generate(this);
			mBuilder.arrayStore(elementType);
			mBuilder.popStackSlots(stack);
		}
	}

	// private void pushParams(OCode code) {
	// for (OCode node : code.getParams()) {
	// node.generate(this);
	// }
	// }

	private void push(OCode node) {
		push(node.getType(), node);
	}

	private void push(OType t, OCode node) {
		int stack = mBuilder.usedStackSlots();
		node.generate(this);
		mBuilder.popStackSlots(stack);
		if (t != null) {
			mBuilder.useStackSlots(t);
		}
	}

	private void pushParams(OType t, OCode code) {
		int stack = mBuilder.usedStackSlots();
		for (OCode node : code.getParams()) {
			node.generate(this);
		}
		mBuilder.popStackSlots(stack);
		if (t != null) {
			mBuilder.useStackSlots(t);
		}
	}

	@Override
	public void pushCast(OCastCode node) {
		if (node.isStupidCast()) {
			node.newErrorCode(env).generate(this);
			return;
		}
		if (node.isDownCast()) {
			OLog.report(env, node.log());
		}
		OType f = node.getFromType();
		OType t = node.getType();
		if (t.is(void.class)) {
			pushParams(t, node);
			mBuilder.pop(f);
			return;
		}
		OMethodHandle m = node.getMethod();
		if (f.isPrimitive() && t.isPrimitive()) {
			pushParams(t, node);
			mBuilder.cast(f.asmType(), t.asmType());
			return;
		}
		if (f.isPrimitive() && t.isAssignableFrom(f.boxType())) {
			if (m == null) {
				pushParams(t, node);
				// ODebug.trace("asm cast box %s => %s", f, t);
				mBuilder.box(f.asmType());
			} else {
				pushMethod(node);
			}
			return;
		}
		if (t.isPrimitive() && t.boxType().eq(f)) {
			if (m == null) {
				pushParams(t, node);
				ODebug.trace("asm cast unbox %s => %s", f, t);
				mBuilder.unbox(t.asmType());
			} else {
				pushMethod(node);
			}
			return;
		}
		if (m == null) {
			pushParams(t, node);
			ODebug.trace("asm cast noconv %s => %s", f, t);
			mBuilder.checkCast(t.asmType());
			return;
		}
		pushMethod(node);
		// ODebug.trace("asm cast conv %s => %s", f, t);
	}

	@Override
	public void pushMethod(OMethodCode node) {
		OMethodHandle m = node.getHandled();
		int ivc = m.getInvocation();
		if (ivc == OMethodHandle.DynamicInvocation) {
			node.boxParams(env);
		}
		pushParams(node.getType(), node);
		Type base = OAsmUtils.asmType(m.getDeclaringClass());
		switch (ivc) {
		case OMethodHandle.DynamicInvocation: {
			Method bsm = OAsmUtils.asmMethod(CallSite.class, "bootstrap", MethodHandles.Lookup.class, String.class, MethodType.class, Class.class, String.class);
			String bsmClassPath = OAsmUtils.getInternalName(m.getCallSite().getClass());
			String bsmDesc = bsm.getDescriptor();
			Handle handle = new Handle(Opcodes.H_INVOKESTATIC, bsmClassPath, "bootstrap", bsmDesc /*false*/);
			String desc = m.methodType().toMethodDescriptorString();
			// ODebug.trace("InvokeDynamic: %s %s", desc, node.getType());
			mBuilder.invokeDynamic(m.getLocalName(), desc, handle, m.getCallSiteParams());
			break;
		}
		case OMethodHandle.StaticInvocation: {
			if (!mBuilder.tryInvokeBinary(m)) {
				Method method = OAsmUtils.asmMethod(m.getReturnType(), m.getName(), m.getParamTypes());
				// ODebug.trace("InvokeStatic: %s\n\t%s", m, node);
				mBuilder.invokeStatic(base, method);
			}
			break;
		}
		case OMethodHandle.SpecialInvocation: {
			Method method = OAsmUtils.asmMethod(env.t(void.class), m.getName(), m.getParamTypes());
			mBuilder.invokeConstructor(base, method);
			break;
		}
		case OMethodHandle.VirtualInvocation: {
			if (!mBuilder.tryInvokeBinary(m)) {
				Method method = OAsmUtils.asmMethod(m.getReturnType(), m.getName(), m.getParamTypes());
				mBuilder.invokeVirtual(base, method);
			}
			break;
		}
		case OMethodHandle.InterfaceInvocation: {
			Method method = OAsmUtils.asmMethod(m.getReturnType(), m.getName(), m.getParamTypes());
			mBuilder.invokeInterface(base, method);
			break;
		}
		case OMethodHandle.StaticGetter: {
			mBuilder.getStatic(base, m.getName(), m.getReturnType().asmType());
			break;
		}
		case OMethodHandle.VirtualGetter: {
			mBuilder.getField(base, m.getName(), m.getReturnType().asmType());
			break;
		}
		case OMethodHandle.StaticSetter: {
			this.mBuilder.dup(node.getType());
			mBuilder.putStatic(base, m.getName(), m.getParamTypes()[0].asmType());
			return; // unnecessary void check
		}
		case OMethodHandle.VirtualSetter: {
			this.mBuilder.dupX1(node.getType());
			mBuilder.putField(base, m.getName(), m.getParamTypes()[0].asmType() /* OK */);
			return; // unnecessary void check
		}

		}
		//
		mBuilder.checkCast(node.getType(), m.getReturnType());
		if (node instanceof ClassInitCode) {
			this.mBuilder.weaveBlock(this, OClassFieldInitBlock.class, null);
		}
	}

	@Override
	public void pushGetter(GetterCode node) {
		OField field = node.getHandled();
		pushParams(field.getType(), node);
		Type base = OAsmUtils.asmType(field.getDeclaringClass());
		Type type = OAsmUtils.asmType(field.getType());
		if (field.isStatic()) {
			mBuilder.getStatic(base, field.getName(), type);
		} else {
			mBuilder.getField(base, field.getName(), type);
		}
	}

	@Override
	public void pushSetter(OSetterCode node) {
		OCode expr = node.expr();
		OField field = node.getHandled();
		if (node.recv().isPresent()) {
			node.recv().get().generate(this);
		} else if (!field.isStatic()) {
			mBuilder.loadThis();
		}
		expr.generate(this);
		Type base = OAsmUtils.asmType(field.getDeclaringClass());
		Type type = OAsmUtils.asmType(field.getType());
		if (field.isStatic()) {
			mBuilder.putStatic(base, field.getName(), type);
		} else {
			mBuilder.putField(base, field.getName(), type);
		}
	}

	@Override
	public void pushConstructor(OConstructorCode node) {
		Type type = OAsmUtils.asmType(node.getDeclaringClass());
		mBuilder.newInstance(type);
		mBuilder.dup();
		pushMethod(node);
	}

	@Override
	public void pushName(ONameCode node) {
		try {
			VarEntry var = mBuilder.getVar(node.getName());
			mBuilder.loadFromVar(var);
			mBuilder.useStackSlots(node.getType());
		} catch (NullPointerException e) {
			ODebug.trace("name=%s", node.getName());
			throw e;
		}
	}

	@Override
	public void pushAssign(OAssignCode node) {
		VarEntry entry;
		push(null, node.right());
		if (!node.getType().is(void.class)) {
			mBuilder.dup(node.getDefinedType());
		}
		if (node.defined) {
			entry = mBuilder.createNewVar(node.getHandled(), node.getDefinedType());
		} else {
			entry = mBuilder.getVar(node.getHandled());
		}
		mBuilder.storeToVar(entry);
	}

	@Override
	public void pushThis() {
		mBuilder.loadThis();
		mBuilder.useStackSlots(1);
	}

	public void pushGetSize(GetSizeCode code) {
		if (code.getMethod() == null) {
			pushParams(code.getType(), code);
			mBuilder.arrayLength();
		} else {
			pushMethod(code);
		}
	}

	@Override
	public void pushGetIndex(GetIndexCode code) {
		if (code.getMethod() == null) {
			pushParams(code.getType(), code);
			mBuilder.arrayLoad(code.getType().asmType());
		} else {
			pushMethod(code);
		}
	}

	@Override
	public void pushSetIndex(SetIndexCode code) {
		if (code.getMethod() == null) {
			pushParams(code.getType(), code);
			mBuilder.arrayStore(code.getType().asmType());
		} else {
			pushMethod(code);
		}
	}

	@Override
	public void pushThrow(OThrowCode code) {
		pushParams(null, code);
		mBuilder.throwException();
	}

	@Override
	public void pushBreak(OBreakCode code) {
		OBreakBlock block = mBuilder.findBlock(OBreakBlock.class, (b) -> b.matchLabel(code.getLabel()));
		mBuilder.weaveBlock(this, OFinallyBlock.class, block);
		if (block == null) {
			ODebug.trace("no block label=%s", code.getLabel());
			/* Throwing new OrigamiBreakException() ; */
			OType t = this.t(OrigamiBreakException.class);
			push(null, t.newConstructorCode(env));
			mBuilder.throwException();
		} else {
			mBuilder.goTo(block.endLabel);
		}
	}

	@Override
	public void pushContinue(OContinueCode code) {
		OContinueBlock block = mBuilder.findBlock(OContinueBlock.class, (b) -> b.matchLabel(code.getLabel()));
		mBuilder.weaveBlock(this, OFinallyBlock.class, block);
		if (block == null) {
			ODebug.trace("no block label=%s", code.getLabel());
			/* Throwing new OrigamiContinueException() ; */
			OType t = this.t(OrigamiContinueException.class);
			push(null, t.newConstructorCode(env));
			mBuilder.throwException();
		} else {
			mBuilder.goTo(block.startLabel);
		}
	}

	@Override
	public void pushBlockCode(OLabelBlockCode code) {
		// ODebug.trace("begin block label=%s", code.getLabel());
		mBuilder.enterScope();
		OContinueBlock block = mBuilder.pushBlock(new OContinueBlock(mBuilder, code.getLabel()));
		push(code.initCode());
		mBuilder.mark(block.startLabel);
		push(code.bodyCode()); // void
		mBuilder.mark(block.endLabel);
		push(code.thusCode());
		mBuilder.popBlock();
		mBuilder.exitScope();
	}

	public void pushLoop(OWhileCode code) {
		Label startLabel = mBuilder.newLabel();
		Label exitLabel = mBuilder.newLabel();

		mBuilder.mark(startLabel);
		pushIfFalse(code.cond(), exitLabel);
		push(code.body());
		mBuilder.goTo(startLabel);
		mBuilder.mark(exitLabel);
	}

	@Override
	public void pushLoop(ForCode code) {
		OContinueBlock block = mBuilder.pushBlock(new OContinueBlock(mBuilder, null));

		Label loopLabel = mBuilder.newLabel();
		Label condLabel = mBuilder.newLabel();
		Label returnLabel = block.endLabel;
		Label continueLabel = block.startLabel;

		code.initClause().ifPresent((c) -> {
			c.generate(this);
		});

		mBuilder.goTo(condLabel);
		mBuilder.mark(loopLabel);

		code.bodyClause().generate(this);

		mBuilder.mark(continueLabel);
		code.iterClause().ifPresent((c) -> {
			c.generate(this);
		});

		mBuilder.mark(condLabel);

		code.condition().ifPresent((c) -> {
			pushIfTrue(c, loopLabel);
		});

		mBuilder.mark(returnLabel);
		mBuilder.popBlock();
	}

	@Override
	public void pushSwitch(SwitchCode code) {
		OBreakBlock block = mBuilder.pushBlock(new OBreakBlock(mBuilder, null));
		Label condLabel = mBuilder.newLabel();
		Label breakLabel = block.endLabel;

		int size = code.caseCode().length;
		Label dfltLabel = breakLabel;
		Label[] labels = new Label[size];
		HashMap<Object, Integer> map = new HashMap<>();
		OCode[] caseCodes = code.caseCode();
		int c = 0;
		boolean disOrdered = false;
		for (; c < size;) {
			CaseCode cc = ((CaseCode) caseCodes[c]);
			labels[c] = mBuilder.newLabel();
			if (cc == null) {
				dfltLabel = labels[c];
				continue;
			}
			map.put(cc.value, c);
			if (!(cc.value instanceof Number) || ((Number) cc.value).intValue() != c) {
				disOrdered = true;
			}
			c++;
		}
		mBuilder.mark(condLabel);
		if (disOrdered) {
			pushValue(newValueCode(map));
			code.condition().generate(this);
			if (code.condition().getType().isPrimitive()) {
				mBuilder.box(code.condition().getType().asmType());
			}
			this.mBuilder.push(((Number) c).intValue());
			mBuilder.box(Type.INT_TYPE);
			mBuilder.invokeVirtual(Type.getType(HashMap.class), Method.getMethod("Object getOrDefault(Object, Object)"));
			mBuilder.unbox(Type.INT_TYPE);
		} else {
			code.condition().generate(this);
		}
		ArrayList<Label> labelList = new ArrayList<>(Arrays.asList(labels));
		labelList.remove(dfltLabel);
		mBuilder.visitTableSwitchInsn(0, c - 1, dfltLabel, labelList.toArray(new Label[0]));

		for (int i = 0; i < size; i++) {
			Label l = labels[i];
			CaseCode cc = (CaseCode) caseCodes[i];
			mBuilder.mark(l);
			// push((ITree) pairs[i + 1]);
			cc.caseClause().generate(this);
		}
		// mBuilder.getLoopLabels().pop();
		mBuilder.mark(breakLabel);
		mBuilder.popBlock();
	}

	@Override
	public void pushError(OErrorCode node) {
		OLog.report(env, node.getLog());
		Constructor<?> c = OTypeUtils.loadConstructor(OrigamiSourceException.class, String.class);
		OConstructorCode code = new OConstructorCode(env, c, env.v(node.getLog().toString()));
		this.push(code);
		mBuilder.throwException();
	}

	@Override
	public void pushWarning(OWarningCode node) {
		OLog.report(env, node.getHandled());
		this.push(node.getFirst());
	}
}
