package blue.origami.asm;

import java.lang.reflect.InvocationTargetException;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import blue.origami.common.ODebug;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeMapper;
import blue.origami.transpiler.ConstMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.target.SyntaxMapper;
import blue.origami.transpiler.type.Ty;

public class AsmMapper extends CodeMapper implements Opcodes {

	private AsmType ts;

	public AsmMapper(Transpiler env) {
		super(env, new SyntaxMapper());
		this.ts = new AsmType(env);
	}

	@Override
	public void init() {
		// this.ts.initProperties();
	}

	@Override
	protected void setup() {
		this.cw0 = null;
		this.cname0 = null;
		this.fieldSec = null;
		super.setup();
	}

	private String cname0;

	String cname() {
		if (this.cname0 == null) {
			this.cname0 = "C$" + this.ts.seq();
		}
		return this.cname0;
	}

	private ClassWriter cw0;

	ClassWriter cw() {
		if (this.cw0 == null) {
			this.cw0 = this.ts.newClassWriter(ClassWriter.COMPUTE_FRAMES);
			this.cw0.visit(V1_8, ACC_PUBLIC, this.cname(), null/* signatrue */, "java/lang/Object", null);
			// this.cw.visitSource("<input>", null);
		}
		return this.cw0;
	}

	AsmSection fieldSec;

	private AsmSection fieldSec() {
		if (this.fieldSec == null) {
			Method m = Method.getMethod("void <clinit>()");
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw());
			this.fieldSec = new AsmSection(this.ts, this.cname(), mw);
		}
		return this.fieldSec;
	}

	private static final Type[] emptyTypes = new Type[0];
	private static final String evalName = "eval";

	@Override
	public void emitTopLevel(Env env, Code code) {
		code = this.emitHeader(env, code);
		if (!code.showError(env) && code.isGenerative()) {
			Method m = new Method(evalName, Type.getType(this.ts.toClass(code.getType())), emptyTypes);
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw());
			AsmSection sec = new AsmSection(this.ts, this.cname(), mw);
			code.emitCode(sec);
			mw.returnValue();
			mw.endMethod();
		}
	}

	@Override
	public boolean isExecutable() {
		return true;
	}

	@Override
	protected Object wrapUp() {
		if (this.cw0 != null) {
			if (this.fieldSec != null) {
				this.fieldSec.mBuilder.returnValue();
				this.fieldSec.mBuilder.endMethod();
				this.fieldSec = null;
			}
			this.cw0.visitEnd();
			byte[] byteCode = this.cw0.toByteArray();
			AsmType.classLoader.store(this.cname0, byteCode);
			// this.cw0 = null;
			try {
				Class<?> c = AsmType.classLoader.loadClass(this.cname0);
				java.lang.reflect.Method m = c.getMethod(evalName);
				return m.invoke(null);
			} catch (InvocationTargetException e) {
				e.getTargetException().printStackTrace();
				return null;
			} catch (NoSuchMethodException e) {
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;

	}

	@Override
	public CodeMap newConstMap(Env env, String lname, Ty ret) {
		StringBuilder sb = new StringBuilder();
		sb.append("F|");
		sb.append(this.cname());
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new ConstMap(lname, template, ret);
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr) {
		AsmMapper asm = env.get(AsmMapper.class);
		if (asm == null) {
			env.add(AsmMapper.class, this);
		}
		FieldNode fn = new FieldNode(ACC_PUBLIC + ACC_STATIC, name, this.ts.desc(type), null, null);
		fn.accept(this.cw());
		AsmSection sec = this.fieldSec();
		expr.emitCode(sec);
		sec.mBuilder.visitFieldInsn(PUTSTATIC, this.cname()/* internal */, name, this.ts.desc(type));
	}

	@Override
	public CodeMap newCodeMap(Env env, String sname, String lname, Ty returnType, Ty... paramTypes) {
		StringBuilder sb = new StringBuilder();
		sb.append("S|");
		sb.append(this.cname());
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new CodeMap(sname, template, returnType, paramTypes);
	}

	@Override
	public void defineFunction(Env env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code) {
		Method m = new Method(name, this.ts.ti(returnType), this.ts.ts(paramTypes));
		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw());
		AsmSection sec = new AsmSection(this.ts, this.cname(), mw);
		for (int i = 0; i < paramNames.length; i++) {
			sec.addVariable(NameHint.safeName(paramNames[i]) + i, paramTypes[i]);
		}
		try {
			code.emitCode(sec);
		} catch (Exception e) {
			e.printStackTrace();
			ODebug.traceException(e);
		}
		mw.returnValue();
		mw.endMethod();
	}

	// local
	// static HashMap<Class<?>, TType> c2tMap = new HashMap<>();

	static String nameApply(Ty t) {
		switch (t.toString()) {
		case "Bool":
			return "applyZ";
		case "Int":
			return "applyI";
		case "Float":
			return "applyD";
		case "String":
			return "applyS";
		}
		return "apply";
	}

}
