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
package blue.origami.ocode;

import java.util.List;

import blue.nez.ast.LocaleFormat;
import blue.nez.ast.SourcePosition;
import blue.origami.ffi.OCast;
import blue.origami.lang.OEnv;
import blue.origami.lang.OEnv.OListMatcher;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OUntypedType;
import blue.origami.util.OConsts;
import blue.origami.util.OLog;

@SuppressWarnings("serial")
public class ErrorCode extends RuntimeException implements OCode {

	private final OLog log;
	private OType ret;

	public ErrorCode(OEnv env, SourcePosition s, LocaleFormat fmt, Object... args) {
		super();
		this.log = new OLog(s, OLog.Error, fmt, args);
		this.ret = env != null ? env.t(OUntypedType.class) : null;
	}

	public ErrorCode(OEnv env, LocaleFormat fmt, Object... args) {
		this(env, SourcePosition.UnknownPosition, fmt, args);
	}

	public ErrorCode(OEnv env, SourcePosition s, String fmt, Object... args) {
		this(env, s, LocaleFormat.wrap(fmt), args);
	}

	public ErrorCode(OEnv env, String fmt, Object... args) {
		this(env, SourcePosition.UnknownPosition, LocaleFormat.wrap(fmt), args);
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
		return this.ret;
	}

	@Override
	public OType valueType() {
		return this.getType();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		this.ret = t;
		return this;
	}

	@Override
	public OCode asAssign(OEnv env, String name) {
		return this;
	}

	@Override
	public OCode setSourcePosition(SourcePosition s) {
		this.log.setSourcePosition(s);
		return this;
	}

	@Override
	public SourcePosition getSourcePosition() {
		return this.log.s;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		throw this;
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushError(this);
	}

	@Override
	public int getMatchCost() {
		return OCast.STUPID;
	}

	// code

	@Override
	public OCode asType(OEnv env, OType t) {
		this.ret = t;
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
	public OCode newAssignCode(OEnv env, OCode right) {
		return this;
	}

	@Override
	public OCode newCastCode(OEnv env, OType t) {
		return this;
	}

}