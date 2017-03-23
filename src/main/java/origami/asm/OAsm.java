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

import origami.asm.OGeneratorAdapter.VarEntry;
import origami.asm.code.ArrayGetCode;
import origami.asm.code.ArrayLengthCode;
import origami.asm.code.CheckCastCode;
import origami.asm.code.DupCode;
import origami.asm.code.LoadArgCode;
import origami.asm.code.LoadThisCode;
import origami.asm.code.OAsmCode;
import origami.code.OAndCode;
import origami.code.OArrayCode;
import origami.code.OAssignCode;
import origami.code.OBreakCode;
import origami.code.OCastCode;
import origami.code.OClassInitCode;
import origami.code.OCode;
import origami.code.OConstructorCode;
import origami.code.OContinueCode;
import origami.code.OEmptyCode;
import origami.code.OErrorCode;
import origami.code.OGenerator;
import origami.code.OGetIndexCode;
import origami.code.OGetSizeCode;
import origami.code.OGetterCode;
import origami.code.OIfCode;
import origami.code.OInstanceOfCode;
import origami.code.OJumpBeforeCode;
import origami.code.OLabelBlockCode;
import origami.code.OMethodCode;
import origami.code.OMultiCode;
import origami.code.ONameCode;
import origami.code.ONotCode;
import origami.code.OOrCode;
import origami.code.OReturnCode;
import origami.code.OSetIndexCode;
import origami.code.OSetterCode;
import origami.code.OSugarCode;
import origami.code.OThrowCode;
import origami.code.OTryCode;
import origami.code.OTryCode.CatchCode;
import origami.code.OValueCode;
import origami.code.OWarningCode;
import origami.code.OWhileCode;
import origami.lang.OClassDecl;
import origami.lang.OEnv;
import origami.lang.OField;
import origami.lang.OFieldDecl;
import origami.lang.OMethodDecl;
import origami.lang.OMethodHandle;
import origami.lang.type.OType;
import origami.rule.java.JavaSwitchCode;
import origami.rule.java.JavaSwitchCode.CaseCode;
import origami.util.OArrayUtils;
import origami.util.ODebug;
import origami.util.OLog;
import origami.util.OTypeUtils;

//import origami.decl.ConstructorDecl;

public class OAsm implements OGenerator, OArrayUtils {

	private final OEnv env;
	public OClassWriter cBuilder = null;
	public OGeneratorAdapter mBuilder = null;

	public OAsm(OEnv env) {
		this.env = env;
	}

	private OType t(Class<?> c) {
		return this.env.t(c);
	}

	private OValueCode newValueCode(Object value) {
		return (OValueCode) this.env.v(value);
	}

	byte[] byteCompile(OClassDecl cdecl) {
		OClassWriter cw = new OClassWriter(cdecl);
		this.cBuilder = cw.push(this.cBuilder);
		ODebug.trace("compiling class %s %s", cdecl.getName(), cdecl.getSignature());
		ArrayList<OCode> staticInits = new ArrayList<>();
		ArrayList<OCode> fieldInits = new ArrayList<>();
		for (OField f : cdecl.fields()) {
			OFieldDecl fdecl = f.getDecl();
			fdecl.typeCheck(this.env);
			OCode init = fdecl.getInitCode(cdecl.env());
			if (init != null) {
				if (fdecl.isStatic()) {
					staticInits.add(init);
				} else {
					fieldInits.add(init);
				}
			}
			this.defineField(cw, fdecl);
		}
		if (staticInits.size() > 0) {
			staticInits.add(new OReturnCode(this.env));
			OMultiCode body = new OMultiCode(staticInits);
			OAnno anno = new OAnno("static,private");
			OMethodDecl mdecl = new OMethodDecl(cdecl.getType(), anno, this.env.t(void.class), "<clinit>", emptyNames,
					emptyTypes, emptyTypes, body);
			this.defineMethod(cw, mdecl);
		}

		if (fieldInits.size() > 0) {
			OMultiCode body = new OMultiCode(fieldInits);
			this.env.add(OClassInitCode.class, body);
		}
		/* if isInterface(), then unnecessary */
		boolean hasConstructor = cdecl.getType().isInterface();
		for (OMethodHandle mh : cdecl.methods()) {
			mh.getDecl().typeCheck(this.env);
			if (mh.isSpecial()) {
				hasConstructor = true;
			}
		}
		if (!hasConstructor) {
			cdecl.addDefaultConstructors();
		}
		for (OMethodHandle mh : cdecl.methods()) {
			this.defineMethod(cw, mh.getDecl());
		}

		OValueCode[] v = cdecl.getPooledValues();
		for (int id = 0; id < v.length; id++) {
			FieldNode fn = new FieldNode(Opcodes.ACC_STATIC, cdecl.constFieldName(id), v[id].getType().typeDesc(0),
					null, null);
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
			this.mBuilder.pushBlock(new OClassFieldInitBlock(this.env.get(OClassInitCode.class)));
		}
		if (mdecl.body != null) {
			this.mBuilder.enterScope();
			String[] paramNames = mdecl.getParamNames();
			OType[] paramTypes = mdecl.getParamTypes();
			if (paramNames != null) {
				for (int i = 0; i < paramNames.length; i++) {
					// ODebug.trace("[%d] %s %s", i, paramNames[i],
					// paramTypes[i]);
					this.mBuilder.defineArgument(paramNames[i], paramTypes[i]);
				}
			}
			mdecl.body.generate(this);
			this.mBuilder.exitScope();
			// if (!mdecl.body.hasReturnCode()) {
			// mBuilder.returnValue();
			// }
			mdecl.body = null;
		}
		this.mBuilder.visitMaxs();
		this.mBuilder.endMethod();
		this.mBuilder = null;
	}

