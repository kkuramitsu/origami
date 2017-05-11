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

package blue.nez.parser.pasm;

public abstract class PegAsmVisitor {

	/* Machine Control */
	public abstract void visitNop(ASMnop inst);

	public abstract void visitExit(ASMexit inst);

	public abstract void visitCov(ASMtrap inst);

	public abstract void visitTrap(ASMtrap inst);

	/* Control */

	public abstract void visitPos(ASMpos inst); // Pos

	public abstract void visitBack(ASMback inst); // Back

	public abstract void visitMove(ASMmove inst); //

	public abstract void visitJump(ASMjump inst); // Jump

	public abstract void visitCall(ASMcall inst); // Call

	public abstract void visitRet(ASMret inst); // Ret

	public abstract void visitAlt(ASMalt inst); // Alt

	public abstract void visitSucc(ASMsucc inst); // Succ

	public abstract void visitFail(ASMfail inst); // Fail

	public abstract void visitGuard(ASMguard inst); // Skip

	public abstract void visitStep(ASMstep inst); // Skip

	/* Matching */

	public abstract void visitByte(ASMbyte inst); // match a byte character

	public abstract void visitAny(ASMany inst); // match any

	public abstract void visitStr(ASMstr inst); // match string

	public abstract void visitSet(ASMbset inst); // match set

	public abstract void visitNByte(ASMNbyte inst); //

	public abstract void visitNAny(ASMNany inst); //

	public abstract void visitNStr(ASMNstr inst); //

	public abstract void visitNSet(ASMNbset inst); //

	public abstract void visitOByte(ASMObyte inst); //

	// public abstract void visitOAny(Moz.OAny inst); //

	public abstract void visitOStr(ASMOstr inst); //

	public abstract void visitOSet(ASMObset inst); //

	public abstract void visitRByte(ASMRbyte inst); //

	// public abstract void visitRAny(Moz.RAny inst); //

	public abstract void visitRStr(ASMRstr inst); //

	public abstract void visitRSet(ASMRbset inst); //

	/* Dispatch */

	public abstract void visitDispatch(ASMdispatch inst); //

	public abstract void visitDDispatch(ASMdfa inst); // Dfa

	/* Matching */

	public abstract void visitTPush(ASMTpush inst);

	public abstract void visitTPop(ASMTpop inst);

	public abstract void visitTBegin(ASMTbegin inst);

	public abstract void visitTEnd(ASMTend inst);

	public abstract void visitTTag(ASMTtag inst);

	public abstract void visitTReplace(ASMTmut inst);

	public abstract void visitTLink(ASMTlink inst);

	public abstract void visitTFold(ASMTfold inst);

	// public abstract void visitTStart(Moz86.TStart inst);

	public abstract void visitTEmit(ASMTemit inst);

	// public abstract void visitTAbort(Moz.TAbort inst);

	/* Symbol */

	public abstract void visitSOpen(ASMSbegin inst);

	public abstract void visitSClose(ASMSend inst);

	public abstract void visitSDef(ASMSdef inst);

	// public abstract void visitSDef2(ASMSdef2 inst);

	// public abstract void visitSPred(ASMSpred inst);

	public abstract void visitSIsDef(ASMSpred2 inst);

	// public abstract void visitSMask(ASMSMask inst);
	// public abstract void visitSExists(ASMSexists inst);
	//
	// public abstract void visitSMatch(ASMSMatch inst);
	//
	// public abstract void visitSIs(ASMSIs inst);
	//
	// public abstract void visitSIsa(ASMSIsa inst);

	/* Number */

	public abstract void visitNScan(ASMSScan inst);

	public abstract void visitNDec(ASMSDec inst);

	/* memoization */

	public abstract void visitLookup(ASMMlookup inst); // match a character

	public abstract void visitMemo(ASMMmemoSucc inst); // match a character

	public abstract void visitMemoFail(ASMMmemoFail inst); // match a
														// character

	public abstract void visitTLookup(ASMMlookupTree inst);

	public abstract void visitTMemo(ASMMmemoTree inst);

}
