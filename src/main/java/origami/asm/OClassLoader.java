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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import origami.code.OValueCode;
import origami.ffi.OrigamiObject;
import origami.lang.OClassDeclType;
import origami.lang.OEnv;
import origami.lang.OClassDecl;
import origami.lang.OField;
import origami.lang.OFieldDecl;
import origami.lang.type.OType;
import origami.util.OConsole;
import origami.util.ODebug;
import origami.util.OTypeUtils;

public class OClassLoader extends ClassLoader {

	private final Map<String, OClassDecl> uncompiledMap;

	public OClassLoader() {
		super();
		this.uncompiledMap = new HashMap<>();
		this.dumpDirectory = System.getenv("DUMPDIR");
	}

	public final static String toClassName(String pathName) {
		return pathName.replace('/', '.');
	}

	public final static String toPathName(String className) {
		return className.replace('.', '/');
	}

	private void addClassDecl(OClassDecl cdecl) {
		String classname = toClassName(cdecl.getName());
		this.uncompiledMap.put(classname, cdecl);
	}

	public OClassDeclType newType(OEnv env, OAnno anno, String cname, OType[] paramTypes, OType superType,
			OType... interfaces) {
		OClassDeclType t = new OClassDeclType(env, anno, cname, paramTypes, superType, interfaces);
		addClassDecl(t.getDecl());
		return t;
	}

	private int currentId = 0;
	private OClassDecl currentDecl = null;

	public OClassDecl currentClassDecl(OEnv env) {
		if (currentDecl == null) {
			String cname = "$C" + currentId++;
			OClassDeclType t = newType(env, new OAnno("public,abstract"), cname, null, env.t(Object.class),
					env.t(OrigamiObject.class));
			this.currentDecl = t.getDecl();
		}
		return this.currentDecl;
	}

	@Override
	protected Class<?> findClass(String cname) throws ClassNotFoundException {
		OClassDecl cdecl = this.uncompiledMap.remove(cname);
		if (cdecl == null) {
			throw new ClassNotFoundException("not found " + cname);
		}
		if (cdecl == this.currentDecl) {
			this.currentDecl = null;
		}
		byte[] byteCode = cdecl.byteCompile();
		this.dump(cname, byteCode);
		Class<?> c = this.defineClass(cname, byteCode, 0, byteCode.length);
		setInitStaticField(c, cdecl);
		return c;
	}

	private void setInitStaticField(Class<?> c, OClassDecl cdecl) {
		for (OField f : cdecl.fields()) {
			if (f.isStatic()) {
				OFieldDecl fdecl = f.getDecl();
				Object value = fdecl.getValue();
				if (value == null) {
					continue;
				}
				Field ff = OTypeUtils.loadField(c, fdecl.getName());
				try {
					ff.setAccessible(true);
					ff.set(null, value);
					ff.setAccessible(false);
				} catch (IllegalAccessException e) {
					ODebug.traceException(e);
				}
			}
		}
		OValueCode[] pools = cdecl.getPooledValues();
		for (int id = 0; id < pools.length; id++) {
			Field f = OTypeUtils.loadField(c, cdecl.constFieldName(id));
			try {
				f.setAccessible(true);
				f.set(null, pools[id].getHandled());
				f.setAccessible(false);
			} catch (IllegalAccessException e) {
				ODebug.traceException(e);
			}
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class<?> foundClass = this.findLoadedClass(name);
		if (foundClass == null) {
			ClassLoader parent = this.getParent();
			try {
				foundClass = parent.loadClass(name);
			} catch (Throwable e) {
			}
		}
		if (foundClass == null) {
			foundClass = this.findClass(name);
		}
		if (resolve) {
			this.resolveClass(foundClass);
		}
		return foundClass;
	}

	public final Class<?> getCompiledClass(String cname) {
		try {
			return Class.forName(cname, true, this);
		} catch (ClassNotFoundException e) {
			// ODebug.traceException(e);
		}
		return null;
	}

	public boolean traceMode = true;
	private String dumpDirectory = null;

	private void dump(String className, byte[] byteCode) {
		if (ODebug.enabled && (traceMode || dumpDirectory != null)) {
			int index = className.lastIndexOf('.');
			String classFileName = className.substring(index + 1) + ".class";
			if (dumpDirectory != null) {
				classFileName = dumpDirectory + "/" + classFileName;
			}
			try (FileOutputStream stream = new FileOutputStream(classFileName)) {
				stream.write(byteCode);
				stream.close();
				if (dumpDirectory == null) {
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

	private int envId = 0;

	public final Class<?> entryPoint(OEnv env, Object o) {
		String cname = "$" + (envId++);
		OClassDeclType ct = newType(env, new OAnno("public"), cname, null, env.t(Object.class));
		ct.addField(new OAnno("public,static"), env.t(o.getClass()), "entry", null);
		// OMethodHandle mh = ct.addTest(env, "test", new DynamicCallCode(env,
		// new OFuncCallSite(), "??", "name"));
		Class<?> c = this.getCompiledClass(cname);
		Field f = OTypeUtils.loadField(c, "entry");
		OTypeUtils.setStaticField(f, o);
		// try {
		// mh.eval(env);
		// } catch (Throwable e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		return c;
	}

	// uniquename

	HashMap<String, String> shortMap = new HashMap<>();
	HashMap<Character, Integer> countMap = new HashMap<>();

	public final String shortName(OType t) {
		String desc = t.typeDesc(0);
		String shortName = shortMap.get(desc);
		if (shortName == null) {
			Character firstChar = t.getName().charAt(0);
			shortName = String.valueOf(firstChar);
			Integer c = countMap.get(firstChar);
			if (c == null) {
				countMap.put(firstChar, 0);
			} else {
				c = c + 1;
				shortName = c + shortName;
				countMap.put(firstChar, c);
			}
			shortMap.put(desc, shortName);
		}
		return shortName;
	}

	public final String uniqueName(String prefix, OType... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		sb.append("$");
		for (OType t : params) {
			sb.append(shortName(t));
		}
		return sb.toString();
	}

	public final String uniqueName(String prefix, int id, OType... params) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		sb.append("$$");
		sb.append(1000 + id);
		sb.append("$");
		for (OType t : params) {
			sb.append(shortName(t));
		}
		return sb.toString();
	}

	/* message */

	private ArrayList<String> messageList = new ArrayList<>();

	public final int getMessageId(String message) {
		messageList.add(message);
		return messageList.size() - 1;
	}

	public final String getMessageById(int md) {
		return messageList.get(md);
	}

}