package blue.origami.transpiler.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

import blue.origami.util.OConsole;
import blue.origami.util.ODebug;

public class TClassLoader extends ClassLoader {

	// private final Map<String, OClassDecl> uncompiledMap;

	public TClassLoader() {
		super();
		this.dumpDirectory = System.getenv("DUMPDIR");
	}

	final static String toClassName(String pathName) {
		return pathName.replace('/', '.');
	}

	final static String toPathName(String className) {
		return className.replace('.', '/');
	}

	// private void addClassDecl(OClassDecl cdecl) {
	// String classname = toClassName(cdecl.getName());
	// this.uncompiledMap.put(classname, cdecl);
	// }
	//
	// public OClassDeclType newType(OEnv env, OAnno anno, String cname, OType[]
	// paramTypes, OType superType,
	// OType... interfaces) {
	// OClassDeclType t = new OClassDeclType(env, anno, cname, paramTypes,
	// superType, interfaces);
	// this.addClassDecl(t.getDecl());
	// return t;
	// }
	//
	// private int currentId = 0;
	// private OClassDecl currentDecl = null;
	//
	// public OClassDecl currentClassDecl(OEnv env) {
	// if (this.currentDecl == null) {
	// String cname = "$C" + this.currentId++;
	// OClassDeclType t = this.newType(env, new OAnno("public,abstract"), cname,
	// null, env.t(Object.class),
	// env.t(OrigamiObject.class));
	// this.currentDecl = t.getDecl();
	// }
	// return this.currentDecl;
	// }
	//
	// @Override
	// protected Class<?> findClass(String cname) throws ClassNotFoundException
	// {
	// OClassDecl cdecl = this.uncompiledMap.remove(cname);
	// if (cdecl == null) {
	// throw new ClassNotFoundException("not found " + cname);
	// }
	// if (cdecl == this.currentDecl) {
	// this.currentDecl = null;
	// }
	// byte[] byteCode = cdecl.byteCompile();
	// this.dump(cname, byteCode);
	// Class<?> c = this.defineClass(cname, byteCode, 0, byteCode.length);
	// this.setInitStaticField(c, cdecl);
	// return c;
	// }
	//
	// private void setInitStaticField(Class<?> c, OClassDecl cdecl) {
	// for (OField f : cdecl.fields()) {
	// if (f.isStatic()) {
	// OFieldDecl fdecl = f.getDecl();
	// Object value = fdecl.getValue();
	// if (value == null) {
	// continue;
	// }
	// Field ff = OTypeUtils.loadField(c, fdecl.getName());
	// try {
	// ff.setAccessible(true);
	// ff.set(null, value);
	// ff.setAccessible(false);
	// } catch (IllegalAccessException e) {
	// ODebug.traceException(e);
	// }
	// }
	// }
	// ValueCode[] pools = cdecl.getPooledValues();
	// for (int id = 0; id < pools.length; id++) {
	// Field f = OTypeUtils.loadField(c, cdecl.constFieldName(id));
	// try {
	// f.setAccessible(true);
	// f.set(null, pools[id].getHandled());
	// f.setAccessible(false);
	// } catch (IllegalAccessException e) {
	// ODebug.traceException(e);
	// }
	// }
	// }
	//
	// @Override
	// protected Class<?> loadClass(String name, boolean resolve) throws
	// ClassNotFoundException {
	// Class<?> foundClass = this.findLoadedClass(name);
	// if (foundClass == null) {
	// ClassLoader parent = this.getParent();
	// try {
	// foundClass = parent.loadClass(name);
	// } catch (Throwable e) {
	// }
	// }
	// if (foundClass == null) {
	// foundClass = this.findClass(name);
	// }
	// if (resolve) {
	// this.resolveClass(foundClass);
	// }
	// return foundClass;
	// }

	public boolean traceMode = true;
	private String dumpDirectory = null;

	private void dump(String className, byte[] byteCode) {
		if (ODebug.enabled && (this.traceMode || this.dumpDirectory != null)) {
			int index = className.lastIndexOf('.');
			String classFileName = className.substring(index + 1) + ".class";
			if (this.dumpDirectory != null) {
				classFileName = this.dumpDirectory + "/" + classFileName;
			}
			try (FileOutputStream stream = new FileOutputStream(classFileName)) {
				stream.write(byteCode);
				stream.close();
				if (this.dumpDirectory == null) {
					new File(classFileName).deleteOnExit();
				}
				OConsole.beginColor(OConsole.Cyan);
				OConsole.println("[Generated] " + classFileName + " size=" + byteCode.length);
				ProcessBuilder pb = new ProcessBuilder("javap", "-c", "-l", "-p", "-s",
						/*
						 * "-v" ,
						 */classFileName);
				pb.redirectOutput(Redirect.INHERIT);
				Process p = pb.start();
				p.waitFor();
				p.destroy();
				OConsole.endColor();
			} catch (IOException e) {
				ODebug.trace("cannot dump " + classFileName + " caused by " + e);
			} catch (InterruptedException e) {
				ODebug.traceException(e);
			}
		}
	}

	// // uniquename
	//
	// HashMap<String, String> shortMap = new HashMap<>();
	// HashMap<Character, Integer> countMap = new HashMap<>();
	//
	// public final String shortName(OType t) {
	// String desc = t.typeDesc(0);
	// String shortName = this.shortMap.get(desc);
	// if (shortName == null) {
	// Character firstChar = t.getName().charAt(0);
	// shortName = String.valueOf(firstChar);
	// Integer c = this.countMap.get(firstChar);
	// if (c == null) {
	// this.countMap.put(firstChar, 0);
	// } else {
	// c = c + 1;
	// shortName = c + shortName;
	// this.countMap.put(firstChar, c);
	// }
	// this.shortMap.put(desc, shortName);
	// }
	// return shortName;
	// }
	//
	// public final String uniqueName(String prefix, OType... params) {
	// StringBuilder sb = new StringBuilder();
	// sb.append(prefix);
	// sb.append("$");
	// for (OType t : params) {
	// sb.append(this.shortName(t));
	// }
	// return sb.toString();
	// }
	//
	// public final String uniqueName(String prefix, int id, OType... params) {
	// StringBuilder sb = new StringBuilder();
	// sb.append(prefix);
	// sb.append("$$");
	// sb.append(1000 + id);
	// sb.append("$");
	// for (OType t : params) {
	// sb.append(this.shortName(t));
	// }
	// return sb.toString();
	// }

	// /* message */
	//
	// private ArrayList<String> messageList = new ArrayList<>();
	//
	// public final int getMessageId(String message) {
	// this.messageList.add(message);
	// return this.messageList.size() - 1;
	// }
	//
	// public final String getMessageById(int md) {
	// return this.messageList.get(md);
	// }

}
