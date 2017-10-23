package blue.origami.parser.peg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import blue.origami.common.OFactory;
import blue.origami.common.OOption;

public class LeftRecursionEliminator extends ExpressionVisitor<Boolean, LeftRecursionEliminator.LREContext>
		implements OFactory<LeftRecursionEliminator> {

	public class LREContext {
		// if reach to expression named lookingNTName, return True
		public String lookingNTName;
		// if leftCornerFlag is true, the most left search(e.g. only choice[0] or only
		// first of pair)
		public Boolean leftCornerFlag;
		// if newNTName is not null, renamed non terminal from lookingNTName to
		// newNTName
		public String newNTName;

		LREContext(String name, boolean flag) {
			this.lookingNTName = name;
			this.leftCornerFlag = Boolean.valueOf(flag);
			this.newNTName = null;
		}

		LREContext(String name, boolean flag, String newNTName) {
			this.lookingNTName = name;
			this.leftCornerFlag = Boolean.valueOf(flag);
			this.newNTName = newNTName;
		}
	}

	private Grammar grammar;

	// ntSet and ntQue is queue of non-terminal to process
	private Set<String> ntSet;
	private Deque<String> ntQue;

	private final boolean isDebug = false;

	public void compute(Grammar g) {
		this.grammar = g;
		this.ntSet = new HashSet<>();
		this.ntQue = new ArrayDeque<>();

		String root = g.getStartProduction().getLocalName();
		this.addProcessQueue(root);
		while (!this.ntQue.isEmpty()) {
			String name = this.ntQue.pollFirst();
			// temporarily throw NullPointerException if production named "name" is
			// undefined
			Expression e = this.grammar.getProduction(name).getExpression();

			Boolean isLR = e.visit(this, new LREContext(name, true));
			if (isLR) {
				String newNTName = this.eliminateLR(name);
				this.addProcessQueue(newNTName);
			}
		}
	}

	private String eliminateLR(String name) {
		Expression expr = this.grammar.getProduction(name).getExpression();
		if (expr instanceof PChoice) {
			String newName = name + "a";
			PChoice choice = (PChoice) expr;

			// divide into recursive exprs and not-recursive exprs from choice
			Map<Boolean, List<Expression>> exprMap = Arrays.stream(choice.inners)
					.collect(Collectors.partitioningBy(e -> e.visit(this, new LREContext(name, false))));
			List<Expression> recursiveExprs = exprMap.get(Boolean.TRUE);
			List<Expression> notRecursiveExprs = exprMap.get(Boolean.FALSE);

			// covert left recursive in first recursive-choice ( and divide into first-expr
			// and
			// sub-exprs )
			Expression firstRecursive = recursiveExprs.get(0);
			List<Expression> subRecursiveExprs = recursiveExprs.subList(1, recursiveExprs.size());
			// If true replaced in only left-recursive-nonterminal
			// If false replaced all non-terminal
			// firstRecursive.visit(this, new LREContext(name, true, newName));
			firstRecursive.visit(this, new LREContext(name, false, newName));
			subRecursiveExprs.forEach(e -> e.visit(this, new LREContext(name, false, newName)));

			// redefine original expression ( converted first-recursive and not-recursives )
			List<Expression> newOrgChoices = new ArrayList<>();
			newOrgChoices.add(firstRecursive);
			newOrgChoices.addAll(notRecursiveExprs);
			PChoice newOrgExpr = new PChoice(false, newOrgChoices.toArray(new Expression[newOrgChoices.size()]));
			this.grammar.setExpression(name, newOrgExpr);

			// define new expression
			List<Expression> newSubChoices = new ArrayList<>();
			newSubChoices.addAll(subRecursiveExprs);
			newSubChoices.addAll(notRecursiveExprs);
			PChoice newSubExpr = new PChoice(false, newSubChoices.toArray(new Expression[newSubChoices.size()]));
			this.grammar.setExpression(newName, newSubExpr);

			/*
			 * Expression c = choice.get(0); Expression[] cs =
			 * Arrays.copyOfRange(choice.inners, 1, choice.size());
			 * 
			 * this.grammar.setExpression(name, c); if (cs.length == 1) {
			 * this.grammar.setExpression(newName, cs[0]); } else {
			 * this.grammar.setExpression(newName, new PChoice(false, cs)); }
			 * 
			 * LREContext context = new LREContext(name, false, newName); Expression orgExpr
			 * = this.grammar.getExpression(name); Expression newExpr =
			 * this.grammar.getExpression(newName); orgExpr.visit(this, context);
			 * newExpr.visit(this, context);
			 */
			return newName;
		}
		return null;
	}

	private Expression updateExpression(Expression e, LREContext context) {
		if (e instanceof PNonTerminal) {
			String name = ((PNonTerminal) e).getLocalName();
			if (context.newNTName != null && context.lookingNTName.equals(name)) {
				return new PNonTerminal(this.grammar, context.newNTName);
			}
		}
		return e;
	}

	private void addProcessQueue(String name) {
		if (name != null && name != "" && !this.ntSet.contains(name)) {
			this.ntSet.add(name);
			this.ntQue.addLast(name);
		}
	}

	private void debug(String str) {
		if (this.isDebug) {
			System.out.println(str);
		}
	}

	@Override
	public LeftRecursionEliminator clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(OOption options) {
		// TODO Auto-generated method stub

	}

	@Override
	public Boolean visitTrap(PTrap e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitTrap : " + e.toString());
		return null;
	}

	@Override
	public Boolean visitNonTerminal(PNonTerminal e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitNonTerminal : " + e.getUniqueName());

		String name = e.getLocalName();
		final String lookingName = context.lookingNTName;
		if (name.equals(lookingName)) {
			return true;
		} else if (!this.ntSet.contains(name)) {
			this.addProcessQueue(name);
		}
		return false;
	}

	@Override
	public Boolean visitEmpty(PEmpty e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitEmpty : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitFail(PFail e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitFail : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitByte(PByte e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitByte : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitByteSet(PByteSet e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitByteSet : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitAny(PAny e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitAny : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitPair(PPair e, LREContext context) {
		this.debug("visitPair : " + e.left.toString() + "," + e.right.toString());

		if (context.leftCornerFlag) {
			Boolean l = e.left.visit(this, context);
			e.left = this.updateExpression(e.left, context);
			return l;
		} else {
			Boolean l = e.left.visit(this, context);
			Boolean r = e.right.visit(this, context);

			e.left = this.updateExpression(e.left, context);
			e.right = this.updateExpression(e.right, context);

			return l.equals(Boolean.TRUE) || r.equals(Boolean.TRUE);
		}
	}

	@Override
	public Boolean visitChoice(PChoice e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitChoice : " + e.toString());
		if (context.leftCornerFlag) {
			Boolean l = e.get(0).visit(this, context);
			e.inners[0] = this.updateExpression(e.inners[0], context);
			return l;
		} else {
			boolean res = false;
			for (Expression ce : e) {
				if (ce.visit(this, context)) {
					res = true;
				}
				ce = this.updateExpression(ce, context);
			}
			return res;
		}
	}

	@Override
	public Boolean visitDispatch(PDispatch e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitDispatch : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitOption(POption e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitOption : " + e.toString());
		return e.get(0).visit(this, context);
	}

	// many is contain only 1 expression, get to call e.get(0)
	@Override
	public Boolean visitMany(PMany e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitMany : " + e.get(0).toString() + "*");
		return e.get(0).visit(this, context);
	}

	@Override
	public Boolean visitAnd(PAnd e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitAnd : " + e.toString());
		return e.get(0).visit(this, context);
	}

	@Override
	public Boolean visitNot(PNot e, LREContext context) {
		// TODO Auto-generated method stub
		return e.get(0).visit(this, context);
	}

	@Override
	public Boolean visitTree(PTree e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitTree : " + e.toString());
		Boolean res = e.get(0).visit(this, context);
		e.set(0, this.updateExpression(e.get(0), context));
		return res;
	}

	@Override
	public Boolean visitDetree(PDetree e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitDetree : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitLinkTree(PLinkTree e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitLinkTree : " + e.toString());
		Boolean res = e.get(0).visit(this, context);
		e.set(0, this.updateExpression(e.get(0), context));
		return res;
	}

	@Override
	public Boolean visitTag(PTag e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitTag : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitValue(PValue e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitValue : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitSymbolScope(PSymbolScope e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitSymbolScope : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitSymbolAction(PSymbolAction e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitSymbolAction : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitSymbolPredicate(PSymbolPredicate e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitSymbolPredicate : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitIf(PIf e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitIf : " + e.toString());
		return false;
	}

	@Override
	public Boolean visitOn(POn e, LREContext context) {
		// TODO Auto-generated method stub
		this.debug("visitOn : " + e.toString());
		return false;
	}

	@Override
	public Class<?> keyClass() {
		// TODO Auto-generated method stub
		return null;
	}
}
