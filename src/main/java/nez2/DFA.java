package nez2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez2.PEG.Char;
import nez2.PEG.Expr;
import nez2.PEG.Or;
import nez2.PEG.PTag;

public class DFA extends Expr {
	byte[] charMap;
	Expr[] indexed;

	public DFA(byte[] charMap, Expr[] indexed) {
		this.ptag = PTag.DFA;
		this.charMap = charMap;
		this.indexed = indexed;
	}

	@Override
	public int size() {
		return this.indexed.length;
	}

	@Override
	public Expr get(int index) {
		return this.indexed[index];
	}

	boolean isDFA() {
		for (Expr e : this.indexed) {
			if (!DFA.car(e).isAny()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("[");
		for (int i = 0; i < this.indexed.length; i++) {
			Expr pe = this.indexed[i];
			BitChar bc = charMapSet(this.charMap, i + 1);
			if (i > 0) {
				sb.append("|");
			}
			if (car(pe).isAny()) {
				bc.strOut(sb);
				sb.append(" ");
				cdr(pe).strOut(sb);
			} else {
				sb.append("&");
				bc.strOut(sb);
				sb.append(" ");
				pe.strOut(sb);
			}
		}
		sb.append("]");
	}

	static Expr optimizeChoice(Expr pe) {
		switch (pe.ptag) {
		case Or:
			if (!pe.isOption()) {
				List<Expr> choice = new ArrayList<>(256);
				appendChoice(choice, pe, false);
				if (choice.size() == 1) {
					return choice.get(0);
				}
				return predictChoice(choice);
			}
		default:
			return PEG.dup(pe, DFA::optimizeChoice);
		}
	}

	static void appendChoice(List<Expr> choice, Expr pe, boolean leftFactoring) {
		if (pe instanceof Or) {
			appendChoice(choice, pe.get(0), leftFactoring);
			appendChoice(choice, pe.get(1), leftFactoring);
			return;
		}
		if (choice.size() > 0) {
			Expr p = choice.get(choice.size() - 1);
			if (p.isEmpty()) {
				return;
			}
			if (p.isChar() && pe.isChar()) {
				choice.set(choice.size() - 1, new Char(((BitChar) p.param(0)).union((BitChar) pe.param(0))));
				return;
			}
			if (leftFactoring) {
				// factoring common prefix
				Expr p1 = car(p);
				Expr p2 = car(pe);
				if (p1.eq(p2)) {
					choice.set(choice.size() - 1, p1.andThen(cdr(p).orElse(cdr(pe))));
					return;
				}
			}
		}
		if (!pe.isFail()) {
			choice.add(pe);
		}
	}

	static Expr car(Expr pe) {
		if (pe.ptag == PTag.Seq) {
			return pe.get(0);
		}
		return pe;
	}

	static Expr cdr(Expr pe) {
		if (pe.ptag == PTag.Seq) {
			return pe.get(1);
		}
		return PEG.Empty_;
	}

	static Expr predictChoice(List<Expr> choice) {
		BitChar[] firstSet = new BitChar[choice.size()];
		for (int i = 0; i < choice.size(); i++) {
			firstSet[i] = First.first(choice.get(i));
			System.err.printf("%d) %s %s\n", i, firstSet[i], choice.get(i));
		}
		byte[] charMap = new byte[256];
		HashMap<String, Byte> indexMap = new HashMap<>();
		List<Expr> indexed = new ArrayList<>();

		ArrayList<Expr> selected = new ArrayList<>(choice.size());
		for (int ch = 0; ch < 256; ch++) {
			StringBuilder sb = new StringBuilder();
			selected.clear();
			for (int i = 0; i < choice.size(); i++) {
				if (firstSet[i].is(ch)) {
					selected.add(choice.get(i));
					sb.append(i);
					sb.append(".");
				}
			}
			if (selected.size() == 0) {
				charMap[ch] = 0;
				continue;
			}
			String bitKey = sb.toString();
			Byte index = indexMap.get(bitKey);
			if (index == null) {
				Expr newe = mergeChoice(selected);
				String eqKey = newe.toString();
				if (indexMap.containsKey(eqKey)) {
					charMap[ch] = indexMap.get(eqKey);
				} else {
					indexed.add(newe);
					charMap[ch] = (byte) indexed.size();
					indexMap.put(bitKey, charMap[ch]);
					indexMap.put(eqKey, charMap[ch]);
				}
			} else {
				charMap[ch] = index;
			}
		}
		for (int i = 0; i < indexed.size(); i++) {
			Expr pe = indexed.get(i);
			if (car(pe).isAny()) {
				Expr pe2 = cdr(pe);
				if (pe2.ptag == PTag.Or) {
					Expr pe3 = optimizeChoice(pe2);
					System.err.println("rec* =>\t" + pe2 + "\n\t" + pe3);
					indexed.set(i, PEG.Any_.andThen(pe3));
				}
			}
		}
		if (indexed.size() == 1) {
			Expr pe = indexed.get(0);
			BitChar bc = charMapSet(charMap, 1);
			if (car(pe).isAny()) {
				return new Char(bc).andThen(cdr(pe));
			}
			return new PEG.And(new Char(bc)).andThen(pe);
		}
		return new DFA(charMap, indexed.toArray(new Expr[indexed.size()]));
	}

	private static BitChar charMapSet(byte[] charMap, int index) {
		BitChar bc = new BitChar();
		for (int i = 0; i < 256; i++) {
			if (charMap[i] == index) {
				bc.set2(i, true);
			}
		}
		return bc;
	}

	static Expr mergeChoice(List<Expr> choice) {
		if (choice.size() == 0) {
			return PEG.Fail_;
		}
		if (choice.size() == 1) {
			Expr pe = choice.get(0);
			Expr d = dc(pe, 0);
			return (d == null) ? pe : PEG.Any_.andThen(d);
		}
		List<Expr> l = new ArrayList<>(choice.size());
		for (int i = 0; i < choice.size(); i++) {
			Expr pe = choice.get(i);
			Expr d = dc(pe, 0);
			appendChoice(l, (d == null) ? pe : PEG.Any_.andThen(d), true);
		}
		return seq2or(0, l);
	}

	static Expr seq2or(int offset, List<Expr> choice) {
		if (choice.size() == offset) {
			return PEG.Fail_;
		}
		if (offset + 1 == choice.size()) {
			return choice.get(offset);
		}
		return choice.get(offset).orElse(seq2or(offset + 1, choice));
	}

	static Expr dc(Expr pe, int depth) {
		switch (pe.ptag) {
		case Char:
			return PEG.Empty_;
		case Empty:
		case Tag:
		case Val:
		case If:
		case Exists:
		case Eval:
		case Not:
		case Many:
		case And:
		case Tree:
		case Link:
		case Fold:
		case Untree:
		case On:
		case Off:
		case Scope:
		case Symbol:
		case Match:
		case Contains:
		case Equals:
			return null;
		case OneMore: {
			Expr p = pe.get(0);
			return p.andThen(new PEG.Many(p));
		}
		case Seq: {
			Expr p = dc(pe.get(0), depth);
			if (p != null) {
				return p.isEmpty() ? pe.get(1) : p.andThen(pe.get(1));
			}
			return null;
		}
		case Alt: {
			Expr p = dc(pe.get(0), depth);
			if (p != null) {
				Expr p2 = dc(pe.get(1), depth);
				if (p2 != null) {
					return p.orAlso(p2);
				}
			}
			return null;
		}
		case Or: {
			Expr p = dc(pe.get(0), depth);
			if (p != null) {
				Expr p2 = dc(pe.get(1), depth);
				if (p2 != null) {
					return p.orElse(p2);
				}
			}
			return null;
		}
		case NonTerm:
			if (depth == 0) {
				return dc(pe.get(0), depth + 1);
			}
			return null;
		default:
			System.err.println("TODO: dc " + pe);
			break;
		}
		return null;
	}

}
