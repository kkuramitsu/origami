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

package blue.origami.rule;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import blue.nez.ast.Source;
import blue.nez.ast.SourcePosition;
import blue.nez.ast.Tree;
import blue.nez.parser.Parser;
import blue.nez.parser.ParserSource;
import blue.nez.parser.TreeConnector;
import blue.nez.parser.TreeConstructor;
import blue.origami.asm.OAnno;
import blue.origami.ffi.OAlias;
import blue.origami.ffi.OCast;
import blue.origami.ffi.OImportable;
import blue.origami.ffi.OSimpleImportable;
import blue.origami.lang.OClassDeclType;
import blue.origami.lang.OConv;
import blue.origami.lang.OEnv;
import blue.origami.lang.OField;
import blue.origami.lang.OGlobalVariable;
import blue.origami.lang.OMethod;
import blue.origami.lang.OTypeName;
import blue.origami.lang.type.OLocalClassType;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;
import blue.origami.ocode.OCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.MultiCode;
import blue.origami.ocode.ReturnCode;
import blue.origami.util.ODebug;
import blue.origami.util.OTree;
import blue.origami.util.OTypeUtils;

public interface ScriptAnalysis {
	public static String parser = " parser";
	public static String InteractiveMode = " interactiveMode";

	public default boolean isInteractiveMode(OEnv env) {
		Boolean b = env.get(InteractiveMode, Boolean.class);
		return b == null ? false : b;
	}

	public default void setInteractiveMode(OEnv env, boolean t) {
		env.set(InteractiveMode, Boolean.class, (Boolean) t);
	}

	// public default Parser getParser(OEnv env) {
	// return env.get(Parser.class);
	// }
	//
	// public default void setParser(OEnv env, Parser p) {
	// env.add(Parser.class, p);
	// assert (p == getParser(env));
	// }

	public static Set<String> symbols(String... names) {
		Set<String> set = new HashSet<>();
		for (String s : names) {
			set.add(s);
		}
		return set;
	}

	public static Set<String> AllSubSymbols = symbols("*");
	public static Set<String> NoSubSymbols = null;

	public default void importClass(OEnv env, SourcePosition s, String path, Set<String> names) {
		Class<?> c = null;
		try {
			c = Class.forName(path);
		} catch (ClassNotFoundException e) {
			throw new ErrorCode(env, OFmt.unfound_class__YY0_by_YY1, path, e);
		}
		importClass(env, s, c, names);
	}

	public default void importClass(OEnv env, SourcePosition s, Class<?> c, Set<String> names) {
		this.importClass(env, s, c, null, names);
	}

	public default void importClass(OEnv env, SourcePosition s, Class<?> c, String alias, Set<String> names) {
		if (OSimpleImportable.class.isAssignableFrom(c)) {
			Object value = OTypeUtils.newInstance(c);
			env.add(s, alias != null ? alias : c.getSimpleName(), value);
			return;
		}
		if (OImportable.class.isAssignableFrom(c)) {
			OImportable mul = (OImportable) OTypeUtils.newInstance(c);
			mul.importDefined(env, s, names);
			return;
		}
		if (names == NoSubSymbols) {
			String name = c.getSimpleName();
			name = alias != null ? alias : name;
			addType(env, s, name, c);
			OAlias a = c.getAnnotation(OAlias.class);
			if (a != null) {
				addType(env, s, a.name(), c);
			}
			importClassMethod(env, s, c);
			return;
		}
		boolean allNames = names.contains("*");
		for (Field f : c.getFields()) {
			if (!OTypeUtils.isPublicStatic(f)) {
				continue;
			}
			String name = f.getName();
			if (!allNames && !names.contains(name)) {
				continue;
			}
			addStaticField(env, s, name, f);
		}
		for (Method m : c.getDeclaredMethods()) {
			if (!OTypeUtils.isPublicStatic(m)) {
				continue;
			}
			String name = m.getName();
			if (!allNames && !names.contains(name)) {
				continue;
			}
			addStaticMethod(env, s, name, m);
		}
	}

	public default Class<?> importClassMethod(OEnv env, SourcePosition s, Class<?> c) {
		for (Method m : c.getDeclaredMethods()) {
			// ODebug.trace("method %s", m);
			if (!OTypeUtils.isPublic(m)) {
				continue;
			}
			OCast ca = m.getAnnotation(OCast.class);
			if (ca != null) {
				// ODebug.trace("conv %s", m);
				OConv.addConv(env, s, ca.cost(), m);
			}
			OAlias a = m.getAnnotation(OAlias.class);
			if (a != null) {
				OMethod mh = new OMethod(env, m);
				mh.setLocalName(a.name());
				env.add(s, a.name(), mh);
				// ODebug.trace("op %s %s", a.name(), mh);
			}
		}
		return c;
	}

	public default void addType(OEnv env, SourcePosition s, String name, Class<?> c) {
		if (name.equals(c.getSimpleName())) {
			env.add(s, name, env.t(c));
		} else {
			OTypeSystem ts = env.getTypeSystem();
			OType t = new OLocalClassType(ts, c, name, null);
			if (!ts.isDefined(c)) {
				ts.define(c, t);
			}
			env.add(s, name, t);
		}
		importClassMethod(env, s, c);
	}

	public default void addName(OEnv env, SourcePosition s, OType t, String... names) {
		for (String n : names) {
			env.add(s, n, OTypeName.newEntry(t));
		}
	}

	public default void addStaticField(OEnv env, SourcePosition s, String name, Field f) {
		env.add(s, name, new OGlobalVariable(new OField(env, f)));
	}

	public default void addStaticMethod(OEnv env, SourcePosition s, String name, Method m) {
		OCast conv = m.getAnnotation(OCast.class);
		if (conv != null) {
			OConv.addConv(env, s, conv.cost(), m);
			return;
		}
		OMethod mh = new OMethod(env, m);
		OAlias op = m.getAnnotation(OAlias.class);
		if (op != null) {
			name = op.name();
			mh.setLocalName(name);
		}
		ODebug.trace("name %s %s", name, mh);
		env.add(s, name, mh);
	}

	@SuppressWarnings("unchecked")
	public default Tree<?> loadScriptFile(OEnv env, Tree<?> t, String file) throws IOException {
		String path = file;
		if (t != null) {
			if (!file.startsWith("/") && !file.startsWith("\\")) {
				Source s = t.getSource();
				if (s != null) {
					path = SourcePosition.extractFilePath(s.getResourceName()) + "/" + file;
				}
			}
		} else {
			t = new OTree();
		}
		Source sc = ParserSource.newFileSource(path, null);
		Parser p = env.get(Parser.class);
		return p.parse(sc, 0, (TreeConstructor<Tree<?>>) t, (TreeConnector<Tree<?>>) t);
	}

	public default Tree<?> loadScriptFile(OEnv env, String file) throws IOException {
		return loadScriptFile(env, null, file);
	}

	public static Object eval(OEnv env, OCode... codes) throws Throwable {
		OClassDeclType ct = OClassDeclType.currentType(env);
		OCode code = codes.length == 0 ? codes[0] : new MultiCode(codes);
		ct.addMethod(new OAnno("public,static"), code.getType(), "f", OType.emptyNames, OType.emptyTypes,
				OType.emptyTypes, new ReturnCode(env, code));
		ODebug.setDebug(true);
		Class<?> c = ct.unwrap(env);
		ODebug.setDebug(false);
		Method m = OTypeUtils.loadMethod(c, "f");
		try {
			return m.invoke(null);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

}
