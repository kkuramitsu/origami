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

package origami.nez.parser;

import origami.nez.ast.Source;
import origami.nez.ast.Symbol;

public final class NZ86ParserContext<T> extends ParserContext<T> {

	public NZ86ParserContext(String s, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		super(s, newTree, linkTree);
		initVM();
	}

	public NZ86ParserContext(Source source, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		super(source, newTree, linkTree);
		initVM();
	}

	@Override
	public <E> NZ86ParserContext<E> newInstance(Source source, TreeConstructor<E> newTree, TreeConnector<E> linkTree) {
		return new NZ86ParserContext<>(source, newTree, linkTree);
	}

	@Override
	public final boolean eof() {
		return this.source.eof(pos);
	}

	@Override
	public final int read() {
		return this.source.byteAt(pos++);
	}

	@Override
	public final int prefetch() {
		return this.source.byteAt(pos);
	}

	@Override
	public final boolean match(byte[] utf8) {
		if (source.match(pos, utf8)) {
			this.move(utf8.length);
			return true;
		}
		return false;
	}

	@Override
	public final byte[] subByte(int start, int end) {
		return source.subByte(start, end);
	}

	@Override
	public final byte byteAt(int pos) {
		return (byte) source.byteAt(pos);
	}

	private int head_pos = 0;

	@Override
	public final void back(int pos) {
		if (head_pos < this.pos) {
			this.head_pos = this.pos;
		}
		this.pos = pos;
	}

	@Override
	public final long getPosition() {
		return this.pos;
	}

	@Override
	public final long getMaximumPosition() {
		return head_pos;
	}

	public final void setPosition(long pos) {
		this.pos = (int) pos;
	}

	// ----------------------------------------------------------------------

	public static class StackData {
		public int value;
		public Object ref;
	}

	private static int StackSize = 64;
	private StackData[] stacks = null;
	private int usedStackTop;
	private int catchStackTop;

	public final void initVM() {
		this.stacks = new StackData[StackSize];
		for (int i = 0; i < StackSize; i++) {
			this.stacks[i] = new StackData();
		}
		this.stacks[0].ref = null;
		this.stacks[0].value = 0;
		this.stacks[1].ref = new NZ86.Exit(false);
		this.stacks[1].value = pos;
		this.stacks[2].ref = this.saveLog();
		this.stacks[2].value = this.saveSymbolPoint();
		this.stacks[3].ref = new NZ86.Exit(true);
		this.stacks[3].value = 0;
		this.catchStackTop = 0;
		this.usedStackTop = 3;
	}

	public final StackData getUsedStackTop() {
		return stacks[usedStackTop];
	}

	public final StackData newUnusedStack() {
		usedStackTop++;
		if (stacks.length == usedStackTop) {
			StackData[] newstack = new StackData[stacks.length * 2];
			System.arraycopy(stacks, 0, newstack, 0, stacks.length);
			for (int i = this.stacks.length; i < newstack.length; i++) {
				newstack[i] = new StackData();
			}
			stacks = newstack;
		}
		return stacks[usedStackTop];
	}

	public final StackData popStack() {
		StackData s = stacks[this.usedStackTop];
		usedStackTop--;
		// assert(this.catchStackTop <= this.usedStackTop);
		return s;
	}

	// Instruction

	public final void xPos() {
		StackData s = this.newUnusedStack();
		s.value = this.pos;
	}

	public final int xPPos() {
		StackData s = this.popStack();
		return s.value;
	}

	public final void xBack() {
		StackData s = this.popStack();
		this.back(s.value);
	}

	public final void xCall(String name, NZ86Instruction jump) {
		StackData s = this.newUnusedStack();
		s.ref = jump;
	}

	public final NZ86Instruction xRet() {
		StackData s = this.popStack();
		return (NZ86Instruction) s.ref;
	}

	public final void xAlt(NZ86Instruction failjump/* op.failjump */) {
		StackData s0 = newUnusedStack();
		StackData s1 = newUnusedStack();
		StackData s2 = newUnusedStack();
		s0.value = catchStackTop;
		catchStackTop = usedStackTop - 2;
		s0.ref = this.left; // ADDED
		s1.ref = failjump;
		s1.value = this.pos;
		s2.value = this.saveLog();
		s2.ref = this.saveSymbolPoint();
	}

	public final void xSucc() {
		StackData s0 = stacks[catchStackTop];
		usedStackTop = catchStackTop - 1;
		catchStackTop = s0.value;
	}

	public final int xSuccPos() { // used in succ
		StackData s0 = stacks[catchStackTop];
		StackData s1 = stacks[catchStackTop + 1];
		usedStackTop = catchStackTop - 1;
		catchStackTop = s0.value;
		return s1.value;
	}

	@SuppressWarnings("unchecked")
	public final NZ86Instruction xFail() {
		StackData s0 = stacks[catchStackTop];
		StackData s1 = stacks[catchStackTop + 1];
		StackData s2 = stacks[catchStackTop + 2];
		usedStackTop = catchStackTop - 1;
		catchStackTop = s0.value;
		this.left = (T) s0.ref;
		// if (s1.value < this.pos) {
		// // if (this.lprof != null) {
		// // this.lprof.statBacktrack(s1.value, this.pos);
		// // }
		this.back(s1.value);
		// }
		this.backLog(s2.value);
		this.backSymbolPoint((Integer) s2.ref); // FIXME slow
		assert (s1.ref != null);
		return (NZ86Instruction) s1.ref;
	}

	// public final void statFail(int memoPoint) {
	// StackData s1 = stacks[catchStackTop + 1];
	// statBack(s1.value, this.pos);
	// }

	public final NZ86Instruction xStep(NZ86Instruction next) {
		StackData s1 = stacks[catchStackTop + 1];
		if (s1.value == this.pos) {
			return xFail();
		}
		s1.value = this.pos;
		StackData s0 = stacks[catchStackTop];
		s0.ref = this.left;
		StackData s2 = stacks[catchStackTop + 2];
		s2.value = this.saveLog();
		s2.ref = this.saveSymbolPoint(); // FIXME slow
		return next;
	}

	public final void xTPush() {
		StackData s = this.newUnusedStack();
		s.ref = this.left;
		s.value = this.saveLog();
	}

	@SuppressWarnings("unchecked")
	public final void xTLink(Symbol label) {
		StackData s = this.popStack();
		this.backLog(s.value);
		this.linkTree((T) s.ref, label);
		this.left = (T) s.ref;
	}

	@SuppressWarnings("unchecked")
	public final void xTPop() {
		StackData s = this.popStack();
		this.backLog(s.value);
		this.left = (T) s.ref;
	}

	public final void xSOpen() {
		StackData s = this.newUnusedStack();
		s.value = this.saveSymbolPoint();
	}

	public final void xSClose() {
		StackData s = this.popStack();
		this.backSymbolPoint(s.value);
	}

	/* ----------------------------------------------------------------- */

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void end() {
		// TODO Auto-generated method stub

	}

	/* ----------------------------------------------------------------- */
	/* Trap */

	TrapAction[] actions;

	public final void setTrap(TrapAction[] array) {
		this.actions = array;
	}

	public final void trap(int trapid, int uid) {
		actions[trapid].performed(this, uid);
	}

}
