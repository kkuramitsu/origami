package blue.origami.transpiler.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import blue.origami.transpiler.CodeTemplate;
import blue.origami.transpiler.ConstTemplate;
import blue.origami.transpiler.Generator;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;

public class AsmGenerator extends Generator implements Opcodes {

	private AsmType ts;

	public AsmGenerator(TEnv env) {
		this.ts = new AsmType(env);
	}

	@Override
	public void init() {
		this.ts.initProperties();
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
			this.cw0 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
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
	public void emit(TEnv env, Code code) {
		code = this.emitHeader(env, code);
		if (!code.isGenerative()) {
			Method m = new Method(evalName, Type.getType(this.ts.toClass(code.getType())), emptyTypes);
			GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw());
			AsmSection sec = new AsmSection(this.ts, this.cname(), mw);
			code.emitCode(env, sec);
			mw.returnValue();
			mw.endMethod();
		}
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
			} catch (NoSuchMethodException e) {
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return e;
			}
		}
		return null;
	}

	@Override
	public CodeTemplate newConstTemplate(TEnv env, String lname, Ty ret) {
		StringBuilder sb = new StringBuilder();
		sb.append("F|");
		sb.append(this.cname());
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new ConstTemplate(lname, ret, template);
	}

	@Override
	public void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr) {
		AsmGenerator asm = env.get(AsmGenerator.class);
		if (asm == null) {
			env.add(AsmGenerator.class, this);
		}
		FieldNode fn = new FieldNode(ACC_PUBLIC + ACC_STATIC, name, this.ts.desc(type), null, null);
		fn.accept(this.cw());
		AsmSection sec = this.fieldSec();
		expr.emitCode(env, sec);
		sec.mBuilder.visitFieldInsn(PUTSTATIC, this.cname()/* internal */, name, this.ts.desc(type));
	}

	@Override
	public CodeTemplate newFuncTemplate(TEnv env, String sname, String lname, Ty returnType, Ty... paramTypes) {
		// this.mw.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt",
		// "(D)D", false);
		StringBuilder sb = new StringBuilder();
		sb.append("S|");
		sb.append(this.cname());
		sb.append("|");
		sb.append(lname);
		String template = sb.toString();
		return new CodeTemplate(sname, returnType, paramTypes, template);
	}

	@Override
	public void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code) {
		Method m = new Method(name, this.ts.ti(returnType), this.ts.ts(paramTypes));
		GeneratorAdapter mw = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, this.cw());
		AsmSection sec = new AsmSection(this.ts, this.cname(), mw);
		for (int i = 0; i < paramNames.length; i++) {
			sec.addVariable(NameHint.safeName(paramNames[i]) + i, paramTypes[i]);
		}
		try {
			code.emitCode(env, sec);
		} catch (Exception e) {
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
