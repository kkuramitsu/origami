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

package blue.origami.lang;

import java.util.ArrayList;
import java.util.List;

import blue.nez.ast.Tree;
import blue.origami.asm.OAnno;
import blue.origami.asm.OCompilationUnit;
import blue.origami.asm.code.LoadArgCode;
import blue.origami.asm.code.LoadThisCode;
import blue.origami.lang.OEnv.OListMatcher;
import blue.origami.lang.type.OParamVarType;
import blue.origami.lang.type.OType;
import blue.origami.ocode.ConstructorInvocationCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.MultiCode;
import blue.origami.ocode.ReturnCode;
import blue.origami.ocode.ValueCode;
import blue.origami.rule.OSymbols;
import blue.origami.rule.TypeAnalysis;

public class OClassDecl implements OCompilationUnit, OSymbols, TypeAnalysis {
	private final OEnv definedClassEnv;
	private final String cname;
	private final OAnno anno;
	private final OClassDeclType thisType;
	private final OType[] paramTypes;
	private final OType superType;
	private final OType[] interfaces;
	private ArrayList<OField> fields = null;
	private ArrayList<OMethodHandle> methods = null;

	private Tree<?> body = null;

	public OClassDecl(OEnv env, OClassDeclType thisType, OAnno anno, String cname, OType[] paramTypes, OType superType,
			OType... interfaces) {
		this.definedClassEnv = env.newEnv();
		this.thisType = thisType;
		this.anno = anno;
		this.cname = cname;
		this.paramTypes = paramTypes == null ? OType.emptyTypes : paramTypes;
		this.superType = superType;
		this.interfaces = interfaces;
		this.definedClassEnv.add(OClassDecl.class, this);
	}

	public String getName() {
		return cname;
	}

	public String getLocalName() {
		int loc = cname.lastIndexOf('.');
		if (loc > 0) {
			return cname.substring(loc + 1);
		}
		return cname;
	}

	public OType[] getParamTypes() {
		return this.paramTypes;
	}

	public OType getSupertype() {
		return this.superType;
	}

	public OType[] getInterfaces() {
		return interfaces;
	}

	public OField[] fields() {
		if (fields == null) {
			return OField.emptyFields;
		}
		return fields.toArray(new OField[fields.size()]);
	}

	/* Don't change to public */
	void add(OField f) {
		if (fields == null) {
			this.fields = new ArrayList<>();
		}
		this.fields.add(f);
		assert (f.getDeclaringClass() == this.thisType);
	}

	public final String uniqueFieldName(String name) {
		for (OField f : fields()) {
			if (f.matchName(name)) {
				return name + "_" + fields.size();
			}
		}
		return name;
	}

	public final String uniqueMethodName(String name) {
		for (OMethodHandle mh : methods()) {
			if (mh.matchName(name)) {
				return name + "_" + methods.size();
			}
		}
		return name;
	}

	public OMethodHandle[] methods() {
		if (methods == null) {
			return OMethodHandle.emptyMethods;
		}
		return methods.toArray(new OMethodHandle[methods.size()]);
	}

	/* Don't change to public */
	void add(OMethodHandle mh) {
		if (methods == null) {
			this.methods = new ArrayList<>();
		}
		assert (mh.getDeclaringClass() == this.thisType);
		this.methods.add(mh);
	}

	public OMethodHandle[] getConstructors() {
		// typeCheck();
		ArrayList<OMethodHandle> l = new ArrayList<>();
		for (OMethodHandle m : methods()) {
			if (m.isSpecial()) {
				l.add(m);
			}
		}
		if (l.size() > 0) {
			return l.toArray(new OMethodHandle[l.size()]);
		}
		return OMethodHandle.emptyMethods;
	}

	public OField getDeclaredField(String name) {
		// typeCheck();
		for (OField f : fields()) {
			if (f.matchName(name)) {
				return f;
			}
		}
		return null;
	}

	public OField[] getDeclaredFields() {
		// typeCheck();
		return fields.toArray(new OField[fields.size()]);
	}

	public OMethodHandle[] getDeclaredMethods(String name) {
		// typeCheck();
		ArrayList<OMethodHandle> l = new ArrayList<>();
		for (OMethodHandle mm : methods()) {
			if (name == null || mm.matchName(name)) {
				l.add(mm);
			}
		}
		if (l.size() > 0) {
			return l.toArray(new OMethodHandle[l.size()]);
		}
		return OMethodHandle.emptyMethods;
	}

