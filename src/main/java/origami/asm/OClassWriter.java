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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.FieldNode;

import origami.ffi.OMutable;
import origami.ffi.ONullable;
import origami.lang.OClassDecl;
import origami.lang.OMethodDecl;
import origami.lang.type.OType;
import origami.util.OArrayUtils;
import origami.util.OStackable;

public class OClassWriter extends ClassWriter implements OStackable<OClassWriter>, Opcodes, OArrayUtils {
	private final String cname;
	public OClassDecl cdecl;

	OClassWriter(OClassDecl cdecl) {
		super(ClassWriter.COMPUTE_FRAMES);
		// super(ClassWriter.COMPUTE_MAXS);
		this.cname = cdecl.getName().replace(".", "/");
		Type superType = cdecl.getSupertype().asmType();
		String[] inames = null;
		OType[] inf = cdecl.getInterfaces();
		if (inf.length > 0) {
			inames = new String[inf.length];
			map(inf, inames, t -> t.asmType().getInternalName());
		}
		this.visit(V1_8, cdecl.getAnno().acc(), this.cname, cdecl.getSignature(), superType.getInternalName(), inames);
		this.visitSource(cdecl.getSourceName(), null);
		this.cdecl = cdecl;
	}

	public String getName() {
		return this.cname;
	}

	public Type getTypeDesc() {
		return Type.getType("L" + this.cname + ";");
	}

	public final void addField(OAnno anno, String name, OType ty, String signature, Object value) {
		// String signature = ty.desc();
		FieldNode fn = new FieldNode(anno.acc(), name, ty.typeDesc(0), signature, value);
		if (ty.isNullable()) {
			anno.setAnnotation(ONullable.class);
		}
		if (ty.isMutable()) {
			anno.setAnnotation(OMutable.class);
		}
		anno.asm(fn);
		fn.accept(this);
	}

	public final OGeneratorAdapter newGeneratorAdapter(OAnno anno, OType returnType, String name, String signature,
			OType... paramTypes) {
		// String desc = (new MethodDeclSignatureWriter(returnType,
		// paramTypes)).getDesc();
		String desc = getDesc(returnType, paramTypes);
		MethodVisitor mv = this.visitMethod(anno.acc(), name, desc, signature, null);
		// ODebug.trace("descriptor = %s\n", desc);
		// ODebug.trace("signature = %s\n", signature);
		JSRInlinerAdapter inlinerAdapter = new JSRInlinerAdapter(mv, anno.acc(), name, desc, signature, null);
		OGeneratorAdapter asapter = new OGeneratorAdapter(inlinerAdapter, anno.acc(), name, desc);
		anno.asm(asapter);
		return asapter;
	}

	public final OGeneratorAdapter newGeneratorAdapter(OMethodDecl mdecl) {
		return newGeneratorAdapter(mdecl.getAnno(), mdecl.getReturnType(), mdecl.getName(), mdecl.getSignature(),
				mdecl.getParamTypes());
	}

	public String getDesc(OType ret, OType[] p) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < p.length; i++) {
			p[i].getBaseType().typeDesc(sb, 0);
		}
		sb.append(")");
		ret.getBaseType().typeDesc(sb, 0);
		return sb.toString();
	}

	public byte[] byteCompile() {
		this.visitEnd();
		return this.toByteArray();
	}

	private OClassWriter parent = null;

	@Override
	public OClassWriter push(OClassWriter onstack) {
		this.parent = onstack;
		return this;
	}

	@Override
	public OClassWriter pop() {
		return this.parent;
	}
}
