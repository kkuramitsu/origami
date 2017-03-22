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

package origami.lang;

import java.lang.reflect.Modifier;
import java.util.List;

import org.objectweb.asm.Type;

import origami.ODebug;
import origami.OEnv;
import origami.OEnv.OListMatcher;
import origami.asm.OAnno;
import origami.asm.OClassLoader;
import origami.code.OCode;
import origami.type.OTypeSystemType;
import origami.util.OTypeUtils;
import origami.type.OType;
import origami.type.OTypeSystem;
import origami.type.OParamType;

public class OClassDeclType extends OTypeSystemType {

	private Class<?> wrapped;
	private OClassDecl cdecl;

	public OClassDeclType(OEnv env, OAnno anno, String cname, OType[] paramTypes, OType superType,
			OType... interfaces) {
		super(env.getTypeSystem());
		this.wrapped = null;
		this.cdecl = new OClassDecl(env, this, anno, cname, paramTypes, superType, interfaces);
	}

	public OClassDecl getDecl() {
		return cdecl;
	}

	@Override
	public Class<?> unwrap() {
		if (wrapped != null) {
			return wrapped;
		}
		throw new RuntimeException("unimplemented ClassDeclType methods");
	}

	@Override
	public Class<?> unwrapOrNull(Class<?> c) {
		if (wrapped != null) {
			return wrapped;
		}
		return c;
	}

	@Override
	public Class<?> unwrap(OEnv env) {
		if (wrapped == null) {
			OClassLoader cl = env.getClassLoader();
			wrapped = cl.getCompiledClass(getName());
			cdecl = null;
		}
		return wrapped;
	}

	@Override
	public String getName() {
		if (wrapped != null) {
			return wrapped.getName();
		}
		return cdecl.getName();
	}

	@Override
	public String getLocalName() {
		if (wrapped != null) {
			return wrapped.getSimpleName();
		}
		return cdecl.getLocalName();
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		if (wrapped != null) {
			sb.append(Type.getDescriptor(unwrap()));
		} else {
			sb.append("L");
			sb.append(this.getName().replace('.', '/'));
			sb.append(";");
		}
	}

	@Override
	public boolean isOrigami() {
		return true;
	}

	@Override
	public Type asmType() {
		return Type.getType(this.typeDesc(1));
	}

	@Override
	public boolean eq(OType t) {
		return (this == t || this.getName().equals(t.getName()));
	}

	@Override
	public OType getSupertype() {
		if (wrapped != null) {
			return newType(wrapped.getSuperclass());
		}
		return cdecl.getSupertype().getBaseType();
	}

	@Override
	public OType getGenericSupertype() {
		if (wrapped != null) {
			return newType(wrapped.getGenericSuperclass());
		}
		return cdecl.getSupertype();
	}

	@Override
	public OType[] getInterfaces() {
		if (wrapped != null) {
			return newTypes(this.unwrap().getInterfaces());
		}
		return cdecl.getInterfaces();
	}

	@Override
	public OType[] getGenericInterfaces() {
		if (wrapped != null) {
			return newTypes(this.unwrap().getGenericInterfaces());
		}
		return cdecl.getInterfaces();
	}

	@Override
	public boolean isAssignableFrom(OType a) {
		if (wrapped != null) {
			return wrapped.isAssignableFrom(a.unwrap());
		}
		ODebug.TODO();
		return false;
	}

	@Override
	public OMethodHandle[] getConstructors() {
		if (wrapped != null) {
			return super.getConstructors();
		}
		return cdecl.getConstructors();
	}

	@Override
	public OField[] getDeclaredFields() {
		if (wrapped != null) {
			return super.getDeclaredFields();
		}
		return cdecl.getDeclaredFields();
	}

	@Override
	public OField getDeclaredField(String name) {
		if (wrapped != null) {
			return super.getDeclaredField(name);
		}
		return cdecl.getDeclaredField(name);
	}

	@Override
	public OMethodHandle[] getDeclaredMethods(String name) {
		if (wrapped != null) {
			return super.getDeclaredMethods(name);
		}
		return cdecl.getDeclaredMethods(name);
	}

	@Override
	public void listMatchedMethods(String name, List<OMethodHandle> l, OListMatcher<OMethodHandle> f) {
		if (wrapped != null) {
			OTypeUtils.listMatchedMethods(this.getTypeSystem(), wrapped, name, l, f);
		} else {
			cdecl.listMethodHandle(name, l, f);
		}
	}

	@Override
	public boolean isInterface() {
		return Modifier.isInterface(cdecl.getAnno().acc());
	}

	@Override
	public boolean is(Class<?> c) {
		if (wrapped != null) {
			return wrapped == c;
		}
		return false; /* reason : class is not compiled */
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isInstance(Object o) {
		return false; /* reason: class is not compiled */
	}

	@Override
	public OType[] getParamTypes() {
		if (this.wrapped != null) {
			return super.getParamTypes();
		}
		return this.cdecl.getParamTypes();
	}

	@Override
	public OType toGenericType() {
		if (wrapped == null) {
			return this;
		}
		return OParamType.of(this, this.getParamTypes());
	}

	// Utilties

	public OMethodHandle addConstructor(OAnno anno, String[] paramNames, OType[] paramTypes, OType[] exceptions,
			OCode body) {
		OTypeSystem ts = this.getTypeSystem();
		OMethodDecl mdecl = new OMethodDecl(this, anno, ts.newType(void.class), "<init>", paramNames, paramTypes,
				exceptions, body);
		OMethodHandle mh = new OConstructor(mdecl.getTypeSystem(), null, null, mdecl);
		this.getDecl().add(mh);
		return mh;
	}

	public OField addField(OAnno anno, OType ret, String name, OCode body) {
		OField f = new OField(this, anno, ret, name, body);
		this.getDecl().add(f);
		return f;
	}

	public OMethodHandle addMethod(OAnno anno, OType ret, String name, String[] paramNames, OType[] paramTypes,
			OType[] exceptions, OCode body) {
		OMethodDecl mdecl = new OMethodDecl(this, anno, ret, name, paramNames, paramTypes, exceptions, body);
		OMethodHandle mh = new OMethod(mdecl.getTypeSystem(), null, null, mdecl);
		this.getDecl().add(mh);
		return mh;
	}

	public OMethodHandle addTest(OEnv env, String name, OCode body) {
		OMethodDecl mdecl = new OMethodDecl(this, new OAnno("public,static"), body.getType(), name, null,
				OType.emptyTypes, OType.emptyTypes, body);
		OMethodHandle mh = new OMethod(mdecl.getTypeSystem(), null, null, mdecl);
		this.getDecl().add(mh);
		return mh;
	}

	public static OClassDeclType currentType(OEnv env) {
		return env.getClassLoader().currentClassDecl(env).getType();
	}

}
