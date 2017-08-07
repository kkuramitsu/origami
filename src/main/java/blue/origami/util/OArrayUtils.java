/// ***********************************************************************
// * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// ***********************************************************************/
//
// package blue.origami.util;
//
// import java.lang.reflect.Array;
//
// import blue.origami.lang.type.OType;
//
// public interface OArrayUtils {
// public static String[] emptyNames = new String[0];
// public static OType[] emptyTypes = new OType[0];
//
// public default <T> T[] append(T first, @SuppressWarnings("unchecked") T...
/// params) {
// @SuppressWarnings("unchecked") //
// T[] v = (T[]) Array.newInstance(params.getClass().getComponentType(),
/// params.length + 1);
// v[0] = first;
// System.arraycopy(params, 0, v, 1, params.length);
// return v;
// }
//
// public default <T> T[] append(T[] params, T last) {
// @SuppressWarnings("unchecked") //
// T[] v = (T[]) Array.newInstance(params.getClass().getComponentType(),
/// params.length + 1);
// System.arraycopy(params, 0, v, 0, params.length);
// v[params.length] = last;
// return v;
// }
//
// public default <T> T[] slice(T[] params, int start, int end) {
// @SuppressWarnings("unchecked") //
// T[] v = (T[]) Array.newInstance(params.getClass().getComponentType(), end -
/// start);
// System.arraycopy(params, start, v, 0, end - start);
// return v;
// }
//
// public default <T> T[] insert(T[] params, int n, T val) {
// if (n == 0) {
// return append(val, params);
// }
// @SuppressWarnings("unchecked") //
// T[] a = (T[]) Array.newInstance(params.getClass().getComponentType(),
/// params.length + 1);
// System.arraycopy(params, 0, a, 0, n);
// a[n] = val;
// System.arraycopy(params, n, a, n + 1, params.length - n);
// return a;
// }
//
// public default <T> T[] ltrim(T[] values) {
// @SuppressWarnings("unchecked")
// T[] v = (T[]) Array.newInstance(values.getClass().getComponentType(),
/// values.length - 1);
// System.arraycopy(values, 1, v, 0, v.length);
// return v;
// }
//
// public default <T> T[] rtrim(T[] values) {
// @SuppressWarnings("unchecked")
// T[] v = (T[]) Array.newInstance(values.getClass().getComponentType(),
/// values.length - 1);
// System.arraycopy(values, 0, v, 0, v.length);
// return v;
// }
//
// public default <T, S> S[] map(T[] a, S[] b, java.util.function.Function<T, S>
/// f) {
// for (int i = 0; i < a.length; i++) {
// b[i] = f.apply(a[i]);
// }
// return b;
// }
//
// }