	// Asm direct operation

	public void pushAsmCode(OAsmCode<?> node) {
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
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.pushIfFalse(node.condCode(), elseLabel);

		// then
		node.thenCode().generate(this);
		this.mBuilder.goTo(mergeLabel);

		// else
		this.mBuilder.mark(elseLabel);
		node.elseCode().generate(this);

		// merge
		this.mBuilder.mark(mergeLabel);
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
		this.push(null, cond);
		// mBuilder.push(true);
		// mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, jump);
		this.mBuilder.visitJumpInsn(Opcodes.IFNE, jump);
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
		this.push(null, cond);
		// mBuilder.push(true);
		// mBuilder.ifCmp(Type.BOOLEAN_TYPE, GeneratorAdapter.NE, jump);
		this.mBuilder.visitJumpInsn(Opcodes.IFEQ, jump);
	}

	@Override
	public void pushReturn(OReturnCode node) {
		this.mBuilder.weaveBefore(node, null, this);
		OCode[] params = node.getParams();
		if (params.length == 1) {
			this.push(null, params[0]);
		}
		this.mBuilder.returnValue();

	}

	@Override
	public void pushTry(OTryCode code) {
		OCode finallyCode = code.finallyCode();
		// this.mBuilder.pushBlock(new OFinallyBlock(code.finallyClause()));

		Label mergeLabel = this.mBuilder.newLabel();
		Label tryStartLabel = this.mBuilder.newLabel();
		Label tryEndLabel = this.mBuilder.newLabel();

		/* try block */
		this.mBuilder.mark(tryStartLabel);
		if (finallyCode instanceof OEmptyCode) {
			code.tryCode().generate(this);
		} else {
			OJumpBeforeCode aspect = new OJumpBeforeCode(code.tryCode());
			aspect.setBeforeContinueCode(finallyCode);
			aspect.setBeforeBreakCode(finallyCode);
			aspect.setBeforeReturnCode(finallyCode);
			aspect.generate(this);
		}
		this.mBuilder.mark(tryEndLabel);
		finallyCode.generate(this);
		this.mBuilder.goTo(mergeLabel);

		/* catch blocks */
		for (CatchCode catchNode : code.catchCode()) {
			OType exceptionType = catchNode.getType();
			Label catchStartLabel = this.mBuilder.newLabel();
			// Label catchEndLabel = mBuilder.newLabel();

			this.mBuilder.catchException(tryStartLabel, tryEndLabel, exceptionType.asmType());
			this.mBuilder.enterScope();

			/* start the current catch block Label */
			this.mBuilder.mark(catchStartLabel);
			this.mBuilder.createNewVarAndStore(catchNode.getName(), exceptionType);

			catchNode.bodyCode().generate(this);
			code.finallyCode().generate(this);

			this.mBuilder.exitScope();
			this.mBuilder.goTo(mergeLabel);
		}

		if (!(finallyCode instanceof OEmptyCode)) {
			Label tryFinallyLabel = this.mBuilder.newLabel();
			this.mBuilder.mark(tryFinallyLabel);
			this.mBuilder.catchException(tryStartLabel, tryEndLabel, null);
			finallyCode.generate(this);
			this.mBuilder.throwException();
		}
		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void pushValue(OValueCode node) {
		Object v = node.getHandled();
		OType t = node.getType();
		// ODebug.trace("pushValue %s %s", t, v);
		if (t.is(void.class)) {
			return;
		}
		this.mBuilder.useStackSlots(t);
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
		this.mBuilder.getStatic(this.cBuilder.getTypeDesc(), name, Type.getType(v.getClass()));
	}

	@Override
	public void pushAnd(OAndCode code) {
		Label elseLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();

		this.pushIfFalse(code.getParams()[0], elseLabel);
		this.pushIfFalse(code.getParams()[1], elseLabel);
		this.pushValue(this.newValueCode(true));
		this.mBuilder.goTo(mergeLabel);

		this.mBuilder.mark(elseLabel);
		this.pushValue(this.newValueCode(false));

		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void pushOr(OOrCode code) {
		Label thenLabel = this.mBuilder.newLabel();
		Label mergeLabel = this.mBuilder.newLabel();
		this.pushIfTrue(code.getParams()[0], thenLabel);
		this.pushIfTrue(code.getParams()[1], thenLabel);

		this.pushValue(this.newValueCode(false));
		this.mBuilder.goTo(mergeLabel);

		this.mBuilder.mark(thenLabel);
		this.pushValue(this.newValueCode(true));

		this.mBuilder.mark(mergeLabel);
	}

	@Override
	public void pushNot(ONotCode code) {
		// TODO Auto-generated method stub
	}

	@Override
	public void pushInstanceOf(OInstanceOfCode code) {
		Type type = OAsmUtils.asmType(code.getInstanceOfType());
		code.getParams()[0].boxCode(this.env).generate(this);
		this.mBuilder.instanceOf(type);
	}

	@Override
	public void pushArray(OArrayCode node) {
		OType type = node.getType();
		OType ctype = type.getParamTypes()[0];
		Type elementType = ctype.asmType();
		OCode[] exprs = node.getParams();
		this.pushValue(this.newValueCode(exprs.length));
		this.mBuilder.newArray(elementType);
		for (int i = 0; i < exprs.length; i++) {
			int stack = this.mBuilder.usedStackSlots();
			this.mBuilder.dup(ctype);
			this.pushValue(this.newValueCode(i));
			exprs[i].generate(this);
			this.mBuilder.arrayStore(elementType);
			this.mBuilder.popStackSlots(stack);
		}
	}

	// private void pushParams(OCode code) {
	// for (OCode node : code.getParams()) {
	// node.generate(this);
	// }
	// }

	@Override
	public void push(OCode node) {
		this.push(node.getType(), node);
	}

	private void push(OType t, OCode node) {
		int stack = this.mBuilder.usedStackSlots();
		node.generate(this);
		this.mBuilder.popStackSlots(stack);
		if (t != null) {
			this.mBuilder.useStackSlots(t);
		}
	}

	private void pushParams(OType t, OCode code) {
		int stack = this.mBuilder.usedStackSlots();
		for (OCode node : code.getParams()) {
			node.generate(this);
		}
		this.mBuilder.popStackSlots(stack);
		if (t != null) {
			this.mBuilder.useStackSlots(t);
		}
	}

	@Override
	public void pushCast(OCastCode node) {
		if (node.isStupidCast()) {
			node.newErrorCode(this.env).generate(this);
			return;
		}
		if (node.isDownCast()) {
			OLog.report(this.env, node.log());
		}
		OType f = node.getFromType();
		OType t = node.getType();
		if (t.is(void.class)) {
			this.pushParams(t, node);
			this.mBuilder.pop(f);
			return;
		}
		OMethodHandle m = node.getMethod();
		if (f.isPrimitive() && t.isPrimitive()) {
			this.pushParams(t, node);
			this.mBuilder.cast(f.asmType(), t.asmType());
			return;
		}
		if (f.isPrimitive() && t.isAssignableFrom(f.boxType())) {
			if (m == null) {
				this.pushParams(t, node);
				// ODebug.trace("asm cast box %s => %s", f, t);
				this.mBuilder.box(f.asmType());
			} else {
				this.pushMethod(node);
			}
			return;
		}
		if (t.isPrimitive() && t.boxType().eq(f)) {
			if (m == null) {
				this.pushParams(t, node);
				ODebug.trace("asm cast unbox %s => %s", f, t);
				this.mBuilder.unbox(t.asmType());
			} else {
				this.pushMethod(node);
			}
			return;
		}
		if (m == null) {
			this.pushParams(t, node);
			ODebug.trace("asm cast noconv %s => %s", f, t);
			this.mBuilder.checkCast(t.asmType());
			return;
		}
		this.pushMethod(node);
		// ODebug.trace("asm cast conv %s => %s", f, t);
	}

	@Override
	public void pushMethod(OMethodCode node) {
		OMethodHandle m = node.getHandled();
		int ivc = m.getInvocation();
		if (ivc == OMethodHandle.DynamicInvocation) {
			node.boxParams(this.env);
		}
		this.pushParams(node.getType(), node);
		Type base = OAsmUtils.asmType(m.getDeclaringClass());
		switch (ivc) {
		case OMethodHandle.DynamicInvocation: {
			Method bsm = OAsmUtils.asmMethod(CallSite.class, "bootstrap", MethodHandles.Lookup.class, String.class,
					MethodType.class, Class.class, String.class);
			String bsmClassPath = OAsmUtils.getInternalName(m.getCallSite().getClass());
			String bsmDesc = bsm.getDescriptor();
			Handle handle = new Handle(Opcodes.H_INVOKESTATIC, bsmClassPath, "bootstrap", bsmDesc /* false */);
			String desc = m.methodType().toMethodDescriptorString();
			// ODebug.trace("InvokeDynamic: %s %s", desc, node.getType());
			this.mBuilder.invokeDynamic(m.getLocalName(), desc, handle, m.getCallSiteParams());
			break;
		}
		case OMethodHandle.StaticInvocation: {
			if (!this.mBuilder.tryInvokeBinary(m)) {
				Method method = OAsmUtils.asmMethod(m.getReturnType(), m.getName(), m.getParamTypes());
				// ODebug.trace("InvokeStatic: %s\n\t%s", m, node);
				this.mBuilder.invokeStatic(base, method);
			}
			break;
		}
		case OMethodHandle.SpecialInvocation: {
			Method method = OAsmUtils.asmMethod(this.env.t(void.class), m.getName(), m.getParamTypes());
			this.mBuilder.invokeConstructor(base, method);
			break;
		}
		case OMethodHandle.VirtualInvocation: {
			if (!this.mBuilder.tryInvokeBinary(m)) {
				Method method = OAsmUtils.asmMethod(m.getReturnType(), m.getName(), m.getParamTypes());
				this.mBuilder.invokeVirtual(base, method);
			}
			break;
		}
		case OMethodHandle.InterfaceInvocation: {
			Method method = OAsmUtils.asmMethod(m.getReturnType(), m.getName(), m.getParamTypes());
			this.mBuilder.invokeInterface(base, method);
			break;
		}
		case OMethodHandle.StaticGetter: {
			this.mBuilder.getStatic(base, m.getName(), m.getReturnType().asmType());
			break;
		}
		case OMethodHandle.VirtualGetter: {
			this.mBuilder.getField(base, m.getName(), m.getReturnType().asmType());
			break;
		}
		case OMethodHandle.StaticSetter: {
			this.mBuilder.dup(node.getType());
			this.mBuilder.putStatic(base, m.getName(), m.getParamTypes()[0].asmType());
			return; // unnecessary void check
		}
		case OMethodHandle.VirtualSetter: {
			this.mBuilder.dupX1(node.getType());
			this.mBuilder.putField(base, m.getName(), m.getParamTypes()[0].asmType() /* OK */);
			return; // unnecessary void check
		}

		}
		//
		this.mBuilder.checkCast(node.getType(), m.getReturnType());
		if (node instanceof OClassInitCode) {
			this.mBuilder.weaveBefore(node, null, this);
		}
	}

	@Override
	public void pushGetter(OGetterCode node) {
		OField field = node.getHandled();
		this.pushParams(field.getType(), node);
		Type base = OAsmUtils.asmType(field.getDeclaringClass());
		Type type = OAsmUtils.asmType(field.getType());
		if (field.isStatic()) {
			this.mBuilder.getStatic(base, field.getName(), type);
		} else {
			this.mBuilder.getField(base, field.getName(), type);
		}
	}

	@Override
	public void pushSetter(OSetterCode node) {
		OCode expr = node.expr();
		OField field = node.getHandled();
		if (node.recv().isPresent()) {
			node.recv().get().generate(this);
		} else if (!field.isStatic()) {
			this.mBuilder.loadThis();
		}
		expr.generate(this);
		Type base = OAsmUtils.asmType(field.getDeclaringClass());
		Type type = OAsmUtils.asmType(field.getType());
		if (field.isStatic()) {
			this.mBuilder.putStatic(base, field.getName(), type);
		} else {
			this.mBuilder.putField(base, field.getName(), type);
		}
	}

	@Override
	public void pushConstructor(OConstructorCode node) {
		Type type = OAsmUtils.asmType(node.getDeclaringClass());
		this.mBuilder.newInstance(type);
		this.mBuilder.dup();
		this.pushMethod(node);
	}

	@Override
	public void pushName(ONameCode node) {
		try {
			VarEntry var = this.mBuilder.getVar(node.getName());
			this.mBuilder.loadFromVar(var);
			this.mBuilder.useStackSlots(node.getType());
		} catch (NullPointerException e) {
			ODebug.trace("name=%s", node.getName());
			throw e;
		}
	}

	@Override
	public void pushAssign(OAssignCode node) {
		VarEntry entry;
		this.push(null, node.rightCode());
		if (!node.getType().is(void.class)) {
			this.mBuilder.dup(node.getDefinedType());
		}
		if (node.defined) {
			entry = this.mBuilder.createNewVar(node.getHandled(), node.getDefinedType());
		} else {
			entry = this.mBuilder.getVar(node.getHandled());
		}
		this.mBuilder.storeToVar(entry);
	}

	@Override
	public void pushThis() {
		this.mBuilder.loadThis();
		this.mBuilder.useStackSlots(1);
	}

	@Override
	public void pushGetSize(OGetSizeCode code) {
		if (code.getMethod() == null) {
			this.pushParams(code.getType(), code);
			this.mBuilder.arrayLength();
		} else {
			this.pushMethod(code);
		}
	}

	@Override
	public void pushGetIndex(OGetIndexCode code) {
		if (code.getMethod() == null) {
			this.pushParams(code.getType(), code);
			this.mBuilder.arrayLoad(code.getType().asmType());
		} else {
			this.pushMethod(code);
		}
	}

	@Override
	public void pushSetIndex(OSetIndexCode code) {
		if (code.getMethod() == null) {
			this.pushParams(code.getType(), code);
			this.mBuilder.arrayStore(code.getType().asmType());
		} else {
			this.pushMethod(code);
		}
	}

	@Override
	public void pushThrow(OThrowCode code) {
		this.pushParams(null, code);
		this.mBuilder.throwException();
	}

	@Override
	public void pushJumpBefore(OJumpBeforeCode code) {
		this.mBuilder.pushBlock(code);
		code.getFirst().generate(this);
		this.mBuilder.popBlock();
	}

	@Override
	public void pushBreak(OBreakCode code) {
		OBreakBlock untilBlock = this.mBuilder.findBlock(OBreakBlock.class, (b) -> b.matchLabel(code.getLabel()));
		this.mBuilder.weaveBefore(code, untilBlock, this);
		if (untilBlock == null) {
			ODebug.trace("no block label=%s", code.getLabel());
			/* Throwing new OrigamiBreakException() ; */
			OType t = this.t(OrigamiBreakException.class);
			this.push(null, t.newConstructorCode(this.env));
			this.mBuilder.throwException();
		} else {
			this.mBuilder.goTo(untilBlock.endLabel);
		}
	}

	@Override
	public void pushContinue(OContinueCode code) {
		OBreakContinueBlock block = this.mBuilder.findBlock(OBreakContinueBlock.class,
				(b) -> b.matchLabel(code.getLabel()));
		this.mBuilder.weaveBefore(code, block, this);
		if (block == null) {
			ODebug.trace("no block label=%s", code.getLabel());
			/* Throwing new OrigamiContinueException() ; */
			OType t = this.t(OrigamiContinueException.class);
			this.push(null, t.newConstructorCode(this.env));
			this.mBuilder.throwException();
		} else {
			this.mBuilder.goTo(block.startLabel);
		}
	}

	@Override
	public void pushBlockCode(OLabelBlockCode code) {
		// ODebug.trace("begin block label=%s", code.getLabel());
		this.mBuilder.enterScope();
		OBreakContinueBlock block = this.mBuilder.pushBlock(new OBreakContinueBlock(this.mBuilder, code.getLabel()));
		this.push(code.initCode());
		this.mBuilder.mark(block.startLabel);
		this.push(code.bodyCode()); // void
		this.mBuilder.mark(block.endLabel);
		this.push(code.thusCode());
		this.mBuilder.popBlock();
		this.mBuilder.exitScope();
	}

	@Override
	public void pushWhile(OWhileCode code) {
		OCode nextCode = code.nextCode();
		if (nextCode instanceof OEmptyCode) {
			OBreakContinueBlock block = this.mBuilder.pushBlock(new OBreakContinueBlock(this.mBuilder, null));
			this.mBuilder.mark(block.startLabel);
			this.pushIfFalse(code.condCode(), block.endLabel);
			this.push(code.bodyCode());
			this.mBuilder.goTo(block.startLabel);
			this.mBuilder.mark(block.endLabel);
		} else {
			OBreakContinueBlock block = this.mBuilder.pushBlock(new OBreakContinueBlock(this.mBuilder, null));
			Label startLabel0 = this.mBuilder.newLabel();
			this.mBuilder.mark(startLabel0);
			this.pushIfFalse(code.condCode(), block.endLabel);
			this.push(code.bodyCode());
			this.mBuilder.goTo(block.startLabel);
			this.mBuilder.mark(block.startLabel);
			this.push(nextCode);
			this.mBuilder.goTo(startLabel0);
			this.mBuilder.mark(block.endLabel);
		}
	}

	public void pushSwitch(JavaSwitchCode code) {
		OBreakBlock block = this.mBuilder.pushBlock(new OBreakBlock(this.mBuilder, null));
		Label condLabel = this.mBuilder.newLabel();
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
			labels[c] = this.mBuilder.newLabel();
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
		this.mBuilder.mark(condLabel);
		if (disOrdered) {
			this.pushValue(this.newValueCode(map));
			code.condition().generate(this);
			if (code.condition().getType().isPrimitive()) {
				this.mBuilder.box(code.condition().getType().asmType());
			}
			this.mBuilder.push(((Number) c).intValue());
			this.mBuilder.box(Type.INT_TYPE);
			this.mBuilder.invokeVirtual(Type.getType(HashMap.class),
					Method.getMethod("Object getOrDefault(Object, Object)"));
			this.mBuilder.unbox(Type.INT_TYPE);
		} else {
			code.condition().generate(this);
		}
		ArrayList<Label> labelList = new ArrayList<>(Arrays.asList(labels));
		labelList.remove(dfltLabel);
		this.mBuilder.visitTableSwitchInsn(0, c - 1, dfltLabel, labelList.toArray(new Label[0]));

		for (int i = 0; i < size; i++) {
			Label l = labels[i];
			CaseCode cc = (CaseCode) caseCodes[i];
			this.mBuilder.mark(l);
			// push((ITree) pairs[i + 1]);
			cc.caseClause().generate(this);
		}
		// mBuilder.getLoopLabels().pop();
		this.mBuilder.mark(breakLabel);
		this.mBuilder.popBlock();
	}

	@Override
	public void pushError(OErrorCode node) {
		OLog.report(this.env, node.getLog());
		Constructor<?> c = OTypeUtils.loadConstructor(OrigamiSourceException.class, String.class);
		OConstructorCode code = new OConstructorCode(this.env, c, this.env.v(node.getLog().toString()));
		this.push(code);
		this.mBuilder.throwException();
	}

	@Override
	public void pushWarning(OWarningCode node) {
		OLog.report(this.env, node.getHandled());
		this.push(node.getFirst());
	}

	@Override
	public void pushSugar(OSugarCode code) {
		code.desugar().generate(this);
	}
}
