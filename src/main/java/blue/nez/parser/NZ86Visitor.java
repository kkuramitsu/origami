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

package blue.nez.parser;

public abstract class NZ86Visitor {

	/* Machine Control */
	public abstract void visitNop(NZ86.Nop inst);

	public abstract void visitExit(NZ86.Exit inst);

	public abstract void visitCov(NZ86.Trap inst);

	public abstract void visitTrap(NZ86.Trap inst);

	/* Control */

	public abstract void visitPos(NZ86.Pos inst); // Pos

	public abstract void visitBack(NZ86.Back inst); // Back

	public abstract void visitMove(NZ86.Move inst); //

	public abstract void visitJump(NZ86.Jump inst); // Jump

	public abstract void visitCall(NZ86.Call inst); // Call

	public abstract void visitRet(NZ86.Ret inst); // Ret

	public abstract void visitAlt(NZ86.Alt inst); // Alt

	public abstract void visitSucc(NZ86.Succ inst); // Succ

	public abstract void visitFail(NZ86.Fail inst); // Fail

	public abstract void visitGuard(NZ86.Guard inst); // Skip

	public abstract void visitStep(NZ86.Step inst); // Skip

	/* Matching */

	public abstract void visitByte(NZ86.Byte inst); // match a byte character

	public abstract void visitAny(NZ86.Any inst); // match any

	public abstract void visitStr(NZ86.Str inst); // match string

	public abstract void visitSet(NZ86.Set inst); // match set

	public abstract void visitNByte(NZ86.NByte inst); //

	public abstract void visitNAny(NZ86.NAny inst); //

	public abstract void visitNStr(NZ86.NStr inst); //

	public abstract void visitNSet(NZ86.NSet inst); //

	public abstract void visitOByte(NZ86.OByte inst); //

	// public abstract void visitOAny(Moz.OAny inst); //

	public abstract void visitOStr(NZ86.OStr inst); //

	public abstract void visitOSet(NZ86.OSet inst); //

	public abstract void visitRByte(NZ86.RByte inst); //

	// public abstract void visitRAny(Moz.RAny inst); //

	public abstract void visitRStr(NZ86.RStr inst); //

	public abstract void visitRSet(NZ86.RSet inst); //

	/* Dispatch */

	public abstract void visitDispatch(NZ86.Dispatch inst); //

	public abstract void visitDDispatch(NZ86.DDispatch inst); // Dfa

	/* Matching */

	public abstract void visitTPush(NZ86.TPush inst);

	public abstract void visitTPop(NZ86.TPop inst);

	public abstract void visitTBegin(NZ86.TBegin inst);

	public abstract void visitTEnd(NZ86.TEnd inst);

	public abstract void visitTTag(NZ86.TTag inst);

	public abstract void visitTReplace(NZ86.TReplace inst);

	public abstract void visitTLink(NZ86.TLink inst);

	public abstract void visitTFold(NZ86.TFold inst);

	// public abstract void visitTStart(Moz86.TStart inst);

	public abstract void visitTEmit(NZ86.TEmit inst);

	// public abstract void visitTAbort(Moz.TAbort inst);

	/* Symbol */

	public abstract void visitSOpen(NZ86.SOpen inst);

	public abstract void visitSClose(NZ86.SClose inst);

	public abstract void visitSMask(NZ86.SMask inst);

	public abstract void visitSDef(NZ86.SDef inst);

	public abstract void visitSIsDef(NZ86.SIsDef inst);

	public abstract void visitSExists(NZ86.SExists inst);

	public abstract void visitSMatch(NZ86.SMatch inst);

	public abstract void visitSIs(NZ86.SIs inst);

	public abstract void visitSIsa(NZ86.SIsa inst);

	/* Number */

	public abstract void visitNScan(NZ86.NScan inst);

	public abstract void visitNDec(NZ86.NDec inst);

	/* memoization */

	public abstract void visitLookup(NZ86.Lookup inst); // match a character

	public abstract void visitMemo(NZ86.Memo inst); // match a character

	public abstract void visitMemoFail(NZ86.MemoFail inst); // match a
															// character

	public abstract void visitTLookup(NZ86.TLookup inst);

	public abstract void visitTMemo(NZ86.TMemo inst);

}
