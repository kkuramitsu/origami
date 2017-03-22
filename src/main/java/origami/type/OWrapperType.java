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

package origami.type;

import org.objectweb.asm.Type;

import origami.code.OCode;
import origami.lang.OEnv;
import origami.lang.OField;
import origami.lang.OMethodHandle;
import origami.util.StringCombinator;

public interface OWrapperType extends OType, StringCombinator {

	/* interface */
	public OType thisType();

	@Override
	public default OTypeSystem getTypeSystem() {
		return thisType().getTypeSystem();
	}

	@Override
	public default Class<?> unwrap() {
		return thisType().unwrap();
	}

	@Override
	public default Class<?> unwrapOrNull(Class<?> c) {
		return thisType().unwrapOrNull(c);
	}

	@Override
	public default Class<?> unwrap(OEnv env) {
		return thisType().unwrap(env);
	}

	@Override
	public default String getLocalName() {
		return thisType().getLocalName();
	}

	// unwrap()

	@Override
	public default String getName() {
		return thisType().getName();
	}

	@Override
	public default Type asmType() {
		return thisType().asmType();
	}

	@Override
	public default void typeDesc(StringBuilder sb, int levelGeneric) {
		thisType().typeDesc(sb, levelGeneric);
	}

	@Override
	public default OCode accept(OEnv env, OCode node, TypeChecker ext) {
		return thisType().accept(env, node, ext);
	}

	@Override
	public default OCode newCastCode(OEnv env, OCode code) {
		return thisType().newCastCode(env, code);
	}

	@Override
	public default boolean isPrimitive() {
		return thisType().isPrimitive();
	}

	@Override
	public default boolean isArray() {
		return thisType().isArray();
	}

	@Override
	public default boolean isInterface() {
		return thisType().isInterface();
	}

	@Override
	public default String getFuncName() {
		return thisType().getFuncName();
	}

	@Override
	public default String rename(String name) {
		return thisType().rename(name);
	}

	@Override
	public default boolean isNullable() {
		return thisType().isNullable();
	}

	@Override
	public default boolean isMutable() {
		return thisType().isMutable();
	}

	@Override
	public default boolean isImmutable() {
		return thisType().isImmutable();
	}

	@Override
	public default boolean isUntyped() {
		return thisType().isUntyped();
	}

	@Override
	public default boolean isDynamic() {
		return thisType().isDynamic();
	}

	@Override
	public default boolean is(Class<?> c) {
		return thisType().is(c);
	}

	@Override
	public default boolean isA(Class<?> c) {
		return thisType().isA(c);
	}

	@Override
	public default boolean eq(OType t) {
		return thisType().eq(t);
	}

	@Override
	public default OType getSupertype() {
		return thisType().getSupertype();
	}

	@Override
	public default OType getGenericSupertype() {
		return thisType().getGenericSupertype();
	}

	@Override
	public default OType[] getInterfaces() {
		return thisType().getInterfaces();
	}

	@Override
	public default OType[] getGenericInterfaces() {
		return thisType().getGenericInterfaces();
	}

	@Override
	public default boolean isAssignableFrom(OType a) {
		return thisType().isAssignableFrom(a);
	}

	@Override
	public default boolean isInstance(Object o) {
		return thisType().isInstance(o);
	}

	@Override
	public default OType getBaseType() {
		return thisType().getBaseType();
	}

	@Override
	public default OType[] getParamTypes() {
		return thisType().getParamTypes();
	}

	@Override
	public default OType toGenericType() {
		return thisType().toGenericType();
	}

	@Override
	public default OType resolveVarType(OVarDomain dom) {
		return thisType().resolveVarType(dom);
	}

	@Override
	public default OType matchVarType(OType a, boolean subMatch, OVarDomain dom) {
		return thisType().matchVarType(a, subMatch, dom);
	}

	@Override
	public default OType valueType() {
		return thisType().valueType();
	}

	@Override
	public default Object getDefaultValue() {
		return thisType().getDefaultValue();
	}

	@Override
	public default OMethodHandle[] getConstructors() {
		return thisType().getConstructors();
	}

	@Override
	public default OField[] getDeclaredFields() {
		return thisType().getDeclaredFields();
	}

	@Override
	public default OField getDeclaredField(String name) {
		return thisType().getDeclaredField(name);
	}

	@Override
	public default OMethodHandle[] getDeclaredMethods(String name) {
		return thisType().getDeclaredMethods(name);
	}

	@Override
	public default OCode newConstructorCode(OEnv env, OCode... params) {
		return thisType().newConstructorCode(env, params);
	}

	@Override
	public default OCode newGetterCode(OEnv env, OCode recv, String name) {
		return thisType().newGetterCode(env, recv, name);
	}

	@Override
	public default OCode newStaticGetterCode(OEnv env, String name) {
		return thisType().newStaticGetterCode(env, name);
	}

}
