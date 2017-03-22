package origami.xdevel;
// package origami.main.tool;
//
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
// package origami.nez.ast;
//
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
//
// import origami.trait.OStringUtils;
//
// public class TreeUtils {
//
// public final static String digestString(Tree<?> node) {
// StringBuilder sb = new StringBuilder();
// byte[] hash = digest(node);
// for (int i = 0; i < hash.length; i++) {
// int d = hash[i] & 0xff;
// // if (d < 0) {
// // d += 256;
// // }
// if (d < 16) {
// sb.append("0");
// }
// sb.append(Integer.toString(d, 16));
// }
// return sb.toString();
// }
//
// public final static byte[] digest(Tree<?> node) {
// try {
// MessageDigest md;
// md = MessageDigest.getInstance("MD5");
// updateDigest(node, md);
// return md.digest();
// } catch (NoSuchAlgorithmException e) {
// e.printStackTrace();
// }
// return new byte[16];
// }
//
// static void updateDigest(Tree<?> node, MessageDigest md) {
// md.update((byte) '#');
// md.update(OStringUtils.utf8(node.getTag().getSymbol()));
// for (int i = 0; i < node.size(); i++) {
// Symbol label = node.getLabel(i);
// if (label != null) {
// md.update((byte) '$');
// md.update(OStringUtils.utf8(label.getSymbol()));
// }
// updateDigest(node.get(i), md);
// }
// if (node.size() == 0) {
// md.update(OStringUtils.utf8(node.toText()));
// }
// }
//
// }
