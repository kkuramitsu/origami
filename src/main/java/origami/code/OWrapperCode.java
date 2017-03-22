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

import origami.lang.OEnv;
import origami.lang.OEnv.OListMatcher;
import origami.lang.type.OType;
import origami.lang.type.OTypeSystem;
import origami.nez.ast.SourcePosition;

public interface OWrapperCode extends OCode {

	public OCode wrapped();

	void wrap(OCode code);

	@Override
	public default OType getType() {
		return wrapped().getType();
	}

	@Override
	public default boolean isUntyped() {
		return wrapped().isUntyped();
	}

	@Override
	public default OCode retypeLocal() {
		wrap(wrapped().retypeLocal());
		return this;
	}

	@Override
	public default OCode retypeAll(int[] c) {
		return wrapped().retypeAll(c);
	}

	@Override
	public default OType valueType() {
		return wrapped().valueType();
	}

	@Override
	public default OTypeSystem getTypeSystem() {
		return wrapped().getTypeSystem();
	}

	@Override
	public default OCode[] getParams() {
		return wrapped().getParams();
	}

	@Override
	public default void find(List<OCode> l, OListMatcher<OCode> mat) {
		if (mat.isMatched(this)) {
			l.add(this);
		}
		wrapped().find(l, mat);
	}

	@Override
	public default OCode setSourcePosition(SourcePosition s) {
		wrapped().setSourcePosition(s);
		return this;
	}

	@Override
	public default SourcePosition getSourcePosition() {
		return wrapped().getSourcePosition();
	}

	@Override
	public default int getMatchCost() {
		return wrapped().getMatchCost();
	}

	@Override
	public default boolean isDefined() {
		return wrapped().isDefined();
	}

	@Override
	public default boolean hasReturnCode() {
		return wrapped().hasReturnCode();
	}

	@Override
	public default Object eval(OEnv env) throws Throwable {
		return wrapped().eval(env);
	}

	@Override
	public default void generate(OGenerator gen) {
		wrapped().generate(gen);
	}

	@Override
	public default OCode thisCode() {
		return wrapped();
	}

	@Override
	public default OCode refineType(OEnv env, OType req) {
		return wrapped().refineType(env, req);
	}

	// Code(params)
	@Override
	public default OCode newApplyCode(OEnv env, OCode... params) {
		return wrapped().newApplyCode(env, params);
	}

	// Code.name(params)
	@Override
	public default OCode newMethodCode(OEnv env, String name, OCode... params) {
		return wrapped().newMethodCode(env, name, params);
	}

	// Code $op param (e.g., code + code)
	@Override
	public default OCode newBinaryCode(OEnv env, String op, OCode params) {
		return wrapped().newBinaryCode(env, op, params);
	}

	// Code $op (e.g., !code)
	@Override
	public default OCode newUnaryCode(OEnv env, String op) {
		return wrapped().newUnaryCode(env, op);
	}

	// Code.name
	@Override
	public default OCode newGetterCode(OEnv env, String name) {
		return wrapped().newGetterCode(env, name);
	}

	// Code = right
	@Override
	public default OCode newAssignCode(OEnv env, OCode right) {
		return wrapped().newAssignCode(env, right);
	}

	// (t)Code
	@Override
	public default OCode newCastCode(OEnv env, OType t) {
		return wrapped().newCastCode(env, t);
	}

	// (c)Code
	@Override
	public default OCode newCastCode(OEnv env, Class<?> c) {
		return wrapped().newCastCode(env, c);
	}

	// (t)Code
	@Override
	public default OCode boxCode(OEnv env) {
		return wrapped().boxCode(env);
	}

	@Override
	public default OCode checkAcc(OEnv env) {
		return wrapped().checkAcc(env);
	}

}
