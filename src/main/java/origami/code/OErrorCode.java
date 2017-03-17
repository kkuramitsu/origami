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
package origami.code;

import java.util.List;

import origami.nez.ast.Tree;
import origami.OConsts;
import origami.OEnv;
import origami.OEnv.OListMatcher;
import origami.OLog;
import origami.asm.OAsm;
import origami.ffi.OCast;
import origami.rule.OFormat;
import origami.type.OType;
import origami.type.OUntypedType;

public class OErrorCode extends RuntimeException implements OCode {

	private final OLog log;
	private OType ret;

	public OErrorCode(OEnv env, Tree<?> s, String fmt, Object... args) {
		super();
		log = new OLog(s, OLog.Error, fmt, args);
		this.ret = env != null ? env.t(OUntypedType.class) : null;
	}

	public OErrorCode(OEnv env, Tree<?> s, OFormat fmt, Object... args) {
		this(env, s, fmt.toString(), args);
	}

	public OErrorCode(OEnv env, String fmt, Object... args) {
		this(env, null, fmt, args);
	}

	public OLog getLog() {
		return this.log;
	}

	@Override
	public boolean isUntyped() {
		return false;
	}

	@Override
	public OCode retypeLocal() {
		return this;
	}

	@Override
	public OCode retypeAll(int[] c) {
		return this;
	}

	@Override
	public OCode[] getParams() {
		return OConsts.emptyNodes;
	}

	@Override
	public void find(List<OCode> l, OListMatcher<OCode> mat) {
		if (mat.isMatched(this)) {
			l.add(this);
		}
	}

	@Override
	public boolean isDefined() {
		return false;
	}

	@Override
	public boolean hasReturnCode() {
		return false;
	}

	@Override
	public OCode thisCode() {
		return this;
	}

	@Override
	public OType getType() {
		return ret;
	}

	@Override
	public OType valueType() {
		return this.getType();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		ret = t;
		return this;
	}

	@Override
	public OCode setSource(Tree<?> s) {
		this.log.setSource(s);
		return this;
	}

	@Override
	public Tree<?> getSource() {
		return log.s;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		throw this;
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushError(this);
	}

	@Override
	public int getMatchCost() {
		return OCast.STUPID;
	}

	// code

	@Override
	public OCode asType(OEnv env, OType t) {
		ret = t;
		return this;
	}

	@Override
	public OCode newApplyCode(OEnv env, OCode... params) {
		return this;
	}

	@Override
	public OCode newMethodCode(OEnv env, String name, OCode... params) {
		return this;
	}

	@Override
	public OCode newBinaryCode(OEnv env, String name, OCode param) {
		return this;
	}

	@Override
	public OCode newUnaryCode(OEnv env, String name) {
		return this;
	}

	@Override
	public OCode newGetterCode(OEnv env, String name) {
		return this;
	}

	@Override
	public OCode newAssignCode(OEnv env, OType type, OCode right) {
		return this;
	}

	@Override
	public OCode newCastCode(OEnv env, OType t) {
		return this;
	}

}