	public void listMethodHandle(String name, List<OMethodHandle> l, OListMatcher<OMethodHandle> f) {
		if (name.equals("<init>")) {
			if (!this.getType().isInterface()) {
				this.addDefaultConstructors();
			}
		} else {
			for (OField ff : fields()) {
				if (ff.matchName(name)) {
					OMethodHandle mh = new OGetter(ff);
					if (f.isMatched(mh)) {
						l.add(mh);
					}
				}
			}
		}
		for (OMethodHandle mh : methods()) {
			if (mh.matchName(name) && f.isMatched(mh)) {
				l.add(mh);
			}
		}
	}

	public String getSignature() {
		StringBuilder sb = new StringBuilder();
		OType[] p = this.getParamTypes();
		if (p.length > 0) {
			sb.append("<");
			for (OType t : p) {
				OParamVarType v = (OParamVarType) t;
				sb.append(v.getLocalName());
				sb.append(":");
				v.getUpperBoundType().typeDesc(sb, 2);
			}
			sb.append(">");
		}
		this.getSupertype().typeDesc(sb, 2);
		for (OType inf : this.getInterfaces()) {
			inf.typeDesc(sb, 2);
		}
		return sb.toString();
	}

	public String getSourceName() {
		// TODO Auto-generated method stub
		return null;
	}

	private ArrayList<ValueCode> constList;

	public int poolConstValue(ValueCode value) {
		if (this.constList == null) {
			this.constList = new ArrayList<>();
		}
		if (constList.contains(value)) {
			return constList.indexOf(value);
		}
		int id = constList.size();
		constList.add(value);
		return id;
	}

	public ValueCode[] getPooledValues() {
		if (constList == null) {
			return new ValueCode[0];
		}
		return constList.toArray(new ValueCode[constList.size()]);
	}

	public String constFieldName(int id) {
		return "_pooled" + id;
	}

	public OEnv env() {
		return this.definedClassEnv;
	}

	public OClassDeclType getType() {
		return this.thisType;
	}

	public OAnno getAnno() {
		return this.anno;
	}

	public void addBody(Tree<?> body) {
		this.body = null;
	}

	public void typeCheck() {
		if (this.body != null) {
			OCode c = typeStmt(this.env(), this.body);
			for (OField f : fields()) {
				f.getDecl().typeCheck(this.env());
			}
			boolean hasConstructor = false;
			for (OMethodHandle mh : methods()) {
				if (mh.isSpecial()) {
					hasConstructor = true;
				}
				mh.getDecl().typeCheck(this.env());
			}
			if (!hasConstructor) {
				addDefaultConstructors();
			}
			this.body = null;
		}
	}

	public void addDefaultConstructors() {
		boolean hasConstructor = false;
		for (OMethodHandle mh : methods()) {
			if (mh.isSpecial()) {
				hasConstructor = true;
			}
		}
		if (hasConstructor) {
			return;
		}
		OType superType = this.getSupertype();
		for (OMethodHandle m : superType.getConstructors()) {
			OType[] p = m.getParamTypes();
			OCode[] args = new OCode[p.length + 1];
			args[0] = new LoadThisCode(thisType);
			for (int i = 0; i < p.length; i++) {
				args[i + 1] = new LoadArgCode(i, p[i]);
			}
			OCode body = new MultiCode(new ConstructorInvocationCode(m, args), new ReturnCode(env()));
			thisType.addConstructor(new OAnno("public"), m.getParamNames(), m.getParamTypes(), m.getExceptionTypes(),
					body);
		}
	}

	public OField addField(OAnno anno, OType type, String name, OCode body) {
		return this.getType().addField(anno, type, name, body);
	}

	public OMethodHandle addConstructorCode(OAnno anno, String[] paramNames, OType[] paramTypes, OType[] exceptions,
			OCode body) {
		return this.getType().addConstructor(anno, paramNames, paramTypes, exceptions, body);
	}

	public OMethodHandle addMethod(OAnno anno, OType ret, String name, String[] paramNames, OType[] paramTypes,
			OType[] exceptions, OCode body) {
		return this.getType().addMethod(anno, ret, name, paramNames, paramTypes, exceptions, body);
	}

}
