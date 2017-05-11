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

import blue.nez.ast.Source;
import blue.nez.ast.Symbol;
import blue.nez.parser.pasm.ASMexit;

public final class PAsmContext<T> extends ParserContext<T> {

	public PAsmContext(Source source, long pos, TreeConstructor<T> newTree, TreeConnector<T> linkTree) {
		super(source, pos, newTree, linkTree);
		this.initVM();
	}

	@Override
	public <E> PAsmContext<E> newInstance(Source source, long pos, TreeConstructor<E> newTree,
			TreeConnector<E> linkTree) {
		return new PAsmContext<>(source, pos, newTree, linkTree);
	}

	private int head_pos = 0;

	@Override
	public final void backtrack(int pos) {
		if (this.head_pos < this.pos) {
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
		return this.head_pos;
	}

	public final void setPosition(long pos) {
		this.pos = (int) pos;
	}

	// ----------------------------------------------------------------------

	public static class StackData {
		public int value;
		public Object ref;
	}

	private static int StackSize = 512;
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
		this.stacks[1].ref = new ASMexit(false);
		this.stacks[1].value = this.pos;
		this.stacks[2].ref = this.loadSymbolTable();
		this.stacks[2].value = this.loadTreeLog();
		this.stacks[3].ref = new ASMexit(true);
		this.stacks[3].value = 0;
		this.catchStackTop = 0;
		this.usedStackTop = 3;
	}

	public final StackData getUsedStackTop() {
		return this.stacks[this.usedStackTop];
	}

	public final StackData newUnusedStack() {
		this.usedStackTop++;
		if (this.stacks.length == this.usedStackTop) {
			StackData[] newstack = new StackData[this.stacks.length * 2];
			System.arraycopy(this.stacks, 0, newstack, 0, this.stacks.length);
			for (int i = this.stacks.length; i < newstack.length; i++) {
				newstack[i] = new StackData();
			}
			this.stacks = newstack;
		}
		return this.stacks[this.usedStackTop];
	}

	public final StackData popStack() {
		StackData s = this.stacks[this.usedStackTop];
		this.usedStackTop--;
		// assert(this.catchStackTop <= this.usedStackTop);
		return s;
	}

	public final int loadCatchPoint() {
		int p = this.catchStackTop;
		this.catchStackTop = this.usedStackTop - 2;
		return p;
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

	public final void xAlt(PAsmInst failjump/* op.failjump */) {
		StackData s0 = this.newUnusedStack();
		StackData s1 = this.newUnusedStack();
		StackData s2 = this.newUnusedStack();
		s0.value = this.catchStackTop;
		this.catchStackTop = this.usedStackTop - 2;
		s0.ref = this.left; // ADDED
		s1.value = this.pos;
		s1.ref = failjump;
		s2.value = this.loadTreeLog();
		s2.ref = this.loadSymbolTable();
	}

	public final void xSucc() {
		StackData s0 = this.stacks[this.catchStackTop];
		this.usedStackTop = this.catchStackTop - 1;
		this.catchStackTop = s0.value;
	}

	public final int xSuccPos() { // used in succ
		StackData s0 = this.stacks[this.catchStackTop];
		StackData s1 = this.stacks[this.catchStackTop + 1];
		this.usedStackTop = this.catchStackTop - 1;
		this.catchStackTop = s0.value;
		return s1.value;
	}

	@SuppressWarnings("unchecked")
	public final PAsmInst raiseFail() {
		StackData s0 = this.stacks[this.catchStackTop];
		StackData s1 = this.stacks[this.catchStackTop + 1];
		StackData s2 = this.stacks[this.catchStackTop + 2];
		this.usedStackTop = this.catchStackTop - 1;
		this.catchStackTop = s0.value;
		this.left = (T) s0.ref;
		this.backtrack(s1.value);
		this.storeTreeLog(s2.value);
		this.storeSymbolTable(s2.ref);
		assert (s1.ref != null);
		return (PAsmInst) s1.ref;
	}

	public final PAsmInst xStep(PAsmInst next) {
		StackData s1 = this.stacks[this.catchStackTop + 1];
		if (s1.value == this.pos) {
			return this.raiseFail();
		}
		s1.value = this.pos;
		StackData s0 = this.stacks[this.catchStackTop];
		s0.ref = this.left;
		StackData s2 = this.stacks[this.catchStackTop + 2];
		s2.value = this.loadTreeLog();
		s2.ref = this.loadSymbolTable();
		return next;
	}

	@SuppressWarnings("unchecked")
	public final void linkTree(Symbol label) {
		StackData s = this.popStack();
		this.storeTreeLog(s.value);
		this.linkTree((T) s.ref, label);
		this.storeTree((T) s.ref);
	}

	public final void pushTree() {
		StackData s = this.newUnusedStack();
		s.ref = this.loadTree();
		s.value = this.loadTreeLog();
	}

	@SuppressWarnings("unchecked")
	public final void popTree() {
		StackData s = this.popStack();
		this.storeTreeLog(s.value);
		this.storeTree((T) s.ref);
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
		this.actions[trapid].performed(this, uid);
	}

}
